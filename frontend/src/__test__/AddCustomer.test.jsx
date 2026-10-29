import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import axios from 'axios';
import AddCustomer from '../pages/customer/AddCustomer';

/* ===== MOCKS ===== */

const mockNavigate = vi.fn();

vi.mock('axios');

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');

  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

/* ===== PROPS ===== */

const customers = [
  {
    name: 'Greshka Lao',
    address: {
      streetNumber: "67 Queen Street",
      city: "Auckland",
      province: "Auckland",
      postalCode: "1010",
    },
    customerType: 'person',
  },
  {
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

describe('AddCustomer', () => {

  /* ===== BEFORE TEST ===== */

  beforeEach(() => {
    vi.clearAllMocks();
  });

  /* ===== TESTS =====*/

  it('renders form fields', () => {
    render(
      <MemoryRouter>
        <AddCustomer />
      </MemoryRouter>
    );

    expect(screen.getByLabelText(/Name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Street/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/City/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Province/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Postal Code/i)).toBeInTheDocument();
  });

  it('creates a person customer', async () => {
    axios.post.mockResolvedValue({});

    render(
      <MemoryRouter>
        <AddCustomer />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: customers[0].name }
    });

    fireEvent.change(screen.getByLabelText(/Street/i), {
      target: { value: customers[0].address.streetNumber }
    });

    fireEvent.change(screen.getByLabelText(/City/i), {
      target: { value: customers[0].address.city },
    });

    fireEvent.change(screen.getByLabelText(/Province/i), {
      target: { value: customers[0].address.province },
    });

    fireEvent.change(screen.getByLabelText(/Postal Code/i), {
      target: { value: customers[0].address.postalCode },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Add' }));

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        expect.stringContaining('/customer/save'),
        customers[0]
      );
    });

    expect(mockNavigate).toHaveBeenCalledWith('/admin/customers');
  });

  it('creates a company customer', async () => {
    axios.post.mockResolvedValue({});

    render(
      <MemoryRouter>
        <AddCustomer />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText(/Customer Type/i), {
      target: { value: 'company' },
    });

    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: customers[1].name },
    });

    fireEvent.change(screen.getByLabelText(/Street/i), {
      target: { value: customers[1].address.streetNumber },
    });

    fireEvent.change(screen.getByLabelText(/City/i), {
      target: { value: customers[1].address.city },
    });

    fireEvent.change(screen.getByLabelText(/Province/i), {
      target: { value: customers[1].address.province },
    });

    fireEvent.change(screen.getByLabelText(/Postal Code/i), {
      target: { value: customers[1].address.postalCode },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Add' }));

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        expect.stringContaining('/customer/save'),
        customers[1]
      );
    });
  });

  it('invalid postal code', async () => {
    axios.post.mockRejectedValue({
      response: {
        status: 400,
        data: 'Postal code not found'
      }
    });

    render(
      <MemoryRouter>
        <AddCustomer />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText(/Customer Type/i), {
      target: { value: 'company' },
    });

    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: customers[1].name },
    });

    fireEvent.change(screen.getByLabelText(/Street/i), {
      target: { value: customers[1].address.streetNumber },
    });

    fireEvent.change(screen.getByLabelText(/Postal Code/i), {
      target: { value: "hello" },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Add' }));

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        expect.stringContaining('/customer/save'),
        {
          customerType: 'company',
          name: customers[1].name,
          address: {
            streetNumber: customers[1].address.streetNumber,
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
    axios.post.mockRejectedValue({
      response: {
        status: 400,
        data: 'Incorrect city or province entered'
      }
    });

    render(
      <MemoryRouter>
        <AddCustomer />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: customers[0].name },
    });

    fireEvent.change(screen.getByLabelText(/Street/i), {
      target: { value: customers[0].address.streetNumber },
    });

    fireEvent.change(screen.getByLabelText(/City/i), {
      target: { value: "Sydney" },
    });

    fireEvent.change(screen.getByLabelText(/Province/i), {
      target: { value: "Wellington" },
    });

    fireEvent.change(screen.getByLabelText(/Postal Code/i), {
      target: { value: customers[0].address.postalCode },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Add' }));

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        expect.stringContaining('/customer/save'),
        {
          customerType: 'person',
          name: customers[0].name,
          address: {
            streetNumber: customers[0].address.streetNumber,
            city: "Sydney",
            province: "Wellington",
            postalCode: customers[0].address.postalCode
          }
        }
      );
    });

    expect(screen.getByText("Invalid city/province. Please enter the valid city/province or leave it blank to auto-fill.")).toBeInTheDocument();
  });
});