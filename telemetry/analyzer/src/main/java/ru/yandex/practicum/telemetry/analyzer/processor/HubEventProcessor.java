package ru.yandex.practicum.telemetry.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.analyzer.service.HubEventService;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Component
@RequiredArgsConstructor
@Slf4j
public class HubEventProcessor implements Runnable {

    private final HubEventService hubEventService;

    @Value("${app.kafka.consumer.hub-events.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.consumer.hub-events.group-id}")
    private String groupId;

    @Value("${app.kafka.consumer.hub-events.topic}")
    private String topic;

    @Value("${app.kafka.consumer.hub-events.key-deserializer}")
    private String keyDeserializer;

    @Value("${app.kafka.consumer.hub-events.value-deserializer}")
    private String valueDeserializer;

    @Value("${app.kafka.consumer.hub-events.auto-offset-reset}")
    private String autoOffsetReset;

    @Value("${app.kafka.consumer.hub-events.enable-auto-commit}")
    private boolean enableAutoCommit;

    @Value("${app.kafka.consumer.hub-events.auto-commit-interval-ms}")
    private int autoCommitIntervalMs;

    @Override
    public void run() {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", groupId);
        props.put("key.deserializer", keyDeserializer);
        props.put("value.deserializer", valueDeserializer);
        props.put("auto.offset.reset", autoOffsetReset);
        props.put("enable.auto.commit", enableAutoCommit);
        props.put("auto.commit.interval.ms", autoCommitIntervalMs);

        try (KafkaConsumer<String, HubEventAvro> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            log.info("Started hub event processor, subscribed to topic: {}", topic);

            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofMillis(1000));
                
                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    try {
                        hubEventService.processHubEvent(record.value());
                    } catch (Exception e) {
                        log.error("Error processing hub event: {}", e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in hub event processor: {}", e.getMessage(), e);
        }
    }
}