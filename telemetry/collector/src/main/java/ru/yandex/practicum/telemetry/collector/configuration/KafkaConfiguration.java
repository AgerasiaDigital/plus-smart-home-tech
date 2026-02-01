package ru.yandex.practicum.telemetry.collector.configuration;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
public class KafkaConfiguration {

    private final KafkaConfig kafkaConfig;

    public KafkaConfiguration(KafkaConfig kafkaConfig) {
        this.kafkaConfig = kafkaConfig;
    }

    @Bean
    public KafkaProducer<String, SpecificRecordBase> kafkaProducer() {
        return new KafkaProducer<>(kafkaConfig.getProducer().getPropertiesAsProperties());
    }

    public String getSensorsTopic() {
        return kafkaConfig.getProducer().getTopic(TopicType.SENSORS_EVENTS);
    }

    public String getHubsTopic() {
        return kafkaConfig.getProducer().getTopic(TopicType.HUBS_EVENTS);
    }
}