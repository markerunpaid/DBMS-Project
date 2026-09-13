# Database Normalization — Quick-Commerce / Dark-Store Platform

This document walks `schema.sql` through 1NF → 2NF → 3NF, table by table, with the
functional dependencies (FDs) that justify each step. Read it alongside `schema.sql`
and `ERD.png` / `Realtional Database.pdf` (the original ER design).

Notation: `X → Y` means "X functionally determines Y" (given a value of X, Y is
uniquely determined).

---

## 1NF — First Normal Form

**Requirement:** every attribute holds a single atomic value from its domain; no
repeating groups or multi-valued attributes; every table has a primary key.

### Violations in the original ER design, and how `schema.sql` resolves them

| # | Violation in the ER design (`ERD.png`) | Why it breaks 1NF | Fix applied in `schema.sql` |
|---|---|---|---|
| 1 | `Dark_Store.operating_hours` modeled as one multi-valued attribute (drawn as a double ellipse) | A store has a different opening/closing time for each day of the week — one column can't hold seven day/time pairs atomically | Extracted into its own table `operating_hours(dark_store_id, day_of_week, opens_at, closes_at)` — one row per store per day |
| 2 | `Coupons.customer_id` is a single FK column, even though the ER diagram's `Owns` relationship between `Customer` and `Coupons` is **M:N** | A single FK can only record one customer per coupon row; letting many customers own the same coupon would mean repeating the entire coupon row per customer — a disguised repeating group | Removed `customer_id` from `coupon`; added junction table `customer_coupon(customer_id, coupon_id, redeemed_at)` |
| 3 | `Category.dark_store_id` is a single FK column, even though the ER diagram's `Has` relationship between `Dark_Store` and `Category` is **M:N** | Same problem as #2 — one category can be stocked by many stores | Removed `dark_store_id` from `category`; added junction table `dark_store_category(dark_store_id, category_id)` |
| 4 | `name` drawn as one attribute bubble per person entity (Customer, Employee, Delivery_Partner) | A person's name is a composite of independently-queried/sorted parts | Split into `first_name`, `middle_name`, `last_name` everywhere a person's name is stored |

### Everything else checked

`phone`, `email`, `price`, `pin_code`, `vehicle_number`, `status`, `quantity`, … —
each holds one indivisible value; no table stores a list/CSV in a single column.
Every table declares a single-column or minimal composite `PRIMARY KEY`.

✅ **All tables in `schema.sql` satisfy 1NF.**

---

## 2NF — Second Normal Form

**Requirement:** table is in 1NF, and every non-key attribute is fully
functionally dependent on the *whole* primary key (no attribute depends on only
part of a composite key). Tables with a single-column PK automatically satisfy 2NF,
so only the composite-PK (junction / weak-entity) tables need checking:

| Table | Composite PK | Non-key attributes | Do they depend on the *whole* key? |
|---|---|---|---|
| `customer_address` | (customer_id, address_id) | label, is_default | Yes — `label`/`is_default` describe how *this customer* tags *this specific saved address*; neither is fixed by customer_id or address_id alone |
| `dark_store_category` | (dark_store_id, category_id) | *(none)* | trivially satisfied |
| `operating_hours` | (dark_store_id, day_of_week) | opens_at, closes_at | Yes — hours are specific to *that store on that day*; two stores differ, and one store's Monday differs from its Sunday |
| `inventory` | (dark_store_id, product_id) | quantity, updated_at | Yes — stock level is a fact of *that product in that store*, not the product globally or the store in general |
| `customer_coupon` | (customer_id, coupon_id) | redeemed_at | Yes — redemption date is specific to *this customer redeeming this coupon* |
| `order_product` | (order_id, product_id) | quantity, price_at_order | Yes — `price_at_order` deliberately freezes the product's price *at the time of that order* (prices change over time), so it cannot be derived from product_id alone |

No partial dependency was found in any composite-key table.

✅ **All tables satisfy 2NF.**

---

## 3NF — Third Normal Form

**Requirement:** table is in 2NF, and no non-key attribute is transitively
dependent on the primary key through another non-key attribute (i.e. no non-key
attribute determines another non-key attribute).

### Violation found

`address(address_id, house_no, street, city, pin_code)`

```
address_id → pin_code → city
```

A PIN code identifies a fixed post office / locality, so `pin_code → city` holds
as a real-world functional dependency. That makes `city` depend on `address_id`
**only transitively**, through the non-key attribute `pin_code` — a textbook 3NF
violation. Symptoms if left as-is:

- **Update anomaly** — correcting a city's name requires updating every address
  row that happens to share that pin code.
- **Insertion anomaly** — a pin code's city can't be recorded without also
  creating a full address row.
- **Deletion anomaly** — deleting the last address using a pin code silently
  loses the pin code → city fact.

### Fix applied

Extracted the transitively-dependent attributes into their own table, keyed on
the determinant (`pin_code`):

```sql
CREATE TABLE pincode (
    pin_code CHAR(6)     NOT NULL,
    city     VARCHAR(80) NOT NULL,
    state    VARCHAR(80) NOT NULL,
    PRIMARY KEY (pin_code)
);

-- address now stores only facts that depend on address_id directly
CREATE TABLE address (
    address_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    house_no   VARCHAR(30),
    street     VARCHAR(150),
    pin_code   CHAR(6) NOT NULL,   -- FK -> pincode(pin_code)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (address_id)
);
```

`city`/`state` are now stored exactly once per pin code and reached via a join —
the transitive dependency is gone.

### Remaining tables re-checked for transitive dependencies

- `orders → customer_id, dark_store_id, delivery_partner_id, coupon_id, delivery_address_id, amount, status, date_time`
  — every one of these is an independent *fact about that order* (who placed it,
  which store fulfills it, which address it ships to). Note `delivery_address_id`
  is deliberately kept on `orders` itself rather than assumed from the customer's
  default address, since a customer can ship different orders to different saved
  addresses — that is not a transitive dependency, it's a direct fact of the order.
- `dark_store → name, address_id, manager_employee_id` — independent facts; no
  attribute determines another.
- `product`, `employee`, `delivery_partner`, `payment_record`, `coupon`,
  `customer` — every non-key column was checked against every other non-key
  column in the same row; each is an independent, directly-owned fact.

✅ **All tables now satisfy 3NF.**
