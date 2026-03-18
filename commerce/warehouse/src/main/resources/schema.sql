CREATE SCHEMA IF NOT EXISTS warehouse;

CREATE TABLE IF NOT EXISTS warehouse.products (
    product_id UUID PRIMARY KEY,
    quantity BIGINT NOT NULL DEFAULT 0,
    width DOUBLE PRECISION,
    height DOUBLE PRECISION,
    depth DOUBLE PRECISION,
    weight DOUBLE PRECISION,
    fragile BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS warehouse.order_bookings (
    booking_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    delivery_id UUID
);

CREATE TABLE IF NOT EXISTS warehouse.order_booking_items (
    booking_id UUID REFERENCES warehouse.order_bookings(booking_id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    quantity BIGINT NOT NULL,
    PRIMARY KEY (booking_id, product_id)
);