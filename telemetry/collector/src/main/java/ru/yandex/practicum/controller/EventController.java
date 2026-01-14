package ru.yandex.practicum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.model.hub.HubEvent;
import ru.yandex.practicum.model.sensor.SensorEvent;
import ru.yandex.practicum.service.EventService;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "events", description = "API для передачи событий от датчиков и хабов")
public class EventController {
    private final EventService eventService;

    @PostMapping("/sensors")
    @Operation(
            summary = "Обработчик событий датчиков",
            description = "Эндпоинт для обработки событий от датчиков"
    )
    public void collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        log.info("Received sensor event: {}", event);
        eventService.collectSensorEvent(event);
    }

    @PostMapping("/hubs")
    @Operation(
            summary = "Обработчик событий хабов",
            description = "Эндпоинт для обработки событий от хаба"
    )
    public void collectHubEvent(@Valid @RequestBody HubEvent event) {
        log.info("Received hub event: {}", event);
        eventService.collectHubEvent(event);
    }
}