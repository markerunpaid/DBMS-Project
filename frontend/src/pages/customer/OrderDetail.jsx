import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, errorMessage } from '../../api/client.js'
import { ErrorBox, Loading } from '../../components/Feedback.jsx'
import OrderTimer from '../../components/OrderTimer.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import { money } from '../../utils/format.js'
import { useApi } from '../../utils/useApi.js'

export function OrderItemsTable({ items }) {
  return (
    <table className="table">
      <thead>
        <tr><th>Item</th><th className="num">Qty</th><th className="num">Price</th><th className="num">Total</th></tr>
      </thead>
      <tbody>
        {items.map((i) => (
          <tr key={i.productId}>
            <td>{i.name} <span className="muted small">{i.unit}</span></td>
            <td className="num">{i.quantity}</td>
            <td className="num">{money(i.priceAtOrder)}</td>
            <td className="num">{money(i.lineTotal)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export default function OrderDetail() {
  const { orderId } = useParams()
  const { data: order, setData, error, loading } = useApi(`/customer/orders/${orderId}`, { pollMs: 4000 })
  const [confirming, setConfirming] = useState(false)
  const [actionError, setActionError] = useState(null)

  async function cancel() {
    setActionError(null)
    try {
      const { data } = await api.post(`/customer/orders/${orderId}/cancel`)
      setData(data)
    } catch (err) {
      setActionError(errorMessage(err))
    }
    setConfirming(false)
  }

  if (loading) return <Loading />
  if (error) return <ErrorBox message={error} />
  const cancellable = order.status === 'PLACED' || order.status === 'PACKED'

  return (
    <div className="grid-2">
      <section>
        <Link to="/orders" className="muted small">← All orders</Link>
        <div className="row-gap">
          <h1>Order #{order.id}</h1>
          <StatusBadge status={order.status} />
        </div>
        <div className="card">
          <OrderItemsTable items={order.items} />
          <p className="row-between"><span>Amount paid{order.couponCode && ` (coupon ${order.couponCode})`}</span><strong>{money(order.amount)}</strong></p>
        </div>
        <div className="card">
          <p><span className="muted">From</span> {order.storeName}</p>
          <p><span className="muted">To</span> {order.deliveryAddress}</p>
          <p>
            <span className="muted">Delivery partner</span>{' '}
            {order.partnerName ? `${order.partnerName} (${order.partnerPhone})` : 'Waiting for a free delivery partner'}
          </p>
          <p><span className="muted">Payment</span> {order.paymentMode} · {order.paymentStatus}</p>
        </div>
      </section>

      <section>
        <div className="card">
          <h2>Delivery timer</h2>
          <OrderTimer timer={order.timer} status={order.status} />
        </div>
        <div className="actions">
          <Link to={`/orders/${order.id}/receipt`} className="btn">View receipt</Link>
          {cancellable && !confirming && (
            <button className="btn btn-danger" onClick={() => setConfirming(true)}>Cancel order</button>
          )}
        </div>
        {confirming && (
          <div className="alert alert-warn">
            <p>Cancel this order? Items go back to the store and any online payment is refunded.</p>
            <div className="actions">
              <button className="btn btn-danger" onClick={cancel}>Yes, cancel it</button>
              <button className="btn" onClick={() => setConfirming(false)}>Keep order</button>
            </div>
          </div>
        )}
        <ErrorBox message={actionError} />
      </section>
    </div>
  )
}
