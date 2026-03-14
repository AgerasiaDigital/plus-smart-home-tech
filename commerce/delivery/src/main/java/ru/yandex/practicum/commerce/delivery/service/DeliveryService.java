package ru.yandex.practicum.commerce.delivery.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.delivery.model.Delivery;
import ru.yandex.practicum.commerce.delivery.model.DeliveryAddress;
import ru.yandex.practicum.commerce.delivery.repository.DeliveryRepository;
import ru.yandex.practicum.commerce.interaction.dto.AddressDto;
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

    private final DeliveryRepository repository;
    private final OrderClient orderClient;
    private final WarehouseClient warehouseClient;

    @Transactional
    public DeliveryDto planDelivery(DeliveryDto dto) {
        Delivery delivery = new Delivery();
        delivery.setFromAddress(toAddress(dto.getFromAddress()));
        delivery.setToAddress(toAddress(dto.getToAddress()));
        delivery.setOrderId(dto.getOrderId());
        delivery.setDeliveryState(DeliveryState.CREATED);
        delivery = repository.save(delivery);
        return toDto(delivery);
    }

    public BigDecimal deliveryCost(OrderDto order) {
        Delivery delivery = repository.findByOrderId(order.getOrderId())
                .orElseThrow(() -> new RuntimeException(
                        "Delivery not found for order: " + order.getOrderId()));

        String warehouseName = delivery.getFromAddress().getCountry();

        double base = 5.0;

        if (warehouseName != null && warehouseName.contains("ADDRESS_2")) {
            base = base * 2;
        }
        base = base + 5.0;

        if (Boolean.TRUE.equals(order.getFragile())) {
            base = base + base * 0.2;
        }

        double weight = order.getDeliveryWeight() != null ? order.getDeliveryWeight() : 0.0;
        base = base + weight * 0.3;

        double volume = order.getDeliveryVolume() != null ? order.getDeliveryVolume() : 0.0;
        base = base + volume * 0.2;

        String warehouseStreet = delivery.getFromAddress().getStreet();
        String deliveryStreet = delivery.getToAddress().getStreet();
        boolean sameStreet = warehouseStreet != null && warehouseStreet.equals(deliveryStreet);
        if (!sameStreet) {
            base = base + base * 0.2;
        }

        return BigDecimal.valueOf(base).setScale(2, RoundingMode.HALF_UP);
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

    private DeliveryAddress toAddress(AddressDto dto) {
        if (dto == null) return new DeliveryAddress();
        return new DeliveryAddress(
                dto.getCountry(), dto.getCity(), dto.getStreet(), dto.getHouse(), dto.getFlat());
    }

    private DeliveryDto toDto(Delivery d) {
        return DeliveryDto.builder()
                .deliveryId(d.getDeliveryId())
                .fromAddress(toAddressDto(d.getFromAddress()))
                .toAddress(toAddressDto(d.getToAddress()))
                .orderId(d.getOrderId())
                .deliveryState(d.getDeliveryState())
                .build();
    }

    private AddressDto toAddressDto(DeliveryAddress a) {
        if (a == null) return null;
        return AddressDto.builder()
                .country(a.getCountry())
                .city(a.getCity())
                .street(a.getStreet())
                .house(a.getHouse())
                .flat(a.getFlat())
                .build();
    }
}