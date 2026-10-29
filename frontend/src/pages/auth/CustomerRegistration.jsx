import { useState } from 'react'
import { Link } from 'react-router-dom'
import Header from '../../components/Header'
import { registerCustomer } from '../../services/customerAuthService'
import '../../App.css'

const initialDetails = {
  name: '',
  address: { streetNumber: '', postalCode: '', city: '', province: '' },
  email: '',
  phoneNumber: '',
  password: ''
}

function CustomerRegistration() {
  const [details, setDetails] = useState(initialDetails)
  const [error, setError] = useState('')
  const [registered, setRegistered] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const updateAddress = (event) => {
    const { name, value } = event.target
    setDetails((current) => ({ ...current, address: { ...current.address, [name]: value } }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setRegistered(null)
    setSubmitting(true)
    try {
      const result = await registerCustomer(details)
      setRegistered(result)
      setDetails(initialDetails)
    } catch (registrationError) {
      setError(registrationError.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <Header />
      <main className="auth-wrapper">
        <section className="auth-card customer-registration-card">
          <div className="auth-header">
            <h2>Customer Registration</h2>
          </div>
          {registered ? (
            <div role="status" className="auth-success">
              <p>Registration successful. Your username is <strong>{registered.username}</strong>.</p>
              <Link to="/customer/login">Continue to customer sign in</Link>
            </div>
          ) : (
            <form className="auth-form customer-registration-form" onSubmit={handleSubmit}>
              <label className="auth-field">
                <span className={`auth-field-label ${details.name ? 'visible' : ''}`} style={{ display: details.name ? 'block' : 'none' }}>Full Name</span>
                <input name="name" aria-label="Full Name" autoComplete="name" placeholder="Full Name" required maxLength={200}
                  value={details.name}
                  onChange={(event) => setDetails((current) => ({ ...current, name: event.target.value }))} />
              </label>
              <label className="auth-field">
                <span className={`auth-field-label ${details.password ? 'visible' : ''}`} style={{ display: details.password ? 'block' : 'none' }}>
                  Password (at least 8 characters)
                </span>
                <input name="password" aria-label="Password (at least 8 characters)" type="password" autoComplete="new-password"
                  placeholder="Password (at least 8 characters)" required minLength={8} maxLength={1024}
                  value={details.password}
                  onChange={(event) => setDetails((current) => ({ ...current, password: event.target.value }))} />
              </label>
              <p className="auth-section-title">Address</p>
              <label className="auth-field">
                <span className={`auth-field-label ${details.address.streetNumber ? 'visible' : ''}`} style={{ display: details.address.streetNumber ? 'block' : 'none' }}>
                  Street number and street
                </span>
                <input name="streetNumber" aria-label="Street number" autoComplete="street-address"
                  placeholder="Street number" required maxLength={200}
                  value={details.address.streetNumber} onChange={updateAddress} />
              </label>
              <label className="auth-field">
                <span className={`auth-field-label ${details.address.city ? 'visible' : ''}`} style={{ display: details.address.city ? 'block' : 'none' }}>City</span>
                <input name="city" aria-label="City" autoComplete="address-level2" placeholder="City" required maxLength={120}
                  value={details.address.city} onChange={updateAddress} />
              </label>
              <label className="auth-field">
                <span className={`auth-field-label ${details.address.postalCode ? 'visible' : ''}`} style={{ display: details.address.postalCode ? 'block' : 'none' }}>Postal code</span>
                <input name="postalCode" aria-label="Postal code" autoComplete="postal-code" placeholder="Postal code"
                  required maxLength={32} value={details.address.postalCode} onChange={updateAddress} />
              </label>
              <label className="auth-field">
                <span className={`auth-field-label ${details.address.province ? 'visible' : ''}`} style={{ display: details.address.province ? 'block' : 'none' }}>Province</span>
                <input name="province" aria-label="Province" autoComplete="address-level1" placeholder="Province"
                  required maxLength={120} value={details.address.province} onChange={updateAddress} />
              </label>
              <label className="auth-field">
                <span className={`auth-field-label ${details.email ? 'visible' : ''}`} style={{ display: details.email ? 'block' : 'none' }}>Email</span>
                <input name="email" aria-label="Email" type="email" autoComplete="email" placeholder="Email"
                  required maxLength={320} value={details.email}
                  onChange={(event) => setDetails((current) => ({ ...current, email: event.target.value }))} />
              </label>
              <p className="auth-section-title">Contact Information</p>
              <label className="auth-field">
                <span className={`auth-field-label ${details.phoneNumber ? 'visible' : ''}`} style={{ display: details.phoneNumber ? 'block' : 'none' }}>
                  Phone number (include country code)
                </span>
                <input name="phoneNumber" aria-label="Phone number (include country code)" type="tel" autoComplete="tel"
                  placeholder="Phone number (include country code)" required pattern="\+[1-9][0-9]{1,14}"
                  value={details.phoneNumber}
                  onChange={(event) => setDetails((current) => ({ ...current, phoneNumber: event.target.value }))} />
              </label>
              {error && <div className="auth-error" role="alert">{error}</div>}
              <button type="submit" disabled={submitting}>
                {submitting ? 'Creating account...' : 'Create account'}
              </button>
            </form>
          )}
          {!registered && (
            <div className="auth-helper">
              <p>Already registered? <Link to="/customer/login">Sign in</Link></p>
            </div>
          )}
        </section>
      </main>
    </>
  )
}

export default CustomerRegistration
