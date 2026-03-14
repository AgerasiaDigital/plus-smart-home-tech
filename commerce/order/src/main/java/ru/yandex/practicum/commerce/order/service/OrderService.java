package ru.yandex.practicum.commerce.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.interaction.dto.*;
import ru.yandex.practicum.commerce.interaction.feign.DeliveryClient;
import ru.yandex.practicum.commerce.interaction.feign.PaymentClient;
import ru.yandex.practicum.commerce.interaction.feign.WarehouseClient;
import ru.yandex.practicum.commerce.order.model.Order;
import ru.yandex.practicum.commerce.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;
    private final WarehouseClient warehouseClient;
    private final DeliveryClient deliveryClient;
    private final PaymentClient paymentClient;

    public List<OrderDto> getClientOrders(String username) {
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Username must not be empty");
        }
        return repository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDto createNewOrder(CreateNewOrderRequest request) {
        ShoppingCartDto cart = request.getShoppingCart();

        Order order = new Order();
        order.setShoppingCartId(cart.getShoppingCartId());
        order.setProducts(new java.util.HashMap<>(cart.getProducts()));
        order.setState(OrderState.NEW);
        order = repository.save(order);

        AssemblyProductsForOrderRequest assemblyRequest = AssemblyProductsForOrderRequest.builder()
                .orderId(order.getOrderId())
                .products(cart.getProducts())
                .build();

        BookedProductsDto booked = warehouseClient.assemblyProductForOrderFromShoppingCart(assemblyRequest);

        order.setDeliveryWeight(booked.getDeliveryWeight());
        order.setDeliveryVolume(booked.getDeliveryVolume());
        order.setFragile(booked.isFragile());

        AddressDto warehouseAddress = warehouseClient.getWarehouseAddress();

        DeliveryDto deliveryDto = DeliveryDto.builder()
                .fromAddress(warehouseAddress)
                .toAddress(request.getDeliveryAddress())
                .orderId(order.getOrderId())
                .deliveryState(DeliveryState.CREATED)
                .build();

        DeliveryDto savedDelivery = deliveryClient.planDelivery(deliveryDto);
        order.setDeliveryId(savedDelivery.getDeliveryId());

        BigDecimal deliveryCost = deliveryClient.deliveryCost(toDto(order));
        order.setDeliveryPrice(deliveryCost);

        BigDecimal productCost = paymentClient.productCost(toDto(order));
        order.setProductPrice(productCost);

        BigDecimal totalCost = paymentClient.getTotalCost(toDto(order));
        order.setTotalPrice(totalCost);

        order.setState(OrderState.ON_PAYMENT);
        order = repository.save(order);

        return toDto(order);
    }

    @Transactional
    public OrderDto payment(UUID orderId) {
        Order order = getOrder(orderId);
        PaymentDto paymentDto = paymentClient.payment(toDto(order));
        order.setPaymentId(paymentDto.getPaymentId());
        order.setState(OrderState.ON_PAYMENT);
        return toDto(repository.save(order));
    }

    @Transactional
    public OrderDto paymentFailed(UUID orderId) {
        Order order = getOrder(orderId);
        order.setState(OrderState.PAYMENT_FAILED);
        return toDto(repository.save(order));
    }

    @Transactional
    public OrderDto delivery(UUID orderId) {
        Order order = getOrder(orderId);
        order.setState(OrderState.DELIVERED);
        return toDto(repository.save(order));
    }

    @Transactional
    public OrderDto deliveryFailed(UUID orderId) {
        Order order = getOrder(orderId);
        order.setState(OrderState.DELIVERY_FAILED);
        return toDto(repository.save(order));
    }

    @Transactional
    public OrderDto assembly(UUID orderId) {
        Order order = getOrder(orderId);
        order.setState(OrderState.ASSEMBLED);
        return toDto(repository.save(order));
    }

    @Transactional
    public OrderDto assemblyFailed(UUID orderId) {
        Order order = getOrder(orderId);
        order.setState(OrderState.ASSEMBLY_FAILED);
        return toDto(repository.save(order));
    }

    @Transactional
    public OrderDto complete(UUID orderId) {
        Order order = getOrder(orderId);
        order.setState(OrderState.COMPLETED);
        return toDto(repository.save(order));
    }

    @Transactional
    public OrderDto calculateDeliveryCost(UUID orderId) {
        Order order = getOrder(orderId);
        BigDecimal cost = deliveryClient.deliveryCost(toDto(order));
        order.setDeliveryPrice(cost);
        return toDto(repository.save(order));
    }

    @Transactional
    public OrderDto calculateTotalCost(UUID orderId) {
        Order order = getOrder(orderId);
        BigDecimal total = paymentClient.getTotalCost(toDto(order));
        order.setTotalPrice(total);
        return toDto(repository.save(order));
    }

    @Transactional
    public OrderDto productReturn(ProductReturnRequest request) {
        Order order = getOrder(request.getOrderId());
        order.setState(OrderState.PRODUCT_RETURNED);
        repository.save(order);
        warehouseClient.returnProducts(request.getProducts());
        return toDto(order);
    }

    private Order getOrder(UUID orderId) {
        return repository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    private OrderDto toDto(Order o) {
        return OrderDto.builder()
                .orderId(o.getOrderId())
                .shoppingCartId(o.getShoppingCartId())
                .products(o.getProducts())
                .paymentId(o.getPaymentId())
                .deliveryId(o.getDeliveryId())
                .state(o.getState())
                .deliveryWeight(o.getDeliveryWeight())
                .deliveryVolume(o.getDeliveryVolume())
                .fragile(o.getFragile())
                .totalPrice(o.getTotalPrice())
                .deliveryPrice(o.getDeliveryPrice())
                .productPrice(o.getProductPrice())
                .build();
    }
}