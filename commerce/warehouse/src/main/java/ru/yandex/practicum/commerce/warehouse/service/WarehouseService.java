package ru.yandex.practicum.commerce.warehouse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.interaction.dto.AddressDto;
import ru.yandex.practicum.commerce.interaction.dto.BookedProductsDto;
import ru.yandex.practicum.commerce.interaction.dto.ShoppingCartDto;
import ru.yandex.practicum.commerce.warehouse.model.WarehouseProduct;
import ru.yandex.practicum.commerce.warehouse.repository.WarehouseProductRepository;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseProductRepository repository;

    private static final String[] ADDRESSES = new String[]{"ADDRESS_1", "ADDRESS_2"};
    private static final String CURRENT_ADDRESS =
            ADDRESSES[new SecureRandom().nextInt(ADDRESSES.length)];

    @Transactional
    public WarehouseProduct addProduct(UUID productId, long quantity, double width,
                                       double height, double depth, double weight, boolean fragile) {
        WarehouseProduct product = repository.findById(productId).orElseGet(() -> {
            WarehouseProduct p = new WarehouseProduct();
            p.setProductId(productId);
            p.setWidth(width);
            p.setHeight(height);
            p.setDepth(depth);
            p.setWeight(weight);
            p.setFragile(fragile);
            p.setQuantity(0);
            return p;
        });
        product.setQuantity(product.getQuantity() + quantity);
        return repository.save(product);
    }

    public BookedProductsDto checkAvailability(ShoppingCartDto cart) {
        Map<UUID, Long> requested = cart.getProducts();
        double totalWeight = 0;
        double totalVolume = 0;
        boolean fragile = false;

        for (Map.Entry<UUID, Long> entry : requested.entrySet()) {
            UUID productId = entry.getKey();
            long requestedQty = entry.getValue();

            WarehouseProduct wp = repository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found in warehouse: " + productId));

            if (wp.getQuantity() < requestedQty) {
                throw new RuntimeException("Not enough quantity for product: " + productId
                        + ". Available: " + wp.getQuantity() + ", requested: " + requestedQty);
            }

            totalWeight += wp.getWeight() * requestedQty;
            totalVolume += wp.getWidth() * wp.getHeight() * wp.getDepth() * requestedQty;
            if (wp.isFragile()) fragile = true;
        }

        return BookedProductsDto.builder()
                .products(requested)
                .deliveryWeight(totalWeight)
                .deliveryVolume(totalVolume)
                .fragile(fragile)
                .build();
    }

    public AddressDto getWarehouseAddress() {
        return AddressDto.builder()
                .country(CURRENT_ADDRESS)
                .city(CURRENT_ADDRESS)
                .street(CURRENT_ADDRESS)
                .house(CURRENT_ADDRESS)
                .flat(CURRENT_ADDRESS)
                .build();
    }
}