package ru.yandex.practicum.commerce.cart.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ChangeQuantityRequest {
    private UUID productId;
    private long newQuantity;
}