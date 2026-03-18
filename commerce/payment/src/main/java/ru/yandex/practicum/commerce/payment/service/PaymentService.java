package ru.yandex.practicum.commerce.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.interaction.dto.*;
import ru.yandex.practicum.commerce.interaction.feign.OrderClient;
import ru.yandex.practicum.commerce.interaction.feign.ShoppingStoreClient;
import ru.yandex.practicum.commerce.payment.model.Payment;
import ru.yandex.practicum.commerce.payment.repository.PaymentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");

    private final PaymentRepository repository;
    private final ShoppingStoreClient shoppingStoreClient;
    private final OrderClient orderClient;

    public BigDecimal productCost(OrderDto order) {
        Map<UUID, Long> products = order.getProducts();
        Set<UUID> productIds = products.keySet();

        Map<UUID, BigDecimal> prices = productIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> shoppingStoreClient.getProduct(id).getPrice()
                ));

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            BigDecimal price = prices.get(entry.getKey());
            total = total.add(price.multiply(BigDecimal.valueOf(entry.getValue())));
        }
        return total;
    }

    public BigDecimal getTotalCost(OrderDto order) {
        BigDecimal productCost = order.getProductPrice() != null
                ? order.getProductPrice()
                : productCost(order);
        BigDecimal tax = productCost.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal withTax = productCost.add(tax);
        BigDecimal deliveryCost = order.getDeliveryPrice() != null ? order.getDeliveryPrice() : BigDecimal.ZERO;
        return withTax.add(deliveryCost).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public PaymentDto payment(OrderDto order) {
        BigDecimal pCost = productCost(order);
        BigDecimal dCost = order.getDeliveryPrice() != null ? order.getDeliveryPrice() : BigDecimal.ZERO;
        BigDecimal total = getTotalCost(OrderDto.builder()
                .products(order.getProducts())
                .productPrice(pCost)
                .deliveryPrice(dCost)
                .build());

        Payment payment = new Payment();
        payment.setOrderId(order.getOrderId());
        payment.setProductPrice(pCost);
        payment.setDeliveryPrice(dCost);
        payment.setTotalPrice(total);
        payment.setPaymentState(PaymentState.PENDING);
        payment = repository.save(payment);

        return toDto(payment);
    }

    @Transactional
    public void refund(UUID paymentId) {
        Payment payment = getPayment(paymentId);
        // Сначала уведомляем внешний сервис, потом фиксируем статус в БД
        orderClient.payment(payment.getOrderId());
        payment.setPaymentState(PaymentState.SUCCESS);
        repository.save(payment);
    }

    @Transactional
    public void paymentFailed(UUID paymentId) {
        Payment payment = getPayment(paymentId);
        // Сначала уведомляем внешний сервис, потом фиксируем статус в БД
        orderClient.paymentFailed(payment.getOrderId());
        payment.setPaymentState(PaymentState.FAILED);
        repository.save(payment);
    }

    private Payment getPayment(UUID paymentId) {
        return repository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    private PaymentDto toDto(Payment p) {
        return PaymentDto.builder()
                .paymentId(p.getPaymentId())
                .productPrice(p.getProductPrice())
                .deliveryPrice(p.getDeliveryPrice())
                .totalPrice(p.getTotalPrice())
                .paymentState(p.getPaymentState())
                .build();
    }
}