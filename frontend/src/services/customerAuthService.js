const API_URL = 'http://localhost:8080/sprint4Deliverable/api/customer-auth'

async function request(path, options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    credentials: 'include',
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers
    }
  })

  const data = response.status === 204 ? null : await response.json().catch(() => null)
  if (!response.ok) {
    const error = new Error(data?.message || 'The request could not be completed.')
    error.status = response.status
    error.code = data?.code
    error.retryAfter = response.headers.get('Retry-After')
    throw error
  }
  return data
}

async function csrfHeaders() {
  const token = await request('/csrf')
  if (!token?.token) {
    throw new Error('Could not obtain a security token. Please reload and try again.')
  }
  return { 'X-CSRF-TOKEN': token.token }
}

export async function registerCustomer(details) {
  return request('/register', {
    method: 'POST',
    headers: await csrfHeaders(),
    body: JSON.stringify(details)
  })
}

export async function loginCustomer(username, password) {
  const result = await request('/login', {
    method: 'POST',
    headers: await csrfHeaders(),
    body: JSON.stringify({ username, password })
  })
  await csrfHeaders()
  return result
}

export async function getCustomerSession() {
  return request('/session')
}

export async function logoutCustomer() {
  return request('/logout', {
    method: 'POST',
    headers: await csrfHeaders()
  })
}
