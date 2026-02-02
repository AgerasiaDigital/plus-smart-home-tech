package ru.yandex.practicum.telemetry.analyzer.configuration;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

@Configuration
@EnableConfigurationProperties
public class KafkaConfiguration {

    private final KafkaConfig kafkaConfig;

    public KafkaConfiguration(KafkaConfig kafkaConfig) {
        this.kafkaConfig = kafkaConfig;
    }

    @Bean
    public KafkaConsumer<String, SensorsSnapshotAvro> snapshotsConsumer() {
        return new KafkaConsumer<>(kafkaConfig.getSnapshotsConsumer().getProperties());
    }

    @Bean
    public KafkaConsumer<String, HubEventAvro> hubsConsumer() {
        return new KafkaConsumer<>(kafkaConfig.getHubsConsumer().getProperties());
    }

    @Bean
    public String snapshotsTopic() {
        return kafkaConfig.getSnapshotsConsumer().getTopic();
    }

    @Bean
    public String hubsTopic() {
        return kafkaConfig.getHubsConsumer().getTopic();
    }
}