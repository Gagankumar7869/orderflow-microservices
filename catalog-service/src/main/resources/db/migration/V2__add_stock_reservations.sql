CREATE TABLE stock_reservations (
    reservation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_reference VARCHAR(64) NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    customer_email VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_reservation_order_product UNIQUE (order_reference, product_id),
    CONSTRAINT fk_reservation_product FOREIGN KEY (product_id) REFERENCES products (product_id),
    CONSTRAINT chk_reservation_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;