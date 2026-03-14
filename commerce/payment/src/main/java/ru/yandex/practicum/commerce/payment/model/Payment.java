package ru.yandex.practicum.commerce.payment.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.yandex.practicum.commerce.interaction.dto.PaymentState;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payments", schema = "payment")
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID paymentId;

    private UUID orderId;

    private BigDecimal productPrice;
    private BigDecimal deliveryPrice;
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private PaymentState paymentState;
}