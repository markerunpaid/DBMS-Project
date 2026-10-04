import { Link } from 'react-router-dom'
import { Empty, ErrorBox, Loading } from '../../components/Feedback.jsx'
import { useApi } from '../../utils/useApi.js'

const hhmm = (t) => t?.slice(0, 5)

export default function AdminStores() {
  const { data: stores, error, loading } = useApi('/admin/stores', { pollMs: 5000 })

  return (
    <div>
      <h1>My dark stores</h1>
      <p className="muted">Stores where you are the manager. Open one to see its items and orders.</p>
      {loading && <Loading />}
      <ErrorBox message={error} />
      {stores?.length === 0 && <Empty>You don't manage any stores.</Empty>}
      <div className="card-grid">
        {stores?.map((s) => (
          <Link key={s.id} to={`/admin/stores/${s.id}`} className="card store-tile">
            <div className="row-between">
              <h3>{s.name}</h3>
              <span className={`badge ${s.openNow ? 'badge-open' : 'badge-cancelled'}`}>{s.openNow ? 'Open' : 'Closed'}</span>
            </div>
            <p className="muted small">{s.address}</p>
            <div className="stats">
              <div><strong>{s.productCount}</strong><span>products</span></div>
              <div className={s.outOfStockCount ? 'warn' : ''}><strong>{s.outOfStockCount}</strong><span>out of stock</span></div>
              <div><strong>{s.activeOrders}</strong><span>active orders</span></div>
            </div>
            <p className="muted small">
              Hours: {s.hours.length === 0 ? 'not set'
                : s.hours.every((h) => h.opensAt === s.hours[0].opensAt && h.closesAt === s.hours[0].closesAt)
                  ? `every day ${hhmm(s.hours[0].opensAt)}–${hhmm(s.hours[0].closesAt)}`
                  : s.hours.map((h) => `${h.day} ${hhmm(h.opensAt)}–${hhmm(h.closesAt)}`).join(', ')}
            </p>
          </Link>
        ))}
      </div>
    </div>
  )
}
