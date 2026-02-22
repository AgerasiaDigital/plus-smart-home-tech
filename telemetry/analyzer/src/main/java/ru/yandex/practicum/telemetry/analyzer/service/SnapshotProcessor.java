package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.analyzer.configuration.KafkaConfig;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    private final KafkaConsumer<String, SensorsSnapshotAvro> consumer;
    private final String snapshotTopic;
    private final ScenarioService scenarioService;
    private final KafkaConfig kafkaConfig;

    public void start() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            consumer.subscribe(List.of(snapshotTopic));
            log.info("SnapshotProcessor subscribed to topic: {}", snapshotTopic);

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofMillis(kafkaConfig.getPollTimeoutMs()));

                records.forEach(record -> {
                    SensorsSnapshotAvro snapshot = record.value();
                    log.info("Processing snapshot: hubId={}, timestamp={}, sensors={}",
                            snapshot.getHubId(), snapshot.getTimestamp(), snapshot.getSensorsState().size());

                    try {
                        scenarioService.processSnapshot(snapshot);
                    } catch (Exception e) {
                        log.error("Error processing snapshot", e);
                    }
                });

                if (!records.isEmpty()) {
                    try {
                        consumer.commitSync();
                        log.debug("Committed {} snapshot records", records.count());
                    } catch (Exception e) {
                        log.error("Error committing offsets", e);
                    }
                }
            }
        } catch (WakeupException e) {
            log.info("SnapshotProcessor wakeup");
        } catch (Exception e) {
            log.error("Error in SnapshotProcessor", e);
        } finally {
            try {
                consumer.commitSync();
            } catch (Exception e) {
                log.error("Error in final commit", e);
            } finally {
                log.info("Closing SnapshotProcessor consumer");
                consumer.close();
            }
        }
    }
}