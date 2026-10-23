import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api, errorMessage } from '../../api/client.js'
import { ErrorBox, Loading } from '../../components/Feedback.jsx'
import { useApi } from '../../utils/useApi.js'

const STATUS_TEXT = {
  AVAILABLE: 'Available for new deliveries',
  BUSY: 'On a delivery',
  OFFLINE: 'Offline',
}

export default function PartnerProfile() {
  const { data: me, setData, error, loading } = useApi('/partner/me', { pollMs: 4000 })
  const [actionError, setActionError] = useState(null)

  async function setStatus(status) {
    setActionError(null)
    try {
      const { data } = await api.put('/partner/me/status', { status })
      setData(data)
    } catch (err) {
      setActionError(errorMessage(err))
    }
  }

  if (loading) return <Loading />
  if (error) return <ErrorBox message={error} />

  return (
    <div className="partner-dashboard">
      <section className="ops-hero partner-hero">
        <div><span className="eyebrow">DELIVERY PARTNER</span><h1>Welcome back, {me.name.split(' ')[0]}.</h1><p>{me.storeName ? `You're connected to ${me.storeName}.` : 'Your delivery profile and shift controls.'}</p></div>
        <div className="rider-art" aria-hidden="true"><span>🛵</span><i /><i /><i /></div>
      </section>
      <div className="ops-metrics partner-metrics">
        <div className="ops-metric"><span>Active deliveries</span><strong>{me.activeDeliveries}</strong><small>Currently assigned to you</small></div>
        <div className="ops-metric"><span>Completed</span><strong>{me.completedDeliveries}</strong><small>Successful handoffs</small></div>
        <div className="ops-metric"><span>Shift status</span><strong className={`status-${me.status.toLowerCase()}`}>{me.status === 'AVAILABLE' ? 'Online' : me.status === 'BUSY' ? 'On route' : 'Offline'}</strong><small>Tap below to update</small></div>
      </div>
      <div className="grid-2">
      <section className="card profile-card">
        <div className="profile-heading"><span className="profile-avatar">{me.name.split(' ').map((part) => part[0]).slice(0, 2).join('')}</span><div><span className="eyebrow">YOUR PROFILE</span><h2>{me.name}</h2></div></div>
        <dl className="details">
          <dt>Email</dt><dd>{me.email}</dd>
          <dt>Phone</dt><dd>{me.phone}</dd>
          <dt>Vehicle</dt><dd>{me.vehicleNumber || 'No vehicle (on foot / bicycle)'}</dd>
          <dt>Home store</dt><dd>{me.storeName ? `${me.storeName} — ${me.storeAddress}` : 'Not attached to a store'}</dd>
        </dl>
      </section>

      <section className="card">
        <span className="eyebrow">SHIFT CONTROLS</span><h2>Availability</h2>
        <p className={`status-${me.status.toLowerCase()}`}>● {STATUS_TEXT[me.status]}</p>
        <div className="actions">
          <button className="btn btn-primary" disabled={me.status !== 'OFFLINE'} onClick={() => setStatus('AVAILABLE')}>Go online</button>
          <button className="btn" disabled={me.status !== 'AVAILABLE'} onClick={() => setStatus('OFFLINE')}>Go offline</button>
        </div>
        <ErrorBox message={actionError} />

        <div className="stats">
          <div><strong>{me.activeDeliveries}</strong><span>active</span></div>
          <div><strong>{me.completedDeliveries}</strong><span>delivered</span></div>
        </div>
        <Link to="/partner/deliveries" className="btn">Open delivery list →</Link>
      </section>
      </div>
    </div>
  )
}
