package ru.yandex.practicum.commerce.store.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ProductDto> createOrUpdateProduct(@RequestBody ProductDto product) {
        if (product.getProductId() == null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.createProduct(product));
        }
        return ResponseEntity.ok(service.updateProduct(product));
    }

    @DeleteMapping
    public ResponseEntity<Void> deactivateProduct(@RequestParam UUID productId) {
        service.deactivateProduct(productId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable UUID productId) {
        try {
            return ResponseEntity.ok(service.getProduct(productId));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/quantityState")
    public boolean setProductQuantityState(@RequestParam UUID productId,
                                           @RequestParam QuantityState quantityState) {
        return service.setQuantityState(productId, quantityState);
    }
}