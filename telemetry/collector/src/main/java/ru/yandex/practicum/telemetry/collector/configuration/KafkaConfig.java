package ru.yandex.practicum.telemetry.collector.configuration;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

@Getter
@Component
public class KafkaConfig {
    private ProducerConfig producer;

    public void setProducer(ProducerConfig producer) {
        this.producer = producer;
    }

    @Getter
    public static class ProducerConfig {
        private final Properties properties;
        private final EnumMap<TopicType, String> topics = new EnumMap<>(TopicType.class);

        public ProducerConfig(Map<String, String> properties, Map<String, String> topics) {
            this.properties = new Properties();
            this.properties.putAll(properties);

            for (Map.Entry<String, String> entry : topics.entrySet()) {
                this.topics.put(TopicType.from(entry.getKey()), entry.getValue());
            }
        }

        public String getTopic(TopicType type) {
            return topics.get(type);
        }
    }
}