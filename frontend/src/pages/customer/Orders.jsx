import { Link } from 'react-router-dom'
import { Empty, ErrorBox, Loading } from '../../components/Feedback.jsx'
import OrderTimer from '../../components/OrderTimer.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import { dateTime, money } from '../../utils/format.js'
import { useApi } from '../../utils/useApi.js'

export default function Orders() {
  const { data: orders, error, loading } = useApi('/customer/orders', { pollMs: 5000 })

  return (
    <div>
      <h1>My orders</h1>
      {loading && <Loading />}
      <ErrorBox message={error} />
      {orders?.length === 0 && <Empty>No orders yet. <Link to="/shop">Start shopping</Link></Empty>}
      <div className="stack">
        {orders?.map((o) => (
          <Link key={o.id} to={`/orders/${o.id}`} className="card order-row">
            <div>
              <div className="row-gap">
                <strong>Order #{o.id}</strong>
                <StatusBadge status={o.status} />
              </div>
              <p className="muted small">
                {o.storeName} · {dateTime(o.timer?.placedAt)} · {o.items.length} item{o.items.length > 1 ? 's' : ''}
              </p>
              <OrderTimer timer={o.timer} status={o.status} compact />
            </div>
            <span className="price">{money(o.amount)}</span>
          </Link>
        ))}
      </div>
    </div>
  )
}
