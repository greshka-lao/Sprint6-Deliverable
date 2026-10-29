import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function ProtectedRoute({ children }) {
  const { user, loading } = useAuth()

  if (loading) {
    return <div className="loading-state">Checking your session...</div>
  }

  const role = user?.role?.toUpperCase()
  const hasAdminRole = ['BANK_TELLER', 'BANK_MANAGER', 'ADMIN'].includes(role)

  if (!user || !hasAdminRole) {
    return <Navigate to="/admin/login" replace />
  }

  return children
}

export default ProtectedRoute
