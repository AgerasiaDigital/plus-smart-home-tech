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
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");

    private final PaymentRepository repository;
    private final ShoppingStoreClient shoppingStoreClient;
    private final OrderClient orderClient;

    public BigDecimal productCost(OrderDto order) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<UUID, Long> entry : order.getProducts().entrySet()) {
            ProductDto product = shoppingStoreClient.getProduct(entry.getKey());
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(entry.getValue()));
            total = total.add(lineTotal);
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
        payment.setPaymentState(PaymentState.SUCCESS);
        repository.save(payment);
        orderClient.payment(payment.getOrderId());
    }

    @Transactional
    public void paymentFailed(UUID paymentId) {
        Payment payment = getPayment(paymentId);
        payment.setPaymentState(PaymentState.FAILED);
        repository.save(payment);
        orderClient.paymentFailed(payment.getOrderId());
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