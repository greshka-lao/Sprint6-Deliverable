import { Navigate } from 'react-router-dom'
import { useCustomerAuth } from '../context/useCustomerAuth'

function CustomerProtectedRoute({ children }) {
  const { customer, loading, loadError, refreshSession } = useCustomerAuth()

  if (loading) {
    return <div className="loading-state">Checking your customer session...</div>
  }

  if (loadError) {
    return (
      <main className="auth-card" role="alert">
        <p>{loadError}</p>
        <button type="button" onClick={refreshSession}>Try again</button>
      </main>
    )
  }

  if (!customer || customer.role !== 'CUSTOMER') {
    return <Navigate to="/customer/login" replace />
  }

  return children
}

export default CustomerProtectedRoute
