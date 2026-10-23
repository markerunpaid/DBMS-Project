import { useMemo, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { Empty, ErrorBox, Loading } from '../../components/Feedback.jsx'
import { useCart } from '../../context/CartContext.jsx'
import { money } from '../../utils/format.js'
import { useApi } from '../../utils/useApi.js'

// Product photography keyed to the seeded catalog, with category fallbacks for manager-created items.
const PRODUCT_PHOTOS = {
  'banana': 'photo-1571771894821-ce9b6c11b08e',
  'tomato': 'photo-1546094096-0df4bcaaa337',
  'onion': 'photo-1518977676601-b53f82aba655',
  'apple': 'photo-1560806887-1e4cd0b6cbd6',
  'milk': 'photo-1550583724-b2692b85b150',
  'bread': 'photo-1509440159596-0249088772ff',
  'paneer': 'photo-1486297678162-eb2a19b0a32d',
  'curd': 'photo-1488477181946-6428a0291777',
  'chips': 'photo-1566478989037-eec170784d0b',
  'peanuts': 'photo-1505576399274-565b52d4ac71',
  'cookies': 'photo-1499636136210-6f4ee915583e',
  'cola': 'photo-1554866585-cd94860890b7',
  'juice': 'photo-1600271886742-f049cd451bba',
  'tea': 'photo-1544787219-7f47ccb76574',
  'toothpaste': 'photo-1609840114035-3c981b782dfe',
  'hand wash': 'photo-1608248543803-ba4f8c70ae0b',
}

export function productPhoto(name = '') {
  const key = Object.keys(PRODUCT_PHOTOS).find((item) => name.toLowerCase().includes(item))
  return key ? `https://images.unsplash.com/${PRODUCT_PHOTOS[key]}?auto=format&fit=crop&w=480&q=80` : null
}

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
            <div className="product-art">
              {productPhoto(p.name)
                ? <><span className="product-fallback" aria-hidden="true">{productEmoji(p.categoryName, p.name)}</span><img src={productPhoto(p.name)} alt={p.name} loading="lazy" onError={(e) => { e.currentTarget.style.display = 'none' }} /></>
                : <span aria-hidden="true">{productEmoji(p.categoryName, p.name)}</span>}
            </div>
            <span className="product-category">{p.categoryName}</span>
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

function productEmoji(category = '', name = '') {
  const text = `${category} ${name}`.toLowerCase()
  if (/fruit|apple|banana|orange|mango|berry|grape/.test(text)) return '🍎'
  if (/vegetable|veggie|tomato|potato|onion|spinach/.test(text)) return '🥑'
  if (/dairy|milk|cheese|yogurt|curd|paneer/.test(text)) return '🥛'
  if (/bakery|bread|cake|biscuit|cookie/.test(text)) return '🥐'
  if (/drink|juice|water|beverage|coffee|tea/.test(text)) return '🧃'
  if (/snack|chips|chocolate|candy/.test(text)) return '🍿'
  return '🛍️'
}
