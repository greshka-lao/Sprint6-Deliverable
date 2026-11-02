import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CustomerClaims from '../pages/admin/customer/CustomerClaims'
import { getPendingCustomerClaims, reviewCustomerClaim } from '../services/customerService'

vi.mock('../services/customerService', () => ({
  getPendingCustomerClaims: vi.fn(),
  reviewCustomerClaim: vi.fn()
}))

const pendingClaim = {
  claimRequestId: 7,
  customerName: 'FDM Group',
  customerType: 'company',
  address: '1 Main Street, Example City, Example Province, A1A 1A1',
  submittedEmail: 'contact@example.com',
  submittedPhoneNumber: '+15551234567',
  submittedAt: '2026-10-30T00:00:00Z'
}

describe('CustomerClaims', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getPendingCustomerClaims.mockResolvedValue([pendingClaim])
  })

  it('displays claim details and refreshes the queue after approval', async () => {
    reviewCustomerClaim.mockResolvedValue({})
    render(<MemoryRouter><CustomerClaims /></MemoryRouter>)

    expect(await screen.findByText('FDM Group')).toBeInTheDocument()
    expect(screen.getByText('contact@example.com')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Approve' }))

    await waitFor(() => {
      expect(reviewCustomerClaim).toHaveBeenCalledWith(7, 'approve')
      expect(getPendingCustomerClaims).toHaveBeenCalledTimes(2)
    })
  })

  it('reports review errors', async () => {
    reviewCustomerClaim.mockRejectedValue({
      response: { data: { message: 'Only pending claims can be reviewed.' } }
    })
    render(<MemoryRouter><CustomerClaims /></MemoryRouter>)
    fireEvent.click(await screen.findByRole('button', { name: 'Reject' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Only pending claims can be reviewed.')
  })
})
