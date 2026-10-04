# DBMS-Project: QuickCart, a quick-commerce / dark-store platform

A MySQL database (normalized to 3NF), a Spring Boot REST API, and a React frontend
for a 10-minute-delivery grocery app.

| Folder / file | What it is |
|---|---|
| `schema.sql` | Creates the `quick_commerce` database: 18 tables, including the `order_timer` weak entity |
| `seed.sql` | Demo data: Varanasi stores, products, stock, users, coupons |
| `NORMALIZATION.md` | 1NF → 2NF → 3NF walkthrough, with the Order Timer additions |
| `ERD.md` / `ERD.png` | Relational diagram (Mermaid) / original conceptual ER diagram |
| `demo/DEMO.md` + `demo/pipeline_demo.sql` | Presentation script: one order from customer → store → partner → receipt, with the SQL that shows each DB change |
| `backend/` | Spring Boot 3.5 · Java 21 · Spring Data JPA · Spring Security + JWT |
| `frontend/` | React 19 · Vite · React Router |

## Roles

| Role | Logs in from | Can do |
|---|---|---|
| **Customer** | `customer` table (sign-up is open) | Save addresses (quick-fill sample locations or "use my location"; new customers get the form right on the Shop page), see the **nearest stores** with distance and ETA, browse a store's items, build a cart, apply a coupon, **place an order**, watch the live **delivery timer**, cancel, and view or print the **receipt** |
| **Admin** | `employee` who is `dark_store.manager_employee_id` | See **all stores they manage**, open a store to see its items, **edit stock**, **add an existing product**, **create a new product** (and category), or **remove** a product, see its orders (with the auto-assigned partner), **mark them packed**, or reassign a partner |
| **Delivery partner** | `delivery_partner` table | See their **profile**, go online or offline, see their **delivery list**, and mark an order **Picked up** and then **Delivered** |

Every request is authorized on the server. A customer only sees their own
addresses and orders, an admin only sees stores they manage, and a partner only
sees orders assigned to them. Anything else returns 403 or 404.

## Order life cycle and the Order Timer

```
PLACED ──(admin: mark packed)──► PACKED ──(partner: picked up)──► OUT_FOR_DELIVERY ──(partner: delivered)──► DELIVERED
   └───────────── customer can cancel (PLACED or PACKED) ─────────────► CANCELLED
```

**Delivery partners are assigned automatically:**
- When an order is placed, the store's first **AVAILABLE** partner gets it in the same transaction, and becomes **BUSY**. The partner row is locked with `SELECT … FOR UPDATE`, so two orders at the same instant can't grab the same partner.
- If every partner is busy, the order waits. The next partner who becomes free (after a delivery, a cancellation, or going online) takes the oldest waiting order of their store.
- The admin can still reassign an order while marking it packed.

**Roles in parallel:** each browser tab keeps its own login (`sessionStorage`), so one browser can have a customer tab, an admin tab and a partner tab open at once. Pages refresh every few seconds, and immediately when you switch to a tab.

| `order_timer` column | Set when |
|---|---|
| `placed_at` | Order is placed |
| `expected_delivery_at` | At placement: `placed_at + 10 min + 2 min per km` from the store |
| `out_for_delivery_at` | Partner taps **Picked up** |
| `received_at` | Partner taps **Delivered** |
| `cancelled_at` | Customer cancels |

Placing an order is a single transaction. It takes the stock (a conditional
`UPDATE … WHERE quantity >= n`, so stock can't be oversold), freezes `price_at_order`,
redeems the coupon, and writes `orders`, `order_product`, `order_timer` and
`payment_record`. If any step fails, the whole transaction rolls back. Cancelling
restocks the items, returns the coupon and refunds the payment.

**Nearest store:** a Haversine distance query over `dark_store ⨝ address`, combined
with `operating_hours` to find which stores are open now. Only stores within 10 km
accept the order.

## Running it

**Prerequisites:**
- MySQL 8
- JDK 21: `winget install EclipseAdoptium.Temurin.21.JDK`, then open a new terminal
- Node 20+

Maven is not needed; the backend ships the Maven wrapper.

```powershell
# 1. database (drops and recreates quick_commerce)
cmd /c "mysql -u root -p < schema.sql"
cmd /c "mysql -u root -p < seed.sql"

# 2. backend -> http://localhost:8080
cd backend
$env:DB_PASSWORD = "<your MySQL root password>"   # DB_USER defaults to root
.\mvnw.cmd spring-boot:run

# 3. frontend -> http://localhost:5173  (new terminal)
cd frontend
npm install
npm run dev
```

If `mysql` isn't on your PATH, use
`"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"`, or run both files from
MySQL Workbench.

### Demo logins (password `QuickCart@2026` for all)

| Role | Email |
|---|---|
| Customer | `customer@qc.com` |
| Admin | `admin@qc.com` (manages the Lanka and Sigra stores), `admin2@qc.com` (Sarnath) |
| Delivery partner | `partner1@qc.com`, `partner2@qc.com` (Lanka), `partner3@qc.com` (Sigra) |

**Try the full flow:**
1. As the customer, place an order at Lanka Dark Store.
2. As `admin@qc.com`, open Lanka → Orders → **Pack & assign** partner1.
3. As `partner1@qc.com`, go to Deliveries and tap **Picked up**, then **Delivered**.

The customer's order page shows the timer updating at each step.

## API summary

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login` `{role, email, password}` → JWT |
| Customer | `GET/POST /api/customer/addresses`, `GET /api/customer/stores/nearest?addressId=`, `GET /api/customer/stores/{id}/items`, `GET /api/customer/coupons`, `POST/GET /api/customer/orders`, `GET /api/customer/orders/{id}`, `GET /api/customer/orders/{id}/receipt`, `POST /api/customer/orders/{id}/cancel` |
| Admin | `GET /api/admin/stores`, `GET /api/admin/stores/{id}/inventory`, `PUT /api/admin/stores/{id}/inventory/{productId}`, `POST /api/admin/stores/{id}/inventory`, `POST /api/admin/stores/{id}/products`, `DELETE /api/admin/stores/{id}/inventory/{productId}`, `GET /api/admin/products`, `GET /api/admin/categories`, `GET /api/admin/stores/{id}/orders`, `GET /api/admin/stores/{id}/partners`, `POST /api/admin/orders/{id}/pack` |
| Partner | `GET /api/partner/me`, `PUT /api/partner/me/status`, `GET /api/partner/deliveries`, `POST /api/partner/deliveries/{id}/pickup`, `POST /api/partner/deliveries/{id}/deliver` |

Errors always come back as `{"message": "..."}`.
