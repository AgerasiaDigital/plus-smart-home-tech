package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final KafkaConsumer<String, HubEventAvro> consumer;
    private final String hubEventTopic;
    private final ScenarioService scenarioService;

    @Override
    public void run() {
        try {
            consumer.subscribe(List.of(hubEventTopic));
            log.info("HubEventProcessor subscribed to topic: {}", hubEventTopic);

            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofMillis(1000));

                records.forEach(record -> {
                    HubEventAvro event = record.value();
                    String hubId = event.getHubId();
                    Object payload = event.getPayload();

                    log.debug("Processing hub event: hubId={}, type={}", hubId, payload.getClass().getSimpleName());

                    try {
                        if (payload instanceof DeviceAddedEventAvro deviceAdded) {
                            scenarioService.addDevice(hubId, deviceAdded.getId(), deviceAdded.getType());
                        } else if (payload instanceof DeviceRemovedEventAvro deviceRemoved) {
                            scenarioService.removeDevice(deviceRemoved.getId());
                        } else if (payload instanceof ScenarioAddedEventAvro scenarioAdded) {
                            scenarioService.addScenario(hubId, scenarioAdded);
                        } else if (payload instanceof ScenarioRemovedEventAvro scenarioRemoved) {
                            scenarioService.removeScenario(hubId, scenarioRemoved.getName());
                        }
                    } catch (Exception e) {
                        log.error("Error processing hub event", e);
                    }
                });

                consumer.commitAsync((offsets, exception) -> {
                    if (exception != null) {
                        log.error("Error committing offsets", exception);
                    }
                });
            }
        } catch (WakeupException e) {
            log.info("HubEventProcessor wakeup");
        } catch (Exception e) {
            log.error("Error in HubEventProcessor", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                log.info("Closing HubEventProcessor consumer");
                consumer.close();
            }
        }
    }
}