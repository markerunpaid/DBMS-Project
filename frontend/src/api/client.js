import axios from 'axios'

const STORAGE_KEY = 'qc_auth'

export const api = axios.create({ baseURL: '/api' })

let onUnauthorized = () => {}

/** AuthContext registers its logout here so an expired token signs the user out. */
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

// The login lives in sessionStorage, which is private to each browser TAB (and survives a
// refresh). So one browser can run customer, admin and partner side by side, one per tab,
// without one login replacing another.
export function loadSession() {
  try {
    return JSON.parse(sessionStorage.getItem(STORAGE_KEY))
  } catch {
    return null
  }
}

export function saveSession(session) {
  if (session) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
  else sessionStorage.removeItem(STORAGE_KEY)
}

api.interceptors.request.use((config) => {
  const session = loadSession()
  if (session?.token) config.headers.Authorization = `Bearer ${session.token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const isLogin = err.config?.url?.startsWith('/auth/')
    if (err.response?.status === 401 && !isLogin) onUnauthorized()
    return Promise.reject(err)
  },
)

const BACKEND_DOWN = "Can't reach the server. Is the backend running? (backend folder: .\\mvnw.cmd spring-boot:run)"

/** The backend always answers errors as {"message": "..."}. */
export function errorMessage(err) {
  const res = err?.response
  if (!res) return BACKEND_DOWN                       // network error
  if (res.data?.message) return res.data.message
  // Vite's proxy answers 500/502/504 with no JSON body when Spring Boot isn't up
  if (res.status >= 500) return BACKEND_DOWN
  return err?.message || 'Something went wrong'
}
