package ru.yandex.practicum.telemetry.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.analyzer.configuration.AnalyzerKafkaConfig;
import ru.yandex.practicum.telemetry.analyzer.service.HubEventService;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final KafkaConsumer<String, HubEventAvro> hubEventConsumer;
    private final HubEventService hubEventService;
    private final AnalyzerKafkaConfig config;

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(hubEventConsumer::wakeup));

        try {
            hubEventConsumer.subscribe(List.of(config.getHubEventConsumer().getTopic()));
            log.info("Hub event processor started, topic: {}",
                    config.getHubEventConsumer().getTopic());

            while (true) {
                ConsumerRecords<String, HubEventAvro> records =
                        hubEventConsumer.poll(Duration.ofSeconds(5));

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    processHubEvent(record.value());
                }

                hubEventConsumer.commitAsync();
            }

        } catch (WakeupException ignored) {
            log.info("Hub event consumer wakeup called");
        } catch (Exception e) {
            log.error("Error in hub event processor", e);
        } finally {
            try {
                hubEventConsumer.commitSync();
            } finally {
                log.info("Closing hub event consumer");
                hubEventConsumer.close();
            }
        }
    }

    private void processHubEvent(HubEventAvro event) {
        log.info("🏠 Processing hub event: hub={}, type={}",
                event.getHubId(), event.getPayload().getClass().getSimpleName());

        hubEventService.processEvent(event);
    }
}