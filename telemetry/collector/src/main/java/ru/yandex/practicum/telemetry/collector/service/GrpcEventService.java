package ru.yandex.practicum.telemetry.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.configuration.KafkaConfiguration;
import ru.yandex.practicum.telemetry.collector.exception.EventProcessingException;
import ru.yandex.practicum.telemetry.collector.mapper.GrpcEventMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrpcEventService {

    private final KafkaProducer<String, SpecificRecordBase> kafkaProducer;
    private final KafkaConfiguration kafkaConfiguration;
    private final GrpcEventMapper grpcEventMapper;

    public void collectSensorEvent(SensorEventProto event) {
        log.info("Processing gRPC sensor event: id={}, hubId={}", event.getId(), event.getHubId());

        try {
            SensorEventAvro avroEvent = grpcEventMapper.mapToAvro(event);
            String topic = kafkaConfiguration.getSensorsTopic();

            long timestamp = event.getTimestamp().getSeconds() * 1000 +
                    event.getTimestamp().getNanos() / 1000000;

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

            log.info("gRPC sensor event published to Kafka topic: {}", topic);
        } catch (SerializationException e) {
            log.error("Error serializing sensor event", e);
            throw new EventProcessingException("Failed to serialize sensor event", e);
        } catch (Exception e) {
            log.error("Error processing sensor event", e);
            throw new EventProcessingException("Failed to process sensor event", e);
        }
    }

    public void collectHubEvent(HubEventProto event) {
        log.info("Processing gRPC hub event: hubId={}, type={}",
                event.getHubId(), event.getPayloadCase());

        try {
            HubEventAvro avroEvent = grpcEventMapper.mapToAvro(event);
            String topic = kafkaConfiguration.getHubsTopic();

            long timestamp = event.getTimestamp().getSeconds() * 1000 +
                    event.getTimestamp().getNanos() / 1000000;

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

            log.info("gRPC hub event published to Kafka topic: {}", topic);
        } catch (SerializationException e) {
            log.error("Error serializing hub event", e);
            throw new EventProcessingException("Failed to serialize hub event", e);
        } catch (Exception e) {
            log.error("Error processing hub event", e);
            throw new EventProcessingException("Failed to process hub event", e);
        }
    }
}