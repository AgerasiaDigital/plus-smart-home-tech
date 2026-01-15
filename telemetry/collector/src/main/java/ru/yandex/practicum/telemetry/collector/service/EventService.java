package ru.yandex.practicum.telemetry.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.configuration.KafkaConfiguration;
import ru.yandex.practicum.telemetry.collector.mapper.EventMapper;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final KafkaProducer<String, SpecificRecordBase> kafkaProducer;
    private final KafkaConfiguration kafkaConfiguration;
    private final EventMapper eventMapper;

    public void collectSensorEvent(SensorEvent event) {
        log.info("Received sensor event: {}", event);

        try {
            SensorEventAvro avroEvent = eventMapper.mapToAvro(event);
            String topic = kafkaConfiguration.getSensorsTopic();

            ProducerRecord<String, SpecificRecordBase> record =
                    new ProducerRecord<>(topic, event.getHubId(), avroEvent);

            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Error sending sensor event to Kafka", exception);
                } else {
                    log.debug("Sensor event sent to Kafka: topic={}, partition={}, offset={}",
                            metadata.topic(), metadata.partition(), metadata.offset());
                }
            });

            log.info("Sensor event published to Kafka topic: {}", topic);
        } catch (Exception e) {
            log.error("Error processing sensor event", e);
            throw new RuntimeException("Failed to process sensor event", e);
        }
    }

    public void collectHubEvent(HubEvent event) {
        log.info("Received hub event: {}", event);

        try {
            HubEventAvro avroEvent = eventMapper.mapToAvro(event);
            String topic = kafkaConfiguration.getHubsTopic();

            ProducerRecord<String, SpecificRecordBase> record =
                    new ProducerRecord<>(topic, event.getHubId(), avroEvent);

            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Error sending hub event to Kafka", exception);
                } else {
                    log.debug("Hub event sent to Kafka: topic={}, partition={}, offset={}",
                            metadata.topic(), metadata.partition(), metadata.offset());
                }
            });

            log.info("Hub event published to Kafka topic: {}", topic);
        } catch (Exception e) {
            log.error("Error processing hub event", e);
            throw new RuntimeException("Failed to process hub event", e);
        }
    }
}