import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function AdminProtectedRoute({ children }) {
  const { user, loading } = useAuth()

  if (loading) {
    return <div className="loading-state">Checking your session...</div>
  }

  const role = user?.role?.toUpperCase()
  if (!['BANK_TELLER', 'BANK_MANAGER', 'ADMIN'].includes(role)) {
    return <Navigate to="/admin/login" replace />
  }

  return children
}

export default AdminProtectedRoute
