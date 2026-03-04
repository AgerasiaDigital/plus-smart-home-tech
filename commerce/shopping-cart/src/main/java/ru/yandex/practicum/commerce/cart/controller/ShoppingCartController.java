package ru.yandex.practicum.commerce.cart.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.cart.dto.ChangeQuantityRequest;
import ru.yandex.practicum.commerce.cart.service.CartService;
import ru.yandex.practicum.commerce.interaction.dto.ShoppingCartDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-cart")
@RequiredArgsConstructor
public class ShoppingCartController {

    private final CartService service;

    @GetMapping
    public ShoppingCartDto getShoppingCart(@RequestParam String username) {
        return service.getCart(username);
    }

    @PostMapping
    public ShoppingCartDto addProducts(@RequestParam String username,
                                       @RequestBody Map<UUID, Long> products) {
        return service.addProducts(username, products);
    }

    @PutMapping
    public ShoppingCartDto addProductsPut(@RequestParam String username,
                                          @RequestBody Map<UUID, Long> products) {
        return service.addProducts(username, products);
    }

    @DeleteMapping
    public void deactivateShoppingCart(@RequestParam String username) {
        service.deactivateCart(username);
    }

    @PostMapping("/remove")
    public ShoppingCartDto removeProductsPost(@RequestParam String username,
                                              @RequestBody List<UUID> productIds) {
        return service.removeProductsByIds(username, productIds);
    }

    @PatchMapping("/remove")
    public ShoppingCartDto removeProductsPatch(@RequestParam String username,
                                               @RequestBody List<UUID> productIds) {
        return service.removeProductsByIds(username, productIds);
    }

    @PostMapping("/change-quantity")
    public ShoppingCartDto changeQuantity(@RequestParam String username,
                                          @RequestBody ChangeQuantityRequest request) {
        return service.changeQuantityByRequest(username, request);
    }

    @PutMapping("/change-quantity")
    public ShoppingCartDto changeQuantityPut(@RequestParam String username,
                                             @RequestBody ChangeQuantityRequest request) {
        return service.changeQuantityByRequest(username, request);
    }
}