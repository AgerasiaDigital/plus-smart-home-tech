package ru.yandex.practicum.commerce.interaction.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.interaction.dto.OrderDto;
import ru.yandex.practicum.commerce.interaction.dto.ProductReturnRequest;

import java.util.UUID;

@FeignClient(name = "order")
public interface OrderClient {

    @PostMapping("/api/v1/order/payment")
    OrderDto payment(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/payment/failed")
    OrderDto paymentFailed(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/delivery")
    OrderDto delivery(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/delivery/failed")
    OrderDto deliveryFailed(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/assembly")
    OrderDto assembly(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/assembly/failed")
    OrderDto assemblyFailed(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/completed")
    OrderDto complete(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/return")
    OrderDto productReturn(@RequestBody ProductReturnRequest request);

    @PostMapping("/api/v1/order/calculate/total")
    OrderDto calculateTotalCost(@RequestBody UUID orderId);

    @PostMapping("/api/v1/order/calculate/delivery")
    OrderDto calculateDeliveryCost(@RequestBody UUID orderId);
}