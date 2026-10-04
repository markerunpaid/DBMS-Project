-- =============================================================================
-- ORDER PIPELINE DEMO -- run each STEP block in MySQL Workbench right after the
-- matching action in the app (see demo/DEMO.md). Select a block, press Ctrl+Shift+Enter.
--
-- Demo order: customer Aarav Sharma orders 2 x Paneer + 1 x Cola from
-- Lanka Dark Store, coupon SAVE10, Cash on delivery.
-- =============================================================================
USE quick_commerce;


-- -----------------------------------------------------------------------------
-- STEP 0 -- BEFORE the order
-- -----------------------------------------------------------------------------
-- stock at Lanka (remember these numbers)
SELECT product, in_stock, updated_at
  FROM v_store_inventory
 WHERE store_name = 'Lanka Dark Store' AND product IN ('Paneer', 'Cola');

-- the customer's existing orders
SELECT order_id, status, store_name, partner_name, amount_paid, placed_at
  FROM v_order_details
 WHERE customer_name = 'Aarav Sharma'
 ORDER BY order_id;

-- Lanka's delivery partners -- both AVAILABLE
SELECT partner_id, CONCAT_WS(' ', first_name, last_name) AS partner, status
  FROM delivery_partner
 WHERE dark_store_id = 1;

-- the customer's coupons -- SAVE10 not used yet (redeemed_at is NULL)
SELECT cp.code, cp.discount_type, cp.value, cc.redeemed_at
  FROM customer_coupon cc
  JOIN coupon cp ON cp.coupon_id = cc.coupon_id
 WHERE cc.customer_id = 1;


-- -----------------------------------------------------------------------------
-- STEP 1 -- customer clicks PLACE ORDER
-- One transaction wrote to orders, order_product, order_timer, payment_record,
-- updated inventory and customer_coupon, and AUTO-ASSIGNED a free delivery partner.
-- -----------------------------------------------------------------------------
SET @o = (SELECT MAX(order_id) FROM orders);

-- new row in orders: status PLACED, delivery_partner_id already set (auto-assigned)
SELECT * FROM orders WHERE order_id = @o;

-- the assigned partner (Amit) went AVAILABLE -> BUSY in the same transaction
SELECT partner_id, CONCAT_WS(' ', first_name, last_name) AS partner, status
  FROM delivery_partner WHERE dark_store_id = 1;

-- line items, with the price frozen at order time
SELECT op.order_id, p.name, op.quantity, op.price_at_order, op.quantity * op.price_at_order AS line_total
  FROM order_product op JOIN product p ON p.product_id = op.product_id
 WHERE op.order_id = @o;

-- the ORDER TIMER starts: placed_at + expected_delivery_at set, the rest still NULL
SELECT * FROM order_timer WHERE order_id = @o;

-- payment row: CASH is PENDING until the partner collects it
SELECT * FROM payment_record WHERE order_id = @o;

-- stock went DOWN: Paneer -2, Cola -1 (compare with STEP 0)
SELECT product, in_stock, updated_at
  FROM v_store_inventory
 WHERE store_name = 'Lanka Dark Store' AND product IN ('Paneer', 'Cola');

-- SAVE10 is now used (redeemed_at filled)
SELECT cp.code, cc.redeemed_at
  FROM customer_coupon cc JOIN coupon cp ON cp.coupon_id = cc.coupon_id
 WHERE cc.customer_id = 1 AND cp.code = 'SAVE10';

-- everything about the order in one row
SELECT * FROM v_order_details WHERE order_id = @o;


-- -----------------------------------------------------------------------------
-- STEP 2 -- store admin clicks MARK PACKED
-- -----------------------------------------------------------------------------
SET @o = (SELECT MAX(order_id) FROM orders);

-- PLACED -> PACKED (same partner; the partner can now pick it up)
SELECT order_id, status, delivery_partner_id FROM orders WHERE order_id = @o;


-- -----------------------------------------------------------------------------
-- STEP 3 -- delivery partner clicks PICKED UP
-- -----------------------------------------------------------------------------
SET @o = (SELECT MAX(order_id) FROM orders);

SELECT order_id, status FROM orders WHERE order_id = @o;
-- out_for_delivery_at is now filled
SELECT * FROM order_timer WHERE order_id = @o;


-- -----------------------------------------------------------------------------
-- STEP 4 -- delivery partner clicks DELIVERED
-- -----------------------------------------------------------------------------
SET @o = (SELECT MAX(order_id) FROM orders);

-- received_at filled -> the timer is complete
SELECT * FROM order_timer WHERE order_id = @o;

-- cash collected: payment PENDING -> SUCCESS
SELECT order_id, mode, status, amount, timestamp FROM payment_record WHERE order_id = @o;

-- partner is free again: BUSY -> AVAILABLE
SELECT partner_id, CONCAT_WS(' ', first_name, last_name) AS partner, status
  FROM delivery_partner WHERE dark_store_id = 1;

-- the whole story in one row (same names/numbers as the receipt)
SELECT order_id, status, customer_name, store_name, partner_name, deliver_to,
       subtotal, coupon_code, amount_paid, payment_mode, payment_status,
       placed_at, expected_delivery_at, out_for_delivery_at, received_at, minutes_to_deliver
  FROM v_order_details WHERE order_id = @o;


-- -----------------------------------------------------------------------------
-- BONUS -- integrity rules enforced by the database itself (each should FAIL)
-- -----------------------------------------------------------------------------
-- stock can't go negative (chk_inventory_qty)
-- UPDATE inventory SET quantity = -1 WHERE dark_store_id = 1 AND product_id = 7;

-- an order can't be both received and cancelled (chk_timer_final)
-- UPDATE order_timer SET cancelled_at = NOW() WHERE order_id = @o;

-- an order can't point at a customer that doesn't exist (fk_order_customer)
-- UPDATE orders SET customer_id = 999 WHERE order_id = @o;
