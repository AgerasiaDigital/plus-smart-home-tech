CREATE SCHEMA IF NOT EXISTS shopping_store;

CREATE TABLE IF NOT EXISTS shopping_store.products (
    product_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_name VARCHAR(255),
    description TEXT,
    image_src VARCHAR(500),
    quantity_state VARCHAR(50),
    product_state VARCHAR(50),
    product_category VARCHAR(50),
    price NUMERIC(19, 2)
);