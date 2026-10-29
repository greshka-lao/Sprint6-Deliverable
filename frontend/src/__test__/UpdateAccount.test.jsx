import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import axios from 'axios';
import UpdateAccount from '../pages/account/UpdateAccount';

/* ===== MOCKS ===== */

const mockNavigate = vi.fn();

vi.mock('axios');

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');

  return {
    ...actual,
    useParams: () => ({
      accountId: '1',
    }),
    useNavigate: () => mockNavigate,
  };
});

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

const oldAccounts = [
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

describe('UpdateAccount', () => {

  /* ===== BEFORE TEST ===== */

  beforeEach(() => {
    vi.clearAllMocks();
  });

  /* ===== TESTS =====*/

  it('renders savings account details', async () => {
    axios.get
      .mockResolvedValueOnce({
        data: customers,
      })
      .mockResolvedValueOnce({
        data: oldAccounts[0],
      });

    render(
      <MemoryRouter>
        <UpdateAccount />
      </MemoryRouter>
    );

    expect(
      await screen.findByDisplayValue('Savings')
    ).toBeInTheDocument();

    expect(
      screen.getByDisplayValue(oldAccounts[0].balance)
    ).toBeInTheDocument();

    expect(
      screen.getByDisplayValue(oldAccounts[0].interestRate)
    ).toBeInTheDocument();
  });

  it('renders checking account details', async () => {
    axios.get
      .mockResolvedValueOnce({
        data: customers,
      })
      .mockResolvedValueOnce({
        data: oldAccounts[1],
      });

    render(
      <MemoryRouter>
        <UpdateAccount />
      </MemoryRouter>
    );

    expect(
      await screen.findByDisplayValue('Checking')
    ).toBeInTheDocument();

    expect(
      screen.getByDisplayValue(oldAccounts[1].balance)
    ).toBeInTheDocument();
    
    expect(
      screen.getByDisplayValue(oldAccounts[1].nextCheckNumber)
    ).toBeInTheDocument();
  });

  it('updates savings account', async () => {
    axios.get
      .mockResolvedValueOnce({
        data: customers,
      })
      .mockResolvedValueOnce({
        data: oldAccounts[0],
      });

    axios.put
      .mockResolvedValueOnce({
        data: {
          accountId: oldAccounts[0].accountId,
          accountType: oldAccounts[0].accountType,
          balance: 2000,
          customerId: customers[1].customerId,
          interestRate: 0.02
        },
      })
      .mockResolvedValueOnce({});

    render(
      <MemoryRouter>
        <UpdateAccount />
      </MemoryRouter>
    );

    fireEvent.change(
      await screen.findByLabelText(/Balance/i),
      {
        target: { value: '2000' },
      }
    );

    fireEvent.change(
      await screen.findByLabelText(/Customer/i),
      {
        target: { value: customers[1].customerId },
      }
    );

    fireEvent.change(
      await screen.findByLabelText(/Interest Rate/i),
      {
        target: { value: 0.02 },
      }
    );

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Update',
      })
    );

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/admin/accounts');
    });
  });

  it('updates checking account', async () => {
    axios.get
      .mockResolvedValueOnce({
        data: customers,
      })
      .mockResolvedValueOnce({
        data: oldAccounts[1],
      });

    axios.put
      .mockResolvedValueOnce({
        data: {
          accountId: oldAccounts[1].accountId,
          accountType: oldAccounts[1].accountType,
          balance: 2000,
          customerId: customers[0].customerId,
          nextCheckNumber: 200
        },
      })
      .mockResolvedValueOnce({});

    render(
      <MemoryRouter>
        <UpdateAccount />
      </MemoryRouter>
    );

    fireEvent.change(
      await screen.findByLabelText(/Balance/i),
      {
        target: { value: '2000' },
      }
    );

    fireEvent.change(
      await screen.findByLabelText(/Customer/i),
      {
        target: { value: customers[0].customerId },
      }
    );

    fireEvent.change(
      await screen.findByLabelText(/Next Check Number/i),
      {
        target: { value: 200 },
      }
    );

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Update',
      })
    );

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/admin/accounts');
    });
  });
});