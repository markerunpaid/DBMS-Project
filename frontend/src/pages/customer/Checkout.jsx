import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, errorMessage } from '../../api/client.js'
import { Empty, ErrorBox } from '../../components/Feedback.jsx'
import { useCart } from '../../context/CartContext.jsx'
import { couponDiscount, money, PAYMENT_MODES } from '../../utils/format.js'
import { useApi } from '../../utils/useApi.js'
import { QtyStepper } from './StoreItems.jsx'

export default function Checkout() {
  const navigate = useNavigate()
  const cart = useCart()
  const { data: addresses } = useApi('/customer/addresses')
  const { data: coupons } = useApi('/customer/coupons')
  const [couponCode, setCouponCode] = useState('')
  const [paymentMode, setPaymentMode] = useState('UPI')
  const [error, setError] = useState(null)
  const [placing, setPlacing] = useState(false)

  if (cart.lines.length === 0) {
    return <Empty>Your cart is empty. <Link to="/shop" className="btn btn-primary">Start shopping</Link></Empty>
  }

  const address = addresses?.find((a) => a.addressId === cart.cart.addressId)
  const coupon = coupons?.find((c) => c.code === couponCode)
  const discount = couponDiscount(coupon, cart.subtotal)
  const total = cart.subtotal - discount

  async function placeOrder() {
    setPlacing(true)
    setError(null)
    try {
      const { data: order } = await api.post('/customer/orders', {
        storeId: cart.cart.store.id,
        addressId: cart.cart.addressId,
        items: cart.lines.map((l) => ({ productId: l.product.productId, quantity: l.qty })),
        couponCode: couponCode || null,
        paymentMode,
      })
      cart.clear()
      navigate(`/orders/${order.id}`)
    } catch (err) {
      setError(errorMessage(err))
      setPlacing(false)
    }
  }

  return (
    <div className="grid-2">
      <section>
        <h1>Checkout</h1>
        <p className="muted">
          From <strong>{cart.cart.store.name}</strong> ·{' '}
          <Link to={`/shop/store/${cart.cart.store.id}`}>add more items</Link>
        </p>
        <div className="card">
          <table className="table">
            <tbody>
              {cart.lines.map(({ product, qty }) => (
                <tr key={product.productId}>
                  <td>
                    <strong>{product.name}</strong>
                    <div className="muted small">{product.unit} · {money(product.price)}</div>
                  </td>
                  <td><QtyStepper product={product} qty={qty} onChange={(q) => cart.setQty(product, q)} /></td>
                  <td className="num">{money(qty * Number(product.price))}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <h3>Deliver to</h3>
        <div className="card">
          {address ? <p><strong>{address.label}</strong> — {address.formatted}</p> : <p className="muted">Loading address…</p>}
          <Link to="/shop" className="small">Change address or store</Link>
        </div>
      </section>

      <section className="card summary">
        <h2>Payment</h2>
        <label>
          Coupon
          <select value={couponCode} onChange={(e) => setCouponCode(e.target.value)}>
            <option value="">No coupon</option>
            {coupons?.map((c) => (
              <option key={c.code} value={c.code}>
                {c.code} — {c.discountType === 'PERCENTAGE' ? `${Number(c.value)}% off` : `${money(c.value)} off`}
              </option>
            ))}
          </select>
        </label>

        <fieldset className="radios">
          <legend>Pay with</legend>
          {PAYMENT_MODES.map((m) => (
            <label key={m} className="check">
              <input type="radio" name="mode" value={m} checked={paymentMode === m} onChange={() => setPaymentMode(m)} />
              {m === 'CASH' ? 'Cash on delivery' : m}
            </label>
          ))}
        </fieldset>

        <dl className="totals">
          <dt>Subtotal</dt><dd>{money(cart.subtotal)}</dd>
          {discount > 0 && (<><dt>Discount ({couponCode})</dt><dd>− {money(discount)}</dd></>)}
          <dt className="total">Total</dt><dd className="total">{money(total)}</dd>
        </dl>

        <ErrorBox message={error} />
        <button className="btn btn-primary btn-block" onClick={placeOrder} disabled={placing}>
          {placing ? 'Placing order…' : `Place order · ${money(total)}`}
        </button>
      </section>
    </div>
  )
}
