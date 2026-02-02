package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Duration;
import java.util.ArrayList;
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

                if (!records.isEmpty()) {
                    List<ConsumerRecord<String, HubEventAvro>> recordList = new ArrayList<>();
                    records.forEach(recordList::add);

                    for (ConsumerRecord<String, HubEventAvro> record : recordList) {
                        HubEventAvro event = record.value();
                        String hubId = event.getHubId();
                        Object payload = event.getPayload();

                        log.info("Processing hub event: hubId={}, type={}", hubId, payload.getClass().getSimpleName());

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
                            throw e;
                        }
                    }

                    try {
                        consumer.commitSync();
                        log.info("Committed {} hub event records", records.count());
                    } catch (Exception e) {
                        log.error("Error committing offsets", e);
                    }
                }
            }
        } catch (WakeupException e) {
            log.info("HubEventProcessor wakeup");
        } catch (Exception e) {
            log.error("Error in HubEventProcessor", e);
        } finally {
            try {
                consumer.commitSync();
            } catch (Exception e) {
                log.error("Error in final commit", e);
            } finally {
                log.info("Closing HubEventProcessor consumer");
                consumer.close();
            }
        }
    }
}