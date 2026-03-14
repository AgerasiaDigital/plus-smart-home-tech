package ru.yandex.practicum.commerce.delivery.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.interaction.dto.DeliveryDto;
import ru.yandex.practicum.commerce.interaction.dto.OrderDto;
import ru.yandex.practicum.commerce.delivery.service.DeliveryService;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService service;

    @PutMapping
    public DeliveryDto planDelivery(@RequestBody DeliveryDto delivery) {
        return service.planDelivery(delivery);
    }

    @PostMapping("/cost")
    public BigDecimal deliveryCost(@RequestBody OrderDto order) {
        return service.deliveryCost(order);
    }

    @PostMapping("/picked")
    public void deliveryPicked(@RequestBody UUID orderId) {
        service.deliveryPicked(orderId);
    }

    @PostMapping("/successful")
    public void deliverySuccessful(@RequestBody UUID orderId) {
        service.deliverySuccessful(orderId);
    }

    @PostMapping("/failed")
    public void deliveryFailed(@RequestBody UUID orderId) {
        service.deliveryFailed(orderId);
    }
}