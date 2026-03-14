package ru.yandex.practicum.commerce.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.interaction.dto.AddressDto;
import ru.yandex.practicum.commerce.interaction.dto.BookedProductsDto;
import ru.yandex.practicum.commerce.interaction.dto.ShippedToDeliveryRequest;
import ru.yandex.practicum.commerce.interaction.dto.ShoppingCartDto;
import ru.yandex.practicum.commerce.warehouse.dto.NewProductInWarehouseRequest;
import ru.yandex.practicum.commerce.warehouse.model.WarehouseProduct;
import ru.yandex.practicum.commerce.warehouse.service.WarehouseService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService service;

    @PostMapping("/check")
    public BookedProductsDto checkProductsAvailability(@RequestBody ShoppingCartDto cart) {
        return service.checkAvailability(cart);
    }

    @GetMapping("/address")
    public AddressDto getWarehouseAddress() {
        return service.getWarehouseAddress();
    }

    @PutMapping
    public WarehouseProduct newProductInWarehouse(@RequestBody NewProductInWarehouseRequest request) {
        return service.newProduct(request);
    }

    @PostMapping("/add")
    public WarehouseProduct addProductToWarehouse(@RequestBody NewProductInWarehouseRequest request) {
        return service.addProduct(request);
    }

    @PostMapping("/assembly")
    public BookedProductsDto assemblyProductForOrderFromShoppingCart(@RequestBody ShoppingCartDto cart) {
        return service.assemblyProductForOrder(cart);
    }

    @PostMapping("/shipped")
    public void shippedToDelivery(@RequestBody ShippedToDeliveryRequest request) {
        service.shippedToDelivery(request);
    }

    @PostMapping("/return")
    public void returnProducts(@RequestBody Map<UUID, Long> products) {
        service.returnProducts(products);
    }
}