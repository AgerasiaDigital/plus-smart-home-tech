package ru.yandex.practicum.telemetry.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.aggregator.configuration.AggregatorKafkaConfig;
import ru.yandex.practicum.telemetry.aggregator.service.SnapshotService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaProducer<String, SpecificRecordBase> producer;
    private final SnapshotService snapshotService;
    private final AggregatorKafkaConfig config;

    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribe(List.of(config.getConsumer().getTopic()));

            log.info("Starting aggregation from topic: {}", config.getConsumer().getTopic());

            while (true) {
                ConsumerRecords<String, SensorEventAvro> records =
                        consumer.poll(Duration.ofSeconds(5));

                int count = 0;
                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    processRecord(record);
                    manageOffsets(record, count);
                    count++;
                }

                // Фиксируем оффсеты синхронно для надежности
                if (!currentOffsets.isEmpty()) {
                    consumer.commitSync(currentOffsets);
                    log.debug("Committed {} offsets synchronously", currentOffsets.size());
                }
            }

        } catch (WakeupException ignored) {
            log.info("Consumer wakeup called");
        } catch (Exception e) {
            log.error("Error during aggregation", e);
        } finally {
            try {
                producer.flush();
                consumer.commitSync(currentOffsets);
            } finally {
                log.info("Closing consumer and producer");
                consumer.close();
                producer.close();
            }
        }
    }

    private void processRecord(ConsumerRecord<String, SensorEventAvro> record) {
        SensorEventAvro event = record.value();
        log.debug("Processing event: sensor={}, hub={}", event.getId(), event.getHubId());

        Optional<SensorsSnapshotAvro> updatedSnapshot = snapshotService.updateState(event);

        updatedSnapshot.ifPresent(snapshot -> {
            String topic = config.getProducer().getTopic();
            ProducerRecord<String, SpecificRecordBase> producerRecord =
                    new ProducerRecord<>(topic, snapshot.getHubId().toString(), snapshot);

            producer.send(producerRecord, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Error sending snapshot to Kafka", exception);
                } else {
                    log.debug("Snapshot sent: topic={}, partition={}, offset={}",
                            metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
        });
    }

    private void manageOffsets(ConsumerRecord<String, SensorEventAvro> record, int count) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 10 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Error committing offsets: {}", offsets, exception);
                }
            });
        }
    }
}