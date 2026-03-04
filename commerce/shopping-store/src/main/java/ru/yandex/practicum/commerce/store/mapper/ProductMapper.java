package ru.yandex.practicum.commerce.store.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.commerce.interaction.dto.ProductDto;
import ru.yandex.practicum.commerce.store.model.Product;

@Component
public class ProductMapper {

    public ProductDto toDto(Product p) {
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

    public Product toEntity(ProductDto dto) {
        Product p = new Product();
        updateEntity(dto, p);
        return p;
    }

    public void updateEntity(ProductDto dto, Product p) {
        p.setProductName(dto.getProductName());
        p.setDescription(dto.getDescription());
        p.setImageSrc(dto.getImageSrc());
        p.setQuantityState(dto.getQuantityState());
        p.setProductState(dto.getProductState());
        p.setProductCategory(dto.getProductCategory());
        p.setPrice(dto.getPrice());
    }
}
