import axios from 'axios'
import { getAdminAuthConfig } from './authService'

const API_URL = 'http://localhost:8080/sprint4Deliverable'

export async function getCustomers() {
  const response = await axios.get(`${API_URL}/customer/getAll`, getAdminAuthConfig())
  return response.data
}

export async function getCustomer(customerId) {
  const response = await axios.get(`${API_URL}/customer/get?customerId=${customerId}`, getAdminAuthConfig())
  return response
}

export async function createCustomer(customer) {
  return axios.post(`${API_URL}/customer/save`, customer, getAdminAuthConfig())
}

export async function updateCustomer(customer) {
  return axios.put(`${API_URL}/customer/edit`, customer, getAdminAuthConfig())
}

export async function deleteCustomer(customerId) {
  return axios.delete(`${API_URL}/customer/delete?customerId=${customerId}`, getAdminAuthConfig())
}

export async function getPendingCustomerClaims() {
  const response = await axios.get(`${API_URL}/customer/claims`, getAdminAuthConfig())
  return response.data
}

export async function reviewCustomerClaim(claimRequestId, decision) {
  return axios.post(
    `${API_URL}/customer/claims/${claimRequestId}/${decision}`,
    {},
    getAdminAuthConfig()
  )
}
