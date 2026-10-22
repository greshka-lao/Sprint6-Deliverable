import React from 'react';
import { render, screen, fireEvent, waitFor, getByPlaceholderText } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import axios from 'axios';
import Customers from '../pages/customer/Customers';

/* ===== MOCKS ===== */

vi.mock('axios');

vi.mock('../components/CustomerAccountsPopUp', () => ({
  default: ({ customer }) => (
    <div>Popup {customer.customerId}</div>
  ),
}));

/* ===== PROPS ===== */

const customers = [
  {
    customerId: 1,
    name: 'Greshka Lao',
    address: {
      streetNumber: "67 Queen Street",
      city: "Auckland",
      province: "Auckland",
      postalCode: "1010",
    },
    customerType: 'person',
    accountIds: [1]
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
    customerType: 'company',
    accountIds: []
  }
]

const accounts = [
  {
    accountId: 1,
    customerId: 1,
    accountType: 'savingsAccount',
    balance: 1500,
    interestRate: 0.05,
  }
]

describe('Customers', () => {

  /* ===== BEFORE TEST ===== */

  beforeEach(() => {
    vi.resetAllMocks();
  });

  /* ===== TESTS =====*/

  it('loads customers', async () => {
    axios.get.mockResolvedValue({
      data: customers,
    });

    render(
      <MemoryRouter>
        <Customers />
      </MemoryRouter>
    );

    expect(
      await screen.findByText(customers[0].name)
    ).toBeInTheDocument();
    expect(
      screen.getByText(customers[0].customerId)
    ).toBeInTheDocument();
    expect(
      screen.getByText(customers[0].address.streetNumber + ", " + customers[0].address.city + ", " + customers[0].address.province + " " + customers[0].address.postalCode)
    ).toBeInTheDocument();

    expect(
      screen.getByText(customers[1].name)
    ).toBeInTheDocument();
  });

  it('filters by id', async () => {
    axios.get.mockResolvedValue({
      data: customers,
    });

    render(
      <MemoryRouter>
        <Customers />
      </MemoryRouter>
    );

    await screen.findByText(customers[0].name);

    fireEvent.change(
      screen.getByPlaceholderText('Search by ID'),
      { target: { value: customers[0].customerId } }
    );

    fireEvent.click(screen.getByText('Search'));

    expect(screen.getByText(customers[0].name))
      .toBeInTheDocument();

    expect(screen.queryByText(customers[1].name))
      .not.toBeInTheDocument();
  });

  it('shows validation alert for invalid id', async () => {
    window.alert = vi.fn();

    axios.get.mockResolvedValue({
      data: customers,
    });

    render(
      <MemoryRouter>
        <Customers />
      </MemoryRouter>
    );

    fireEvent.change(
      screen.getByPlaceholderText('Search by ID'),
      { target: { value: 'abc' } }
    );

    fireEvent.click(screen.getByText('Search'));

    expect(window.alert)
      .toHaveBeenCalledWith(
        'Please enter a valid ID number.'
      );
  });

  it('clear search input when x button clicked', async () => {
    window.alert = vi.fn();

    axios.get.mockResolvedValue({
      data: customers,
    });

    render(
      <MemoryRouter>
        <Customers />
      </MemoryRouter>
    );

    fireEvent.change(
      screen.getByPlaceholderText('Search by ID'),
      { target: { value: 'abc' } }
    );

    fireEvent.click(screen.getByText('x'));

    expect(screen.getByPlaceholderText('Search by ID')).toHaveValue('');
  });

  it('opens accounts popup', async () => {
    axios.get
      .mockResolvedValueOnce({
        data: [customers[0]],
      })
      .mockResolvedValueOnce({
        data: accounts,
      });

    render(
      <MemoryRouter>
        <Customers />
      </MemoryRouter>
    );

    const buttons = await screen.findAllByRole("button", { name: 'Accounts' });

    fireEvent.click(buttons[0]);

    expect(await screen.findByText("Popup " + customers[0].customerId))
  });

  it('alerts when customer has no accounts', async () => {
    window.alert = vi.fn();

    axios.get
      .mockResolvedValue({
        data: [customers[1]],
      })

    render(
      <MemoryRouter>
        <Customers />
      </MemoryRouter>
    );

    const buttons = await screen.findAllByRole("button", { name: 'Accounts' });

    fireEvent.click(buttons[0]);

    expect(window.alert).toHaveBeenCalledWith('This customer has no accounts.');
  });

  it('deletes customer when confirmed', async () => {
    window.confirm = vi.fn(() => true);

    axios.get.mockResolvedValue({
      data: customers,
    });

    axios.delete.mockResolvedValue({});

    render(
      <MemoryRouter>
        <Customers />
      </MemoryRouter>
    );

    const deleteButtons =
      await screen.findAllByText('Delete');

    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      expect(axios.delete).toHaveBeenCalledWith(
        expect.stringContaining(
          '/customer/delete?customerId=1'
        )
      );
    });
  });

  it('does not delete when confirmation cancelled', async () => {
    window.confirm = vi.fn(() => false);

    axios.get.mockResolvedValue({
      data: customers,
    });

    render(
      <MemoryRouter>
        <Customers />
      </MemoryRouter>
    );

    const deleteButtons =
      await screen.findAllByText('Delete');

    fireEvent.click(deleteButtons[0]);

    expect(axios.delete)
      .not.toHaveBeenCalled();
  });
});