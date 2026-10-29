import '../App.css'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCustomerAuth } from '../context/useCustomerAuth'

function AdminSidebar() {
  const { user } = useAuth()

  return (
    <nav className="sidebar" aria-label="Admin navigation">
      {user ? (
        <>
          <Link to="/admin/dashboard">Dashboard</Link>
          <Link to="/admin/customers">Customers</Link>
          <Link to="/admin/accounts">Accounts</Link>
        </>
      ) : (
        <Link to="/admin/login">Admin Login</Link>
      )}
    </nav>
  )
}

function CustomerSidebar() {
  const { customer } = useCustomerAuth()

  return (
    <nav className="sidebar customer-sidebar" aria-label="Customer navigation">
      {customer && <Link to="/customer/home">Dashboard</Link>}
    </nav>
  )
}

function Sidebar({ variant = 'admin' }) {
  return variant === 'customer' ? <CustomerSidebar /> : <AdminSidebar />
}

export default Sidebar