package ru.yandex.practicum.telemetry.aggregator.configuration;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Configuration
public class KafkaConfiguration {

    private final AggregatorKafkaConfig config;

    public KafkaConfiguration(AggregatorKafkaConfig config) {
        this.config = config;
    }

    @Bean
    public KafkaConsumer<String, SensorEventAvro> kafkaConsumer() {
        return new KafkaConsumer<>(config.getConsumer().getProperties());
    }

    @Bean
    public KafkaProducer<String, SpecificRecordBase> kafkaProducer() {
        return new KafkaProducer<>(config.getProducer().getProperties());
    }
}