import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import Header from '../../components/Header'
import { useCustomerAuth } from '../../context/useCustomerAuth'
import '../../App.css'

function CustomerLogin() {
  const { customer, login } = useCustomerAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (customer) {
    return <Navigate to="/customer/home" replace />
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(username, password)
      setPassword('')
      navigate('/customer/home')
    } catch (loginError) {
      const retryText = loginError.status === 429 && loginError.retryAfter
        ? ` Try again in ${loginError.retryAfter} seconds.`
        : ''
      setError(`${loginError.message}${retryText}`)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <Header />
      <main className="auth-wrapper">
        <section className="auth-card">
          <div className="auth-header">
            <h2>Customer Sign In</h2>
          </div>

          <form className="auth-form" onSubmit={handleSubmit}>
            <label className="auth-field">
              <span className={`auth-field-label ${username ? 'visible' : ''}`} style={{ display: username ? 'block' : 'none' }}>
                Username
              </span>
              <input
                type="text"
                name="username"
                aria-label="Username"
                autoComplete="username"
                placeholder="Username"
                required
                value={username}
                onChange={(event) => setUsername(event.target.value)}
              />
            </label>
            <label className="auth-field">
              <span className={`auth-field-label ${password ? 'visible' : ''}`} style={{ display: password ? 'block' : 'none' }}>
                Password
              </span>
              <input
                type="password"
                name="password"
                aria-label="Password"
                autoComplete="current-password"
                placeholder="Password"
                required
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </label>
            {error && <div className="auth-error" role="alert">{error}</div>}
            <button type="submit" disabled={submitting}>
              {submitting ? 'Signing in...' : 'Sign In'}
            </button>
          </form>

          <div className="auth-helper">
            <p>New customer? <Link to="/customer/register">Create an account</Link></p>
          </div>
        </section>
      </main>
    </>
  )
}

export default CustomerLogin
