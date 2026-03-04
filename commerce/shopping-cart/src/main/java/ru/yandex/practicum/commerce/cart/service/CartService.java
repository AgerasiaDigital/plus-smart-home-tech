package ru.yandex.practicum.commerce.cart.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.cart.model.Cart;
import ru.yandex.practicum.commerce.cart.repository.CartRepository;
import ru.yandex.practicum.commerce.interaction.dto.ShoppingCartDto;
import ru.yandex.practicum.commerce.interaction.feign.WarehouseClient;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository repository;
    private final WarehouseClient warehouseClient;

    public ShoppingCartDto getCart(String username) {
        Cart cart = getOrCreateCart(username);
        return toDto(cart);
    }

    @Transactional
    public ShoppingCartDto addProducts(String username, Map<UUID, Long> products) {
        Cart cart = getOrCreateCart(username);
        if (!cart.isActive()) {
            throw new RuntimeException("Cart is deactivated for user: " + username);
        }
        cart.getProducts().putAll(products);

        // Проверяем наличие на складе через Feign + Circuit Breaker
        warehouseClient.checkProductsAvailability(toDto(cart));

        return toDto(repository.save(cart));
    }

    @Transactional
    public void deactivateCart(String username) {
        Cart cart = getOrCreateCart(username);
        cart.setActive(false);
        repository.save(cart);
    }

    @Transactional
    public ShoppingCartDto removeProducts(String username, Map<UUID, Long> products) {
        Cart cart = getOrCreateCart(username);
        if (!cart.isActive()) {
            throw new RuntimeException("Cart is deactivated for user: " + username);
        }
        products.keySet().forEach(cart.getProducts()::remove);
        return toDto(repository.save(cart));
    }

    private Cart getOrCreateCart(String username) {
        return repository.findByUsernameAndActiveTrue(username).orElseGet(() -> {
            Cart c = new Cart();
            c.setUsername(username);
            c.setActive(true);
            return repository.save(c);
        });
    }

    private ShoppingCartDto toDto(Cart cart) {
        return ShoppingCartDto.builder()
                .shoppingCartId(cart.getShoppingCartId())
                .username(cart.getUsername())
                .products(cart.getProducts())
                .build();
    }
}
