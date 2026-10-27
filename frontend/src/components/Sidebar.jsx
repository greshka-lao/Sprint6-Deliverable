import '../App.css'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function Sidebar() {
  const { user } = useAuth()

  return (
    <nav className="sidebar">
      {user ? (
        <>
          <Link to="/dashboard">Dashboard</Link>
          <Link to="/customer">Customers</Link>
          <Link to="/account">Accounts</Link>
        </>
      ) : (
        <Link to="/login">Admin Login</Link>
      )}
    </nav>
  )
}

export default Sidebar