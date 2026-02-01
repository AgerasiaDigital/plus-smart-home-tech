package ru.yandex.practicum.telemetry.collector.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GrpcEventMapper {

    public SensorEventAvro mapToAvro(SensorEventProto proto) {
        long timestampMs = proto.getTimestamp().getSeconds() * 1000 +
                proto.getTimestamp().getNanos() / 1000000;

        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                .setId(proto.getId())
                .setHubId(proto.getHubId())
                .setTimestamp(Instant.ofEpochMilli(timestampMs));

        Object payload = switch (proto.getPayloadCase()) {
            case MOTION_SENSOR -> mapMotionSensor(proto.getMotionSensor());
            case TEMPERATURE_SENSOR -> mapTemperatureSensor(proto.getTemperatureSensor());
            case LIGHT_SENSOR -> mapLightSensor(proto.getLightSensor());
            case CLIMATE_SENSOR -> mapClimateSensor(proto.getClimateSensor());
            case SWITCH_SENSOR -> mapSwitchSensor(proto.getSwitchSensor());
            default -> throw new IllegalArgumentException("Unknown sensor type: " + proto.getPayloadCase());
        };

        builder.setPayload(payload);
        return builder.build();
    }

    private MotionSensorAvro mapMotionSensor(MotionSensorProto proto) {
        return MotionSensorAvro.newBuilder()
                .setLinkQuality(proto.getLinkQuality())
                .setMotion(proto.getMotion())
                .setVoltage(proto.getVoltage())
                .build();
    }

    private TemperatureSensorAvro mapTemperatureSensor(TemperatureSensorProto proto) {
        return TemperatureSensorAvro.newBuilder()
                .setTemperatureC(proto.getTemperatureC())
                .setTemperatureF(proto.getTemperatureF())
                .build();
    }

    private LightSensorAvro mapLightSensor(LightSensorProto proto) {
        return LightSensorAvro.newBuilder()
                .setLinkQuality(proto.getLinkQuality())
                .setLuminosity(proto.getLuminosity())
                .build();
    }

    private ClimateSensorAvro mapClimateSensor(ClimateSensorProto proto) {
        return ClimateSensorAvro.newBuilder()
                .setTemperatureC(proto.getTemperatureC())
                .setHumidity(proto.getHumidity())
                .setCo2Level(proto.getCo2Level())
                .build();
    }

    private SwitchSensorAvro mapSwitchSensor(SwitchSensorProto proto) {
        return SwitchSensorAvro.newBuilder()
                .setState(proto.getState())
                .build();
    }

    public HubEventAvro mapToAvro(HubEventProto proto) {
        long timestampMs = proto.getTimestamp().getSeconds() * 1000 +
                proto.getTimestamp().getNanos() / 1000000;

        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(proto.getHubId())
                .setTimestamp(Instant.ofEpochMilli(timestampMs));

        Object payload = switch (proto.getPayloadCase()) {
            case DEVICE_ADDED -> mapDeviceAdded(proto.getDeviceAdded());
            case DEVICE_REMOVED -> mapDeviceRemoved(proto.getDeviceRemoved());
            case SCENARIO_ADDED -> mapScenarioAdded(proto.getScenarioAdded());
            case SCENARIO_REMOVED -> mapScenarioRemoved(proto.getScenarioRemoved());
            default -> throw new IllegalArgumentException("Unknown hub event type: " + proto.getPayloadCase());
        };

        builder.setPayload(payload);
        return builder.build();
    }

    private DeviceAddedEventAvro mapDeviceAdded(DeviceAddedEventProto proto) {
        return DeviceAddedEventAvro.newBuilder()
                .setId(proto.getId())
                .setType(DeviceTypeAvro.valueOf(proto.getType().name()))
                .build();
    }

    private DeviceRemovedEventAvro mapDeviceRemoved(DeviceRemovedEventProto proto) {
        return DeviceRemovedEventAvro.newBuilder()
                .setId(proto.getId())
                .build();
    }

    private ScenarioAddedEventAvro mapScenarioAdded(ScenarioAddedEventProto proto) {
        log.info("Mapping SCENARIO_ADDED: name={}, conditions={}, actions={}",
                proto.getName(), proto.getConditionsCount(), proto.getActionsCount());

        return ScenarioAddedEventAvro.newBuilder()
                .setName(proto.getName())
                .setConditions(proto.getConditionsList().stream()
                        .map(this::mapCondition)
                        .collect(Collectors.toList()))
                .setActions(proto.getActionsList().stream()
                        .map(this::mapAction)
                        .collect(Collectors.toList()))
                .build();
    }

    private ScenarioRemovedEventAvro mapScenarioRemoved(ScenarioRemovedEventProto proto) {
        return ScenarioRemovedEventAvro.newBuilder()
                .setName(proto.getName())
                .build();
    }

    private ScenarioConditionAvro mapCondition(ScenarioConditionProto proto) {
        log.info("Mapping condition: sensorId={}, type={}, operation={}, valueCase={}",
                proto.getSensorId(), proto.getType(), proto.getOperation(), proto.getValueCase());

        ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(ConditionTypeAvro.valueOf(proto.getType().name()))
                .setOperation(ConditionOperationAvro.valueOf(proto.getOperation().name()));

        // Определяем тип значения по типу условия согласно Avro схеме
        switch (proto.getType()) {
            case MOTION, SWITCH -> {
                // Для MOTION и SWITCH используем boolean значения
                boolean value;
                if (proto.getValueCase() == ScenarioConditionProto.ValueCase.BOOL_VALUE) {
                    value = proto.getBoolValue();
                } else if (proto.getValueCase() == ScenarioConditionProto.ValueCase.INT_VALUE) {
                    value = proto.getIntValue() != 0;
                } else {
                    value = false;
                }
                log.info("Setting BOOL_VALUE for {}: {}", proto.getType(), value);
                builder.setValue(value);
            }
            case TEMPERATURE, LUMINOSITY, CO2LEVEL, HUMIDITY -> {
                // Для числовых типов используем int значения
                int value;
                if (proto.getValueCase() == ScenarioConditionProto.ValueCase.INT_VALUE) {
                    value = proto.getIntValue();
                } else if (proto.getValueCase() == ScenarioConditionProto.ValueCase.BOOL_VALUE) {
                    value = proto.getBoolValue() ? 1 : 0;
                } else {
                    value = 0;
                }
                log.info("Setting INT_VALUE for {}: {}", proto.getType(), value);
                builder.setValue(value);
            }
            default -> {
                log.warn("Unknown condition type: {}", proto.getType());
                builder.setValue(null);
            }
        }

        return builder.build();
    }

    private DeviceActionAvro mapAction(DeviceActionProto proto) {
        return DeviceActionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(ActionTypeAvro.valueOf(proto.getType().name()))
                .setValue(proto.getValue() != 0 ? proto.getValue() : null)
                .build();
    }
}