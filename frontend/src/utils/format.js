const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' })

export const money = (v) => inr.format(Number(v || 0))

/** Backend LocalDateTime strings ("2026-09-29T18:10:00") are local time. */
export const toDate = (s) => (s ? new Date(s) : null)

export function dateTime(s) {
  const d = toDate(s)
  return d ? d.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '—'
}

export function time(s) {
  const d = toDate(s)
  return d ? d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }) : '—'
}

/** 125 -> "2:05" */
export function mmss(totalSeconds) {
  const s = Math.max(0, Math.floor(totalSeconds))
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`
}

export const STATUS_LABEL = {
  PLACED: 'Placed',
  PACKED: 'Packed',
  OUT_FOR_DELIVERY: 'Out for delivery',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
}

export const PAYMENT_MODES = ['UPI', 'CARD', 'WALLET', 'NETBANKING', 'CASH']

/** Same rule as OrderService.place -- the backend recomputes it, this is only a preview. */
export function couponDiscount(coupon, subtotal) {
  if (!coupon) return 0
  const raw = coupon.discountType === 'PERCENTAGE'
    ? Math.round(subtotal * Number(coupon.value)) / 100
    : Number(coupon.value)
  return Math.min(raw, subtotal)
}
