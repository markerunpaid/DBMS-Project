import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AddressForm from '../../components/AddressForm.jsx'
import { Empty, ErrorBox, Loading } from '../../components/Feedback.jsx'
import { useCart } from '../../context/CartContext.jsx'
import { money } from '../../utils/format.js'
import { useApi } from '../../utils/useApi.js'
import { productEmoji, productPhoto, QtyStepper } from './StoreItems.jsx'

export default function ChooseStore() {
  const navigate = useNavigate()
  const cart = useCart()
  const { data: addresses, error: addrError, loading: addrLoading, reload: reloadAddresses } = useApi('/customer/addresses')
  const [addressId, setAddressId] = useState(cart.cart.addressId)
  const [activeTab, setActiveTab] = useState('items') // 'items' | 'stores'
  const [category, setCategory] = useState('All')
  const [search, setSearch] = useState('')

  // default to the cart's address, else the default saved address
  useEffect(() => {
    if (!addresses?.length) return
    if (!addresses.some((a) => a.addressId === addressId)) {
      const defaultId = (addresses.find((a) => a.isDefault) || addresses[0]).addressId
      setAddressId(defaultId)
      cart.setAddressId(defaultId)
    }
  }, [addresses, addressId, cart])

  const { data: stores, error: storesError, loading: storesLoading } = useApi(
    addressId ? `/customer/stores/nearest?addressId=${addressId}` : null
  )

  const { data: catalogItems, error: catalogError, loading: catalogLoading } = useApi(
    addressId ? `/customer/catalog?addressId=${addressId}` : null
  )

  function onSelectAddress(newId) {
    setAddressId(newId)
    cart.setAddressId(newId)
  }

  function shopAtStore(store) {
    if (!(store.openNow && store.deliverable)) return
    cart.startShopping(store, addressId, { force: true })
    navigate(`/shop/store/${store.id}`)
  }

  const categories = useMemo(() => ['All', ...new Set((catalogItems || []).map((i) => i.categoryName))], [catalogItems])

  const shownItems = (catalogItems || []).filter((i) =>
    (category === 'All' || i.categoryName === category)
    && i.name.toLowerCase().includes(search.toLowerCase())
  )

  if (addrLoading) return <Loading />
  if (addrError) return <ErrorBox message={addrError} />

  // brand-new customer: collect an address right here
  if (addresses?.length === 0) {
    return (
      <div className="narrow">
        <h1>Where should we deliver?</h1>
        <p className="muted">Add your address and we'll show all available items and the dark stores that can reach you.</p>
        <div className="card">
          <AddressForm makeDefault onSaved={(a) => { setAddressId(a.addressId); cart.setAddressId(a.addressId); reloadAddresses({ quiet: true }) }} />
        </div>
      </div>
    )
  }

  const anyOrderable = stores?.some((s) => s.openNow && s.deliverable)

  return (
    <div className="with-cart-bar">
      <section className="shop-hero">
        <div className="shop-hero-copy">
          <span className="eyebrow">MULTI-STORE QUICK COMMERCE</span>
          <h1>Your everyday picks,<br />at your doorstep.</h1>
          <p>Order anything you need. If items are in different stores, multiple partners deliver them right to your door.</p>
          <div className="hero-perks">
            <span>✦ Fast local delivery</span>
            <span>✦ Multi-store fulfillment</span>
            <span>✦ Live partner tracking</span>
          </div>
        </div>
        <div className="shop-hero-image" role="img" aria-label="Fresh groceries ready for delivery" />
        <span className="hero-sticker">Good food<br /><strong>good mood</strong> ✿</span>
      </section>

      {/* Address Bar */}
      <div className="row-between" style={{ alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.5rem' }}>
        <div>
          <span className="eyebrow">DELIVERY LOCATION</span>
          <div className="row-gap" style={{ marginTop: '0.2rem' }}>
            <label className="inline" style={{ fontWeight: 600 }}>
              Deliver to{' '}
              <select value={addressId || ''} onChange={(e) => onSelectAddress(Number(e.target.value))}>
                {addresses?.map((a) => (
                  <option key={a.addressId} value={a.addressId}>{a.label || 'Address'} — {a.formatted}</option>
                ))}
              </select>
            </label>
            <Link to="/addresses" className="small">+ New address</Link>
          </div>
        </div>

        {/* View mode toggle */}
        <div className="tabs" style={{ marginBottom: 0 }}>
          <button
            className={`tab ${activeTab === 'items' ? 'active' : ''}`}
            onClick={() => setActiveTab('items')}
          >
            All Items (Combined)
          </button>
          <button
            className={`tab ${activeTab === 'stores' ? 'active' : ''}`}
            onClick={() => setActiveTab('stores')}
          >
            Nearby Dark Stores ({stores?.filter((s) => s.deliverable).length || 0})
          </button>
        </div>
      </div>

      <ErrorBox message={storesError || catalogError} />

      {stores && !anyOrderable && (
        <div className="alert alert-warn">
          No open store delivers to this address (we deliver within 10 km). Pick another address above or{' '}
          <Link to="/addresses">add one in Varanasi</Link>.
        </div>
      )}

      {/* TAB 1: ALL ITEMS (UNIFIED CATALOG) */}
      {activeTab === 'items' && (
        <div>
          <div className="row-between" style={{ marginBottom: '1rem' }}>
            <div className="chips">
              {categories.map((c) => (
                <button
                  key={c}
                  className={c === category ? 'chip active' : 'chip'}
                  onClick={() => setCategory(c)}
                >
                  {c}
                </button>
              ))}
            </div>
            <input
              className="search"
              placeholder="Search all items…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ minWidth: '220px' }}
            />
          </div>

          {catalogLoading && <Loading text="Loading catalog from nearby stores…" />}
          {catalogItems && shownItems.length === 0 && <Empty>No matching items found.</Empty>}

          <div className="product-grid">
            {shownItems.map((p) => {
              const qtyInCart = cart.qtyOf(p.productId)
              return (
                <div key={p.productId} className="card product">
                  <div className="product-art">
                    {productPhoto(p.name) ? (
                      <>
                        <span className="product-fallback" aria-hidden="true">
                          {productEmoji(p.categoryName, p.name)}
                        </span>
                        <img
                          src={productPhoto(p.name)}
                          alt={p.name}
                          loading="lazy"
                          onError={(e) => { e.currentTarget.style.display = 'none' }}
                        />
                      </>
                    ) : (
                      <span aria-hidden="true">{productEmoji(p.categoryName, p.name)}</span>
                    )}
                  </div>
                  <span className="product-category">{p.categoryName}</span>
                  <strong>{p.name}</strong>
                  <span className="muted">{p.unit}</span>

                  <div className="row-between" style={{ fontSize: '0.8rem', margin: '0.2rem 0' }}>
                    {p.totalAvailable <= 5 ? (
                      <span className="low-stock">Only {p.totalAvailable} left</span>
                    ) : (
                      <span className="muted">Stock: {p.totalAvailable}</span>
                    )}
                    {p.stores?.length > 1 && (
                      <span className="badge badge-delivered" title={`Available in ${p.stores.map((s) => s.storeName).join(', ')}`}>
                        {p.stores.length} stores
                      </span>
                    )}
                  </div>

                  <div className="row-between" style={{ marginTop: 'auto', paddingTop: '0.5rem' }}>
                    <span className="price">{money(p.price)}</span>
                    <QtyStepper
                      product={p}
                      qty={qtyInCart}
                      onChange={(q) => cart.setQty(p, q, addressId)}
                    />
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* TAB 2: NEARBY DARK STORES */}
      {activeTab === 'stores' && (
        <div>
          {storesLoading && <Loading text="Finding the nearest stores…" />}
          <div className="stack">
            {stores?.map((s, i) => {
              const canOrder = s.openNow && s.deliverable
              return (
                <div
                  key={s.id}
                  className={`card store-card ${canOrder ? 'clickable' : 'disabled'}`}
                  role="button"
                  tabIndex={canOrder ? 0 : -1}
                  aria-disabled={!canOrder}
                  onClick={() => shopAtStore(s)}
                  onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); shopAtStore(s) } }}
                >
                  <div className={`store-photo store-photo-${i % 3}`} aria-hidden="true">
                    <span>{['🥬', '🍊', '🥖'][i % 3]}</span>
                  </div>
                  <div className="store-info">
                    <div className="row-gap">
                      <h3>{s.name}</h3>
                      {i === 0 && canOrder && <span className="badge badge-delivered">Nearest</span>}
                      <span className={`badge ${s.openNow ? 'badge-open' : 'badge-cancelled'}`}>
                        {s.openNow ? 'Open' : 'Closed'}
                      </span>
                      {!s.deliverable && <span className="badge badge-cancelled">Out of delivery range</span>}
                    </div>
                    <p className="muted">{s.address}</p>
                    <p>{s.distanceKm} km away · delivery in ~{s.etaMinutes} min</p>
                  </div>
                  <button
                    className="btn btn-primary"
                    disabled={!canOrder}
                    onClick={(e) => { e.stopPropagation(); shopAtStore(s) }}
                  >
                    Shop this store →
                  </button>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* Persistent floating Cart Bar */}
      {cart.count > 0 && (
        <div className="cart-bar">
          <span>{cart.count} item{cart.count > 1 ? 's' : ''} · <strong>{money(cart.subtotal)}</strong></span>
          <Link to="/checkout" className="btn btn-primary">Go to checkout →</Link>
        </div>
      )}
    </div>
  )
}
