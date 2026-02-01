package ru.yandex.practicum.telemetry.analyzer.configuration;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

@Configuration
public class KafkaConfiguration {

    private final AnalyzerKafkaConfig config;

    public KafkaConfiguration(AnalyzerKafkaConfig config) {
        this.config = config;
    }

    @Bean
    public KafkaConsumer<String, SensorsSnapshotAvro> snapshotConsumer() {
        return new KafkaConsumer<>(config.getSnapshotConsumer().getProperties());
    }

    @Bean
    public KafkaConsumer<String, HubEventAvro> hubEventConsumer() {
        return new KafkaConsumer<>(config.getHubEventConsumer().getProperties());
    }
}