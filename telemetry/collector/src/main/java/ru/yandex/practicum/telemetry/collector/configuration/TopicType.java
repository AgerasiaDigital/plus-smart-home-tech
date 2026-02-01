package ru.yandex.practicum.telemetry.collector.configuration;

public enum TopicType {
    SENSORS_EVENTS("sensors-events"),
    HUBS_EVENTS("hubs-events");

    private final String key;

    TopicType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static TopicType from(String key) {
        for (TopicType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown topic type: " + key);
    }
}