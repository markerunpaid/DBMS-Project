import { Navigate, Outlet } from 'react-router-dom'
import { HOME, useAuth } from '../context/AuthContext.jsx'

/**
 * Client-side guard: not logged in -> /login; wrong role -> that role's home.
 * The backend enforces the same rules on every /api/{role}/** call.
 */
export default function ProtectedRoute({ role }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (user.role !== role) return <Navigate to={HOME[user.role]} replace />
  return <Outlet />
}
