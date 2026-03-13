package ru.yandex.practicum.commerce.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookedProductsDto {
    private Map<UUID, Long> products;
    private double deliveryWeight;
    private double deliveryVolume;
    private boolean fragile;
}