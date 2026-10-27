import { Link } from 'react-router-dom'
import Header from '../../components/Header'
import Sidebar from '../../components/Sidebar'
import { useAuth } from '../../context/AuthContext'
import '../../App.css'

function AdminDashboard() {
  const { user } = useAuth()

  return (
    <div>
      <Header />
      <div id="layout">
        <Sidebar />
        <div id="content">
          <div id="header">
            <h2>Admin Dashboard</h2>
          </div>
          <div className="dashboard-card">
            <h3>Welcome, {user?.username || 'Administrator'}</h3>
            <p>You are signed in with the {user?.role || 'admin'} role.</p>
          </div>
          <div className="dashboard-actions">
            <Link to="/customer"><button>Manage Customers</button></Link>
            <Link to="/account"><button>Manage Accounts</button></Link>
          </div>
        </div>
      </div>
    </div>
  )
}

export default AdminDashboard
