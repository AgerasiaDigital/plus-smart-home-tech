package ru.yandex.practicum.telemetry.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class SnapshotService {

    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId();
        String sensorId = event.getId();
        
        log.info("Processing sensor event: hubId={}, sensorId={}, timestamp={}", 
            hubId, sensorId, event.getTimestamp());

        SensorsSnapshotAvro snapshot = snapshots.get(hubId);

        Map<String, SensorStateAvro> sensorsState;

        if (snapshot == null) {
            sensorsState = new HashMap<>();
            log.info("Creating new snapshot for hub: {}", hubId);
        } else {
            sensorsState = new HashMap<>(snapshot.getSensorsState());
            log.debug("Updating existing snapshot for hub: {}, current sensors: {}", 
                hubId, sensorsState.size());
        }

        SensorStateAvro existingState = sensorsState.get(sensorId);

        // Проверяем только временные метки - игнорируем дедупликацию данных
        if (existingState != null) {
            if (existingState.getTimestamp().compareTo(event.getTimestamp()) > 0) {
                log.warn("⚠️ Ignoring outdated event for sensor {} in hub {}: existing={}, new={}", 
                    sensorId, hubId, existingState.getTimestamp(), event.getTimestamp());
                return Optional.empty();
            }
            log.info("✅ Updating sensor {} in hub {} with newer timestamp: {} -> {}", 
                sensorId, hubId, existingState.getTimestamp(), event.getTimestamp());
        } else {
            log.info("✅ Adding new sensor {} to hub {}", sensorId, hubId);
        }

        // ВСЕГДА создаем новый снапшот, независимо от изменения данных
        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();

        sensorsState.put(sensorId, newState);

        SensorsSnapshotAvro updatedSnapshot = SensorsSnapshotAvro.newBuilder()
                .setHubId(hubId)
                .setTimestamp(event.getTimestamp())
                .setSensorsState(sensorsState)
                .build();

        snapshots.put(hubId, updatedSnapshot);

        log.info("📸 Generated snapshot for hub {}, sensor count: {}, updated sensor: {}", 
            hubId, sensorsState.size(), sensorId);
        return Optional.of(updatedSnapshot);
    }
}