import { useState } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage } from '../api/client.js'
import { ErrorBox } from '../components/Feedback.jsx'
import { useAuth } from '../context/AuthContext.jsx'

const EMPTY = { firstName: '', middleName: '', lastName: '', phone: '', email: '', password: '' }

export default function Register() {
  const { register } = useAuth()
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const field = (name) => ({
    value: form[name],
    onChange: (e) => setForm({ ...form, [name]: e.target.value }),
  })

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await register(form)
    } catch (err) {
      setError(errorMessage(err))
      setBusy(false)
    }
  }

  return (
    <div className="auth-card card">
      <h1>Create account</h1>
      <form onSubmit={submit} className="form">
        <div className="row3">
          <label>First name<input {...field('firstName')} required /></label>
          <label>Middle name<input {...field('middleName')} /></label>
          <label>Last name<input {...field('lastName')} /></label>
        </div>
        <label>Phone<input {...field('phone')} placeholder="9876543210" required /></label>
        <label>Email<input type="email" {...field('email')} required /></label>
        <label>Password<input type="password" {...field('password')} minLength={6} required /></label>
        <ErrorBox message={error} />
        <button className="btn btn-primary" disabled={busy}>{busy ? 'Creating…' : 'Sign up'}</button>
      </form>
      <p>Already registered? <Link to="/login">Log in</Link></p>
    </div>
  )
}
