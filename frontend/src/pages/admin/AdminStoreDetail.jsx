import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, errorMessage } from '../../api/client.js'
import { Empty, ErrorBox, Loading } from '../../components/Feedback.jsx'
import OrderTimer from '../../components/OrderTimer.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import { dateTime, money } from '../../utils/format.js'
import { useApi } from '../../utils/useApi.js'

function StockRow({ storeId, item, onSaved, onRemoved }) {
  const [qty, setQty] = useState(item.quantity)
  const [state, setState] = useState(null)   // null | 'saving' | 'saved' | error message
  const [confirmRemove, setConfirmRemove] = useState(false)
  const dirty = qty !== '' && Number(qty) !== item.quantity

  async function save() {
    setState('saving')
    try {
      const { data } = await api.put(`/admin/stores/${storeId}/inventory/${item.productId}`, { quantity: Number(qty) })
      onSaved(data)
      setState('saved')
    } catch (err) {
      setState(errorMessage(err))
    }
  }

  async function remove() {
    try {
      await api.delete(`/admin/stores/${storeId}/inventory/${item.productId}`)
      onRemoved(item.productId)
    } catch (err) {
      setState(errorMessage(err))
      setConfirmRemove(false)
    }
  }

  return (
    <tr className={item.quantity === 0 ? 'row-warn' : ''}>
      <td><strong>{item.name}</strong> <span className="muted small">{item.unit}</span></td>
      <td>{item.categoryName}</td>
      <td className="num">{money(item.price)}</td>
      <td>
        <div className="row-gap">
          <input type="number" min="0" className="qty-input" value={qty} aria-label={`Stock of ${item.name}`}
                 onChange={(e) => { setQty(e.target.value); setState(null) }}
                 onKeyDown={(e) => { if (e.key === 'Enter' && dirty) save() }} />
          <button className="btn btn-sm btn-primary" disabled={!dirty || state === 'saving'} onClick={save}
                  title={dirty ? 'Save new stock level' : 'Change the number first'}>
            Save
          </button>
        </div>
        {state && state !== 'saving' && (
          <span className={state === 'saved' ? 'small timer-ok' : 'small error-text'}>{state === 'saved' ? '✓ Saved' : state}</span>
        )}
      </td>
      <td className="muted small">{dateTime(item.updatedAt)}</td>
      <td className="num">
        {confirmRemove ? (
          <span className="row-gap">
            <button className="btn btn-sm btn-danger" onClick={remove}>Remove</button>
            <button className="btn btn-sm" onClick={() => setConfirmRemove(false)}>Keep</button>
          </span>
        ) : (
          <button className="btn btn-sm" onClick={() => setConfirmRemove(true)} title="Take this product off this store's shelf">
            Remove
          </button>
        )}
      </td>
    </tr>
  )
}

const NEW_PRODUCT = { name: '', unit: '', price: '', expiry: '', categoryId: '', categoryName: '', quantity: '10' }

function AddItemPanel({ storeId, stockedIds, onAdded }) {
  const [mode, setMode] = useState('existing')
  const { data: catalog, reload: reloadCatalog } = useApi('/admin/products')
  const { data: categories, reload: reloadCategories } = useApi('/admin/categories')
  const [productId, setProductId] = useState('')
  const [qty, setQty] = useState('10')
  const [form, setForm] = useState(NEW_PRODUCT)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState(null)

  const addable = (catalog || []).filter((p) => !stockedIds.has(p.id))
  const field = (name) => ({ value: form[name], onChange: (e) => setForm({ ...form, [name]: e.target.value }) })

  async function run(request, label) {
    setBusy(true)
    setError(null)
    setDone(null)
    try {
      const { data } = await request()
      onAdded(data)
      setDone(`${label} added to this store`)
      return true
    } catch (err) {
      setError(errorMessage(err))
      return false
    } finally {
      setBusy(false)
    }
  }

  async function addExisting(e) {
    e.preventDefault()
    const p = addable.find((x) => x.id === Number(productId))
    const ok = await run(
      () => api.post(`/admin/stores/${storeId}/inventory`, { productId: Number(productId), quantity: Number(qty) }),
      p?.name,
    )
    if (ok) setProductId('')
  }

  async function createNew(e) {
    e.preventDefault()
    const isNewCategory = form.categoryId === 'new'
    const ok = await run(() => api.post(`/admin/stores/${storeId}/products`, {
      name: form.name,
      unit: form.unit,
      price: Number(form.price),
      expiry: form.expiry || null,
      categoryId: isNewCategory ? null : Number(form.categoryId),
      categoryName: isNewCategory ? form.categoryName : null,
      quantity: Number(form.quantity),
    }), form.name)
    if (ok) {
      setForm(NEW_PRODUCT)
      reloadCatalog({ quiet: true })
      reloadCategories({ quiet: true })
    }
  }

  return (
    <div className="card add-panel">
      <div className="row-between">
        <h3>Add an item to this store</h3>
        <div className="tabs compact">
          <button type="button" className={mode === 'existing' ? 'tab active' : 'tab'} onClick={() => setMode('existing')}>Existing product</button>
          <button type="button" className={mode === 'new' ? 'tab active' : 'tab'} onClick={() => setMode('new')}>New product</button>
        </div>
      </div>

      {mode === 'existing' ? (
        <form className="row-gap" onSubmit={addExisting}>
          <select value={productId} onChange={(e) => setProductId(e.target.value)} required aria-label="Product">
            <option value="">{addable.length ? 'Choose a product…' : 'Every catalog product is already here'}</option>
            {addable.map((p) => (
              <option key={p.id} value={p.id}>{p.name} ({p.unit}) — {p.categoryName} — {money(p.price)}</option>
            ))}
          </select>
          <input type="number" min="0" className="qty-input" value={qty} onChange={(e) => setQty(e.target.value)} required aria-label="Quantity" />
          <button className="btn btn-primary" disabled={busy || !productId}>Add to store</button>
        </form>
      ) : (
        <form className="form" onSubmit={createNew}>
          <div className="row3">
            <label>Name<input {...field('name')} required placeholder="e.g. Basmati Rice" /></label>
            <label>Unit<input {...field('unit')} required placeholder="e.g. 1 kg" /></label>
            <label>Price (₹)<input {...field('price')} type="number" min="0" step="0.01" required /></label>
          </div>
          <div className="row3">
            <label>
              Category
              <select {...field('categoryId')} required>
                <option value="">Choose…</option>
                {categories?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                <option value="new">+ New category…</option>
              </select>
            </label>
            {form.categoryId === 'new' && <label>New category name<input {...field('categoryName')} required /></label>}
            <label>Expiry (optional)<input {...field('expiry')} type="date" /></label>
            <label>Opening stock<input {...field('quantity')} type="number" min="0" required /></label>
          </div>
          <button className="btn btn-primary" disabled={busy}>Create product and add to store</button>
        </form>
      )}
      <ErrorBox message={error} />
      {done && <p className="small timer-ok">✓ {done}</p>}
    </div>
  )
}

function ItemsTab({ storeId }) {
  const { data: items, setData, error, loading } = useApi(`/admin/stores/${storeId}/inventory`)
  if (loading) return <Loading />
  if (error) return <ErrorBox message={error} />

  const replace = (updated) => setData(items.map((i) => (i.productId === updated.productId ? updated : i)))
  const added = (item) => setData([...items, item].sort((a, b) =>
    a.categoryName.localeCompare(b.categoryName) || a.name.localeCompare(b.name)))
  const removed = (productId) => setData(items.filter((i) => i.productId !== productId))

  return (
    <div className="stack">
      <AddItemPanel storeId={storeId} stockedIds={new Set(items.map((i) => i.productId))} onAdded={added} />
      {items.length === 0 ? <Empty>No products stocked here yet — add one above.</Empty> : (
        <div className="card table-wrap">
          <table className="table">
            <thead>
              <tr><th>Product</th><th>Category</th><th className="num">Price</th><th>In stock</th><th>Last updated</th><th /></tr>
            </thead>
            <tbody>
              {items.map((i) => (
                <StockRow key={i.productId} storeId={storeId} item={i} onSaved={replace} onRemoved={removed} />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

const PARTNER_STATUS = { AVAILABLE: 'available', BUSY: 'on a delivery', OFFLINE: 'offline' }

function PackControl({ order, partners, onDone }) {
  // '' = keep the partner the system auto-assigned; otherwise reassign to this partner
  const [partnerId, setPartnerId] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const others = partners.filter((p) => p.id !== order.partnerId)

  async function pack() {
    setBusy(true)
    setError(null)
    try {
      await api.post(`/admin/orders/${order.id}/pack`, partnerId ? { partnerId: Number(partnerId) } : {})
      onDone()
    } catch (err) {
      setError(errorMessage(err))
      setBusy(false)
    }
  }

  return (
    <div className="pack-control">
      <div className="row-gap">
        <span className="small">
          {order.partnerName
            ? <>Partner: <strong>{order.partnerName}</strong> <span className="muted">(auto-assigned)</span></>
            : <span className="error-text">Waiting for a free partner</span>}
        </span>
        <select value={partnerId} onChange={(e) => setPartnerId(e.target.value)} aria-label="Delivery partner">
          <option value="">{order.partnerName ? 'Keep this partner' : 'Assign automatically'}</option>
          {others.map((p) => (
            <option key={p.id} value={p.id} disabled={p.status !== 'AVAILABLE'}>
              Reassign to {p.name} ({PARTNER_STATUS[p.status]})
            </option>
          ))}
        </select>
        <button className="btn btn-primary btn-sm" disabled={busy} onClick={pack}>
          Mark packed
        </button>
      </div>
      <ErrorBox message={error} />
    </div>
  )
}

function OrdersTab({ storeId }) {
  const { data: orders, error, loading, reload } = useApi(`/admin/stores/${storeId}/orders`, { pollMs: 4000 })
  const { data: partners, reload: reloadPartners } = useApi(`/admin/stores/${storeId}/partners`, { pollMs: 4000 })

  if (loading) return <Loading />
  if (error) return <ErrorBox message={error} />
  if (orders.length === 0) return <Empty>No orders at this store yet.</Empty>

  const refresh = () => { reload({ quiet: true }); reloadPartners({ quiet: true }) }
  return (
    <div className="stack">
      {orders.map((o) => (
        <div key={o.id} className="card">
          <div className="row-between">
            <div className="row-gap">
              <strong>#{o.id}</strong>
              <StatusBadge status={o.status} />
              <span className="muted small">{dateTime(o.timer?.placedAt)}</span>
            </div>
            <strong>{money(o.amount)}</strong>
          </div>
          <p className="small">
            {o.customerName} ({o.customerPhone}) · {o.deliveryAddress}
          </p>
          <p className="small muted">{o.items.map((i) => `${i.quantity} × ${i.name}`).join(', ')} · {o.paymentMode} {o.paymentStatus}</p>
          <div className="row-between">
            <OrderTimer timer={o.timer} status={o.status} compact />
            {o.status === 'PLACED'
              ? <PackControl order={o} partners={partners || []} onDone={refresh} />
              : o.partnerName && <span className="small">Partner: {o.partnerName}</span>}
          </div>
        </div>
      ))}
    </div>
  )
}

export default function AdminStoreDetail() {
  const storeId = Number(useParams().storeId)
  const [tab, setTab] = useState('items')
  const { data: stores } = useApi('/admin/stores', { pollMs: 4000 })
  const store = stores?.find((s) => s.id === storeId)

  return (
    <div>
      <Link to="/admin" className="muted small">← All my stores</Link>
      <h1>{store?.name || 'Store'}</h1>
      {store && <p className="muted">{store.address}</p>}
      <div className="tabs">
        <button className={tab === 'items' ? 'tab active' : 'tab'} onClick={() => setTab('items')}>Items</button>
        <button className={tab === 'orders' ? 'tab active' : 'tab'} onClick={() => setTab('orders')}>
          Orders{store?.activeOrders ? ` (${store.activeOrders} active)` : ''}
        </button>
      </div>
      {tab === 'items' ? <ItemsTab storeId={storeId} /> : <OrdersTab storeId={storeId} />}
    </div>
  )
}
