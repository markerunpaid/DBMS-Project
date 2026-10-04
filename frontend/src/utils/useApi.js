import { useCallback, useEffect, useState } from 'react'
import { api, errorMessage } from '../api/client.js'

/**
 * GET `url` on mount (and whenever it changes). `pollMs` re-fetches quietly in the
 * background, which keeps order status / timers fresh across roles.
 */
export function useApi(url, { pollMs } = {}) {
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(Boolean(url))

  const reload = useCallback(async ({ quiet = false } = {}) => {
    if (!url) return
    if (!quiet) setLoading(true)
    try {
      const res = await api.get(url)
      setData(res.data)
      setError(null)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [url])

  useEffect(() => {
    reload()
  }, [reload])

  useEffect(() => {
    if (!pollMs) return undefined
    const id = setInterval(() => reload({ quiet: true }), pollMs)
    // browsers slow down timers in background tabs -- catch up the moment the tab is shown again
    const onShow = () => { if (document.visibilityState === 'visible') reload({ quiet: true }) }
    document.addEventListener('visibilitychange', onShow)
    window.addEventListener('focus', onShow)
    return () => {
      clearInterval(id)
      document.removeEventListener('visibilitychange', onShow)
      window.removeEventListener('focus', onShow)
    }
  }, [reload, pollMs])

  return { data, setData, error, loading, reload }
}
