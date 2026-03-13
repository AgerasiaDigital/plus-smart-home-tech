package ru.yandex.practicum.commerce.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class ChangeQuantityRequest {
    @NotNull
    private UUID productId;

    @Positive
    private long newQuantity;
}