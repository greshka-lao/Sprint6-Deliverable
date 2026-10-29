import { useContext } from 'react'
import CustomerAuthContext from './CustomerAuthValue'

export function useCustomerAuth() {
  const context = useContext(CustomerAuthContext)
  if (!context) {
    throw new Error('useCustomerAuth must be used within CustomerAuthProvider')
  }
  return context
}
