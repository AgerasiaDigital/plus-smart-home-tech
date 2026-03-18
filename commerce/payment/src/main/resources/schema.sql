CREATE SCHEMA IF NOT EXISTS payment;

CREATE TABLE IF NOT EXISTS payment.payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID,
    product_price NUMERIC(19, 2),
    delivery_price NUMERIC(19, 2),
    total_price NUMERIC(19, 2),
    payment_state VARCHAR(50)
);