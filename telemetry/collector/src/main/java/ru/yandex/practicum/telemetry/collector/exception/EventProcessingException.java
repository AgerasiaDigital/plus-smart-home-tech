package ru.yandex.practicum.telemetry.collector.exception;

public class EventProcessingException extends RuntimeException {

    public EventProcessingException(String message) {
        super(message);
    }

    public EventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}