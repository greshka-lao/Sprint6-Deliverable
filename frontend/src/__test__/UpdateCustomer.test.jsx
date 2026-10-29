import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import UpdateCustomer from '../pages/customer/UpdateCustomer'
import axios from 'axios'
import { MemoryRouter } from 'react-router-dom'

/* ===== MOCKS ===== */

vi.mock('axios')

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')

  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ customerId: '1' })
  }
})

vi.mock('../components/Header', () => ({
  default: () => <div>Header</div>
}))

vi.mock('../components/Sidebar', () => ({
  default: () => <div>Sidebar</div>
}))

/* ===== PROPS ===== */

const oldCustomers = [
  {
    customerId: 1,
    name: 'Greshka Lao',
    address: {
      streetNumber: "67 Queen Street",
      city: "Christchurch",
      province: "Canterbury",
      postalCode: "8011",
    },
    customerType: 'person',
  },
  {
    customerId: 2,
    name: 'FDM Group',
    address: {
      streetNumber: "697 Collins Street",
      city: "Melbourne",
      province: "Victoria",
      postalCode: "3000",
    },
    customerType: 'company'
  }
]

describe('UpdateCustomer', () => {
  
  /* ===== BEFORE TEST ===== */

  beforeEach(() => {
    vi.clearAllMocks()

    axios.get.mockResolvedValue({
      data: oldCustomers[0]
    })

    axios.put.mockResolvedValue({})
  })

  const renderComponent = () =>
  render(
    <MemoryRouter>
      <UpdateCustomer />
    </MemoryRouter>
  )

  /* ===== TESTS =====*/

  it('loads customer on mount', async () => {
    renderComponent()

    await waitFor(() => {
      expect(axios.get).toHaveBeenCalledWith(
        'http://localhost:8080/sprint4Deliverable/customer/get?customerId=' + oldCustomers[0].customerId
      )
    })
  })

  it('renders loaded customer data', async () => {
    renderComponent()

    expect(await screen.findByDisplayValue(oldCustomers[0].name)).toBeInTheDocument()
    expect(screen.getByDisplayValue(oldCustomers[0].address.streetNumber)).toBeInTheDocument()
    expect(screen.getByDisplayValue(oldCustomers[0].address.city)).toBeInTheDocument()
    expect(screen.getByDisplayValue(oldCustomers[0].address.province)).toBeInTheDocument()
    expect(screen.getByDisplayValue(oldCustomers[0].address.postalCode)).toBeInTheDocument()

    expect(screen.getByDisplayValue('Person')).toBeInTheDocument()
  })

  it('renders company customer type', async () => {
    axios.get.mockResolvedValueOnce({
      data: oldCustomers[1]
    })

    renderComponent()

    expect(await screen.findByDisplayValue('Company')).toBeInTheDocument()
  })

  it('updates form fields', async () => {
    renderComponent()

    const nameInput = await screen.findByDisplayValue(oldCustomers[0].name)
    const streetInput = screen.getByDisplayValue(oldCustomers[0].address.streetNumber)
    const cityInput = screen.getByDisplayValue(oldCustomers[0].address.city)
    const provinceInput = screen.getByDisplayValue(oldCustomers[0].address.province)
    const postalInput = screen.getByDisplayValue(oldCustomers[0].address.postalCode)

    fireEvent.change(nameInput, {
      target: { value: 'Lyn Lapid' }
    })

    fireEvent.change(streetInput, {
      target: { value: '130 Adelaide Street' }
    })

    fireEvent.change(cityInput, {
      target: { value: 'Toronto' }
    })

    fireEvent.change(provinceInput, {
      target: { value: 'Ontario' }
    })

    fireEvent.change(postalInput, {
      target: { value: 'M5H 3P5' }
    })

    expect(nameInput.value).toBe('Lyn Lapid')
    expect(streetInput.value).toBe('130 Adelaide Street')
    expect(cityInput.value).toBe('Toronto')
    expect(provinceInput.value).toBe('Ontario')
    expect(postalInput.value).toBe('M5H 3P5')
  })

  it('submits updated customer and navigates', async () => {
    renderComponent()

    const nameInput = await screen.findByDisplayValue(oldCustomers[0].name)

    fireEvent.change(nameInput, {
      target: { value: 'Lyn Lapid' }
    })

    fireEvent.click(screen.getByRole('button', { name: 'Update' }))

    await waitFor(() => {
      expect(axios.put).toHaveBeenCalledWith(
        'http://localhost:8080/sprint4Deliverable/customer/edit',
        {
          ...oldCustomers[0],
          name: 'Lyn Lapid'
        }
      )
    })

    expect(mockNavigate).toHaveBeenCalledWith('/admin/customers')
  })

  it('has cancel link to customer page', async () => {
    renderComponent()

    await screen.findByDisplayValue(oldCustomers[0].name)

    const cancelLink = screen.getByRole('link')

    expect(cancelLink).toHaveAttribute('href', '/admin/customers')
  })

  it('renders readonly customer fields', async () => {
    renderComponent()

    const idInput = await screen.findByDisplayValue(oldCustomers[0].customerId)
    const typeInput = screen.getByDisplayValue('Person')

    expect(idInput).toHaveAttribute('readonly')
    expect(typeInput).toHaveAttribute('readonly')
  })

  it('invalid postal code', async () => {
    axios.put.mockRejectedValue({
      response: {
        status: 400,
        data: 'Postal code not found'
      }
    });

    renderComponent();

    const cityInput = await screen.findByDisplayValue(oldCustomers[0].address.city)
    const provinceInput = await screen.findByDisplayValue(oldCustomers[0].address.province)
    const postalInput = await screen.findByDisplayValue(oldCustomers[0].address.postalCode)

    fireEvent.change(cityInput, {
      target: { value: '' }
    })

    fireEvent.change(provinceInput, {
      target: { value: '' }
    })

    fireEvent.change(postalInput, {
      target: { value: 'hello' }
    })

    fireEvent.click(screen.getByRole('button', { name: 'Update' }));

    await waitFor(() => {
      expect(axios.put).toHaveBeenCalledWith(
        expect.stringContaining('/customer/edit'),
        {
          customerId: oldCustomers[0].customerId,
          customerType: 'person',
          name: oldCustomers[0].name,
          address: {
            streetNumber: oldCustomers[0].address.streetNumber,
            city: "",
            province: "",
            postalCode: "hello"
          }
        }
      );
    });

    expect(screen.getByText("Invalid postal code. Please enter a valid postal code.")).toBeInTheDocument();
  });

  it('incorrect postal code to entered city and province', async () => {
    axios.put.mockRejectedValue({
      response: {
        status: 400,
        data: 'Incorrect city or province entered'
      }
    });

    renderComponent();

    const cityInput = await screen.findByDisplayValue(oldCustomers[0].address.city)
    const provinceInput = await screen.findByDisplayValue(oldCustomers[0].address.province)

    fireEvent.change(cityInput, {
      target: { value: 'Sydney' }
    })

    fireEvent.change(provinceInput, {
      target: { value: 'Wellington' }
    })

    fireEvent.click(screen.getByRole('button', { name: 'Update' }));

    await waitFor(() => {
      expect(axios.put).toHaveBeenCalledWith(
        expect.stringContaining('/customer/edit'),
        {
          customerId: oldCustomers[0].customerId,
          customerType: 'person',
          name: oldCustomers[0].name,
          address: {
            streetNumber: oldCustomers[0].address.streetNumber,
            city: "Sydney",
            province: "Wellington",
            postalCode: oldCustomers[0].address.postalCode
          }
        }
      );
    });
  });
})