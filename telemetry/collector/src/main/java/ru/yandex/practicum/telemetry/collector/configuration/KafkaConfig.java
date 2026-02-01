package ru.yandex.practicum.telemetry.collector.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

@Getter
@Setter
@Component
@ConfigurationProperties("collector.kafka")
public class KafkaConfig {
    private ProducerConfig producer;

    @Getter
    @Setter
    public static class ProducerConfig {
        private Properties properties;
        private EnumMap<TopicType, String> topics = new EnumMap<>(TopicType.class);

        public void setProperties(Map<String, String> props) {
            this.properties = new Properties();
            this.properties.putAll(props);
        }

        public void setTopics(Map<String, String> topicsMap) {
            for (Map.Entry<String, String> entry : topicsMap.entrySet()) {
                this.topics.put(TopicType.from(entry.getKey()), entry.getValue());
            }
        }

        public String getTopic(TopicType type) {
            return topics.get(type);
        }
    }
}