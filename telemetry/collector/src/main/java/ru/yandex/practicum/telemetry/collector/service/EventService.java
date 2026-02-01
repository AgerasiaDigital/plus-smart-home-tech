package ru.yandex.practicum.telemetry.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.telemetry.collector.configuration.KafkaConfiguration;
import ru.yandex.practicum.telemetry.collector.exception.EventProcessingException;
import ru.yandex.practicum.telemetry.collector.mapper.EventMapper;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

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

            long timestamp = event.getTimestamp().toEpochMilli();

            ProducerRecord<String, SpecificRecordBase> record =
                    new ProducerRecord<>(topic, null, timestamp, event.getHubId(), avroEvent);

            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Error sending sensor event to Kafka", exception);
                } else {
                    log.debug("Sensor event sent to Kafka: topic={}, partition={}, offset={}",
                            metadata.topic(), metadata.partition(), metadata.offset());
                }
            });

            log.info("Sensor event published to Kafka topic: {}", topic);
        } catch (SerializationException e) {
            log.error("Error serializing sensor event", e);
            throw new EventProcessingException("Failed to serialize sensor event", e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid sensor event data", e);
            throw new EventProcessingException("Invalid sensor event data", e);
        }
    }

    public void collectHubEvent(HubEvent event) {
        log.info("Received hub event: {}", event);

        try {
            HubEventAvro avroEvent = eventMapper.mapToAvro(event);
            String topic = kafkaConfiguration.getHubsTopic();

            long timestamp = event.getTimestamp().toEpochMilli();

            ProducerRecord<String, SpecificRecordBase> record =
                    new ProducerRecord<>(topic, null, timestamp, event.getHubId(), avroEvent);

            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Error sending hub event to Kafka", exception);
                } else {
                    log.debug("Hub event sent to Kafka: topic={}, partition={}, offset={}",
                            metadata.topic(), metadata.partition(), metadata.offset());
                }
            });

            log.info("Hub event published to Kafka topic: {}", topic);
        } catch (SerializationException e) {
            log.error("Error serializing hub event", e);
            throw new EventProcessingException("Failed to serialize hub event", e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid hub event data", e);
            throw new EventProcessingException("Invalid hub event data", e);
        }
    }
}