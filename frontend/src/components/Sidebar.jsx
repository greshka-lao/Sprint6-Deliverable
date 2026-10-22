import '../App.css'
import { Link } from 'react-router-dom'

function Sidebar() {
  return (
    <nav className="sidebar">
      <Link to="/">Home</Link>
      <Link to="/customer">Customers</Link>
      <Link to="/account">Accounts</Link>
    </nav>
  )
}

export default Sidebar