package ru.yandex.practicum.commerce.cart.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
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
@Validated
public class ShoppingCartController {

    private final CartService service;

    @GetMapping
    public ShoppingCartDto getShoppingCart(@RequestParam @NotBlank String username) {
        return service.getCart(username);
    }

    @PostMapping
    public ShoppingCartDto addProducts(@RequestParam @NotBlank String username,
                                       @RequestBody @NotEmpty Map<UUID, Long> products) {
        return service.addProducts(username, products);
    }

    @PutMapping
    public ShoppingCartDto addProductsPut(@RequestParam @NotBlank String username,
                                          @RequestBody @NotEmpty Map<UUID, Long> products) {
        return service.addProducts(username, products);
    }

    @DeleteMapping
    public void deactivateShoppingCart(@RequestParam @NotBlank String username) {
        service.deactivateCart(username);
    }

    @PostMapping("/remove")
    public ShoppingCartDto removeProductsPost(@RequestParam @NotBlank String username,
                                              @RequestBody @NotEmpty List<UUID> productIds) {
        return service.removeProductsByIds(username, productIds);
    }

    @PatchMapping("/remove")
    public ShoppingCartDto removeProductsPatch(@RequestParam @NotBlank String username,
                                               @RequestBody @NotEmpty List<UUID> productIds) {
        return service.removeProductsByIds(username, productIds);
    }

    @PostMapping("/change-quantity")
    public ShoppingCartDto changeQuantity(@RequestParam @NotBlank String username,
                                          @RequestBody @Valid ChangeQuantityRequest request) {
        return service.changeQuantityByRequest(username, request);
    }

    @PutMapping("/change-quantity")
    public ShoppingCartDto changeQuantityPut(@RequestParam @NotBlank String username,
                                             @RequestBody @Valid ChangeQuantityRequest request) {
        return service.changeQuantityByRequest(username, request);
    }
}