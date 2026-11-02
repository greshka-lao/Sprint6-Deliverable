import { useEffect, useState } from 'react'
import Header from '../../components/Header'
import Sidebar from '../../components/Sidebar'
import { useCustomerAuth } from '../../context/useCustomerAuth'
import { getCustomerDashboard } from '../../services/customerAuthService'
import '../../App.css'

const currencyFormatter = new Intl.NumberFormat('en-NZ', {
  style: 'currency',
  currency: 'NZD',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
})

function formatCurrency(amount) {
  return currencyFormatter.format(Number(amount))
}

function formatAddress(address) {
  if (!address) return null

  const parts = [
    address.streetNumber,
    address.city,
    address.province,
    address.postalCode
  ].filter((part) => typeof part === 'string' && part.trim())

  return parts.length > 0 ? parts.join(', ') : null
}

function CustomerHome() {
  const { customer } = useCustomerAuth()
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    getCustomerDashboard()
      .then((data) => {
        if (active) setDashboard(data)
      })
      .catch((requestError) => {
        if (active) setError(requestError.message || 'Unable to load your dashboard.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  const profile = dashboard?.profile
  const address = formatAddress(profile?.address)

  return (
    <div>
      <Header variant="customer" />
      <div id="layout">
        <Sidebar variant="customer" />
        <main id="content">
          <div id="header">
            <h2>Customer Dashboard</h2>
          </div>
          {loading && <p className="loading-state" role="status">Loading your dashboard...</p>}
          {!loading && error && (
            <section className="dashboard-card" role="alert">
              <p>{error}</p>
            </section>
          )}
          {!loading && !error && dashboard && (
            <>
              <section className="dashboard-card customer-dashboard-card">
                <h3>Welcome, {customer?.username || 'Customer'}</h3>
                <h4>Personal details</h4>
                <dl className="customer-profile">
                  <div>
                    <dt>Name</dt>
                    <dd>{profile?.name || 'Not provided'}</dd>
                  </div>
                  <div>
                    <dt>Email</dt>
                    <dd>{profile?.email || 'Not provided'}</dd>
                  </div>
                  <div>
                    <dt>Address</dt>
                    <dd>{address || 'Not provided'}</dd>
                  </div>
                  <div>
                    <dt>Phone number</dt>
                    <dd>{profile?.phoneNumber || 'Not provided'}</dd>
                  </div>
                </dl>
              </section>

              <section className="dashboard-card customer-dashboard-card">
                <h3>Bank accounts</h3>
                {dashboard.accounts.length === 0 ? (
                  <p>No bank accounts are currently linked to your profile.</p>
                ) : (
                  <div className="customer-account-table-wrapper">
                    <table className="customer-account-table">
                      <thead>
                        <tr>
                          <th scope="col">Account</th>
                          <th scope="col">Type</th>
                          <th scope="col">Balance</th>
                        </tr>
                      </thead>
                      <tbody>
                        {dashboard.accounts.map((account) => (
                          <tr key={account.accountId}>
                            <td>{account.accountId}</td>
                            <td>{account.accountType === 'savingsAccount' ? 'Savings' : 'Checking'}</td>
                            <td>{formatCurrency(account.balance)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
                <p className="customer-account-total">
                  Total balance: {formatCurrency(dashboard.total.amount)}
                </p>
              </section>
            </>
          )}
        </main>
      </div>
    </div>
  )
}

export default CustomerHome
