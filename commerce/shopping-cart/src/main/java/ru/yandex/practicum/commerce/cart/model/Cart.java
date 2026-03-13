package ru.yandex.practicum.commerce.cart.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "carts", schema = "shopping_cart")
@Getter
@Setter
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID shoppingCartId;

    private String username;

    private boolean active;

    @ElementCollection
    @CollectionTable(
            name = "cart_items",
            schema = "shopping_cart",
            joinColumns = @JoinColumn(name = "cart_id")
    )
    @MapKeyColumn(name = "product_id")
    @Column(name = "quantity")
    private Map<UUID, Long> products = new HashMap<>();
}
