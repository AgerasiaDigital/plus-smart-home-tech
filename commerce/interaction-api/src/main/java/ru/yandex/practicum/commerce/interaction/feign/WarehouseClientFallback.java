package ru.yandex.practicum.commerce.interaction.feign;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.commerce.interaction.dto.AddressDto;
import ru.yandex.practicum.commerce.interaction.dto.BookedProductsDto;
import ru.yandex.practicum.commerce.interaction.dto.ShippedToDeliveryRequest;
import ru.yandex.practicum.commerce.interaction.dto.ShoppingCartDto;

import java.util.Map;
import java.util.UUID;

@Component
public class WarehouseClientFallback implements WarehouseClient {

    @Override
    public BookedProductsDto checkProductsAvailability(ShoppingCartDto cart) {
        throw new RuntimeException("Сервис склада временно недоступен. Пожалуйста, попробуйте позже.");
    }

    @Override
    public AddressDto getWarehouseAddress() {
        throw new RuntimeException("Сервис склада временно недоступен. Пожалуйста, попробуйте позже.");
    }

    @Override
    public BookedProductsDto assemblyProductForOrderFromShoppingCart(ShoppingCartDto cart) {
        throw new RuntimeException("Сервис склада временно недоступен. Пожалуйста, попробуйте позже.");
    }

    @Override
    public void shippedToDelivery(ShippedToDeliveryRequest request) {
        throw new RuntimeException("Сервис склада временно недоступен. Пожалуйста, попробуйте позже.");
    }

    @Override
    public void returnProducts(Map<UUID, Long> products) {
        throw new RuntimeException("Сервис склада временно недоступен. Пожалуйста, попробуйте позже.");
    }
}