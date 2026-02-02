package ru.yandex.practicum.telemetry.analyzer.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.entity.*;
import ru.yandex.practicum.telemetry.analyzer.enums.ActionType;
import ru.yandex.practicum.telemetry.analyzer.enums.ConditionOperation;
import ru.yandex.practicum.telemetry.analyzer.enums.ConditionType;
import ru.yandex.practicum.telemetry.analyzer.repository.ActionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.SensorRepository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class HubEventProcessor implements Runnable {

    private final KafkaConsumer<String, HubEventAvro> consumer;
    private final String hubsTopic;
    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final TransactionTemplate transactionTemplate;

    public HubEventProcessor(
            KafkaConsumer<String, HubEventAvro> consumer,
            String hubsTopic,
            SensorRepository sensorRepository,
            ScenarioRepository scenarioRepository,
            ConditionRepository conditionRepository,
            ActionRepository actionRepository,
            TransactionTemplate transactionTemplate) {
        this.consumer = consumer;
        this.hubsTopic = hubsTopic;
        this.sensorRepository = sensorRepository;
        this.scenarioRepository = scenarioRepository;
        this.conditionRepository = conditionRepository;
        this.actionRepository = actionRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            consumer.subscribe(List.of(hubsTopic));
            log.info("HubEventProcessor subscribed to topic: {}", hubsTopic);

            while (true) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofSeconds(5));

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    log.info("Processing hub event: key={}, value={}", record.key(), record.value());
                    transactionTemplate.executeWithoutResult(status -> {
                        try {
                            processEvent(record.value());
                        } catch (Exception e) {
                            log.error("Error processing event", e);
                            status.setRollbackOnly();
                        }
                    });
                }

                consumer.commitSync();
            }

        } catch (WakeupException ignored) {
            log.info("HubEventProcessor wakeup");
        } catch (Exception e) {
            log.error("Error in HubEventProcessor", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                log.info("Closing HubEventProcessor consumer");
                consumer.close();
            }
        }
    }

    public void processEvent(HubEventAvro event) {
        String hubId = event.getHubId();
        Object payload = event.getPayload();

        log.info("Processing event for hub: {}, payload type: {}", hubId, payload.getClass().getSimpleName());

        if (payload instanceof DeviceAddedEventAvro deviceAdded) {
            handleDeviceAdded(hubId, deviceAdded);
        } else if (payload instanceof DeviceRemovedEventAvro deviceRemoved) {
            handleDeviceRemoved(hubId, deviceRemoved);
        } else if (payload instanceof ScenarioAddedEventAvro scenarioAdded) {
            handleScenarioAdded(hubId, scenarioAdded);
        } else if (payload instanceof ScenarioRemovedEventAvro scenarioRemoved) {
            handleScenarioRemoved(hubId, scenarioRemoved);
        }
    }

    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro event) {
        Optional<Sensor> existing = sensorRepository.findById(event.getId());
        if (existing.isPresent()) {
            log.debug("Sensor already exists: {}", event.getId());
            return;
        }

        Sensor sensor = new Sensor();
        sensor.setId(event.getId());
        sensor.setHubId(hubId);
        sensorRepository.save(sensor);
        log.info("Sensor added: id={}, hubId={}", event.getId(), hubId);
    }

    private void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro event) {
        sensorRepository.findByIdAndHubId(event.getId(), hubId)
                .ifPresent(sensor -> {
                    sensorRepository.delete(sensor);
                    log.info("Sensor removed: id={}, hubId={}", event.getId(), hubId);
                });
    }

    private void handleScenarioAdded(String hubId, ScenarioAddedEventAvro event) {
        log.info("Adding scenario: name={}, hubId={}", event.getName(), hubId);

        Optional<Scenario> existing = scenarioRepository.findByHubIdAndName(hubId, event.getName());

        Scenario scenario;
        if (existing.isPresent()) {
            scenario = existing.get();
            scenario.getConditions().clear();
            scenario.getActions().clear();
            log.debug("Updating existing scenario: {}", event.getName());
        } else {
            scenario = new Scenario();
            scenario.setHubId(hubId);
            scenario.setName(event.getName());
        }

        log.info("Processing {} conditions", event.getConditions().size());
        for (ScenarioConditionAvro condAvro : event.getConditions()) {
            Sensor sensor = sensorRepository.findByIdAndHubId(condAvro.getSensorId(), hubId)
                    .orElseGet(() -> {
                        Sensor newSensor = new Sensor();
                        newSensor.setId(condAvro.getSensorId());
                        newSensor.setHubId(hubId);
                        return sensorRepository.save(newSensor);
                    });

            Condition condition = new Condition();
            condition.setType(ConditionType.valueOf(condAvro.getType().name()));
            condition.setOperation(ConditionOperation.valueOf(condAvro.getOperation().name()));

            Object value = condAvro.getValue();
            if (value instanceof Boolean) {
                condition.setValue((Boolean) value ? 1 : 0);
            } else if (value instanceof Integer) {
                condition.setValue((Integer) value);
            }

            conditionRepository.save(condition);
            log.debug("Saved condition: type={}, operation={}, value={}",
                    condition.getType(), condition.getOperation(), condition.getValue());

            ScenarioCondition scenarioCondition = new ScenarioCondition();
            scenarioCondition.setScenario(scenario);
            scenarioCondition.setSensor(sensor);
            scenarioCondition.setCondition(condition);
            scenario.getConditions().add(scenarioCondition);
        }

        log.info("Processing {} actions", event.getActions().size());
        for (DeviceActionAvro actAvro : event.getActions()) {
            Sensor sensor = sensorRepository.findByIdAndHubId(actAvro.getSensorId(), hubId)
                    .orElseGet(() -> {
                        Sensor newSensor = new Sensor();
                        newSensor.setId(actAvro.getSensorId());
                        newSensor.setHubId(hubId);
                        return sensorRepository.save(newSensor);
                    });

            Action action = new Action();
            action.setType(ActionType.valueOf(actAvro.getType().name()));
            action.setValue(actAvro.getValue());
            actionRepository.save(action);
            log.debug("Saved action: type={}, value={}", action.getType(), action.getValue());

            ScenarioAction scenarioAction = new ScenarioAction();
            scenarioAction.setScenario(scenario);
            scenarioAction.setSensor(sensor);
            scenarioAction.setAction(action);
            scenario.getActions().add(scenarioAction);
        }

        scenarioRepository.save(scenario);
        log.info("Scenario saved: name={}, hubId={}, conditions={}, actions={}",
                event.getName(), hubId, scenario.getConditions().size(), scenario.getActions().size());
    }

    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro event) {
        scenarioRepository.findByHubIdAndName(hubId, event.getName())
                .ifPresent(scenario -> {
                    scenarioRepository.delete(scenario);
                    log.info("Scenario removed: name={}, hubId={}", event.getName(), hubId);
                });
    }
}