import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useCart } from '../context/CartContext.jsx'

const LINKS = {
  CUSTOMER: [
    ['/shop', 'Shop'],
    ['/checkout', 'Cart'],
    ['/orders', 'My orders'],
    ['/addresses', 'Addresses'],
  ],
  ADMIN: [['/admin', 'My stores']],
  PARTNER: [
    ['/partner', 'Profile'],
    ['/partner/deliveries', 'Deliveries'],
  ],
}

const ROLE_LABEL = { CUSTOMER: 'Customer', ADMIN: 'Store admin', PARTNER: 'Delivery partner' }

export default function NavBar() {
  const { user, logout } = useAuth()
  const { count } = useCart()

  return (
    <header className="nav no-print">
      <div className="nav-inner">
        <span className="brand"><span className="brand-mark" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none"><path d="M3 4h2l2.1 10.2a2 2 0 0 0 2 1.6h8.4a2 2 0 0 0 1.9-1.4L21 8H6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/><circle cx="10" cy="20" r="1.5" fill="currentColor"/><circle cx="18" cy="20" r="1.5" fill="currentColor"/></svg></span>QuickCart</span>
        {user && (
          <nav className="nav-links">
            {LINKS[user.role].map(([to, label]) => (
              <NavLink key={to} to={to} end>
                {label}
                {to === '/checkout' && count > 0 && <span className="pill">{count}</span>}
              </NavLink>
            ))}
          </nav>
        )}
        {user && (
          <div className="nav-user">
            <span className="muted">
              {user.name} · {ROLE_LABEL[user.role]}
            </span>
            <button className="btn btn-ghost" onClick={logout}>Log out</button>
          </div>
        )}
      </div>
    </header>
  )
}
