package ru.yandex.practicum.telemetry.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.telemetry.collector.configuration.KafkaConfiguration;
import ru.yandex.practicum.telemetry.collector.mapper.ProtoEventMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventGrpcService {

    private final KafkaProducer<String, SpecificRecordBase> kafkaProducer;
    private final KafkaConfiguration kafkaConfiguration;
    private final ProtoEventMapper protoEventMapper;

    public void collectSensorEvent(SensorEventProto event) {
        log.info("Processing gRPC sensor event: id={}, hubId={}", event.getId(), event.getHubId());

        try {
            var avroEvent = protoEventMapper.toAvro(event);
            String topic = kafkaConfiguration.getSensorsTopic();

            long timestamp = event.getTimestamp().getSeconds() * 1000 + event.getTimestamp().getNanos() / 1_000_000;

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
        } catch (Exception e) {
            log.error("Error processing gRPC sensor event", e);
            throw e;
        }
    }

    public void collectHubEvent(HubEventProto event) {
        log.info("Processing gRPC hub event: hubId={}", event.getHubId());

        try {
            var avroEvent = protoEventMapper.toAvro(event);
            String topic = kafkaConfiguration.getHubsTopic();

            long timestamp = event.getTimestamp().getSeconds() * 1000 + event.getTimestamp().getNanos() / 1_000_000;

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
        } catch (Exception e) {
            log.error("Error processing gRPC hub event", e);
            throw e;
        }
    }
}