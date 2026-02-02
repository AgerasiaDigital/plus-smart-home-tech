package ru.yandex.practicum.telemetry.aggregator.configuration;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Configuration
@EnableConfigurationProperties
public class KafkaConfiguration {

    private final KafkaConfig kafkaConfig;

    public KafkaConfiguration(KafkaConfig kafkaConfig) {
        this.kafkaConfig = kafkaConfig;
    }

    @Bean
    public KafkaConsumer<String, SensorEventAvro> kafkaConsumer() {
        return new KafkaConsumer<>(kafkaConfig.getConsumer().getProperties());
    }

    @Bean
    public KafkaProducer<String, SpecificRecordBase> kafkaProducer() {
        return new KafkaProducer<>(kafkaConfig.getProducer().getProperties());
    }

    @Bean
    public String sensorsTopicIn() {
        return kafkaConfig.getConsumer().getTopic();
    }

    @Bean
    public String snapshotsTopicOut() {
        return kafkaConfig.getProducer().getTopic();
    }
}