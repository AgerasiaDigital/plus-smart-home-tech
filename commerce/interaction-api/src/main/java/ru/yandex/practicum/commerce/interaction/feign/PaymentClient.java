package ru.yandex.practicum.commerce.interaction.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.interaction.dto.OrderDto;
import ru.yandex.practicum.commerce.interaction.dto.PaymentDto;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "payment")
public interface PaymentClient {

    @PostMapping("/api/v1/payment")
    PaymentDto payment(@RequestBody OrderDto order);

    @PostMapping("/api/v1/payment/productCost")
    BigDecimal productCost(@RequestBody OrderDto order);

    @PostMapping("/api/v1/payment/totalCost")
    BigDecimal getTotalCost(@RequestBody OrderDto order);

    @PostMapping("/api/v1/payment/refund")
    void refund(@RequestBody UUID paymentId);

    @PostMapping("/api/v1/payment/failed")
    void paymentFailed(@RequestBody UUID paymentId);
}
