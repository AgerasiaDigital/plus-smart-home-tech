package ru.yandex.practicum.commerce.delivery.mapper;

import ru.yandex.practicum.commerce.delivery.model.Delivery;
import ru.yandex.practicum.commerce.delivery.model.DeliveryAddress;
import ru.yandex.practicum.commerce.interaction.dto.AddressDto;
import ru.yandex.practicum.commerce.interaction.dto.DeliveryDto;

public class DeliveryMapper {

    private DeliveryMapper() {}

    public static DeliveryAddress toAddress(AddressDto dto) {
        if (dto == null) return new DeliveryAddress();
        return new DeliveryAddress(
                dto.getCountry(), dto.getCity(), dto.getStreet(), dto.getHouse(), dto.getFlat());
    }

    public static AddressDto toAddressDto(DeliveryAddress a) {
        if (a == null) return null;
        return AddressDto.builder()
                .country(a.getCountry())
                .city(a.getCity())
                .street(a.getStreet())
                .house(a.getHouse())
                .flat(a.getFlat())
                .build();
    }

    public static DeliveryDto toDto(Delivery d) {
        return DeliveryDto.builder()
                .deliveryId(d.getDeliveryId())
                .fromAddress(toAddressDto(d.getFromAddress()))
                .toAddress(toAddressDto(d.getToAddress()))
                .orderId(d.getOrderId())
                .deliveryState(d.getDeliveryState())
                .build();
    }
}