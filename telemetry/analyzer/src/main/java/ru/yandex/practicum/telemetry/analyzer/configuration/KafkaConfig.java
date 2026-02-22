package ru.yandex.practicum.telemetry.analyzer.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;

@Getter
@Setter
@Component
@ConfigurationProperties("analyzer.kafka")
public class KafkaConfig {
    private ConsumerConfig snapshotConsumer;
    private ConsumerConfig hubEventConsumer;
    private long pollTimeoutMs;

    @Getter
    @Setter
    public static class ConsumerConfig {
        private Properties properties;
        private String topic;

        public ConsumerConfig(Map<String, String> properties, String topic) {
            this.properties = new Properties();
            this.properties.putAll(properties);
            this.topic = topic;
        }
    }
}