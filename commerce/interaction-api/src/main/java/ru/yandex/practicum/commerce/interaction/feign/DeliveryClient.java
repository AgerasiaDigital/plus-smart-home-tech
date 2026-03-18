package ru.yandex.practicum.commerce.interaction.feign;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.interaction.dto.DeliveryDto;
import ru.yandex.practicum.commerce.interaction.dto.OrderDto;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "delivery")
@Validated
public interface DeliveryClient {

    @PutMapping("/api/v1/delivery")
    DeliveryDto planDelivery(@RequestBody @Valid DeliveryDto delivery);

    @PostMapping("/api/v1/delivery/cost")
    BigDecimal deliveryCost(@RequestBody @Valid OrderDto order);

    @PostMapping("/api/v1/delivery/picked")
    void deliveryPicked(@RequestBody @NotNull UUID orderId);

    @PostMapping("/api/v1/delivery/successful")
    void deliverySuccessful(@RequestBody @NotNull UUID orderId);

    @PostMapping("/api/v1/delivery/failed")
    void deliveryFailed(@RequestBody @NotNull UUID orderId);
}