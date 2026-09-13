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
