package ru.yandex.practicum.commerce.store.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.interaction.dto.ProductCategory;
import ru.yandex.practicum.commerce.interaction.dto.ProductDto;
import ru.yandex.practicum.commerce.interaction.dto.QuantityState;
import ru.yandex.practicum.commerce.interaction.feign.ShoppingStoreClient;
import ru.yandex.practicum.commerce.store.service.ProductService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-store")
@RequiredArgsConstructor
public class ShoppingStoreController implements ShoppingStoreClient {

    private final ProductService service;

    @Override
    @GetMapping
    public Page<ProductDto> getProducts(@RequestParam ProductCategory category, Pageable pageable) {
        return service.getProducts(category, pageable);
    }

    @Override
    @PostMapping
    public ProductDto createProduct(@RequestBody ProductDto product) {
        return service.createProduct(product);
    }

    @Override
    @PutMapping
    public ProductDto updateProduct(@RequestBody ProductDto product) {
        return service.updateProduct(product);
    }

    @Override
    @DeleteMapping
    public boolean deactivateProduct(@RequestParam UUID productId) {
        return service.deactivateProduct(productId);
    }

    @Override
    @GetMapping("/{productId}")
    public ProductDto getProduct(@PathVariable UUID productId) {
        return service.getProduct(productId);
    }

    @Override
    @PostMapping("/quantityState")
    public boolean setProductQuantityState(@RequestParam UUID productId,
                                           @RequestParam QuantityState quantityState) {
        return service.setQuantityState(productId, quantityState);
    }
}
