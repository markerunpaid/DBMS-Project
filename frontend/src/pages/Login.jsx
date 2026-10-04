import { useState } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage } from '../api/client.js'
import { ErrorBox } from '../components/Feedback.jsx'
import { useAuth } from '../context/AuthContext.jsx'

const ROLES = [
  ['CUSTOMER', 'Customer', 'customer@qc.com'],
  ['ADMIN', 'Store admin', 'admin@qc.com'],
  ['PARTNER', 'Delivery partner', 'partner1@qc.com'],
]

export default function Login() {
  const { login } = useAuth()
  const [role, setRole] = useState('CUSTOMER')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(role, email, password)
    } catch (err) {
      setError(errorMessage(err))
      setBusy(false)
    }
  }

  const sample = ROLES.find(([r]) => r === role)[2]

  return (
    <div className="auth-card card">
      <h1>Log in</h1>
      <div className="tabs" role="tablist">
        {ROLES.map(([r, label]) => (
          <button key={r} type="button" role="tab" aria-selected={role === r}
                  className={role === r ? 'tab active' : 'tab'} onClick={() => setRole(r)}>
            {label}
          </button>
        ))}
      </div>
      <form onSubmit={submit} className="form">
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        <ErrorBox message={error} />
        <button className="btn btn-primary" disabled={busy}>{busy ? 'Logging in…' : 'Log in'}</button>
      </form>
      <p className="muted small">
        Demo account: <code>{sample}</code> / <code>QuickCart@2026</code>
      </p>
      {role === 'CUSTOMER' && (
        <p>New here? <Link to="/register">Create a customer account</Link></p>
      )}
    </div>
  )
}
