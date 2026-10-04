import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, loadSession, saveSession, setUnauthorizedHandler } from '../api/client.js'

const AuthContext = createContext(null)

/** Landing page for each role after login. */
export const HOME = {
  CUSTOMER: '/shop',
  ADMIN: '/admin',
  PARTNER: '/partner',
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(loadSession)
  const navigate = useNavigate()

  const logout = useCallback(() => {
    saveSession(null)
    setUser(null)   // CartProvider sees the user change and drops this user's cart
    navigate('/login')
  }, [navigate])

  useEffect(() => setUnauthorizedHandler(logout), [logout])

  const finish = useCallback((session) => {
    saveSession(session)
    setUser(session)
    navigate(HOME[session.role])
  }, [navigate])

  const login = useCallback(async (role, email, password) => {
    const { data } = await api.post('/auth/login', { role, email, password })
    finish(data)
  }, [finish])

  const register = useCallback(async (form) => {
    const { data } = await api.post('/auth/register', form)
    finish(data)
  }, [finish])

  const value = useMemo(() => ({ user, login, register, logout }), [user, login, register, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
