package ru.yandex.practicum.telemetry.collector.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
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
        private Map<String, String> properties = new HashMap<>();
        private Map<String, String> topics = new HashMap<>();

        private EnumMap<TopicType, String> topicsEnum = new EnumMap<>(TopicType.class);

        public Properties getPropertiesAsProperties() {
            Properties props = new Properties();
            props.putAll(properties);
            return props;
        }

        public void setTopics(Map<String, String> topicsMap) {
            this.topics = topicsMap;
            this.topicsEnum.clear();
            for (Map.Entry<String, String> entry : topicsMap.entrySet()) {
                this.topicsEnum.put(TopicType.from(entry.getKey()), entry.getValue());
            }
        }

        public String getTopic(TopicType type) {
            return topicsEnum.get(type);
        }
    }
}