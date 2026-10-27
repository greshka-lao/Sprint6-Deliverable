const API_URL = 'http://localhost:8080/sprint4Deliverable/api/auth'

export async function loginAdmin(username, password) {
  const response = await fetch(API_URL + '/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  })

  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(data.message || 'Login failed')
  }

  return data
}

export async function validateSession(token) {
  const response = await fetch(API_URL + '/session', {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  })

  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(data.message || 'Session validation failed')
  }

  return data
}

export async function logoutAdmin(token) {
  const response = await fetch(API_URL + '/logout', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  })

  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(data.message || 'Logout failed')
  }

  return data
}

export function getStoredToken() {
  return localStorage.getItem('hooperBankToken')
}

export function setStoredToken(token) {
  if (token) {
    localStorage.setItem('hooperBankToken', token)
  }
}

export function clearStoredSession() {
  localStorage.removeItem('hooperBankToken')
}
