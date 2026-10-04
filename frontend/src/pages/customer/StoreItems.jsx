import { useMemo, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { Empty, ErrorBox, Loading } from '../../components/Feedback.jsx'
import { useCart } from '../../context/CartContext.jsx'
import { money } from '../../utils/format.js'
import { useApi } from '../../utils/useApi.js'

export function QtyStepper({ product, qty, onChange }) {
  if (qty === 0) {
    return <button className="btn btn-primary btn-sm" onClick={() => onChange(1)}>Add</button>
  }
  return (
    <div className="stepper">
      <button onClick={() => onChange(qty - 1)} aria-label="Remove one">−</button>
      <span>{qty}</span>
      <button onClick={() => onChange(qty + 1)} disabled={qty >= product.available} aria-label="Add one">+</button>
    </div>
  )
}

export default function StoreItems() {
  const storeId = Number(useParams().storeId)
  const cart = useCart()
  const { data: items, error, loading } = useApi(`/customer/stores/${storeId}/items`)
  const [category, setCategory] = useState('All')
  const [search, setSearch] = useState('')

  const categories = useMemo(() => ['All', ...new Set((items || []).map((i) => i.categoryName))], [items])
  const shown = (items || []).filter((i) =>
    (category === 'All' || i.categoryName === category)
    && i.name.toLowerCase().includes(search.toLowerCase()))

  // the cart must be tied to this store (and a delivery address) -- pick it on /shop
  if (cart.cart.store?.id !== storeId) return <Navigate to="/shop" replace />

  return (
    <div className="with-cart-bar">
      <div className="row-between">
        <div>
          <Link to="/shop" className="muted small">← Change store</Link>
          <h1>{cart.cart.store.name}</h1>
        </div>
        <input className="search" placeholder="Search items…" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>

      <div className="chips">
        {categories.map((c) => (
          <button key={c} className={c === category ? 'chip active' : 'chip'} onClick={() => setCategory(c)}>{c}</button>
        ))}
      </div>

      {loading && <Loading />}
      <ErrorBox message={error} />
      {items && shown.length === 0 && <Empty>No items match.</Empty>}

      <div className="product-grid">
        {shown.map((p) => (
          <div key={p.productId} className="card product">
            <span className="muted small">{p.categoryName}</span>
            <strong>{p.name}</strong>
            <span className="muted">{p.unit}</span>
            {p.available <= 5 && <span className="low-stock">Only {p.available} left</span>}
            <div className="row-between">
              <span className="price">{money(p.price)}</span>
              <QtyStepper product={p} qty={cart.qtyOf(p.productId)} onChange={(q) => cart.setQty(p, q)} />
            </div>
          </div>
        ))}
      </div>

      {cart.count > 0 && (
        <div className="cart-bar">
          <span>{cart.count} item{cart.count > 1 ? 's' : ''} · <strong>{money(cart.subtotal)}</strong></span>
          <Link to="/checkout" className="btn btn-primary">Go to checkout →</Link>
        </div>
      )}
    </div>
  )
}
