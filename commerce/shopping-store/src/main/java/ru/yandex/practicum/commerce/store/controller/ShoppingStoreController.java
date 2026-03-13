package ru.yandex.practicum.commerce.store.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.interaction.dto.ProductCategory;
import ru.yandex.practicum.commerce.interaction.dto.ProductDto;
import ru.yandex.practicum.commerce.interaction.dto.QuantityState;
import ru.yandex.practicum.commerce.store.service.ProductService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-store")
@RequiredArgsConstructor
public class ShoppingStoreController {

    private final ProductService service;

    @GetMapping
    public Page<ProductDto> getProducts(@RequestParam ProductCategory category, Pageable pageable) {
        return service.getProducts(category, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createProduct(@RequestBody ProductDto product) {
        return service.createProduct(product);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createOrUpdateProduct(@RequestBody ProductDto product) {
        if (product.getProductId() == null) {
            return service.createProduct(product);
        }
        return service.createOrUpdate(product);
    }

    @DeleteMapping("/{productId}")
    public ProductDto deactivateProductByPath(@PathVariable UUID productId) {
        return service.deactivateProduct(productId);
    }

    @PostMapping("/removeProductFromStore")
    public ProductDto removeProductFromStore(@RequestBody UUID productId) {
        return service.deactivateProduct(productId);
    }

    @GetMapping("/{productId}")
    public ProductDto getProduct(@PathVariable UUID productId) {
        return service.getProduct(productId);
    }

    @PostMapping("/quantityState")
    public boolean setProductQuantityState(@RequestParam UUID productId,
                                           @RequestParam QuantityState quantityState) {
        return service.setQuantityState(productId, quantityState);
    }
}