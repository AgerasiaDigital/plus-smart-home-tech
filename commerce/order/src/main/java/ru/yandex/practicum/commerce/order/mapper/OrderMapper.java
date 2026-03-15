package ru.yandex.practicum.commerce.order.mapper;

import ru.yandex.practicum.commerce.interaction.dto.OrderDto;
import ru.yandex.practicum.commerce.order.model.Order;

public class OrderMapper {

    private OrderMapper() {}

    public static OrderDto toDto(Order o) {
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