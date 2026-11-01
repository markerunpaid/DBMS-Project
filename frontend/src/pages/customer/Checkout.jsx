import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, errorMessage } from '../../api/client.js'
import { Empty, ErrorBox, Loading } from '../../components/Feedback.jsx'
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

  // Multi-store fulfillment plan
  const [plan, setPlan] = useState(null)
  const [planLoading, setPlanLoading] = useState(false)
  const [planError, setPlanError] = useState(null)

  // resolve current address
  const currentAddressId = cart.cart.addressId || addresses?.find((a) => a.isDefault)?.addressId || addresses?.[0]?.addressId

  useEffect(() => {
    if (currentAddressId && currentAddressId !== cart.cart.addressId) {
      cart.setAddressId(currentAddressId)
    }
  }, [currentAddressId, cart])

  // fetch fulfillment plan whenever cart items or address changes
  useEffect(() => {
    if (!currentAddressId || cart.lines.length === 0) {
      setPlan(null)
      return
    }

    let active = true
    setPlanLoading(true)
    setPlanError(null)

    api.post('/customer/orders/plan', {
      addressId: currentAddressId,
      items: cart.lines.map((l) => ({ productId: l.product.productId, quantity: l.qty })),
    })
      .then(({ data }) => {
        if (active) {
          setPlan(data)
          setPlanLoading(false)
        }
      })
      .catch((err) => {
        if (active) {
          setPlanError(errorMessage(err))
          setPlanLoading(false)
        }
      })

    return () => { active = false }
  }, [currentAddressId, cart.lines])

  if (cart.lines.length === 0) {
    return <Empty>Your cart is empty. <Link to="/shop" className="btn btn-primary">Start shopping</Link></Empty>
  }

  const address = addresses?.find((a) => a.addressId === currentAddressId)
  const coupon = coupons?.find((c) => c.code === couponCode)
  const discount = couponDiscount(coupon, cart.subtotal)
  const total = Math.max(0, cart.subtotal - discount)

  async function placeOrder() {
    if (!currentAddressId) {
      setError('Please choose a delivery address')
      return
    }
    setPlacing(true)
    setError(null)
    try {
      const { data: placedOrders } = await api.post('/customer/orders/multi', {
        addressId: currentAddressId,
        items: cart.lines.map((l) => ({ productId: l.product.productId, quantity: l.qty })),
        couponCode: couponCode || null,
        paymentMode,
      })
      cart.clear()
      if (placedOrders?.length === 1) {
        navigate(`/orders/${placedOrders[0].id}`)
      } else {
        navigate('/orders')
      }
    } catch (err) {
      setError(errorMessage(err))
      setPlacing(false)
    }
  }

  return (
    <div className="grid-2">
      <section>
        <h1>Checkout</h1>

        {planLoading && <Loading text="Calculating optimal dark store fulfillment…" />}
        <ErrorBox message={planError} />

        {plan?.isSplit && (
          <div className="alert alert-warn" style={{ marginBottom: '1rem' }}>
            <strong>⚡ Multi-Store Split Fulfillment</strong>
            <p className="small" style={{ margin: '0.3rem 0 0' }}>
              Your items are split across <strong>{plan.packages.length} dark stores</strong> to fulfill your full order.
              Each package will be dispatched with its own delivery partner and live timer!
            </p>
          </div>
        )}

        {/* Fulfillment Packages */}
        {plan?.packages?.map((pkg, idx) => (
          <div key={pkg.storeId} className="card" style={{ marginBottom: '1.25rem' }}>
            <div className="row-between" style={{ borderBottom: '1px solid var(--line)', paddingBottom: '0.6rem', marginBottom: '0.6rem' }}>
              <div>
                <strong>Package {idx + 1}: {pkg.storeName}</strong>
                <div className="muted small">{pkg.distanceKm} km away · ETA ~{pkg.etaMinutes} min</div>
              </div>
              <span className="badge badge-open">ETA {pkg.etaMinutes} min</span>
            </div>
            <table className="table">
              <tbody>
                {pkg.items.map((item) => {
                  const lineProduct = cart.lines.find((l) => l.product.productId === item.productId)?.product || {
                    productId: item.productId,
                    name: item.name,
                    unit: item.unit,
                    price: item.price,
                  }
                  return (
                    <tr key={item.productId}>
                      <td>
                        <strong>{item.name}</strong>
                        <div className="muted small">{item.unit} · {money(item.price)}</div>
                      </td>
                      <td>
                        <QtyStepper
                          product={lineProduct}
                          qty={cart.qtyOf(item.productId)}
                          onChange={(q) => cart.setQty(lineProduct, q, currentAddressId)}
                        />
                      </td>
                      <td className="num">{money(item.lineTotal)}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
            <div className="row-between" style={{ marginTop: '0.5rem', paddingTop: '0.5rem', borderTop: '1px dashed var(--line)' }}>
              <span className="muted small">Package subtotal</span>
              <strong>{money(pkg.subtotal)}</strong>
            </div>
          </div>
        ))}

        {!plan && !planLoading && (
          <div className="card">
            <table className="table">
              <tbody>
                {cart.lines.map(({ product, qty }) => (
                  <tr key={product.productId}>
                    <td>
                      <strong>{product.name}</strong>
                      <div className="muted small">{product.unit} · {money(product.price)}</div>
                    </td>
                    <td><QtyStepper product={product} qty={qty} onChange={(q) => cart.setQty(product, q, currentAddressId)} /></td>
                    <td className="num">{money(qty * Number(product.price))}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <h3>Deliver to</h3>
        <div className="card">
          {address ? <p><strong>{address.label}</strong> — {address.formatted}</p> : <p className="muted">Loading address…</p>}
          <Link to="/shop" className="small">Change address</Link>
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
        <button
          className="btn btn-primary btn-block"
          onClick={placeOrder}
          disabled={placing || planLoading || !!planError}
        >
          {placing ? 'Placing order…' : `Place order · ${money(total)}`}
        </button>
      </section>
    </div>
  )
}
