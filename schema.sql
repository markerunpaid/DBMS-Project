-- =============================================================================
-- Quick-Commerce / Dark-Store Delivery Platform
-- Normalized to 3NF — see NORMALIZATION.md for the full FD-by-FD proof.
-- =============================================================================

DROP DATABASE IF EXISTS quick_commerce;
CREATE DATABASE quick_commerce
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE quick_commerce;

SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- 1. PINCODE
-- =============================================================================
-- 3NF: city was a column on address, but pin_code -> city holds as a real-world
-- FD (a PIN code identifies one fixed locality), so city was only transitively
-- dependent on address_id via pin_code. Extracted here so city/state are stored
-- once per pin code instead of once per address.
CREATE TABLE pincode (
    pin_code CHAR(6)     NOT NULL,
    city     VARCHAR(80) NOT NULL,
    state    VARCHAR(80) NOT NULL,
    PRIMARY KEY (pin_code)
) ENGINE = InnoDB;

-- =============================================================================
-- 2. ADDRESS
-- =============================================================================
CREATE TABLE address (
    address_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    house_no     VARCHAR(30),
    street       VARCHAR(150),
    pin_code     CHAR(6)          NOT NULL,
    created_at   TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (address_id),
    CONSTRAINT fk_address_pincode
        FOREIGN KEY (pin_code) REFERENCES pincode (pin_code)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_address_pin (pin_code)
) ENGINE = InnoDB;

-- =============================================================================
-- 3. CUSTOMER
-- =============================================================================
CREATE TABLE customer (
    customer_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    first_name      VARCHAR(60)     NOT NULL,
    middle_name     VARCHAR(60),
    last_name       VARCHAR(60),
    phone           VARCHAR(15)     NOT NULL,
    email           VARCHAR(120)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (customer_id),
    UNIQUE KEY uq_customer_phone (phone),
    UNIQUE KEY uq_customer_email (email)
) ENGINE = InnoDB;

-- Customer <-> Address  (M:N — a customer can save many addresses)
-- 2NF: label/is_default depend on the full (customer_id, address_id) pair, not
-- on either column alone — no partial dependency.
CREATE TABLE customer_address (
    customer_id  BIGINT UNSIGNED NOT NULL,
    address_id   BIGINT UNSIGNED NOT NULL,
    label        VARCHAR(30),                 -- 'Home', 'Work', ...
    is_default   BOOLEAN         NOT NULL DEFAULT FALSE,
    PRIMARY KEY (customer_id, address_id),
    CONSTRAINT fk_custaddr_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_custaddr_address
        FOREIGN KEY (address_id) REFERENCES address (address_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

-- =============================================================================
-- 4. CATEGORY  /  PRODUCT
-- =============================================================================
CREATE TABLE category (
    category_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name         VARCHAR(80)     NOT NULL,
    PRIMARY KEY (category_id),
    UNIQUE KEY uq_category_name (name)
) ENGINE = InnoDB;

CREATE TABLE product (
    product_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name         VARCHAR(150)    NOT NULL,
    price        DECIMAL(10,2)   NOT NULL,
    expiry       DATE,
    unit         VARCHAR(30)     NOT NULL,          -- '500 g', '1 L', 'pack of 6'
    category_id  BIGINT UNSIGNED NOT NULL,
    created_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id),
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id) REFERENCES category (category_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_product_price CHECK (price >= 0),
    INDEX idx_product_category (category_id),
    INDEX idx_product_name (name)
) ENGINE = InnoDB;

-- =============================================================================
-- 5. DARK STORE  /  EMPLOYEE  /  OPERATING HOURS
-- =============================================================================
CREATE TABLE dark_store (
    dark_store_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name                VARCHAR(120)    NOT NULL,
    address_id          BIGINT UNSIGNED NOT NULL,
    manager_employee_id BIGINT UNSIGNED,                 -- FK added after employee exists
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (dark_store_id),
    CONSTRAINT fk_darkstore_address
        FOREIGN KEY (address_id) REFERENCES address (address_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_darkstore_address (address_id)
) ENGINE = InnoDB;

CREATE TABLE employee (
    employee_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    first_name    VARCHAR(60)     NOT NULL,
    middle_name   VARCHAR(60),
    last_name     VARCHAR(60),
    phone         VARCHAR(15)     NOT NULL,
    dark_store_id BIGINT UNSIGNED NOT NULL,              -- store the employee works at
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (employee_id),
    UNIQUE KEY uq_employee_phone (phone),
    CONSTRAINT fk_employee_darkstore
        FOREIGN KEY (dark_store_id) REFERENCES dark_store (dark_store_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_employee_darkstore (dark_store_id)
) ENGINE = InnoDB;

ALTER TABLE dark_store
    ADD CONSTRAINT fk_darkstore_manager
        FOREIGN KEY (manager_employee_id) REFERENCES employee (employee_id)
        ON DELETE SET NULL ON UPDATE CASCADE;

-- 1NF: the ER design had operating_hours as a single multi-valued attribute on
-- Dark_Store (different hours per day can't fit atomically in one column) —
-- pulled out into its own table, one row per store per day.
-- 2NF: opens_at/closes_at depend on the full (dark_store_id, day_of_week) pair
-- — a store's hours differ by day, so neither column alone determines them.
CREATE TABLE operating_hours (
    dark_store_id BIGINT UNSIGNED NOT NULL,
    day_of_week   ENUM('MON','TUE','WED','THU','FRI','SAT','SUN') NOT NULL,
    opens_at      TIME NOT NULL,
    closes_at     TIME NOT NULL,
    PRIMARY KEY (dark_store_id, day_of_week),
    CONSTRAINT fk_ophours_darkstore
        FOREIGN KEY (dark_store_id) REFERENCES dark_store (dark_store_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

-- 1NF: the ER design put dark_store_id directly on Category for what is an
-- M:N relationship (a category can be stocked by many stores) — a single FK
-- can't represent that without repeating whole category rows, so it's a
-- junction table instead.
-- Dark Store <-> Category  (which categories a store stocks)
CREATE TABLE dark_store_category (
    dark_store_id BIGINT UNSIGNED NOT NULL,
    category_id   BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (dark_store_id, category_id),
    CONSTRAINT fk_dsc_darkstore
        FOREIGN KEY (dark_store_id) REFERENCES dark_store (dark_store_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_dsc_category
        FOREIGN KEY (category_id) REFERENCES category (category_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

-- =============================================================================
-- 6. INVENTORY  (Dark Store <-> Product, with stock level)
-- =============================================================================
-- 2NF: quantity/updated_at depend on the full (dark_store_id, product_id) pair
-- — stock level is per-product-per-store, not a fact of the product or store alone.
CREATE TABLE inventory (
    dark_store_id BIGINT UNSIGNED NOT NULL,
    product_id    BIGINT UNSIGNED NOT NULL,
    quantity      INT             NOT NULL DEFAULT 0,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (dark_store_id, product_id),
    CONSTRAINT fk_inventory_darkstore
        FOREIGN KEY (dark_store_id) REFERENCES dark_store (dark_store_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_inventory_product
        FOREIGN KEY (product_id) REFERENCES product (product_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_inventory_qty CHECK (quantity >= 0),
    INDEX idx_inventory_product (product_id)
) ENGINE = InnoDB;

-- =============================================================================
-- 7. DELIVERY PARTNER
-- =============================================================================
CREATE TABLE delivery_partner (
    partner_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    first_name     VARCHAR(60)     NOT NULL,
    middle_name    VARCHAR(60),
    last_name      VARCHAR(60),
    phone          VARCHAR(15)     NOT NULL,
    vehicle_number VARCHAR(20),
    status         ENUM('AVAILABLE','BUSY','OFFLINE') NOT NULL DEFAULT 'OFFLINE',
    dark_store_id  BIGINT UNSIGNED,                 -- home store the partner is attached to
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (partner_id),
    UNIQUE KEY uq_partner_phone (phone),
    CONSTRAINT fk_partner_darkstore
        FOREIGN KEY (dark_store_id) REFERENCES dark_store (dark_store_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_partner_status (status)
) ENGINE = InnoDB;

-- =============================================================================
-- 8. COUPON
-- =============================================================================
CREATE TABLE coupon (
    coupon_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code          VARCHAR(30)     NOT NULL,
    discount_type ENUM('PERCENTAGE','FLAT') NOT NULL,
    value         DECIMAL(10,2)   NOT NULL,
    valid_from    DATETIME        NOT NULL,
    valid_to      DATETIME        NOT NULL,
    PRIMARY KEY (coupon_id),
    UNIQUE KEY uq_coupon_code (code),
    CONSTRAINT chk_coupon_value  CHECK (value >= 0),
    CONSTRAINT chk_coupon_window CHECK (valid_to > valid_from)
) ENGINE = InnoDB;

-- 1NF: the ER design put customer_id directly on Coupons for what is an M:N
-- relationship (a coupon can be owned by many customers) — same fix as
-- dark_store_category above.
-- Customer <-> Coupon  (coupons available / redeemed by a customer)
-- 2NF: redeemed_at depends on the full (customer_id, coupon_id) pair — it's
-- when *this customer* redeemed *this coupon*.
CREATE TABLE customer_coupon (
    customer_id BIGINT UNSIGNED NOT NULL,
    coupon_id   BIGINT UNSIGNED NOT NULL,
    redeemed_at DATETIME,
    PRIMARY KEY (customer_id, coupon_id),
    CONSTRAINT fk_custcoupon_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_custcoupon_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupon (coupon_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

-- =============================================================================
-- 9. ORDER  (`orders` — "order" is a reserved word)
-- =============================================================================
CREATE TABLE orders (
    order_id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    customer_id       BIGINT UNSIGNED NOT NULL,
    dark_store_id     BIGINT UNSIGNED NOT NULL,       -- store that fulfils the order
    delivery_partner_id BIGINT UNSIGNED,              -- assigned later
    coupon_id         BIGINT UNSIGNED,                -- optional
    delivery_address_id BIGINT UNSIGNED NOT NULL,     -- where it ships
    amount            DECIMAL(10,2)   NOT NULL,       -- final payable amount
    status            ENUM('PLACED','PACKED','OUT_FOR_DELIVERY','DELIVERED','CANCELLED')
                                      NOT NULL DEFAULT 'PLACED',
    date_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id),
    CONSTRAINT fk_order_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_order_darkstore
        FOREIGN KEY (dark_store_id) REFERENCES dark_store (dark_store_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_order_partner
        FOREIGN KEY (delivery_partner_id) REFERENCES delivery_partner (partner_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_order_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupon (coupon_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_order_address
        FOREIGN KEY (delivery_address_id) REFERENCES address (address_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_order_amount CHECK (amount >= 0),
    INDEX idx_order_customer (customer_id),
    INDEX idx_order_darkstore (dark_store_id),
    INDEX idx_order_partner (delivery_partner_id),
    INDEX idx_order_status (status),
    INDEX idx_order_datetime (date_time)
) ENGINE = InnoDB;

-- Order line items  (Order <-> Product, M:N)
-- 2NF: quantity/price_at_order depend on the full (order_id, product_id) pair —
-- price_at_order in particular freezes the product's price at purchase time,
-- so it can't be derived from product_id alone (prices change later).
CREATE TABLE order_product (
    order_id       BIGINT UNSIGNED NOT NULL,
    product_id     BIGINT UNSIGNED NOT NULL,
    quantity       INT             NOT NULL,
    price_at_order DECIMAL(10,2)   NOT NULL,          -- unit price captured at purchase
    PRIMARY KEY (order_id, product_id),
    CONSTRAINT fk_orderproduct_order
        FOREIGN KEY (order_id) REFERENCES orders (order_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_orderproduct_product
        FOREIGN KEY (product_id) REFERENCES product (product_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_orderproduct_qty   CHECK (quantity > 0),
    CONSTRAINT chk_orderproduct_price CHECK (price_at_order >= 0),
    INDEX idx_orderproduct_product (product_id)
) ENGINE = InnoDB;

-- =============================================================================
-- 10. PAYMENT RECORD  (1:1 with order)
-- =============================================================================
CREATE TABLE payment_record (
    payment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id   BIGINT UNSIGNED NOT NULL,
    amount     DECIMAL(10,2)   NOT NULL,
    mode       ENUM('UPI','CARD','CASH','WALLET','NETBANKING') NOT NULL,
    status     ENUM('PENDING','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    timestamp  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (payment_id),
    UNIQUE KEY uq_payment_order (order_id),          -- enforces 1:1
    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id) REFERENCES orders (order_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (amount >= 0)
) ENGINE = InnoDB;

SET FOREIGN_KEY_CHECKS = 1;
