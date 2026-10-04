import { useEffect, useState } from 'react'
import { mmss, time, toDate } from '../utils/format.js'

/**
 * Renders one order_timer row: a live countdown to expected_delivery_at while the
 * order is in progress, and the placed -> out for delivery -> received/cancelled timeline.
 */
export default function OrderTimer({ timer, status, compact = false }) {
  const [now, setNow] = useState(() => Date.now())
  const running = status === 'PLACED' || status === 'PACKED' || status === 'OUT_FOR_DELIVERY'

  useEffect(() => {
    if (!running) return undefined
    const id = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(id)
  }, [running])

  if (!timer) return null
  const placed = toDate(timer.placedAt)
  const expected = toDate(timer.expectedDeliveryAt)

  let headline
  if (status === 'DELIVERED') {
    const took = (toDate(timer.receivedAt) - placed) / 1000
    const onTime = toDate(timer.receivedAt) <= expected
    headline = (
      <span className={onTime ? 'timer-ok' : 'timer-late'}>
        Delivered in {mmss(took)} min {onTime ? '· on time' : '· late'}
      </span>
    )
  } else if (status === 'CANCELLED') {
    headline = <span className="muted">Cancelled at {time(timer.cancelledAt)}</span>
  } else {
    const left = (expected - now) / 1000
    headline = left >= 0
      ? <span className="timer-live">Arriving in <strong>{mmss(left)}</strong></span>
      : <span className="timer-late">Running late by <strong>{mmss(-left)}</strong></span>
  }

  if (compact) return <div className="timer compact">{headline}</div>

  const steps = [
    ['Placed', timer.placedAt],
    ['Out for delivery', timer.outForDeliveryAt],
    status === 'CANCELLED' ? ['Cancelled', timer.cancelledAt] : ['Received', timer.receivedAt],
  ]

  return (
    <div className="timer">
      <div className="timer-headline">{headline}</div>
      <ol className="timeline">
        {steps.map(([label, at]) => (
          <li key={label} className={at ? 'done' : ''}>
            <span className="dot" />
            <span>{label}</span>
            <span className="muted">{at ? time(at) : '—'}</span>
          </li>
        ))}
      </ol>
      <p className="muted small">Expected by {time(timer.expectedDeliveryAt)}</p>
    </div>
  )
}
