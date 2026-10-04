# Demo: one order through the whole pipeline

This walks one order through the whole system: **Customer → Dark store (admin) → Delivery partner → Receipt**.
All three roles run at the same time, each in its own browser tab, and every tab updates by itself.
After every click, a query in MySQL Workbench shows the rows that changed.

## Setup (5 minutes before you present)

1. **Fresh data.** In MySQL Workbench, run `schema.sql`, then `seed.sql`. This also creates the views
   `v_order_details` and `v_store_inventory`.
2. **Start the backend:**
   ```powershell
   cd backend
   $env:DB_PASSWORD = "password"
   .\mvnw.cmd spring-boot:run
   ```
   Wait for `Started QuickCommerceApplication`.
3. **Start the frontend:** `cd frontend; npm run dev`.
4. **Open three TABS in the same browser**, one per role. Each tab keeps its own login, so there's no
   logging out and back in. Drag the tabs into three windows side by side if you want all of them
   visible at once.

   | Tab | Open http://localhost:5173 and log in as | Leave it on |
   |---|---|---|
   | 1: Customer | `customer@qc.com` | Shop |
   | 2: Store admin | `admin@qc.com` | Lanka Dark Store → **Orders** tab |
   | 3: Delivery partner | `partner1@qc.com` | **Deliveries** |

   The password for all three is `QuickCart@2026`. The admin and partner tabs refresh by themselves
   every few seconds, and immediately when you switch to them.
5. **Open `demo/pipeline_demo.sql` in Workbench.** To run one STEP block, select it and press **Ctrl+Shift+Enter**.

## The demo

### Step 0: Before anything happens
Run **STEP 0** and point out:
- Lanka stock: **Paneer = 15**, **Cola = 50**
- The customer has **1** old order
- Amit Verma and Rahul Gupta are both **AVAILABLE**
- `SAVE10` has **redeemed_at = NULL** (unused)

### Step 1: Customer places the order (Customer tab)
1. **Shop** lists the 3 dark stores, nearest first. The distance comes from a Haversine SQL query on
   the address latitude/longitude. Sarnath is marked "Out of delivery range" (over 10 km away).
2. Click **Lanka Dark Store**. Add **Paneer ×2** and **Cola ×1**, then click **Go to checkout**.
3. Choose coupon **SAVE10**, select **Cash on delivery**, and click **Place order**.
4. The order page opens with a live countdown timer and **Delivery partner: Amit Verma**. The partner
   was assigned automatically.

Right away, **without reloading**:
- The **partner tab** shows the new order ("waiting for the store to pack it"); **Picked up** is still locked.
- The **admin tab** shows it with "Partner: Amit Verma (auto-assigned)".

Run **STEP 1** and point out that **one transaction** wrote to 7 tables:

| Table | What changed (example values) |
|---|---|
| `orders` | New row: `status = PLACED`, `amount = 207.00`, **`delivery_partner_id = 1`** (auto-assigned) |
| `delivery_partner` | **Amit AVAILABLE → BUSY**. The row was locked with `SELECT … FOR UPDATE`, so two orders can't grab the same partner |
| `order_product` | 2 rows. `price_at_order` freezes today's price (Paneer 95, Cola 40) |
| `order_timer` | `placed_at` and `expected_delivery_at` (+14 min) filled in; the other 3 columns are NULL |
| `payment_record` | `CASH / PENDING`, because cash is collected at the door |
| `inventory` | **Paneer 15 → 13, Cola 50 → 49**, and `updated_at` changed |
| `customer_coupon` | `SAVE10.redeemed_at` is now filled in |

The last query shows the same order through `v_order_details`: **subtotal 230, 10% off, 207 paid**.

### Step 2: The store packs the order (Admin tab)
1. The **Items** tab shows Paneer at **13** now. **My stores** lists only this admin's stores, not Sarnath.
2. On the **Orders** tab, the order shows "Partner: Amit Verma (auto-assigned)". Click **Mark packed**.
   The dropdown next to it can reassign the order to another free partner if needed.

Run **STEP 2** and point out: `PLACED → PACKED`. In the **partner tab**, **Picked up** unlocks by itself.

### Step 3: The partner picks the order up (Partner tab)
1. On **Deliveries**, the order shows the customer's name, phone and address, the store to collect
   from, and "Collect ₹207.00 in cash". **Profile** shows Amit as "On a delivery".
2. Click **Picked up**.

Run **STEP 3** and point out: `OUT_FOR_DELIVERY`, and `order_timer.out_for_delivery_at` is now filled in.
The customer tab's timeline updates by itself.

### Step 4: The partner delivers (Partner tab)
Click **Delivered**.

Run **STEP 4** and point out:
- `order_timer.received_at` is filled in, so the timer is complete (`minutes_to_deliver` in the view).
- The payment went **PENDING → SUCCESS** because the cash was collected.
- **Amit is AVAILABLE again.**
- The final `v_order_details` row matches the receipt exactly.

### Step 5: The customer sees the result and the receipt (Customer tab)
1. Without a reload, the order page shows **Delivered in m:ss · on time**, and the timeline shows
   Placed → Out for delivery → Received.
2. Click **View receipt**. It shows:
   - **Billed to** Aarav Sharma, and **Delivered to** the hostel address
   - **Delivery partner** Amit Verma
   - **Delivery timeline**: placed, expected, out for delivery and received times
   - The items at their order-time prices, the SAVE10 discount, and the total of ₹207
   - **Paid via CASH · SUCCESS**
3. **Print receipt** opens the browser's print dialog.

## Extras if you have time

- **All partners busy:** place **3** orders quickly. The first goes to Amit and the second to Rahul.
  The third shows "Waiting for a free delivery partner", with `delivery_partner_id = NULL`. When Amit
  delivers his order, the waiting order moves to him automatically, and the partner tab shows it
  without a reload.
- **Cancel flow:** place another order and cancel it from the customer's order page. `inventory` goes
  back up, `customer_coupon.redeemed_at` returns to NULL, `payment_record` becomes `REFUNDED` (or
  `FAILED` for cash), `order_timer.cancelled_at` is filled in, and the partner is freed.
- **Out of stock:** as admin, set Paneer's stock to 1 at Lanka, then try to order 2 as the customer.
  The whole order is rejected and nothing is written, because the transaction rolls back.
- **Constraints in the database itself:** uncomment the BONUS queries at the bottom of
  `pipeline_demo.sql`. MySQL rejects negative stock, a timer that is both received and cancelled,
  and an order that points at a customer who doesn't exist.
- **Admin catalog:** create a new product (for example "Basmati Rice", in a new category "Staples")
  at Sigra. A row appears in `product`, `inventory` and `dark_store_category`, plus in `category` if
  the category is new.

## Resetting between rehearsals
Run `schema.sql`, then `seed.sql` again. Stock returns to Paneer 15, Cola 50, and the next order is #2 again.
