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

        SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(
                hubId,
                id -> SensorsSnapshotAvro.newBuilder()
                        .setHubId(id)
                        .setTimestamp(event.getTimestamp())
                        .setSensorsState(new HashMap<>())
                        .build()
        );

        Map<CharSequence, SensorStateAvro> sensorsState = new HashMap<>(snapshot.getSensorsState());

        SensorStateAvro existingState = sensorsState.get(event.getId());

        if (existingState != null) {
            if (existingState.getTimestamp() >= event.getTimestamp()) {
                // Событие устарело, игнорируем
                log.debug("Ignoring outdated event for sensor {} in hub {}", event.getId(), hubId);
                return Optional.empty();
            }

            if (existingState.getData().equals(event.getPayload())) {
                log.debug("Data unchanged for sensor {} in hub {}", event.getId(), hubId);
                return Optional.empty();
            }
        }

        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();

        sensorsState.put(event.getId(), newState);

        SensorsSnapshotAvro updatedSnapshot = SensorsSnapshotAvro.newBuilder()
                .setHubId(hubId)
                .setTimestamp(event.getTimestamp())
                .setSensorsState(sensorsState)
                .build();

        snapshots.put(hubId, updatedSnapshot);

        log.info("Updated snapshot for hub {}, sensor count: {}", hubId, sensorsState.size());
        return Optional.of(updatedSnapshot);
    }
}