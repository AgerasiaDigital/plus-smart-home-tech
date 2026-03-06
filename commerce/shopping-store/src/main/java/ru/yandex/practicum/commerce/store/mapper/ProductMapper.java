package ru.yandex.practicum.commerce.store.mapper;

import ru.yandex.practicum.commerce.interaction.dto.ProductDto;
import ru.yandex.practicum.commerce.store.model.Product;

public class ProductMapper {

    private ProductMapper() {}

    public static ProductDto toDto(Product p) {
        return ProductDto.builder()
                .productId(p.getProductId())
                .productName(p.getProductName())
                .description(p.getDescription())
                .imageSrc(p.getImageSrc())
                .quantityState(p.getQuantityState())
                .productState(p.getProductState())
                .productCategory(p.getProductCategory())
                .price(p.getPrice())
                .build();
    }

    public static Product toEntity(ProductDto dto) {
        Product p = new Product();
        updateEntity(dto, p);
        return p;
    }

    public static void updateEntity(ProductDto dto, Product p) {
        p.setProductName(dto.getProductName());
        p.setDescription(dto.getDescription());
        p.setImageSrc(dto.getImageSrc());
        p.setQuantityState(dto.getQuantityState());
        p.setProductState(dto.getProductState());
        p.setProductCategory(dto.getProductCategory());
        p.setPrice(dto.getPrice());
    }
}