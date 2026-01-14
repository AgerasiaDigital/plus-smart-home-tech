package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.hub.*;
import ru.yandex.practicum.model.sensor.*;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {
    private final KafkaTemplate<String, SensorEventAvro> sensorEventKafkaTemplate;
    private final KafkaTemplate<String, HubEventAvro> hubEventKafkaTemplate;

    private static final String SENSOR_TOPIC = "telemetry.sensors.v1";
    private static final String HUB_TOPIC = "telemetry.hubs.v1";

    public void collectSensorEvent(SensorEvent event) {
        log.info("Collecting sensor event: {}", event);
        SensorEventAvro avroEvent = toAvro(event);
        ProducerRecord<String, SensorEventAvro> record = new ProducerRecord<>(
                SENSOR_TOPIC,
                event.getId(),
                avroEvent
        );
        sensorEventKafkaTemplate.send(record);
        log.info("Sensor event sent to Kafka: {}", event.getId());
    }

    public void collectHubEvent(HubEvent event) {
        log.info("Collecting hub event: {}", event);
        HubEventAvro avroEvent = toAvro(event);
        ProducerRecord<String, HubEventAvro> record = new ProducerRecord<>(
                HUB_TOPIC,
                event.getHubId(),
                avroEvent
        );
        hubEventKafkaTemplate.send(record);
        log.info("Hub event sent to Kafka: {}", event.getHubId());
    }

    private SensorEventAvro toAvro(SensorEvent event) {
        Object payload = switch (event) {
            case LightSensorEvent e -> LightSensorAvro.newBuilder()
                    .setLinkQuality(e.getLinkQuality() != null ? e.getLinkQuality() : 0)
                    .setLuminosity(e.getLuminosity() != null ? e.getLuminosity() : 0)
                    .build();
            case MotionSensorEvent e -> MotionSensorAvro.newBuilder()
                    .setLinkQuality(e.getLinkQuality())
                    .setMotion(e.getMotion())
                    .setVoltage(e.getVoltage())
                    .build();
            case TemperatureSensorEvent e -> TemperatureSensorAvro.newBuilder()
                    .setTemperatureC(e.getTemperatureC())
                    .setTemperatureF(e.getTemperatureF())
                    .build();
            case ClimateSensorEvent e -> ClimateSensorAvro.newBuilder()
                    .setTemperatureC(e.getTemperatureC())
                    .setHumidity(e.getHumidity())
                    .setCo2Level(e.getCo2Level())
                    .build();
            case SwitchSensorEvent e -> SwitchSensorAvro.newBuilder()
                    .setState(e.getState())
                    .build();
            default -> throw new IllegalArgumentException("Unknown sensor event type: " + event.getClass());
        };

        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp().toEpochMilli())
                .setPayload(payload)
                .build();
    }

    private HubEventAvro toAvro(HubEvent event) {
        Object payload = switch (event) {
            case DeviceAddedEvent e -> DeviceAddedEventAvro.newBuilder()
                    .setId(e.getId())
                    .setType(DeviceTypeAvro.valueOf(e.getDeviceType().name()))
                    .build();
            case DeviceRemovedEvent e -> DeviceRemovedEventAvro.newBuilder()
                    .setId(e.getId())
                    .build();
            case ScenarioAddedEvent e -> ScenarioAddedEventAvro.newBuilder()
                    .setName(e.getName())
                    .setConditions(e.getConditions().stream()
                            .map(this::toAvroCondition)
                            .collect(Collectors.toList()))
                    .setActions(e.getActions().stream()
                            .map(this::toAvroAction)
                            .collect(Collectors.toList()))
                    .build();
            case ScenarioRemovedEvent e -> ScenarioRemovedEventAvro.newBuilder()
                    .setName(e.getName())
                    .build();
            default -> throw new IllegalArgumentException("Unknown hub event type: " + event.getClass());
        };

        return HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp().toEpochMilli())
                .setPayload(payload)
                .build();
    }

    private ScenarioConditionAvro toAvroCondition(ScenarioCondition condition) {
        ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder()
                .setSensorId(condition.getSensorId())
                .setType(ConditionTypeAvro.valueOf(condition.getType().name()))
                .setOperation(ConditionOperationAvro.valueOf(condition.getOperation().name()));

        if (condition.getValue() != null) {
            builder.setValue(condition.getValue());
        } else {
            builder.setValue(null);
        }

        return builder.build();
    }

    private DeviceActionAvro toAvroAction(DeviceAction action) {
        DeviceActionAvro.Builder builder = DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(ActionTypeAvro.valueOf(action.getType().name()));

        if (action.getValue() != null) {
            builder.setValue(action.getValue());
        } else {
            builder.setValue(null);
        }

        return builder.build();
    }
}