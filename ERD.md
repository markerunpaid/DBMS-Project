# ER Diagram — relational schema (after normalization)

This is `schema.sql` drawn as a diagram. It renders on GitHub and in VS Code's Markdown
preview. The hand-drawn conceptual ERD is still in `ERD.png`.

**What's new compared to `ERD.png`:**
- **Order_Timer**, a weak entity tied to Order by the 1:1 identifying relationship *Tracked By*
- latitude/longitude on Address
- login columns on Employee and Delivery_Partner

```mermaid
erDiagram
    PINCODE ||--o{ ADDRESS : "locates"
    CUSTOMER ||--o{ CUSTOMER_ADDRESS : "saves"
    ADDRESS ||--o{ CUSTOMER_ADDRESS : "saved as"
    ADDRESS ||--o{ DARK_STORE : "houses"
    DARK_STORE ||--o{ EMPLOYEE : "employs"
    EMPLOYEE |o--o{ DARK_STORE : "manages"
    DARK_STORE ||--o{ OPERATING_HOURS : "open during"
    DARK_STORE ||--o{ DARK_STORE_CATEGORY : "stocks"
    CATEGORY ||--o{ DARK_STORE_CATEGORY : "stocked in"
    CATEGORY ||--o{ PRODUCT : "groups"
    DARK_STORE ||--o{ INVENTORY : "holds"
    PRODUCT ||--o{ INVENTORY : "held as"
    DARK_STORE |o--o{ DELIVERY_PARTNER : "home store of"
    CUSTOMER ||--o{ CUSTOMER_COUPON : "owns"
    COUPON ||--o{ CUSTOMER_COUPON : "owned via"
    CUSTOMER ||--o{ ORDERS : "places"
    DARK_STORE ||--o{ ORDERS : "fulfils"
    DELIVERY_PARTNER |o--o{ ORDERS : "delivers"
    COUPON |o--o{ ORDERS : "applied to"
    ADDRESS ||--o{ ORDERS : "ships to"
    ORDERS ||--|| ORDER_TIMER : "tracked by"
    ORDERS ||--|{ ORDER_PRODUCT : "contains"
    PRODUCT ||--o{ ORDER_PRODUCT : "ordered as"
    ORDERS ||--|| PAYMENT_RECORD : "paid by"

    PINCODE {
        char pin_code PK
        varchar city
        varchar state
    }
    ADDRESS {
        bigint address_id PK
        varchar house_no
        varchar street
        char pin_code FK
        decimal latitude
        decimal longitude
    }
    CUSTOMER {
        bigint customer_id PK
        varchar first_name
        varchar middle_name
        varchar last_name
        varchar phone UK
        varchar email UK
        varchar password_hash
    }
    CUSTOMER_ADDRESS {
        bigint customer_id PK,FK
        bigint address_id PK,FK
        varchar label
        boolean is_default
    }
    CATEGORY {
        bigint category_id PK
        varchar name UK
    }
    PRODUCT {
        bigint product_id PK
        varchar name
        decimal price
        date expiry
        varchar unit
        bigint category_id FK
    }
    DARK_STORE {
        bigint dark_store_id PK
        varchar name
        bigint address_id FK
        bigint manager_employee_id FK
    }
    EMPLOYEE {
        bigint employee_id PK
        varchar first_name
        varchar middle_name
        varchar last_name
        varchar phone UK
        varchar email UK
        varchar password_hash
        bigint dark_store_id FK
    }
    OPERATING_HOURS {
        bigint dark_store_id PK,FK
        enum day_of_week PK
        time opens_at
        time closes_at
    }
    DARK_STORE_CATEGORY {
        bigint dark_store_id PK,FK
        bigint category_id PK,FK
    }
    INVENTORY {
        bigint dark_store_id PK,FK
        bigint product_id PK,FK
        int quantity
        timestamp updated_at
    }
    DELIVERY_PARTNER {
        bigint partner_id PK
        varchar first_name
        varchar middle_name
        varchar last_name
        varchar phone UK
        varchar email UK
        varchar password_hash
        varchar vehicle_number
        enum status
        bigint dark_store_id FK
    }
    COUPON {
        bigint coupon_id PK
        varchar code UK
        enum discount_type
        decimal value
        datetime valid_from
        datetime valid_to
    }
    CUSTOMER_COUPON {
        bigint customer_id PK,FK
        bigint coupon_id PK,FK
        datetime redeemed_at
    }
    ORDERS {
        bigint order_id PK
        bigint customer_id FK
        bigint dark_store_id FK
        bigint delivery_partner_id FK
        bigint coupon_id FK
        bigint delivery_address_id FK
        decimal amount
        enum status
    }
    ORDER_TIMER {
        bigint order_id PK,FK
        datetime placed_at
        datetime expected_delivery_at
        datetime out_for_delivery_at
        datetime received_at
        datetime cancelled_at
    }
    ORDER_PRODUCT {
        bigint order_id PK,FK
        bigint product_id PK,FK
        int quantity
        decimal price_at_order
    }
    PAYMENT_RECORD {
        bigint payment_id PK
        bigint order_id FK,UK
        decimal amount
        enum mode
        enum status
        datetime timestamp
    }
```

## Updating `ERD.png`: the Order_Timer entity

To bring the hand-drawn Chen-notation diagram up to date:

1. Draw **Order_Timer** as a *weak entity* (double rectangle) next to **Order**.
2. Connect them with an *identifying relationship* (double diamond) called
   **Tracked By**: Order **1** to Order_Timer **1**. Order_Timer's side has total
   participation (double line).
3. Give Order_Timer these attributes: `placed_at`, `expected_delivery_at`,
   `out_for_delivery_at`, `received_at`, `cancelled_at`. It has no key of its own,
   because it is identified through Order.
4. Remove `date time` from Order; `placed_at` replaces it.
5. Add `latitude` and `longitude` to Address.
