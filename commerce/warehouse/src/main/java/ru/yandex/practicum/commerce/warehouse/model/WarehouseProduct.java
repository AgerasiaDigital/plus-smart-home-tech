package ru.yandex.practicum.commerce.warehouse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "products", schema = "warehouse")
@Getter
@Setter
public class WarehouseProduct {

    @Id
    private UUID productId;

    private long quantity;
    private double width;
    private double height;
    private double depth;
    private double weight;
    private boolean fragile;
}