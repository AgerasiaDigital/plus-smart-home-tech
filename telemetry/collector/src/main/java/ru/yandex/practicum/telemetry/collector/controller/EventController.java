package ru.yandex.practicum.telemetry.collector.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.service.EventService;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;

    @PostMapping("/sensors")
    public ResponseEntity<Void> collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        log.info("POST /events/sensors - Received sensor event");
        eventService.collectSensorEvent(event);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/hubs")
    public ResponseEntity<Void> collectHubEvent(@Valid @RequestBody HubEvent event) {
        log.info("POST /events/hubs - Received hub event");
        eventService.collectHubEvent(event);
        return ResponseEntity.ok().build();
    }
}