package ru.yandex.practicum.commerce.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.interaction.dto.ProductCategory;
import ru.yandex.practicum.commerce.interaction.dto.ProductDto;
import ru.yandex.practicum.commerce.interaction.dto.ProductState;
import ru.yandex.practicum.commerce.interaction.dto.QuantityState;
import ru.yandex.practicum.commerce.store.mapper.ProductMapper;
import ru.yandex.practicum.commerce.store.model.Product;
import ru.yandex.practicum.commerce.store.repository.ProductRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    public Page<ProductDto> getProducts(ProductCategory category, Pageable pageable) {
        return repository.findAllByProductCategoryAndProductState(
                category, ProductState.ACTIVE, pageable).map(mapper::toDto);
    }

    @Transactional
    public ProductDto createProduct(ProductDto dto) {
        Product product = mapper.toEntity(dto);

        if (product.getProductState() == null) {
            product.setProductState(ProductState.ACTIVE);
        }
        return mapper.toDto(repository.save(product));
    }

    @Transactional
    public ProductDto updateProduct(ProductDto dto) {
        Product product = repository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + dto.getProductId()));
        mapper.updateEntity(dto, product);
        return mapper.toDto(repository.save(product));
    }

    @Transactional
    public ProductDto deactivateProduct(UUID productId) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        product.setProductState(ProductState.DEACTIVATE);
        return mapper.toDto(repository.save(product));
    }

    public ProductDto getProduct(UUID productId) {
        return mapper.toDto(repository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId)));
    }

    @Transactional
    public boolean setQuantityState(UUID productId, QuantityState quantityState) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        product.setQuantityState(quantityState);
        repository.save(product);
        return true;
    }
}