package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.analyzer.entity.*;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.SensorRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HubEventService {

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;

    @Transactional
    public void processHubEvent(HubEventAvro hubEvent) {
        String hubId = hubEvent.getHubId();
        Object payload = hubEvent.getPayload();
        
        if (payload instanceof ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro deviceAdded) {
            handleDeviceAdded(hubId, deviceAdded);
        } else if (payload instanceof ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro deviceRemoved) {
            handleDeviceRemoved(hubId, deviceRemoved);
        } else if (payload instanceof ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro scenarioAdded) {
            handleScenarioAdded(hubId, scenarioAdded);
        } else if (payload instanceof ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro scenarioRemoved) {
            handleScenarioRemoved(hubId, scenarioRemoved);
        }
    }

    private void handleDeviceAdded(String hubId, ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro deviceAdded) {
        String deviceId = deviceAdded.getId();
        
        Optional<Sensor> existingSensor = sensorRepository.findByIdAndHubId(deviceId, hubId);
        if (existingSensor.isPresent()) {
            log.debug("Device {} already exists in hub {}", deviceId, hubId);
            return;
        }

        Sensor sensor = new Sensor();
        sensor.setId(deviceId);
        sensor.setHubId(hubId);
        sensorRepository.save(sensor);
        
        log.info("Added device {} to hub {}", deviceId, hubId);
    }

    private void handleDeviceRemoved(String hubId, ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro deviceRemoved) {
        String deviceId = deviceRemoved.getId();
        
        Optional<Sensor> sensor = sensorRepository.findByIdAndHubId(deviceId, hubId);
        if (sensor.isPresent()) {
            sensorRepository.delete(sensor.get());
            log.info("Removed device {} from hub {}", deviceId, hubId);
        } else {
            log.debug("Device {} not found in hub {}", deviceId, hubId);
        }
    }

    private void handleScenarioAdded(String hubId, ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro scenarioAdded) {
        String scenarioName = scenarioAdded.getName();
        
        Optional<Scenario> existingScenario = scenarioRepository.findByHubIdAndName(hubId, scenarioName);
        Scenario scenario;
        
        if (existingScenario.isPresent()) {
            scenario = existingScenario.get();
            scenario.getConditions().clear();
            scenario.getActions().clear();
            log.debug("Updating existing scenario {} in hub {}", scenarioName, hubId);
        } else {
            scenario = new Scenario();
            scenario.setHubId(hubId);
            scenario.setName(scenarioName);
            scenario.setConditions(new ArrayList<>());
            scenario.setActions(new ArrayList<>());
            log.info("Creating new scenario {} in hub {}", scenarioName, hubId);
        }

        // Add conditions
        scenarioAdded.getConditions().forEach(conditionAvro -> {
            Sensor sensor = sensorRepository.findByIdAndHubId(conditionAvro.getSensorId(), hubId)
                    .orElseThrow(() -> new IllegalArgumentException("Sensor not found: " + conditionAvro.getSensorId()));

            Condition condition = new Condition();
            condition.setType(conditionAvro.getType().toString());
            condition.setOperation(conditionAvro.getOperation().toString());
            
            Object value = conditionAvro.getValue();
            if (value instanceof Integer intValue) {
                condition.setValue(intValue);
            } else if (value instanceof Boolean boolValue) {
                condition.setValue(boolValue ? 1 : 0);
            } else {
                condition.setValue(0);
            }

            ScenarioCondition scenarioCondition = new ScenarioCondition();
            scenarioCondition.setScenario(scenario);
            scenarioCondition.setSensor(sensor);
            scenarioCondition.setCondition(condition);
            
            scenario.getConditions().add(scenarioCondition);
        });

        // Add actions
        scenarioAdded.getActions().forEach(actionAvro -> {
            Sensor sensor = sensorRepository.findByIdAndHubId(actionAvro.getSensorId(), hubId)
                    .orElseThrow(() -> new IllegalArgumentException("Sensor not found: " + actionAvro.getSensorId()));

            Action action = new Action();
            action.setType(actionAvro.getType().toString());
            
            Object value = actionAvro.getValue();
            if (value instanceof Integer intValue) {
                action.setValue(intValue);
            } else {
                action.setValue(0);
            }

            ScenarioAction scenarioAction = new ScenarioAction();
            scenarioAction.setScenario(scenario);
            scenarioAction.setSensor(sensor);
            scenarioAction.setAction(action);
            
            scenario.getActions().add(scenarioAction);
        });

        scenarioRepository.save(scenario);
    }

    private void handleScenarioRemoved(String hubId, ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro scenarioRemoved) {
        String scenarioName = scenarioRemoved.getName();
        
        Optional<Scenario> scenario = scenarioRepository.findByHubIdAndName(hubId, scenarioName);
        if (scenario.isPresent()) {
            scenarioRepository.delete(scenario.get());
            log.info("Removed scenario {} from hub {}", scenarioName, hubId);
        } else {
            log.debug("Scenario {} not found in hub {}", scenarioName, hubId);
        }
    }
}