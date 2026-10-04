import { Link, useParams } from 'react-router-dom'
import { ErrorBox, Loading } from '../../components/Feedback.jsx'
import { dateTime, money, STATUS_LABEL, time } from '../../utils/format.js'
import { useApi } from '../../utils/useApi.js'
import { OrderItemsTable } from './OrderDetail.jsx'

export default function Receipt() {
  const { orderId } = useParams()
  const { data: r, error, loading } = useApi(`/customer/orders/${orderId}/receipt`)

  if (loading) return <Loading />
  if (error) return <ErrorBox message={error} />

  return (
    <div>
      <div className="actions no-print">
        <Link to={`/orders/${orderId}`} className="btn">← Back to order</Link>
        <button className="btn btn-primary" onClick={() => window.print()}>Print receipt</button>
      </div>

      <article className="card receipt">
        <header className="row-between">
          <div>
            <h1>⚡ QuickCart</h1>
            <p className="muted">{r.storeName}<br />{r.storeAddress}</p>
          </div>
          <div className="right">
            <h2>Receipt</h2>
            <p>Order #{r.orderId}<br />{dateTime(r.placedAt)}<br />Status: {STATUS_LABEL[r.status]}</p>
          </div>
        </header>

        <section className="receipt-parties">
          <div>
            <h4>Billed to</h4>
            <p>{r.customerName}<br />{r.customerEmail}<br />{r.customerPhone}</p>
          </div>
          <div>
            <h4>Delivered to</h4>
            <p>{r.deliveryAddress}</p>
          </div>
          <div>
            <h4>Delivery partner</h4>
            <p>{r.partnerName ? <>{r.partnerName}<br />{r.partnerPhone}</> : 'Not assigned yet'}</p>
          </div>
          <div>
            <h4>Delivery timeline</h4>
            <p className="small">
              Placed: {time(r.timer?.placedAt)}<br />
              Expected by: {time(r.timer?.expectedDeliveryAt)}<br />
              Out for delivery: {time(r.timer?.outForDeliveryAt)}<br />
              {r.timer?.cancelledAt ? <>Cancelled: {time(r.timer.cancelledAt)}</> : <>Received: {time(r.timer?.receivedAt)}</>}
            </p>
          </div>
        </section>

        <OrderItemsTable items={r.items} />

        <dl className="totals">
          <dt>Subtotal</dt><dd>{money(r.subtotal)}</dd>
          {Number(r.discount) > 0 && (<><dt>Coupon {r.couponCode}</dt><dd>− {money(r.discount)}</dd></>)}
          <dt className="total">Total</dt><dd className="total">{money(r.total)}</dd>
        </dl>

        <p className="muted small">
          Paid via {r.paymentMode} · {r.paymentStatus}
          {r.paymentTime && ` · ${dateTime(r.paymentTime)}`}
        </p>
        <p className="muted small">Item prices are the prices at the time of ordering.</p>
      </article>
    </div>
  )
}
