import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, beforeEach, expect } from 'vitest';
import axios from 'axios';
import AccountPopUp from '../components/AccountPopUp';

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

const mockSelectAccountId = vi.fn();

/* ===== PROPS ===== */

const customers = [
  {
    customerId: 1,
    name: 'Greshka Lao',
    customerType: 'person'
  },
  {
    customerId: 2,
    name: 'FDM Group',
    customerType: 'company'
  }
]

const accounts = [
  {
    accountId: 1,
    customerId: 1,
    accountType: 'savingsAccount',
    balance: 1500,
    interestRate: 0.05,
  },
  {
    accountId: 2,
    customerId: 2,
    accountType: 'checkingAccount',
    balance: 3200,
    nextCheckNumber: 101,
  }
]

describe('AccountPopUp', () => {

  /* ===== BEFORE TEST ===== */

  beforeEach(() => {
    vi.clearAllMocks();
  });

  /* ===== TESTS =====*/

  it('renders savings account details', async () => {
    axios.get
      .mockResolvedValueOnce({
        data: accounts[0],
      })
      .mockResolvedValueOnce({
        data: customers[0],
      });

    render(
      <MemoryRouter>
        <AccountPopUp
          accountId={accounts[0].accountId}
          selectAccountId={mockSelectAccountId}
        />
      </MemoryRouter>
    );

    expect(await screen.findByText(accounts[0].accountId)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText(customers[0].name)).toBeInTheDocument();
      expect(screen.getByText('Savings')).toBeInTheDocument();
      expect(screen.getByText('5%')).toBeInTheDocument();
      expect(screen.getByText('Interest Rate:')).toBeInTheDocument();
    });
  });

  it('renders checking account details', async () => {
    axios.get
      .mockResolvedValueOnce({
        data: accounts[1],
      })
      .mockResolvedValueOnce({
        data: customers[1],
      });

    render(
      <MemoryRouter>
        <AccountPopUp
          accountId={accounts[1].accountId}
          selectAccountId={mockSelectAccountId}
        />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(customers[1].name)).toBeInTheDocument();
      expect(screen.getByText('Checking')).toBeInTheDocument();
      expect(screen.getByText('Next Check Number:')).toBeInTheDocument();
      expect(screen.getByText(accounts[1].nextCheckNumber)).toBeInTheDocument();
    });
  });

  it('creates update link with account id', async () => {
    axios.get
      .mockResolvedValueOnce({
        data: accounts[0],
      })
      .mockResolvedValueOnce({
        data: customers[0],
      });

    render(
      <MemoryRouter>
        <AccountPopUp
          accountId={accounts[0].accountId}
          selectAccountId={mockSelectAccountId}
        />
      </MemoryRouter>
    );

    await waitFor(() => {
      const updateButton = screen.getByRole('button', {
        name: 'Update',
      });

      const link = updateButton.closest('a');

      expect(link).toHaveAttribute('href', '/account/edit/' + accounts[0].accountId);
    });
  });

  it('deletes account when confirmed', async () => {
    window.confirm = vi.fn(() => true);

    axios.get
      .mockResolvedValueOnce({
        data: accounts[0],
      })
      .mockResolvedValueOnce({
        data: customers[0],
      });

    axios.delete.mockResolvedValue({});

    render(
      <MemoryRouter>
        <AccountPopUp
          accountId={accounts[0].accountId}
          selectAccountId={mockSelectAccountId}
        />
      </MemoryRouter>
    );

    const deleteButton = await screen.findByRole('button', {
      name: 'Delete',
    });

    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(axios.delete).toHaveBeenCalledWith(
        'http://localhost:8080/sprint4Deliverable/account/delete?accountId=' + accounts[0].accountId
      );

      expect(mockNavigate).toHaveBeenCalledWith('/account');
      expect(mockSelectAccountId).toHaveBeenCalledWith(null);
    });
  });

  it('does not delete account when confirmation is cancelled', async () => {
    window.confirm = vi.fn(() => false);

    axios.get
      .mockResolvedValueOnce({
        data: accounts[0],
      })
      .mockResolvedValueOnce({
        data: customers[0],
      });

    render(
      <MemoryRouter>
        <AccountPopUp
          accountId={accounts[0].accountId}
          selectAccountId={mockSelectAccountId}
        />
      </MemoryRouter>
    );

    const deleteButton = await screen.findByRole('button', {
      name: 'Delete',
    });

    fireEvent.click(deleteButton);

    expect(axios.delete).not.toHaveBeenCalled();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('closes popup when close button is clicked', async () => {
    axios.get
      .mockResolvedValueOnce({
        data: accounts[0],
      })
      .mockResolvedValueOnce({
        data: customers[0],
      });

    render(
      <MemoryRouter>
        <AccountPopUp
          accountId={accounts[0].accountId}
          selectAccountId={mockSelectAccountId}
        />
      </MemoryRouter>
    );

    const closeButton = await screen.findByRole('button', {
      name: 'Close',
    });

    fireEvent.click(closeButton);

    expect(mockSelectAccountId).toHaveBeenCalledWith(null);
  });
});