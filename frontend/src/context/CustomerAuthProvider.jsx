import { useCallback, useEffect, useMemo, useState } from 'react'
import { getCustomerSession, loginCustomer, logoutCustomer } from '../services/customerAuthService'
import CustomerAuthContext from './CustomerAuthValue'

export function CustomerAuthProvider({ children }) {
  const [customer, setCustomer] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const refreshSession = useCallback(async () => {
    setLoadError('')
    try {
      const session = await getCustomerSession()
      setCustomer(session?.authenticated ? session.customer : null)
    } catch (error) {
      if (error.status === 401) {
        setCustomer(null)
      } else {
        setLoadError(error.message)
      }
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let active = true
    getCustomerSession()
      .then((session) => {
        if (active) setCustomer(session?.authenticated ? session.customer : null)
      })
      .catch((error) => {
        if (!active) return
        if (error.status === 401) {
          setCustomer(null)
        } else {
          setLoadError(error.message)
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  const login = useCallback(async (username, password) => {
    const result = await loginCustomer(username, password)
    setCustomer(result)
    return result
  }, [])

  const logout = useCallback(async () => {
    try {
      await logoutCustomer()
    } finally {
      setCustomer(null)
    }
  }, [])

  const value = useMemo(() => ({
    customer,
    loading,
    loadError,
    login,
    logout,
    refreshSession
  }), [customer, loading, loadError, login, logout, refreshSession])

  return <CustomerAuthContext.Provider value={value}>{children}</CustomerAuthContext.Provider>
}
