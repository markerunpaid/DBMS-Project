import { useState } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage } from '../api/client.js'
import { ErrorBox } from '../components/Feedback.jsx'
import { useAuth } from '../context/AuthContext.jsx'

const ROLES = [
  { id: 'CUSTOMER', label: 'Customer', accounts: [{ name: 'Aarav Sharma', email: 'customer@qc.com' }] },
  { id: 'ADMIN', label: 'Store manager', accounts: [{ name: 'Ravi Kumar · Lanka & Sigra', email: 'admin@qc.com' }, { name: 'Neha Singh · Sarnath', email: 'admin2@qc.com' }] },
  { id: 'PARTNER', label: 'Delivery partner', accounts: [{ name: 'Amit Verma', email: 'partner1@qc.com' }, { name: 'Rahul Gupta', email: 'partner2@qc.com' }, { name: 'Vikram Chauhan', email: 'partner3@qc.com' }] },
]
const DEMO_PASSWORD = 'QuickCart@2026'

export default function Login() {
  const { login } = useAuth()
  const [role, setRole] = useState('CUSTOMER')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const selectedRole = ROLES.find((item) => item.id === role)

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

  function useDemo(account) {
    setEmail(account.email)
    setPassword(DEMO_PASSWORD)
    setError(null)
  }

  return (
    <div className="auth-page">
      <aside className="auth-visual">
        <img src="https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=1400&q=90" alt="Fresh produce at a neighborhood market" />
        <div className="auth-visual-shade" />
        <div className="auth-brand"><span className="brand-mark" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none"><path d="M3 4h2l2.1 10.2a2 2 0 0 0 2 1.6h8.4a2 2 0 0 0 1.9-1.4L21 8H6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/><circle cx="10" cy="20" r="1.5" fill="currentColor"/><circle cx="18" cy="20" r="1.5" fill="currentColor"/></svg></span><span>QuickCart</span></div>
        <div className="auth-story-copy">
          <span className="auth-kicker"><i /> YOUR NEIGHBORHOOD, ON YOUR SCHEDULE</span>
          <h1>Good things<br />are closer than<br /><em>you think.</em></h1>
          <p>Everyday groceries from nearby stores, picked with care and brought right to your door.</p>
          <div className="auth-story-tags"><span>Fresh produce</span><span>Local stores</span><span>Easy delivery</span></div>
        </div>
        <div className="auth-location"><span>⌖</span> Made for Varanasi <i>·</i> Built around your neighborhood</div>
      </aside>

      <section className="auth-form-side">
        <div className="auth-form-wrap">
          <div className="auth-mobile-brand"><span className="brand-mark" aria-hidden="true">↗</span> QuickCart</div>
          <span className="eyebrow">WELCOME TO QUICKCART</span>
          <h2>Sign in to get started</h2>
          <p className="auth-intro">Choose your account type, then sign in to continue.</p>

          <div className="role-switch" role="tablist" aria-label="Account type">
            {ROLES.map((item) => (
              <button key={item.id} type="button" role="tab" aria-selected={role === item.id}
                className={role === item.id ? 'role-option active' : 'role-option'}
                onClick={() => { setRole(item.id); setError(null) }}>
                {item.label}
              </button>
            ))}
          </div>

          <form onSubmit={submit} className="form auth-form">
            <label>Email address
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="username" autoFocus />
            </label>
            <label>Password
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" />
            </label>
            <ErrorBox message={error} />
            <button className="btn btn-primary auth-submit" disabled={busy}>{busy ? 'Signing you in…' : <>Continue <span aria-hidden="true">→</span></>}</button>
          </form>

          <div className="demo-box">
            <div className="demo-heading"><span className="demo-spark">✦</span><div><strong>Just exploring?</strong><small>Use a seeded demo account</small></div></div>
            <div className="demo-accounts">
              {selectedRole.accounts.map((account) => (
                <button key={account.email} type="button" className="demo-account" onClick={() => useDemo(account)}>
                  <span>{account.name}</span><b>Use demo <span aria-hidden="true">↗</span></b>
                </button>
              ))}
            </div>
            <p className="demo-password">Demo password <code>{DEMO_PASSWORD}</code></p>
          </div>

          {role === 'CUSTOMER' && <p className="auth-register">New to QuickCart? <Link to="/register">Create an account <span aria-hidden="true">→</span></Link></p>}
          <p className="auth-footnote">By continuing, you agree to shop local and eat well.</p>
        </div>
      </section>
    </div>
  )
}
