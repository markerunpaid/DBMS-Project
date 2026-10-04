import { useState } from 'react'
import { api, errorMessage } from '../api/client.js'
import { ErrorBox } from './Feedback.jsx'

const EMPTY = {
  label: 'Home', houseNo: '', street: '', pinCode: '', city: '', state: '',
  latitude: '', longitude: '', makeDefault: false,
}

// Real places near the seeded dark stores, so a demo address always has stores in range.
const SAMPLES = [
  { name: 'IIT BHU hostel', label: 'Hostel', houseNo: 'Room 101', street: 'IIT BHU Campus', pinCode: '221005', latitude: '25.262000', longitude: '82.989000' },
  { name: 'Assi Ghat', label: 'Home', houseNo: 'B 1/12', street: 'Assi Ghat Road', pinCode: '221005', latitude: '25.289100', longitude: '83.006500' },
  { name: 'Godowlia', label: 'Work', houseNo: '18/42', street: 'Dashashwamedh Road', pinCode: '221001', latitude: '25.309500', longitude: '83.007500' },
]

/** Saves a new address for the logged-in customer, then calls onSaved(address). */
export default function AddressForm({ onSaved, makeDefault = false }) {
  const [form, setForm] = useState({ ...EMPTY, makeDefault })
  const [error, setError] = useState(null)
  const [locating, setLocating] = useState(false)
  const [saving, setSaving] = useState(false)

  const field = (name) => ({
    value: form[name],
    onChange: (e) => setForm({ ...form, [name]: e.target.value }),
  })

  function useMyLocation() {
    if (!navigator.geolocation) {
      setError('This browser cannot share its location — pick a sample or type the coordinates.')
      return
    }
    setLocating(true)
    setError(null)
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setForm((f) => ({ ...f, latitude: pos.coords.latitude.toFixed(6), longitude: pos.coords.longitude.toFixed(6) }))
        setLocating(false)
      },
      (err) => {
        setError(`Couldn't get your location (${err.message}). Pick a sample location instead.`)
        setLocating(false)
      },
      { timeout: 10000 },
    )
  }

  function useSample(s) {
    setError(null)
    setForm((f) => ({ ...f, ...s, city: '', state: '' }))
  }

  async function submit(e) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const { data } = await api.post('/customer/addresses', {
        ...form,
        latitude: Number(form.latitude),
        longitude: Number(form.longitude),
      })
      setForm({ ...EMPTY, makeDefault })
      onSaved?.(data)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={submit} className="form">
      <div className="quick-fill">
        <span className="muted small">Quick fill:</span>
        {SAMPLES.map((s) => (
          <button key={s.name} type="button" className="chip" onClick={() => useSample(s)}>{s.name}</button>
        ))}
        <button type="button" className="chip" onClick={useMyLocation} disabled={locating}>
          {locating ? 'Locating…' : '📍 My location'}
        </button>
      </div>
      <div className="row3">
        <label>Label<input {...field('label')} placeholder="Home / Work" /></label>
        <label>House / flat<input {...field('houseNo')} /></label>
        <label>PIN code<input {...field('pinCode')} pattern="[0-9]{6}" title="6 digits" required /></label>
      </div>
      <label>Street<input {...field('street')} /></label>
      <div className="row3">
        <label>City<input {...field('city')} placeholder="only for a new PIN" /></label>
        <label>State<input {...field('state')} placeholder="only for a new PIN" /></label>
      </div>
      <div className="row3">
        <label>Latitude<input {...field('latitude')} type="number" step="any" required /></label>
        <label>Longitude<input {...field('longitude')} type="number" step="any" required /></label>
      </div>
      <label className="check">
        <input type="checkbox" checked={form.makeDefault}
               onChange={(e) => setForm({ ...form, makeDefault: e.target.checked })} />
        Make this my default address
      </label>
      <ErrorBox message={error} />
      <button className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save address'}</button>
    </form>
  )
}
