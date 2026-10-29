import { Link, Navigate, Route, Routes } from 'react-router-dom'
import Customers from './pages/admin/customer/Customers'
import Accounts from './pages/admin/account/Accounts'
import AddCustomer from './pages/admin/customer/AddCustomer'
import AddAccount from './pages/admin/account/AddAccount'
import UpdateCustomer from './pages/admin/customer/UpdateCustomer'
import UpdateAccount from './pages/admin/account/UpdateAccount'
import AdminLogin from './pages/auth/AdminLogin'
import AdminDashboard from './pages/admin/AdminDashboard'
import AdminProtectedRoute from './components/AdminProtectedRoute'
import { AuthProvider } from './context/AuthContext'
import CustomerLogin from './pages/auth/CustomerLogin'
import CustomerRegistration from './pages/auth/CustomerRegistration'
import CustomerHome from './pages/customer/CustomerHome'
import CustomerProtectedRoute from './components/CustomerProtectedRoute'
import { CustomerAuthProvider } from './context/CustomerAuthProvider'
import './App.css'

function PublicHome() {
  return (
    <main className="customer-public-home">
      <h1>Hooper Bank</h1>
      <p>{new Date().getHours() < 12 ? 'Good morning!' : new Date().getHours() < 18 ? 'Good afternoon!' : 'Good evening!'} How would you like to proceed?</p>
      <nav aria-label="Account options">
        <Link to="/customer/login">Customer</Link>
        <Link to="/admin/login">Admin</Link>
      </nav>
    </main>
  )
}

function App() {
  return (
    <AuthProvider>
      <CustomerAuthProvider>
        <Routes>
          <Route path="/" element={<PublicHome />} />
          <Route path="/admin/login" element={<AdminLogin />} />
          <Route path="/admin/dashboard" element={<AdminProtectedRoute><AdminDashboard /></AdminProtectedRoute>} />
          <Route path="/admin/customers" element={<AdminProtectedRoute><Customers /></AdminProtectedRoute>} />
          <Route path="/admin/customers/add" element={<AdminProtectedRoute><AddCustomer /></AdminProtectedRoute>} />
          <Route path="/admin/customers/edit/:customerId" element={<AdminProtectedRoute><UpdateCustomer /></AdminProtectedRoute>} />
          <Route path="/admin/accounts" element={<AdminProtectedRoute><Accounts /></AdminProtectedRoute>} />
          <Route path="/admin/accounts/add" element={<AdminProtectedRoute><AddAccount /></AdminProtectedRoute>} />
          <Route path="/admin/accounts/edit/:accountId" element={<AdminProtectedRoute><UpdateAccount /></AdminProtectedRoute>} />
          <Route path="/customer/login" element={<CustomerLogin />} />
          <Route path="/customer/register" element={<CustomerRegistration />} />
          <Route path="/customer/home" element={<CustomerProtectedRoute><CustomerHome /></CustomerProtectedRoute>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </CustomerAuthProvider>
    </AuthProvider>
  )
}

export default App;
