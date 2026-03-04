package ru.yandex.practicum.commerce.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.interaction.dto.AddressDto;
import ru.yandex.practicum.commerce.interaction.dto.BookedProductsDto;
import ru.yandex.practicum.commerce.interaction.dto.ShoppingCartDto;
import ru.yandex.practicum.commerce.interaction.feign.WarehouseClient;
import ru.yandex.practicum.commerce.warehouse.dto.NewProductInWarehouseRequest;
import ru.yandex.practicum.commerce.warehouse.service.WarehouseService;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseController implements WarehouseClient {

    private final WarehouseService service;

    @Override
    @PostMapping("/check")
    public BookedProductsDto checkProductsAvailability(@RequestBody ShoppingCartDto cart) {
        return service.checkAvailability(cart);
    }

    @Override
    @GetMapping("/address")
    public AddressDto getWarehouseAddress() {
        return service.getWarehouseAddress();
    }

    @PutMapping
    public ru.yandex.practicum.commerce.warehouse.model.WarehouseProduct newProductInWarehouse(
            @RequestBody NewProductInWarehouseRequest request) {
        return service.newProduct(request);
    }

    @PostMapping("/add")
    public ru.yandex.practicum.commerce.warehouse.model.WarehouseProduct addProductToWarehouse(
            @RequestBody NewProductInWarehouseRequest request) {
        return service.addProduct(request);
    }
}