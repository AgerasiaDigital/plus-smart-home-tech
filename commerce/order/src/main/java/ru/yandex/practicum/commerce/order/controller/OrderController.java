
package ru.yandex.practicum.commerce.order.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.interaction.dto.CreateNewOrderRequest;
import ru.yandex.practicum.commerce.interaction.dto.OrderDto;
import ru.yandex.practicum.commerce.interaction.dto.ProductReturnRequest;
import ru.yandex.practicum.commerce.order.service.OrderService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @GetMapping
    public List<OrderDto> getClientOrders(@RequestParam String username) {
        return service.getClientOrders(username);
    }

    @PutMapping
    public OrderDto createNewOrder(@RequestBody CreateNewOrderRequest request) {
        return service.createNewOrder(request);
    }

    @PostMapping("/return")
    public OrderDto productReturn(@RequestBody ProductReturnRequest request) {
        return service.productReturn(request);
    }

    @PostMapping("/payment")
    public OrderDto payment(@RequestBody UUID orderId) {
        return service.payment(orderId);
    }

    @PostMapping("/payment/failed")
    public OrderDto paymentFailed(@RequestBody UUID orderId) {
        return service.paymentFailed(orderId);
    }

    @PostMapping("/delivery")
    public OrderDto delivery(@RequestBody UUID orderId) {
        return service.delivery(orderId);
    }

    @PostMapping("/delivery/failed")
    public OrderDto deliveryFailed(@RequestBody UUID orderId) {
        return service.deliveryFailed(orderId);
    }

    @PostMapping("/completed")
    public OrderDto complete(@RequestBody UUID orderId) {
        return service.complete(orderId);
    }

    @PostMapping("/calculate/total")
    public OrderDto calculateTotalCost(@RequestBody UUID orderId) {
        return service.calculateTotalCost(orderId);
    }

    @PostMapping("/calculate/delivery")
    public OrderDto calculateDeliveryCost(@RequestBody UUID orderId) {
        return service.calculateDeliveryCost(orderId);
    }

    @PostMapping("/assembly")
    public OrderDto assembly(@RequestBody UUID orderId) {
        return service.assembly(orderId);
    }

    @PostMapping("/assembly/failed")
    public OrderDto assemblyFailed(@RequestBody UUID orderId) {
        return service.assemblyFailed(orderId);
    }
}