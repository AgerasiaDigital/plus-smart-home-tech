package ru.yandex.practicum.telemetry.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.analyzer.configuration.AnalyzerKafkaConfig;
import ru.yandex.practicum.telemetry.analyzer.service.ScenarioAnalyzerService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    private final KafkaConsumer<String, SensorsSnapshotAvro> snapshotConsumer;
    private final ScenarioAnalyzerService scenarioAnalyzerService;
    private final AnalyzerKafkaConfig config;

    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(snapshotConsumer::wakeup));

        try {
            snapshotConsumer.subscribe(List.of(config.getSnapshotConsumer().getTopic()));
            log.info("Snapshot processor started, topic: {}",
                    config.getSnapshotConsumer().getTopic());

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records =
                        snapshotConsumer.poll(Duration.ofSeconds(5));

                int count = 0;
                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    processSnapshot(record.value());
                    manageOffsets(record, count);
                    count++;
                }

                // Фиксируем оффсеты синхронно для надежности
                if (!currentOffsets.isEmpty()) {
                    snapshotConsumer.commitSync(currentOffsets);
                    log.debug("Committed {} offsets synchronously", currentOffsets.size());
                }
            }

        } catch (WakeupException ignored) {
            log.info("Snapshot consumer wakeup called");
        } catch (Exception e) {
            log.error("Error in snapshot processor", e);
        } finally {
            try {
                snapshotConsumer.commitSync(currentOffsets);
            } finally {
                log.info("Closing snapshot consumer");
                snapshotConsumer.close();
            }
        }
    }

    private void processSnapshot(SensorsSnapshotAvro snapshot) {
        log.info("📥 Processing snapshot for hub: {}, sensors: {}", 
            snapshot.getHubId(), snapshot.getSensorsState().keySet());
        scenarioAnalyzerService.analyzeSnapshot(snapshot);
    }

    private void manageOffsets(ConsumerRecord<String, SensorsSnapshotAvro> record, int count) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 5 == 0) {
            snapshotConsumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Error committing offsets: {}", offsets, exception);
                }
            });
        }
    }
}