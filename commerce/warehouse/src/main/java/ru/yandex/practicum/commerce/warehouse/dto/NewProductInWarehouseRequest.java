package ru.yandex.practicum.commerce.warehouse.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class NewProductInWarehouseRequest {
    private UUID productId;
    private boolean fragile;
    private double weight;
    private Dimension dimension;
    private long quantity;

    @Data
    public static class Dimension {
        private double width;
        private double height;
        private double depth;
    }
}