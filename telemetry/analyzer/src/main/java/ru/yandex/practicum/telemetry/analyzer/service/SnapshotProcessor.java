package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    private final KafkaConsumer<String, SensorsSnapshotAvro> consumer;
    private final String snapshotTopic;
    private final ScenarioService scenarioService;

    public void start() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            consumer.subscribe(List.of(snapshotTopic));
            log.info("SnapshotProcessor subscribed to topic: {}", snapshotTopic);

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofMillis(1000));

                records.forEach(record -> {
                    SensorsSnapshotAvro snapshot = record.value();
                    log.debug("Processing snapshot: hubId={}, timestamp={}",
                            snapshot.getHubId(), snapshot.getTimestamp());

                    try {
                        scenarioService.processSnapshot(snapshot);
                    } catch (Exception e) {
                        log.error("Error processing snapshot", e);
                    }
                });

                consumer.commitSync();
            }
        } catch (WakeupException e) {
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
}