package ru.yandex.practicum.commerce.warehouse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "order_bookings", schema = "warehouse")
@Getter
@Setter
public class OrderBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID bookingId;

    private UUID orderId;

    private UUID deliveryId;

    @ElementCollection
    @CollectionTable(
            name = "order_booking_items",
            schema = "warehouse",
            joinColumns = @JoinColumn(name = "booking_id")
    )
    @MapKeyColumn(name = "product_id")
    @Column(name = "quantity")
    private Map<UUID, Long> products = new HashMap<>();
}
