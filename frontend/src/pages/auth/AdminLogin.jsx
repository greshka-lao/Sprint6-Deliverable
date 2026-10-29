import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import Header from '../../components/Header'
import { useAuth } from '../../context/AuthContext'
import '../../App.css'

function AdminLogin() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [formData, setFormData] = useState({ username: '', password: '' })
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (user) {
    return <Navigate to="/admin/dashboard" replace />
  }

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((previous) => ({ ...previous, [name]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')

    if (!formData.username.trim() || !formData.password.trim()) {
      setError('Username and password are required.')
      return
    }

    setIsSubmitting(true)

    try {
      await login(formData.username, formData.password)
      navigate('/admin/dashboard')
    } catch (loginError) {
      setError(loginError.message || 'Invalid username or password.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div>
      <Header />

      <div className="auth-wrapper">
        <div className="auth-card">
          <div className="auth-header">
            <h2>Admin Sign In</h2>
          </div>

          <form className="auth-form" onSubmit={handleSubmit}>
            <label className="auth-field">
              <span
                className={`auth-field-label ${formData.username ? 'visible' : ''}`}
                style={{ display: formData.username ? 'block' : 'none' }}
              >
                Username
              </span>
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                placeholder="Username"
                autoComplete="username"
              />
            </label>

            <label className="auth-field">
              <span
                className={`auth-field-label ${formData.password ? 'visible' : ''}`}
                style={{ display: formData.password ? 'block' : 'none' }}
              >
                Password
              </span>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="Password"
                autoComplete="current-password"
              />
            </label>

            {error && <div className="auth-error">{error}</div>}

            <button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Signing in...' : 'Sign In'}
            </button>
          </form>

        </div>
      </div>
    </div>
  )
}

export default AdminLogin
