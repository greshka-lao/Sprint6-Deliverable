import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import '../../../App.css'
import Header from '../../../components/Header'
import Sidebar from '../../../components/Sidebar'
import { getPendingCustomerClaims, reviewCustomerClaim } from '../../../services/customerService'

function CustomerClaims() {
  const [claims, setClaims] = useState([])
  const [error, setError] = useState('')

  const loadClaims = () => getPendingCustomerClaims()

  useEffect(() => {
    let active = true
    loadClaims()
      .then((items) => {
        if (active) {
          setClaims(items)
          setError('')
        }
      })
      .catch((loadError) => {
        if (active) {
          setError(loadError.response?.data?.message || 'Unable to load pending claims.')
        }
      })

    return () => {
      active = false
    }
  }, [])

  const review = async (claimRequestId, decision) => {
    try {
      await reviewCustomerClaim(claimRequestId, decision)
      setClaims(await loadClaims())
      setError('')
    } catch (reviewError) {
      setError(reviewError.response?.data?.message || 'Unable to review this claim.')
    }
  }

  return (
    <>
      <Header />
      <div id="layout">
        <Sidebar />
        <main id="content">
          <div id="header">
            <h2>Customer Profile Claims</h2>
            <Link to="/admin/customer">Customers</Link>
          </div>
          {error && <p role="alert">{error}</p>}
          {claims.length === 0 ? (
            <p>No pending customer claims.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Customer</th>
                  <th>Type</th>
                  <th>Address</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Submitted</th>
                  <th>Review</th>
                </tr>
              </thead>
              <tbody>
                {claims.map((claim) => (
                  <tr key={claim.claimRequestId}>
                    <td>{claim.customerName}</td>
                    <td>{claim.customerType}</td>
                    <td>{claim.address || ''}</td>
                    <td>{claim.submittedEmail}</td>
                    <td>{claim.submittedPhoneNumber}</td>
                    <td>{claim.submittedAt ? new Date(claim.submittedAt).toLocaleString() : ''}</td>
                    <td>
                      <button onClick={() => review(claim.claimRequestId, 'approve')}>Approve</button>
                      <button onClick={() => review(claim.claimRequestId, 'reject')}>Reject</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </main>
      </div>
    </>
  )
}

export default CustomerClaims
