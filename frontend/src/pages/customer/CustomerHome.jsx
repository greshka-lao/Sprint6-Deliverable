import Header from '../../components/Header'
import Sidebar from '../../components/Sidebar'
import { useCustomerAuth } from '../../context/useCustomerAuth'
import '../../App.css'

function CustomerHome() {
  const { customer } = useCustomerAuth()

  return (
    <div>
      <Header variant="customer" />
      <div id="layout">
        <Sidebar variant="customer" />
        <main id="content">
          <div id="header">
            <h2>Customer Dashboard</h2>
          </div>
          <section className="dashboard-card">
            <h3>Welcome, {customer?.username || 'Customer'}</h3>
            <p>Your customer account is signed in and ready to use.</p>
            <p>Customer services will appear here.</p>
          </section>
        </main>
      </div>
    </div>
  )
}

export default CustomerHome
