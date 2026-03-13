package ru.yandex.practicum.commerce.interaction.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.interaction.dto.ShoppingCartDto;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "shopping-cart")
public interface ShoppingCartClient {

    @GetMapping("/api/v1/shopping-cart")
    ShoppingCartDto getShoppingCart(@RequestParam String username);

    @PostMapping("/api/v1/shopping-cart")
    ShoppingCartDto addProducts(@RequestParam String username,
                                @RequestBody Map<UUID, Long> products);

    @DeleteMapping("/api/v1/shopping-cart")
    void deactivateShoppingCart(@RequestParam String username);

    @PatchMapping("/api/v1/shopping-cart/remove")
    ShoppingCartDto removeProducts(@RequestParam String username,
                                   @RequestBody Map<UUID, Long> products);
}