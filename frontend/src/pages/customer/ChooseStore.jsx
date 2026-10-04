import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AddressForm from '../../components/AddressForm.jsx'
import { ErrorBox, Loading } from '../../components/Feedback.jsx'
import { useCart } from '../../context/CartContext.jsx'
import { useApi } from '../../utils/useApi.js'

export default function ChooseStore() {
  const navigate = useNavigate()
  const cart = useCart()
  const { data: addresses, error: addrError, loading: addrLoading, reload: reloadAddresses } = useApi('/customer/addresses')
  const [addressId, setAddressId] = useState(cart.cart.addressId)
  const [pending, setPending] = useState(null)   // store waiting for "replace cart?" confirmation

  // default to the cart's address, else the default saved address
  useEffect(() => {
    if (!addresses?.length) return
    if (!addresses.some((a) => a.addressId === addressId)) {
      setAddressId((addresses.find((a) => a.isDefault) || addresses[0]).addressId)
    }
  }, [addresses, addressId])

  const { data: stores, error, loading } = useApi(addressId ? `/customer/stores/nearest?addressId=${addressId}` : null)

  function shopAt(store, force = false) {
    if (!(store.openNow && store.deliverable)) return
    if (!cart.startShopping(store, addressId, { force })) {
      setPending(store)
      return
    }
    navigate(`/shop/store/${store.id}`)
  }

  if (addrLoading) return <Loading />
  if (addrError) return <ErrorBox message={addrError} />

  // brand-new customer: collect an address right here instead of a dead end
  if (addresses?.length === 0) {
    return (
      <div className="narrow">
        <h1>Where should we deliver?</h1>
        <p className="muted">Add your address and we'll show the dark stores that can reach you. Use a quick-fill location to try the demo.</p>
        <div className="card">
          <AddressForm makeDefault onSaved={(a) => { setAddressId(a.addressId); reloadAddresses({ quiet: true }) }} />
        </div>
      </div>
    )
  }

  const anyOrderable = stores?.some((s) => s.openNow && s.deliverable)

  return (
    <div>
      <div className="row-between">
        <h1>Choose a store</h1>
        <div className="row-gap">
          <label className="inline">
            Deliver to{' '}
            <select value={addressId || ''} onChange={(e) => setAddressId(Number(e.target.value))}>
              {addresses?.map((a) => (
                <option key={a.addressId} value={a.addressId}>{a.label || 'Address'} — {a.formatted}</option>
              ))}
            </select>
          </label>
          <Link to="/addresses" className="small">+ New address</Link>
        </div>
      </div>
      <ErrorBox message={error} />

      {pending && (
        <div className="alert alert-warn row-between">
          <span>Your cart has items from <strong>{cart.cart.store?.name}</strong>. Start a new cart at {pending.name}?</span>
          <span className="actions">
            <button className="btn btn-primary" onClick={() => { const s = pending; setPending(null); shopAt(s, true) }}>Start new cart</button>
            <button className="btn" onClick={() => setPending(null)}>Keep my cart</button>
          </span>
        </div>
      )}

      {stores && !anyOrderable && (
        <div className="alert alert-warn">
          No open store delivers to this address (we deliver within 10 km). Pick another address above or{' '}
          <Link to="/addresses">add one in Varanasi</Link>.
        </div>
      )}

      {loading && <Loading text="Finding the nearest stores…" />}
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
              onClick={() => shopAt(s)}
              onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); shopAt(s) } }}
            >
              <div>
                <div className="row-gap">
                  <h3>{s.name}</h3>
                  {i === 0 && canOrder && <span className="badge badge-delivered">Nearest</span>}
                  <span className={`badge ${s.openNow ? 'badge-open' : 'badge-cancelled'}`}>{s.openNow ? 'Open' : 'Closed'}</span>
                  {!s.deliverable && <span className="badge badge-cancelled">Out of delivery range</span>}
                </div>
                <p className="muted">{s.address}</p>
                <p>{s.distanceKm} km away · delivery in ~{s.etaMinutes} min</p>
              </div>
              <button className="btn btn-primary" disabled={!canOrder}
                      onClick={(e) => { e.stopPropagation(); shopAt(s) }}>
                Shop here →
              </button>
            </div>
          )
        })}
      </div>
    </div>
  )
}
