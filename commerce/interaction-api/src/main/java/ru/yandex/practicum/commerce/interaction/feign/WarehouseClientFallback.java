package ru.yandex.practicum.commerce.interaction.feign;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.commerce.interaction.dto.AddressDto;
import ru.yandex.practicum.commerce.interaction.dto.BookedProductsDto;
import ru.yandex.practicum.commerce.interaction.dto.ShoppingCartDto;

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
}