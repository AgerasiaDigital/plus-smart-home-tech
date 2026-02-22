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
    public KafkaConsumer<String, SensorsSnapshotAvro> snapshotConsumer() {
        return new KafkaConsumer<>(kafkaConfig.getSnapshotConsumer().getProperties());
    }

    @Bean
    public KafkaConsumer<String, HubEventAvro> hubEventConsumer() {
        return new KafkaConsumer<>(kafkaConfig.getHubEventConsumer().getProperties());
    }

    @Bean
    public String snapshotTopic() {
        return kafkaConfig.getSnapshotConsumer().getTopic();
    }

    @Bean
    public String hubEventTopic() {
        return kafkaConfig.getHubEventConsumer().getTopic();
    }
}