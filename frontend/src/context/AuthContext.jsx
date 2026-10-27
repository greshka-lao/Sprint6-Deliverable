import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { clearStoredSession, getStoredToken, loginAdmin, logoutAdmin, setStoredToken, validateSession } from '../services/authService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const restoreSession = async () => {
      const token = getStoredToken()
      if (!token) {
        setLoading(false)
        return
      }

      try {
        const session = await validateSession(token)
        setUser({
          username: session.username,
          role: session.role,
          token
        })
      } catch (error) {
        clearStoredSession()
        setUser(null)
      } finally {
        setLoading(false)
      }
    }

    restoreSession()
  }, [])

  const login = async (username, password) => {
    const response = await loginAdmin(username, password)
    setStoredToken(response.token)
    setUser({
      username: response.username,
      role: response.role,
      token: response.token
    })
    return response
  }

  const logout = async () => {
    const token = getStoredToken()
    if (token) {
      try {
        await logoutAdmin(token)
      } catch (error) {
        console.warn('Logout warning:', error.message)
      }
    }

    clearStoredSession()
    setUser(null)
  }

  const value = useMemo(() => ({ user, login, logout, loading }), [user, loading])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
