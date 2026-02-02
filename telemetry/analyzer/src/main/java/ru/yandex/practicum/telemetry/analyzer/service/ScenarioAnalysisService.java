package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.analyzer.entity.Scenario;
import ru.yandex.practicum.telemetry.analyzer.entity.ScenarioAction;
import ru.yandex.practicum.telemetry.analyzer.entity.ScenarioCondition;
import ru.yandex.practicum.telemetry.analyzer.model.ConditionOperation;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScenarioAnalysisService {

    private final ScenarioRepository scenarioRepository;

    public List<DeviceActionProto> analyzeSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        log.info("Analyzing snapshot for hub: {}", hubId);

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        log.info("Found {} scenarios for hub: {}", scenarios.size(), hubId);
        
        Map<String, SensorStateAvro> sensorStates = snapshot.getSensorsState();
        log.info("Snapshot contains {} sensor states", sensorStates.size());

        List<DeviceActionProto> actions = scenarios.stream()
                .filter(scenario -> {
                    boolean matches = evaluateScenario(scenario, sensorStates);
                    log.info("Scenario '{}' evaluation result: {}", scenario.getName(), matches);
                    return matches;
                })
                .flatMap(scenario -> {
                    log.info("Processing actions for scenario: {}", scenario.getName());
                    return scenario.getActions().stream();
                })
                .map(this::convertToDeviceAction)
                .collect(Collectors.toList());
                
        log.info("Generated {} actions for hub: {}", actions.size(), hubId);
        return actions;
    }

    private boolean evaluateScenario(Scenario scenario, Map<String, SensorStateAvro> sensorStates) {
        log.info("Evaluating scenario '{}' with {} conditions", scenario.getName(), scenario.getConditions().size());
        
        boolean result = scenario.getConditions().stream()
                .allMatch(condition -> {
                    boolean conditionResult = evaluateCondition(condition, sensorStates);
                    log.info("Condition for sensor {} evaluated to: {}", condition.getSensor().getId(), conditionResult);
                    return conditionResult;
                });
                
        log.info("Scenario '{}' overall result: {}", scenario.getName(), result);
        return result;
    }

    private boolean evaluateCondition(ScenarioCondition scenarioCondition, Map<String, SensorStateAvro> sensorStates) {
        String sensorId = scenarioCondition.getSensor().getId();
        SensorStateAvro sensorState = sensorStates.get(sensorId);
        
        if (sensorState == null) {
            log.warn("Sensor state not found for sensor: {}", sensorId);
            return false;
        }

        return createConditionPredicate(scenarioCondition).test(sensorState);
    }

    private Predicate<SensorStateAvro> createConditionPredicate(ScenarioCondition scenarioCondition) {
        String operation = scenarioCondition.getCondition().getOperation();
        Integer expectedValue = scenarioCondition.getCondition().getValue();
        String conditionType = scenarioCondition.getCondition().getType();

        return sensorState -> {
            Integer actualValue = extractSensorValue(sensorState, conditionType);
            if (actualValue == null) {
                return false;
            }

            ConditionOperation op = ConditionOperation.valueOf(operation);
            return switch (op) {
                case EQUALS -> actualValue.equals(expectedValue);
                case GREATER_THAN -> actualValue > expectedValue;
                case LOWER_THAN -> actualValue < expectedValue;
            };
        };
    }

    private Integer extractSensorValue(SensorStateAvro sensorState, String conditionType) {
        Object data = sensorState.getData();
        
        return switch (conditionType) {
            case "TEMPERATURE" -> {
                if (data instanceof ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro temp) {
                    yield temp.getTemperatureC();
                } else if (data instanceof ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro climate) {
                    yield climate.getTemperatureC();
                }
                yield null;
            }
            case "HUMIDITY" -> {
                if (data instanceof ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro climate) {
                    yield climate.getHumidity();
                }
                yield null;
            }
            case "CO2LEVEL" -> {
                if (data instanceof ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro climate) {
                    yield climate.getCo2Level();
                }
                yield null;
            }
            case "LUMINOSITY" -> {
                if (data instanceof ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro light) {
                    yield light.getLuminosity();
                }
                yield null;
            }
            case "MOTION" -> {
                if (data instanceof ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro motion) {
                    yield motion.getMotion() ? 1 : 0;
                }
                yield null;
            }
            case "SWITCH" -> {
                if (data instanceof ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro switchSensor) {
                    yield switchSensor.getState() ? 1 : 0;
                }
                yield null;
            }
            default -> {
                log.warn("Unknown condition type: {}", conditionType);
                yield null;
            }
        };
    }

    private DeviceActionProto convertToDeviceAction(ScenarioAction scenarioAction) {
        return DeviceActionProto.newBuilder()
                .setSensorId(scenarioAction.getSensor().getId())
                .setType(ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto.valueOf(
                        scenarioAction.getAction().getType()))
                .setValue(scenarioAction.getAction().getValue())
                .build();
    }
}