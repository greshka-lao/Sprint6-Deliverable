import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import Customers from '../pages/admin/customer/Customers'

vi.mock('axios')

const customers = [
  {
    customerId: 1,
    name: 'Greshka Lao',
    username: 'greshkalao001',
    address: '67 Queen Street, Auckland, Auckland, 1010',
    customerType: 'person',
    phoneNumber: '+15551234567',
    email: 'greshka@example.com'
  },
  {
    customerId: 2,
    name: 'FDM Group',
    username: '',
    address: '',
    customerType: 'company',
    phoneNumber: '',
    email: ''
  }
]

describe('Customers', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    axios.get.mockResolvedValue({ data: customers })
  })

  it('renders the seven customer fields in the required order and handles missing values', async () => {
    render(<MemoryRouter><Customers /></MemoryRouter>)

    expect(await screen.findByText('greshkalao001')).toBeInTheDocument()
    const headings = within(screen.getByRole('table')).getAllByRole('columnheader')
      .map((heading) => heading.textContent)
    expect(headings.slice(0, 7)).toEqual([
      'ID', 'Name', 'Username', 'Address', 'Customer Type', 'Phone', 'Email'
    ])
    expect(screen.getByText('67 Queen Street, Auckland, Auckland, 1010')).toBeInTheDocument()
    expect(screen.getByText('FDM Group')).toBeInTheDocument()
    expect(screen.getAllByRole('row')).toHaveLength(3)
  })

  it('filters by customer id', async () => {
    render(<MemoryRouter><Customers /></MemoryRouter>)
    await screen.findByText('Greshka Lao')
    fireEvent.change(screen.getByPlaceholderText('Search by ID'), { target: { value: '1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Search' }))
    expect(screen.getByText('Greshka Lao')).toBeInTheDocument()
    expect(screen.queryByText('FDM Group')).not.toBeInTheDocument()
  })

  it('rejects invalid ID search and can clear the search input', async () => {
    window.alert = vi.fn()
    render(<MemoryRouter><Customers /></MemoryRouter>)
    await screen.findByText('Greshka Lao')
    fireEvent.change(screen.getByPlaceholderText('Search by ID'), { target: { value: 'abc' } })
    fireEvent.click(screen.getByRole('button', { name: 'Search' }))
    expect(window.alert).toHaveBeenCalledWith('Please enter a valid ID number.')
    fireEvent.click(screen.getByRole('button', { name: 'x' }))
    expect(screen.getByPlaceholderText('Search by ID')).toHaveValue('')
  })

  it('deletes a customer and reloads the list when confirmed', async () => {
    window.confirm = vi.fn(() => true)
    axios.delete.mockResolvedValue({})
    render(<MemoryRouter><Customers /></MemoryRouter>)
    const deleteButtons = await screen.findAllByRole('button', { name: 'Delete' })
    fireEvent.click(deleteButtons[0])
    await waitFor(() => {
      expect(axios.delete).toHaveBeenCalledWith(
        expect.stringContaining('/customer/delete?customerId=1'),
        expect.any(Object)
      )
    })
    expect(axios.get).toHaveBeenCalledTimes(2)
  })
})
