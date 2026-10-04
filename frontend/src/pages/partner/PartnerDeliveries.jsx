import { useState } from 'react'
import { api, errorMessage } from '../../api/client.js'
import { Empty, ErrorBox, Loading } from '../../components/Feedback.jsx'
import OrderTimer from '../../components/OrderTimer.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import { dateTime, money } from '../../utils/format.js'
import { useApi } from '../../utils/useApi.js'

function DeliveryCard({ order, onChanged }) {
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  async function act(step) {
    setBusy(true)
    setError(null)
    try {
      const { data } = await api.post(`/partner/deliveries/${order.id}/${step}`)
      onChanged(data)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const cashDue = order.paymentMode === 'CASH' && order.paymentStatus === 'PENDING'
  return (
    <div className="card">
      <div className="row-between">
        <div className="row-gap"><strong>Order #{order.id}</strong><StatusBadge status={order.status} /></div>
        <strong>{money(order.amount)}</strong>
      </div>
      <p><span className="muted">Pick up</span> {order.storeName} — {order.storeAddress}</p>
      <p><span className="muted">Deliver to</span> {order.customerName} ({order.customerPhone}) — {order.deliveryAddress}</p>
      <p className="small muted">{order.items.map((i) => `${i.quantity} × ${i.name}`).join(', ')}</p>
      {cashDue && <p className="alert alert-warn small">Collect {money(order.amount)} in cash</p>}
      <OrderTimer timer={order.timer} status={order.status} />
      <div className="actions">
        {order.status === 'PLACED' && (
          <>
            <button className="btn btn-primary" disabled title="The store must pack the order first">Picked up</button>
            <span className="muted small">New order assigned to you — waiting for the store to pack it…</span>
          </>
        )}
        {order.status === 'PACKED' && (
          <button className="btn btn-primary" disabled={busy} onClick={() => act('pickup')}>Picked up</button>
        )}
        {order.status === 'OUT_FOR_DELIVERY' && (
          <button className="btn btn-primary" disabled={busy} onClick={() => act('deliver')}>Delivered</button>
        )}
      </div>
      <ErrorBox message={error} />
    </div>
  )
}

export default function PartnerDeliveries() {
  const { data: orders, setData, error, loading } = useApi('/partner/deliveries', { pollMs: 4000 })

  if (loading) return <Loading />
  if (error) return <ErrorBox message={error} />

  const active = orders.filter((o) => o.status === 'PLACED' || o.status === 'PACKED' || o.status === 'OUT_FOR_DELIVERY')
  const past = orders.filter((o) => !active.includes(o))
  const replace = (updated) => setData(orders.map((o) => (o.id === updated.id ? updated : o)))

  return (
    <div>
      <h1>Deliveries</h1>
      <h2>Active</h2>
      {active.length === 0 && <Empty>No active deliveries. When a customer orders from your store you are assigned automatically and it shows up here.</Empty>}
      <div className="stack">
        {active.map((o) => <DeliveryCard key={o.id} order={o} onChanged={replace} />)}
      </div>

      <h2>History</h2>
      {past.length === 0 ? <p className="muted">Nothing yet.</p> : (
        <div className="card table-wrap">
          <table className="table">
            <thead>
              <tr><th>Order</th><th>Customer</th><th>Placed</th><th>Status</th><th>Delivery time</th><th className="num">Amount</th></tr>
            </thead>
            <tbody>
              {past.map((o) => (
                <tr key={o.id}>
                  <td>#{o.id}</td>
                  <td>{o.customerName}</td>
                  <td>{dateTime(o.timer?.placedAt)}</td>
                  <td><StatusBadge status={o.status} /></td>
                  <td><OrderTimer timer={o.timer} status={o.status} compact /></td>
                  <td className="num">{money(o.amount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
