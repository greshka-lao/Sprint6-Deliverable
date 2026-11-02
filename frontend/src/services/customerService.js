import axios from 'axios'
import { getStoredToken } from './authService'

const API_URL = 'http://localhost:8080/sprint4Deliverable'

function adminConfig() {
  const storage = globalThis.localStorage
  const token = storage && typeof storage.getItem === 'function'
    ? getStoredToken()
    : null
  return {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  }
}

export async function getCustomers() {
  const response = await axios.get(`${API_URL}/customer/getAll`, adminConfig())
  return response.data
}

export async function getCustomer(customerId) {
  const response = await axios.get(`${API_URL}/customer/get?customerId=${customerId}`, adminConfig())
  return response
}

export async function createCustomer(customer) {
  return axios.post(`${API_URL}/customer/save`, customer, adminConfig())
}

export async function updateCustomer(customer) {
  return axios.put(`${API_URL}/customer/edit`, customer, adminConfig())
}

export async function deleteCustomer(customerId) {
  return axios.delete(`${API_URL}/customer/delete?customerId=${customerId}`, adminConfig())
}

export async function getPendingCustomerClaims() {
  const response = await axios.get(`${API_URL}/customer/claims`, adminConfig())
  return response.data
}

export async function reviewCustomerClaim(claimRequestId, decision) {
  return axios.post(
    `${API_URL}/customer/claims/${claimRequestId}/${decision}`,
    {},
    adminConfig()
  )
}
