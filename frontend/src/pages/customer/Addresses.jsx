import { Link } from 'react-router-dom'
import AddressForm from '../../components/AddressForm.jsx'
import { Empty, ErrorBox, Loading } from '../../components/Feedback.jsx'
import { useApi } from '../../utils/useApi.js'

export default function Addresses() {
  const { data: addresses, error, loading, reload } = useApi('/customer/addresses')

  return (
    <div className="grid-2">
      <section>
        <h1>Saved addresses</h1>
        {loading && <Loading />}
        <ErrorBox message={error} />
        {addresses?.length === 0 && <Empty>No addresses yet — add one to find stores near you.</Empty>}
        <div className="stack">
          {addresses?.map((a) => (
            <div key={a.addressId} className="card">
              <div className="row-between">
                <strong>{a.label || 'Address'}</strong>
                {a.isDefault && <span className="badge">Default</span>}
              </div>
              <p>{a.formatted}</p>
              <p className="muted small">{a.latitude}, {a.longitude}</p>
            </div>
          ))}
        </div>
        {addresses?.length > 0 && <Link to="/shop" className="btn btn-primary">Shop from nearby stores →</Link>}
      </section>

      <section className="card">
        <h2>Add an address</h2>
        <AddressForm onSaved={() => reload({ quiet: true })} />
      </section>
    </div>
  )
}
