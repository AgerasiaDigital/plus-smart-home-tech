package ru.yandex.practicum.commerce.delivery.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.delivery.mapper.DeliveryMapper;
import ru.yandex.practicum.commerce.delivery.model.Delivery;
import ru.yandex.practicum.commerce.delivery.repository.DeliveryRepository;
import ru.yandex.practicum.commerce.interaction.dto.DeliveryDto;
import ru.yandex.practicum.commerce.interaction.dto.DeliveryState;
import ru.yandex.practicum.commerce.interaction.dto.OrderDto;
import ru.yandex.practicum.commerce.interaction.dto.ShippedToDeliveryRequest;
import ru.yandex.practicum.commerce.interaction.feign.OrderClient;
import ru.yandex.practicum.commerce.interaction.feign.WarehouseClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private static final BigDecimal BASE_RATE_ADDRESS_1 = new BigDecimal("5.00");
    private static final BigDecimal ADDRESS_2_MULTIPLIER = new BigDecimal("2");
    private static final BigDecimal FIXED_SURCHARGE = new BigDecimal("5.00");
    private static final BigDecimal FRAGILE_RATE = new BigDecimal("0.20");
    private static final BigDecimal WEIGHT_RATE = new BigDecimal("0.30");
    private static final BigDecimal VOLUME_RATE = new BigDecimal("0.20");
    private static final BigDecimal DIFFERENT_STREET_RATE = new BigDecimal("0.20");

    private final DeliveryRepository repository;
    private final OrderClient orderClient;
    private final WarehouseClient warehouseClient;

    @Transactional
    public DeliveryDto planDelivery(DeliveryDto dto) {
        Delivery delivery = new Delivery();
        delivery.setFromAddress(DeliveryMapper.toAddress(dto.getFromAddress()));
        delivery.setToAddress(DeliveryMapper.toAddress(dto.getToAddress()));
        delivery.setOrderId(dto.getOrderId());
        delivery.setDeliveryState(DeliveryState.CREATED);
        delivery = repository.save(delivery);
        return DeliveryMapper.toDto(delivery);
    }

    public BigDecimal deliveryCost(OrderDto order) {
        Delivery delivery = repository.findByOrderId(order.getOrderId())
                .orElseThrow(() -> new RuntimeException(
                        "Delivery not found for order: " + order.getOrderId()));

        String warehouseName = delivery.getFromAddress().getCountry();

        BigDecimal base = BASE_RATE_ADDRESS_1;

        if (warehouseName != null && warehouseName.contains("ADDRESS_2")) {
            base = base.multiply(ADDRESS_2_MULTIPLIER);
        }
        base = base.add(FIXED_SURCHARGE);

        if (Boolean.TRUE.equals(order.getFragile())) {
            base = base.add(base.multiply(FRAGILE_RATE));
        }

        BigDecimal weight = order.getDeliveryWeight() != null
                ? BigDecimal.valueOf(order.getDeliveryWeight())
                : BigDecimal.ZERO;
        base = base.add(weight.multiply(WEIGHT_RATE));

        BigDecimal volume = order.getDeliveryVolume() != null
                ? BigDecimal.valueOf(order.getDeliveryVolume())
                : BigDecimal.ZERO;
        base = base.add(volume.multiply(VOLUME_RATE));

        String warehouseStreet = delivery.getFromAddress().getStreet();
        String deliveryStreet = delivery.getToAddress().getStreet();
        boolean sameStreet = warehouseStreet != null && warehouseStreet.equals(deliveryStreet);
        if (!sameStreet) {
            base = base.add(base.multiply(DIFFERENT_STREET_RATE));
        }

        return base.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void deliveryPicked(UUID orderId) {
        Delivery delivery = getByOrderId(orderId);
        delivery.setDeliveryState(DeliveryState.IN_PROGRESS);
        repository.save(delivery);

        orderClient.assembly(orderId);

        ShippedToDeliveryRequest req = ShippedToDeliveryRequest.builder()
                .orderId(orderId)
                .deliveryId(delivery.getDeliveryId())
                .build();
        warehouseClient.shippedToDelivery(req);
    }

    @Transactional
    public void deliverySuccessful(UUID orderId) {
        Delivery delivery = getByOrderId(orderId);
        delivery.setDeliveryState(DeliveryState.DELIVERED);
        repository.save(delivery);
        orderClient.delivery(orderId);
    }

    @Transactional
    public void deliveryFailed(UUID orderId) {
        Delivery delivery = getByOrderId(orderId);
        delivery.setDeliveryState(DeliveryState.FAILED);
        repository.save(delivery);
        orderClient.deliveryFailed(orderId);
    }

    private Delivery getByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "Delivery not found for order: " + orderId));
    }
}