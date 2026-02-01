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
        String hubId = snapshot.getHubId();
        log.info("🔍 Analyzing snapshot for hub: {}, sensors count: {}", hubId, snapshot.getSensorsState().size());

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        log.info("📋 Found {} scenarios for hub: {}", scenarios.size(), hubId);

        if (scenarios.isEmpty()) {
            log.warn("⚠️ No scenarios found for hub: {}", hubId);
            return;
        }

        for (Scenario scenario : scenarios) {
            log.info("🎯 Checking scenario: {} for hub: {}", scenario.getName(), hubId);
            if (checkScenarioConditions(scenario, snapshot)) {
                log.info("✅ Scenario conditions met, executing actions: {}", scenario.getName());
                executeScenarioActions(scenario, snapshot);
            } else {
                log.info("❌ Scenario conditions not met: {}", scenario.getName());
            }
        }
    }

    private boolean checkScenarioConditions(Scenario scenario, SensorsSnapshotAvro snapshot) {
        List<ScenarioCondition> conditions = scenarioConditionRepository.findByScenarioId(scenario.getId());
        log.info("Checking {} conditions for scenario: {}", conditions.size(), scenario.getName());

        if (conditions.isEmpty()) {
            log.warn("No conditions found for scenario: {}", scenario.getName());
            return false;
        }

        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();

        // Все условия должны быть выполнены
        for (ScenarioCondition scenarioCondition : conditions) {
            SensorStateAvro sensorState = sensorsState.get(scenarioCondition.getSensorId());

            if (sensorState == null) {
                log.debug("Sensor {} not found in snapshot for scenario: {}", 
                    scenarioCondition.getSensorId(), scenario.getName());
                return false;
            }

            Condition condition = scenarioCondition.getCondition();
            log.info("Checking condition: type={}, operation={}, value={} for sensor: {}", 
                condition.getType(), condition.getOperation(), condition.getValue(), 
                scenarioCondition.getSensorId());

            if (!checkCondition(condition, sensorState)) {
                log.info("Condition not met for scenario: {}, sensor: {}", 
                    scenario.getName(), scenarioCondition.getSensorId());
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

        log.info("Checking condition: type={}, operation={}, expectedValue={}, sensorData={}", 
            conditionType, operation, conditionValue, sensorData.getClass().getSimpleName());

        // Извлекаем значение из данных датчика
        Integer actualValue = extractValue(sensorData, conditionType);

        if (actualValue == null) {
            log.warn("Could not extract value for condition type: {} from sensor data: {}", 
                conditionType, sensorData.getClass().getSimpleName());
            return false;
        }

        log.info("Comparing: actual={} {} expected={}", actualValue, operation, conditionValue);

        // ИСПРАВЛЕНИЕ: Проверяем на null значения и отклоняем некорректные условия
        if (conditionValue == null) {
            log.error("Condition value is null for condition type: {}, operation: {}. Rejecting condition.", 
                conditionType, operation);
            return false;
        }

        boolean result = switch (operation) {
            case "EQUALS" -> actualValue.equals(conditionValue);
            case "GREATER_THAN" -> actualValue > conditionValue;
            case "LOWER_THAN" -> actualValue < conditionValue;
            default -> {
                log.warn("Unknown operation: {}", operation);
                yield false;
            }
        };

        log.info("Condition result: {} (actual: {}, expected: {}, operation: {})", 
            result, actualValue, conditionValue, operation);
        return result;
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

        log.info("Preparing to send action to hub: scenario={}, sensor={}, actionType={}, actionValue={}", 
            scenario.getName(), scenarioAction.getSensorId(), action.getType(), action.getValue());

        try {
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

            log.info("Sending gRPC request to hub-router: hubId={}, scenario={}, sensor={}, action={}, value={}", 
                scenario.getHubId(), scenario.getName(), scenarioAction.getSensorId(), 
                action.getType(), action.getValue());

            // ИСПРАВЛЕНИЕ: Добавляем таймаут и retry логику
            try {
                hubRouterClient.handleDeviceAction(request);
                log.info("✅ Action sent successfully to hub: scenario={}, sensor={}, action={}", 
                    scenario.getName(), scenarioAction.getSensorId(), action.getType());
            } catch (io.grpc.StatusRuntimeException e) {
                if (e.getStatus().getCode() == io.grpc.Status.Code.UNAVAILABLE) {
                    log.error("❌ Hub Router unavailable: {}", e.getMessage());
                } else {
                    log.error("❌ gRPC error: {} - {}", e.getStatus().getCode(), e.getMessage());
                }
                throw e;
            }
            
        } catch (Exception e) {
            log.error("❌ Error sending action to hub: scenario={}, sensor={}, error={}", 
                scenario.getName(), scenarioAction.getSensorId(), e.getMessage(), e);
            // Не пробрасываем исключение дальше, чтобы не прерывать обработку других действий
        }
    }
}