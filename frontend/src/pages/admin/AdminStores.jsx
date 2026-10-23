import { Link } from 'react-router-dom'
import { Empty, ErrorBox, Loading } from '../../components/Feedback.jsx'
import { useApi } from '../../utils/useApi.js'

const hhmm = (t) => t?.slice(0, 5)

export default function AdminStores() {
  const { data: stores, error, loading } = useApi('/admin/stores', { pollMs: 5000 })
  const activeOrders = stores?.reduce((sum, store) => sum + store.activeOrders, 0) || 0
  const stockAlerts = stores?.reduce((sum, store) => sum + store.outOfStockCount, 0) || 0

  return (
    <div>
      <section className="ops-hero manager-hero">
        <div><span className="eyebrow">QUICKCART OPERATIONS</span><h1>Your stores, at a glance.</h1>
          <p>Keep shelves ready and orders moving across your stores.</p></div>
        <div className="ops-hero-icon" aria-hidden="true">⌂</div>
      </section>
      <div className="ops-metrics">
        <div className="ops-metric"><span>My stores</span><strong>{stores?.length ?? '—'}</strong><small>Locations you manage</small></div>
        <div className="ops-metric"><span>Active orders</span><strong>{activeOrders}</strong><small>Being prepared or delivered</small></div>
        <div className={`ops-metric ${stockAlerts ? 'metric-alert' : ''}`}><span>Out of stock</span><strong>{stockAlerts}</strong><small>Items needing a restock</small></div>
      </div>
      <div className="section-heading"><div><span className="eyebrow">LIVE OVERVIEW</span><h2>Store locations</h2></div><span className="live-indicator"><i /> Live updates</span></div>
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
