package ru.yandex.practicum.telemetry.analyzer.service;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.hubrouter.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.model.*;
import ru.yandex.practicum.telemetry.analyzer.repository.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    @Transactional
    public void addDevice(String hubId, String sensorId, DeviceTypeAvro deviceType) {
        Sensor sensor = sensorRepository.findById(sensorId).orElse(new Sensor());
        sensor.setId(sensorId);
        sensor.setHubId(hubId);
        sensorRepository.save(sensor);
        log.info("Device added: hubId={}, sensorId={}, type={}", hubId, sensorId, deviceType);
    }

    @Transactional
    public void removeDevice(String sensorId) {
        sensorRepository.deleteById(sensorId);
        log.info("Device removed: sensorId={}", sensorId);
    }

    @Transactional
    public void addScenario(String hubId, ScenarioAddedEventAvro event) {
        Scenario scenario = scenarioRepository.findByHubIdAndName(hubId, event.getName())
                .orElse(new Scenario());
        scenario.setHubId(hubId);
        scenario.setName(event.getName());

        scenario.getConditions().clear();
        scenario.getActions().clear();

        for (ScenarioConditionAvro conditionAvro : event.getConditions()) {
            Sensor sensor = sensorRepository.findById(conditionAvro.getSensorId())
                    .orElseThrow(() -> new IllegalArgumentException("Sensor not found: " + conditionAvro.getSensorId()));

            Condition condition = new Condition();
            condition.setType(ConditionType.valueOf(conditionAvro.getType().name()));
            condition.setOperation(ConditionOperation.valueOf(conditionAvro.getOperation().name()));

            Object value = conditionAvro.getValue();
            if (value instanceof Boolean) {
                condition.setValue(((Boolean) value) ? 1 : 0);
            } else if (value instanceof Integer) {
                condition.setValue((Integer) value);
            }

            conditionRepository.save(condition);

            ScenarioCondition scenarioCondition = new ScenarioCondition();
            scenarioCondition.setScenario(scenario);
            scenarioCondition.setSensor(sensor);
            scenarioCondition.setCondition(condition);
            scenario.getConditions().add(scenarioCondition);
        }

        for (DeviceActionAvro actionAvro : event.getActions()) {
            Sensor sensor = sensorRepository.findById(actionAvro.getSensorId())
                    .orElseThrow(() -> new IllegalArgumentException("Sensor not found: " + actionAvro.getSensorId()));

            Action action = new Action();
            action.setType(ActionType.valueOf(actionAvro.getType().name()));
            action.setValue((Integer) actionAvro.getValue());
            actionRepository.save(action);

            ScenarioAction scenarioAction = new ScenarioAction();
            scenarioAction.setScenario(scenario);
            scenarioAction.setSensor(sensor);
            scenarioAction.setAction(action);
            scenario.getActions().add(scenarioAction);
        }

        scenarioRepository.save(scenario);
        log.info("Scenario added: hubId={}, name={}", hubId, event.getName());
    }

    @Transactional
    public void removeScenario(String hubId, String scenarioName) {
        scenarioRepository.findByHubIdAndName(hubId, scenarioName)
                .ifPresent(scenario -> {
                    scenarioRepository.delete(scenario);
                    log.info("Scenario removed: hubId={}, name={}", hubId, scenarioName);
                });
    }

    @Transactional(readOnly = true)
    public void processSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);

        log.debug("Processing snapshot for hubId={}, scenarios count={}", hubId, scenarios.size());

        scenarios.stream()
                .filter(scenario -> checkScenarioConditions(scenario, snapshot))
                .forEach(scenario -> executeScenarioActions(scenario, snapshot));
    }

    private boolean checkScenarioConditions(Scenario scenario, SensorsSnapshotAvro snapshot) {
        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();

        return scenario.getConditions().stream()
                .allMatch(sc -> {
                    String sensorId = sc.getSensor().getId();
                    SensorStateAvro state = sensorsState.get(sensorId);

                    if (state == null) {
                        log.debug("Sensor state not found: sensorId={}", sensorId);
                        return false;
                    }

                    return checkCondition(sc.getCondition(), state.getData());
                });
    }

    private boolean checkCondition(Condition condition, Object sensorData) {
        Integer actualValue = extractValue(condition.getType(), sensorData);
        Integer expectedValue = condition.getValue();

        if (actualValue == null) {
            return false;
        }

        return switch (condition.getOperation()) {
            case EQUALS -> actualValue.equals(expectedValue);
            case GREATER_THAN -> actualValue > expectedValue;
            case LOWER_THAN -> actualValue < expectedValue;
        };
    }

    private Integer extractValue(ConditionType type, Object data) {
        return switch (type) {
            case TEMPERATURE -> {
                if (data instanceof TemperatureSensorAvro) {
                    yield ((TemperatureSensorAvro) data).getTemperatureC();
                } else if (data instanceof ClimateSensorAvro) {
                    yield ((ClimateSensorAvro) data).getTemperatureC();
                }
                yield null;
            }
            case HUMIDITY -> data instanceof ClimateSensorAvro ?
                    ((ClimateSensorAvro) data).getHumidity() : null;
            case CO2LEVEL -> data instanceof ClimateSensorAvro ?
                    ((ClimateSensorAvro) data).getCo2Level() : null;
            case LUMINOSITY -> data instanceof LightSensorAvro ?
                    ((LightSensorAvro) data).getLuminosity() : null;
            case MOTION -> data instanceof MotionSensorAvro ?
                    (((MotionSensorAvro) data).getMotion() ? 1 : 0) : null;
            case SWITCH -> data instanceof SwitchSensorAvro ?
                    (((SwitchSensorAvro) data).getState() ? 1 : 0) : null;
        };
    }

    private void executeScenarioActions(Scenario scenario, SensorsSnapshotAvro snapshot) {
        log.info("Executing scenario: hubId={}, scenario={}", scenario.getHubId(), scenario.getName());

        List<DeviceActionProto> actions = scenario.getActions().stream()
                .map(this::toDeviceActionProto)
                .collect(Collectors.toList());

        if (actions.isEmpty()) {
            return;
        }

        for (DeviceActionProto action : actions) {
            try {
                Instant timestamp = snapshot.getTimestamp();
                DeviceActionRequest request = DeviceActionRequest.newBuilder()
                        .setHubId(scenario.getHubId())
                        .setScenarioName(scenario.getName())
                        .setAction(action)
                        .setTimestamp(Timestamp.newBuilder()
                                .setSeconds(timestamp.getEpochSecond())
                                .setNanos(timestamp.getNano())
                                .build())
                        .build();

                hubRouterClient.handleDeviceAction(request);
                log.info("Action executed: hubId={}, scenario={}, sensor={}",
                        scenario.getHubId(), scenario.getName(), action.getSensorId());
            } catch (Exception e) {
                log.error("Error executing action for scenario: {}", scenario.getName(), e);
            }
        }
    }

    private DeviceActionProto toDeviceActionProto(ScenarioAction scenarioAction) {
        DeviceActionProto.Builder builder = DeviceActionProto.newBuilder()
                .setSensorId(scenarioAction.getSensor().getId())
                .setType(ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto.valueOf(
                        scenarioAction.getAction().getType().name()));

        if (scenarioAction.getAction().getValue() != null) {
            builder.setValue(scenarioAction.getAction().getValue());
        }

        return builder.build();
    }
}