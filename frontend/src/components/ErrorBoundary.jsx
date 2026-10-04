import { Component } from 'react'

/** A render crash shows what went wrong instead of a blank white page. */
export default class ErrorBoundary extends Component {
  state = { error: null }

  static getDerivedStateFromError(error) {
    return { error }
  }

  componentDidCatch(error, info) {
    console.error('Page crashed:', error, info.componentStack)
  }

  componentDidUpdate(prev) {
    // navigating to another page clears the error
    if (prev.resetKey !== this.props.resetKey && this.state.error) this.setState({ error: null })
  }

  render() {
    if (!this.state.error) return this.props.children
    return (
      <div className="alert alert-error">
        <p><strong>This page hit an error:</strong> {String(this.state.error.message || this.state.error)}</p>
        <div className="actions">
          <button className="btn" onClick={() => this.setState({ error: null })}>Try again</button>
          <button className="btn" onClick={() => {
            Object.keys(localStorage).filter((k) => k.startsWith('qc_cart')).forEach((k) => localStorage.removeItem(k))
            window.location.assign('/')
          }}>
            Reset cart and go home
          </button>
        </div>
      </div>
    )
  }
}
