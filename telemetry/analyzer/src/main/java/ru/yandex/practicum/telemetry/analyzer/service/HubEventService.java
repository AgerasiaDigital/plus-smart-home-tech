package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.entity.*;
import ru.yandex.practicum.telemetry.analyzer.repository.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class HubEventService {

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;

    @Transactional
    public void processEvent(HubEventAvro event) {
        Object payload = event.getPayload();

        if (payload instanceof DeviceAddedEventAvro deviceAdded) {
            handleDeviceAdded(event.getHubId().toString(), deviceAdded);
        } else if (payload instanceof DeviceRemovedEventAvro deviceRemoved) {
            handleDeviceRemoved(deviceRemoved);
        } else if (payload instanceof ScenarioAddedEventAvro scenarioAdded) {
            handleScenarioAdded(event.getHubId().toString(), scenarioAdded);
        } else if (payload instanceof ScenarioRemovedEventAvro scenarioRemoved) {
            handleScenarioRemoved(event.getHubId().toString(), scenarioRemoved);
        }
    }

    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro event) {
        Sensor sensor = new Sensor();
        sensor.setId(event.getId().toString());
        sensor.setHubId(hubId);

        sensorRepository.save(sensor);
        log.info("Device added: id={}, hubId={}", event.getId(), hubId);
    }

    private void handleDeviceRemoved(DeviceRemovedEventAvro event) {
        sensorRepository.deleteById(event.getId().toString());
        log.info("Device removed: id={}", event.getId());
    }

    private void handleScenarioAdded(String hubId, ScenarioAddedEventAvro event) {
        log.info("Processing SCENARIO_ADDED: name={}, hubId={}, conditions={}, actions={}", 
            event.getName(), hubId, event.getConditions().size(), event.getActions().size());
        
        // Логируем все условия для диагностики
        for (int i = 0; i < event.getConditions().size(); i++) {
            ScenarioConditionAvro conditionAvro = event.getConditions().get(i);
            log.info("Condition {}: sensorId={}, type={}, operation={}, value={}, valueClass={}", 
                i, conditionAvro.getSensorId(), conditionAvro.getType(), conditionAvro.getOperation(),
                conditionAvro.getValue(), conditionAvro.getValue() != null ? conditionAvro.getValue().getClass().getSimpleName() : "null");
        }
        
        // Проверяем, существует ли уже сценарий
        var existing = scenarioRepository.findByHubIdAndName(hubId, event.getName().toString());
        Scenario scenario;

        if (existing.isPresent()) {
            scenario = existing.get();
            log.info("Updating existing scenario: id={}", scenario.getId());
            // Удаляем старые условия и действия
            scenarioConditionRepository.deleteByScenarioId(scenario.getId());
            scenarioActionRepository.deleteByScenarioId(scenario.getId());
        } else {
            scenario = new Scenario();
            scenario.setHubId(hubId);
            scenario.setName(event.getName().toString());
            scenario = scenarioRepository.save(scenario);
            log.info("Created new scenario: id={}", scenario.getId());
        }

        // Добавляем условия
        for (ScenarioConditionAvro conditionAvro : event.getConditions()) {
            Condition condition = new Condition();
            condition.setType(conditionAvro.getType().toString());
            condition.setOperation(conditionAvro.getOperation().toString());

            Object value = conditionAvro.getValue();
            log.info("Processing condition value: type={}, value={}, valueClass={}", 
                conditionAvro.getType(), value, value != null ? value.getClass().getSimpleName() : "null");
                
            if (value instanceof Integer intValue) {
                condition.setValue(intValue);
                log.info("Saved condition value as int: {}", intValue);
            } else if (value instanceof Boolean boolValue) {
                // ИСПРАВЛЕНИЕ: Для boolean значений не конвертируем в 1/0
                // Сохраняем как есть, но в базе храним как int
                condition.setValue(boolValue ? 1 : 0);
                log.info("Saved condition value as bool->int: {} -> {}", boolValue, boolValue ? 1 : 0);
            } else if (value == null) {
                condition.setValue(null);
                log.warn("Condition value is null, setting to null");
            } else {
                log.warn("Unknown condition value type: {}, setting to null", value.getClass());
                condition.setValue(null);
            }

            condition = conditionRepository.save(condition);

            ScenarioCondition scenarioCondition = new ScenarioCondition();
            scenarioCondition.setScenarioId(scenario.getId());
            scenarioCondition.setSensorId(conditionAvro.getSensorId().toString());
            scenarioCondition.setConditionId(condition.getId());
            scenarioConditionRepository.save(scenarioCondition);
            
            log.info("Added condition: type={}, operation={}, value={}, sensorId={}", 
                condition.getType(), condition.getOperation(), condition.getValue(), 
                conditionAvro.getSensorId());
        }

        // Добавляем действия
        for (DeviceActionAvro actionAvro : event.getActions()) {
            Action action = new Action();
            action.setType(actionAvro.getType().toString());
            action.setValue(actionAvro.getValue());
            action = actionRepository.save(action);

            ScenarioAction scenarioAction = new ScenarioAction();
            scenarioAction.setScenarioId(scenario.getId());
            scenarioAction.setSensorId(actionAvro.getSensorId().toString());
            scenarioAction.setActionId(action.getId());
            scenarioActionRepository.save(scenarioAction);
            
            log.info("Added action: type={}, value={}, sensorId={}", 
                action.getType(), action.getValue(), actionAvro.getSensorId());
        }

        log.info("Scenario added/updated successfully: name={}, hubId={}", event.getName(), hubId);
    }

    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro event) {
        scenarioRepository.findByHubIdAndName(hubId, event.getName().toString())
                .ifPresent(scenario -> {
                    scenarioConditionRepository.deleteByScenarioId(scenario.getId());
                    scenarioActionRepository.deleteByScenarioId(scenario.getId());
                    scenarioRepository.delete(scenario);
                    log.info("Scenario removed: name={}, hubId={}", event.getName(), hubId);
                });
    }
}