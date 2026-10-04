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
        <span className="brand">⚡ QuickCart</span>
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
