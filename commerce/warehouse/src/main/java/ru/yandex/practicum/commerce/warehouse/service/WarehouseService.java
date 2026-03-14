package ru.yandex.practicum.commerce.warehouse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.interaction.dto.AddressDto;
import ru.yandex.practicum.commerce.interaction.dto.BookedProductsDto;
import ru.yandex.practicum.commerce.interaction.dto.ShippedToDeliveryRequest;
import ru.yandex.practicum.commerce.interaction.dto.ShoppingCartDto;
import ru.yandex.practicum.commerce.warehouse.dto.NewProductInWarehouseRequest;
import ru.yandex.practicum.commerce.warehouse.model.OrderBooking;
import ru.yandex.practicum.commerce.warehouse.model.WarehouseProduct;
import ru.yandex.practicum.commerce.warehouse.repository.OrderBookingRepository;
import ru.yandex.practicum.commerce.warehouse.repository.WarehouseProductRepository;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseProductRepository repository;
    private final OrderBookingRepository bookingRepository;

    private static final String[] ADDRESSES = {"ADDRESS_1", "ADDRESS_2"};
    private static final String CURRENT_ADDRESS =
            ADDRESSES[new SecureRandom().nextInt(ADDRESSES.length)];

    @Transactional
    public WarehouseProduct newProduct(NewProductInWarehouseRequest req) {
        WarehouseProduct p = repository.findById(req.getProductId()).orElseGet(WarehouseProduct::new);
        p.setProductId(req.getProductId());
        p.setFragile(req.isFragile());
        p.setWeight(req.getWeight());
        if (req.getDimension() != null) {
            p.setWidth(req.getDimension().getWidth());
            p.setHeight(req.getDimension().getHeight());
            p.setDepth(req.getDimension().getDepth());
        }
        return repository.save(p);
    }

    @Transactional
    public WarehouseProduct addProduct(NewProductInWarehouseRequest req) {
        WarehouseProduct p = repository.findById(req.getProductId()).orElseGet(() -> {
            WarehouseProduct np = new WarehouseProduct();
            np.setProductId(req.getProductId());
            np.setFragile(req.isFragile());
            np.setWeight(req.getWeight());
            if (req.getDimension() != null) {
                np.setWidth(req.getDimension().getWidth());
                np.setHeight(req.getDimension().getHeight());
                np.setDepth(req.getDimension().getDepth());
            }
            np.setQuantity(0);
            return np;
        });
        long qty = req.getQuantity() > 0 ? req.getQuantity() : 100L;
        p.setQuantity(p.getQuantity() + qty);
        return repository.save(p);
    }

    public BookedProductsDto checkAvailability(ShoppingCartDto cart) {
        return buildBookedProducts(cart.getProducts(), false);
    }

    @Transactional
    public BookedProductsDto assemblyProductForOrder(ShoppingCartDto cart) {
        BookedProductsDto booked = buildBookedProducts(cart.getProducts(), true);

        OrderBooking booking = new OrderBooking();
        booking.setOrderId(cart.getShoppingCartId());
        booking.setProducts(new java.util.HashMap<>(cart.getProducts()));
        bookingRepository.save(booking);

        return booked;
    }

    @Transactional
    public void shippedToDelivery(ShippedToDeliveryRequest request) {
        OrderBooking booking = bookingRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new RuntimeException(
                        "Booking not found for order: " + request.getOrderId()));
        booking.setDeliveryId(request.getDeliveryId());
        bookingRepository.save(booking);
    }

    @Transactional
    public void returnProducts(Map<UUID, Long> products) {
        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            WarehouseProduct wp = repository.findById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + entry.getKey()));
            wp.setQuantity(wp.getQuantity() + entry.getValue());
            repository.save(wp);
        }
    }

    private BookedProductsDto buildBookedProducts(Map<UUID, Long> requested, boolean decreaseStock) {
        Set<UUID> productIds = requested.keySet();
        Map<UUID, WarehouseProduct> warehouseProducts = repository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(WarehouseProduct::getProductId, Function.identity()));

        for (UUID id : productIds) {
            if (!warehouseProducts.containsKey(id)) {
                throw new RuntimeException("Product not found in warehouse: " + id);
            }
        }

        double totalWeight = 0;
        double totalVolume = 0;
        boolean fragile = false;

        for (Map.Entry<UUID, Long> entry : requested.entrySet()) {
            UUID productId = entry.getKey();
            long requestedQty = entry.getValue();
            WarehouseProduct wp = warehouseProducts.get(productId);

            if (wp.getQuantity() < requestedQty) {
                throw new RuntimeException("Not enough quantity for product: " + productId
                        + ". Available: " + wp.getQuantity() + ", requested: " + requestedQty);
            }

            if (decreaseStock) {
                wp.setQuantity(wp.getQuantity() - requestedQty);
                repository.save(wp);
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