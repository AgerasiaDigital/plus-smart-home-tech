package ru.yandex.practicum.commerce.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    private UUID paymentId;
    private BigDecimal productPrice;
    private BigDecimal deliveryPrice;
    private BigDecimal totalPrice;
    private PaymentState paymentState;
}