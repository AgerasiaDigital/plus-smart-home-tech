package ru.yandex.practicum.telemetry.aggregator.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;

@Getter
@Setter
@Component
@ConfigurationProperties("aggregator.kafka")
public class AggregatorKafkaConfig {
    private ConsumerConfig consumer;
    private ProducerConfig producer;

    @Getter
    @Setter
    public static class ConsumerConfig {
        private Properties properties;
        private String topic;

        public void setProperties(Map<String, String> props) {
            this.properties = new Properties();
            this.properties.putAll(props);
        }
    }

    @Getter
    @Setter
    public static class ProducerConfig {
        private Properties properties;
        private String topic;

        public void setProperties(Map<String, String> props) {
            this.properties = new Properties();
            this.properties.putAll(props);
        }
    }
}