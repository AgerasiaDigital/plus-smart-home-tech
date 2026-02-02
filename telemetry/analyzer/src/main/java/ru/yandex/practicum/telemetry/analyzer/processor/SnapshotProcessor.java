package ru.yandex.practicum.telemetry.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.entity.*;
import ru.yandex.practicum.telemetry.analyzer.enums.ConditionOperation;
import ru.yandex.practicum.telemetry.analyzer.enums.ConditionType;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.service.ActionExecutor;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    private final KafkaConsumer<String, SensorsSnapshotAvro> consumer;
    private final String snapshotsTopic;
    private final ScenarioRepository scenarioRepository;
    private final ActionExecutor actionExecutor;

    public void start() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            consumer.subscribe(List.of(snapshotsTopic));
            log.info("SnapshotProcessor subscribed to topic: {}", snapshotsTopic);

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    log.info("Received snapshot: hubId={}, timestamp={}",
                            record.value().getHubId(), record.value().getTimestamp());
                    processSnapshot(record.value());
                }

                consumer.commitSync();
            }

        } catch (WakeupException ignored) {
            log.info("SnapshotProcessor wakeup");
        } catch (Exception e) {
            log.error("Error in SnapshotProcessor", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                log.info("Closing SnapshotProcessor consumer");
                consumer.close();
            }
        }
    }

    private void processSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();

        log.info("Processing snapshot for hub: {}, sensors count: {}", hubId, sensorsState.size());

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        log.info("Found {} scenarios for hub: {}", scenarios.size(), hubId);

        for (Scenario scenario : scenarios) {
            log.debug("Checking scenario: name={}, conditions={}, actions={}",
                    scenario.getName(), scenario.getConditions().size(), scenario.getActions().size());

            boolean conditionsMet = checkScenarioConditions(scenario, sensorsState);
            log.info("Scenario '{}' conditions met: {}", scenario.getName(), conditionsMet);

            if (conditionsMet) {
                executeScenarioActions(scenario);
            }
        }
    }

    private boolean checkScenarioConditions(Scenario scenario, Map<String, SensorStateAvro> sensorsState) {
        if (scenario.getConditions().isEmpty()) {
            log.warn("Scenario '{}' has no conditions", scenario.getName());
            return false;
        }

        for (ScenarioCondition scenarioCondition : scenario.getConditions()) {
            boolean conditionMet = checkCondition(scenarioCondition, sensorsState);
            log.debug("Condition check: sensor={}, type={}, met={}",
                    scenarioCondition.getSensor().getId(),
                    scenarioCondition.getCondition().getType(),
                    conditionMet);

            if (!conditionMet) {
                return false;
            }
        }

        return true;
    }

    private boolean checkCondition(ScenarioCondition scenarioCondition, Map<String, SensorStateAvro> sensorsState) {
        String sensorId = scenarioCondition.getSensor().getId();
        SensorStateAvro state = sensorsState.get(sensorId);

        if (state == null) {
            log.debug("No state found for sensor: {}", sensorId);
            return false;
        }

        Condition condition = scenarioCondition.getCondition();
        Object data = state.getData();

        Integer actualValue = extractValue(data, condition.getType());
        if (actualValue == null) {
            log.debug("Could not extract value from data: type={}, data={}",
                    condition.getType(), data.getClass().getSimpleName());
            return false;
        }

        boolean result = evaluateCondition(actualValue, condition.getOperation(), condition.getValue());
        log.debug("Evaluated condition: actual={}, operation={}, expected={}, result={}",
                actualValue, condition.getOperation(), condition.getValue(), result);

        return result;
    }

    private Integer extractValue(Object data, ConditionType type) {
        return switch (type) {
            case TEMPERATURE -> {
                if (data instanceof TemperatureSensorAvro temp) {
                    yield temp.getTemperatureC();
                } else if (data instanceof ClimateSensorAvro climate) {
                    yield climate.getTemperatureC();
                }
                yield null;
            }
            case HUMIDITY -> data instanceof ClimateSensorAvro climate ? climate.getHumidity() : null;
            case CO2LEVEL -> data instanceof ClimateSensorAvro climate ? climate.getCo2Level() : null;
            case LUMINOSITY -> data instanceof LightSensorAvro light ? light.getLuminosity() : null;
            case MOTION -> data instanceof MotionSensorAvro motion ? (motion.getMotion() ? 1 : 0) : null;
            case SWITCH -> data instanceof SwitchSensorAvro switchSensor ? (switchSensor.getState() ? 1 : 0) : null;
        };
    }

    private boolean evaluateCondition(Integer actualValue, ConditionOperation operation, Integer expectedValue) {
        if (expectedValue == null) {
            log.debug("Expected value is null, condition passes");
            return true;
        }

        return switch (operation) {
            case EQUALS -> actualValue.equals(expectedValue);
            case GREATER_THAN -> actualValue > expectedValue;
            case LOWER_THAN -> actualValue < expectedValue;
        };
    }

    private void executeScenarioActions(Scenario scenario) {
        log.info("Executing scenario: name={}, hubId={}, actions count={}",
                scenario.getName(), scenario.getHubId(), scenario.getActions().size());

        scenario.getActions().forEach(scenarioAction -> {
            log.info("Executing action: sensor={}, type={}",
                    scenarioAction.getSensor().getId(),
                    scenarioAction.getAction().getType());
            actionExecutor.executeAction(scenario.getHubId(), scenario.getName(), scenarioAction);
        });
    }
}