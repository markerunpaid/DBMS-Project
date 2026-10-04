export function Loading({ text = 'Loading…' }) {
  return <p className="muted">{text}</p>
}

export function ErrorBox({ message }) {
  if (!message) return null
  return <div className="alert alert-error">{message}</div>
}

export function Empty({ children }) {
  return <div className="empty">{children}</div>
}
