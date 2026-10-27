import { Navigate, Route, Routes } from 'react-router-dom'
import Customers from './pages/customer/Customers'
import Accounts from './pages/account/Accounts'
import AddCustomer from './pages/customer/AddCustomer'
import AddAccount from './pages/account/AddAccount'
import UpdateCustomer from './pages/customer/UpdateCustomer'
import UpdateAccount from './pages/account/UpdateAccount'
import Login from './pages/auth/Login'
import AdminDashboard from './pages/admin/AdminDashboard'
import ProtectedRoute from './components/ProtectedRoute'
import { AuthProvider } from './context/AuthContext'
import './App.css'

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/dashboard" element={<ProtectedRoute><AdminDashboard /></ProtectedRoute>} />
        <Route path="/" element={<Navigate to="/customer" replace />} />
        <Route path="/customer" element={<ProtectedRoute><Customers /></ProtectedRoute>} />
        <Route path="/customer/add" element={<ProtectedRoute><AddCustomer /></ProtectedRoute>} />
        <Route path="/account/add" element={<ProtectedRoute><AddAccount /></ProtectedRoute>} />
        <Route path="/account" element={<ProtectedRoute><Accounts /></ProtectedRoute>} />
        <Route path="/customer/edit/:customerId" element={<ProtectedRoute><UpdateCustomer /></ProtectedRoute>} />
        <Route path="/account/edit/:accountId" element={<ProtectedRoute><UpdateAccount /></ProtectedRoute>} />
      </Routes>
    </AuthProvider>
  )
}

export default App;
