package ru.yandex.practicum.telemetry.analyzer.service;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.entity.*;
import ru.yandex.practicum.telemetry.analyzer.repository.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScenarioAnalyzerService {

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    private final ScenarioRepository scenarioRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;

    public void analyzeSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId().toString();
        log.debug("Analyzing snapshot for hub: {}", hubId);

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);

        for (Scenario scenario : scenarios) {
            if (checkScenarioConditions(scenario, snapshot)) {
                executeScenarioActions(scenario, snapshot);
            }
        }
    }

    private boolean checkScenarioConditions(Scenario scenario, SensorsSnapshotAvro snapshot) {
        List<ScenarioCondition> conditions = scenarioConditionRepository.findByScenarioId(scenario.getId());

        if (conditions.isEmpty()) {
            return false;
        }

        Map<CharSequence, SensorStateAvro> sensorsState = snapshot.getSensorsState();

        // Все условия должны быть выполнены
        for (ScenarioCondition scenarioCondition : conditions) {
            SensorStateAvro sensorState = sensorsState.get(scenarioCondition.getSensorId());

            if (sensorState == null) {
                log.debug("Sensor {} not found in snapshot", scenarioCondition.getSensorId());
                return false;
            }

            if (!checkCondition(scenarioCondition.getCondition(), sensorState)) {
                return false;
            }
        }

        log.info("All conditions met for scenario: {}", scenario.getName());
        return true;
    }

    private boolean checkCondition(Condition condition, SensorStateAvro sensorState) {
        Object sensorData = sensorState.getData();
        String conditionType = condition.getType();
        String operation = condition.getOperation();
        Integer conditionValue = condition.getValue();

        Integer actualValue = extractValue(sensorData, conditionType);

        if (actualValue == null) {
            return false;
        }

        return switch (operation) {
            case "EQUALS" -> actualValue.equals(conditionValue);
            case "GREATER_THAN" -> actualValue > conditionValue;
            case "LOWER_THAN" -> actualValue < conditionValue;
            default -> false;
        };
    }

    private Integer extractValue(Object sensorData, String conditionType) {
        return switch (conditionType) {
            case "MOTION" -> {
                if (sensorData instanceof MotionSensorAvro motion) {
                    yield motion.getMotion() ? 1 : 0;
                }
                yield null;
            }
            case "LUMINOSITY" -> {
                if (sensorData instanceof LightSensorAvro light) {
                    yield light.getLuminosity();
                }
                yield null;
            }
            case "SWITCH" -> {
                if (sensorData instanceof SwitchSensorAvro switchSensor) {
                    yield switchSensor.getState() ? 1 : 0;
                }
                yield null;
            }
            case "TEMPERATURE" -> {
                if (sensorData instanceof TemperatureSensorAvro temp) {
                    yield temp.getTemperatureC();
                } else if (sensorData instanceof ClimateSensorAvro climate) {
                    yield climate.getTemperatureC();
                }
                yield null;
            }
            case "CO2LEVEL" -> {
                if (sensorData instanceof ClimateSensorAvro climate) {
                    yield climate.getCo2Level();
                }
                yield null;
            }
            case "HUMIDITY" -> {
                if (sensorData instanceof ClimateSensorAvro climate) {
                    yield climate.getHumidity();
                }
                yield null;
            }
            default -> null;
        };
    }

    private void executeScenarioActions(Scenario scenario, SensorsSnapshotAvro snapshot) {
        List<ScenarioAction> actions = scenarioActionRepository.findByScenarioId(scenario.getId());

        for (ScenarioAction scenarioAction : actions) {
            sendActionToHub(scenario, scenarioAction, snapshot);
        }
    }

    private void sendActionToHub(Scenario scenario, ScenarioAction scenarioAction,
                                 SensorsSnapshotAvro snapshot) {
        Action action = scenarioAction.getAction();

        DeviceActionProto deviceAction = DeviceActionProto.newBuilder()
                .setSensorId(scenarioAction.getSensorId())
                .setType(ActionTypeProto.valueOf(action.getType()))
                .setValue(action.getValue() != null ? action.getValue() : 0)
                .build();

        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();

        DeviceActionRequest request = DeviceActionRequest.newBuilder()
                .setHubId(scenario.getHubId())
                .setScenarioName(scenario.getName())
                .setAction(deviceAction)
                .setTimestamp(timestamp)
                .build();

        try {
            hubRouterClient.handleDeviceAction(request);
            log.info("Action sent to hub: scenario={}, sensor={}, action={}",
                    scenario.getName(), scenarioAction.getSensorId(), action.getType());
        } catch (Exception e) {
            log.error("Error sending action to hub", e);
        }
    }
}