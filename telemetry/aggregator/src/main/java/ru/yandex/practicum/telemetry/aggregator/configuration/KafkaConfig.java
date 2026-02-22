package ru.yandex.practicum.telemetry.aggregator.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Getter
@Setter
@Component
@ConfigurationProperties("aggregator.kafka")
public class KafkaConfig {
    private ConsumerConfig consumer;
    private ProducerConfig producer;

    @Getter
    @Setter
    public static class ConsumerConfig {
        private Properties properties;
        private String topic;

        public ConsumerConfig(java.util.Map<String, String> properties, String topic) {
            this.properties = new Properties();
            this.properties.putAll(properties);
            this.topic = topic;
        }
    }

    @Getter
    @Setter
    public static class ProducerConfig {
        private Properties properties;
        private String topic;

        public ProducerConfig(java.util.Map<String, String> properties, String topic) {
            this.properties = new Properties();
            this.properties.putAll(properties);
            this.topic = topic;
        }
    }
}