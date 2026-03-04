package ru.yandex.practicum.commerce.cart.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.cart.service.CartService;
import ru.yandex.practicum.commerce.interaction.dto.ShoppingCartDto;
import ru.yandex.practicum.commerce.interaction.feign.ShoppingCartClient;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-cart")
@RequiredArgsConstructor
public class ShoppingCartController implements ShoppingCartClient {

    private final CartService service;

    @Override
    @GetMapping
    public ShoppingCartDto getShoppingCart(@RequestParam String username) {
        return service.getCart(username);
    }

    @Override
    @PostMapping
    public ShoppingCartDto addProducts(@RequestParam String username,
                                       @RequestBody Map<UUID, Long> products) {
        return service.addProducts(username, products);
    }

    @Override
    @DeleteMapping
    public void deactivateShoppingCart(@RequestParam String username) {
        service.deactivateCart(username);
    }

    @Override
    @PatchMapping("/remove")
    public ShoppingCartDto removeProducts(@RequestParam String username,
                                          @RequestBody Map<UUID, Long> products) {
        return service.removeProducts(username, products);
    }
}
