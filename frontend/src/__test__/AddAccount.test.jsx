import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import axios from 'axios';
import AddAccount from '../pages/account/AddAccount';

/* ===== MOCKS ===== */

const mockNavigate = vi.fn();

window.alert = vi.fn();

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
    customerId: 1,
    name: 'Greshka Lao',
    customerType: 'person',
  },
  {
    customerId: 2,
    name: 'FDM Group',
    customerType: 'company'
  }
]

const accounts = [
  {
    customerId: 1,
    accountType: 'savingsAccount',
    balance: 1500,
    interestRate: 0.05,
  },
  {
    customerId: 2,
    accountType: 'checkingAccount',
    balance: 3200,
    nextCheckNumber: 101,
  }
]

describe('AddAccount', () => {

  /* ===== BEFORE TEST ===== */

  beforeEach(() => {
    vi.clearAllMocks();
  });

  /* ===== TESTS =====*/

  it('renders checking fields when account type changes', async () => {
    axios.get.mockResolvedValue({
      data: customers,
    });

    render(
      <MemoryRouter>
        <AddAccount />
      </MemoryRouter>
    );

    fireEvent.change(
      await screen.findByLabelText(/Account Type/i),
      {
        target: {
          value: 'checkingAccount',
        },
      }
    );

    expect(
      screen.getByLabelText(/Next Check Number/i)
    ).toBeInTheDocument();
  });

  it('creates savings account', async () => {
    axios.get.mockResolvedValue({
      data: customers,
    });

    axios.post.mockResolvedValue({
      data: accounts[0],
    });

    axios.put.mockResolvedValue({});

    render(
      <MemoryRouter>
        <AddAccount />
      </MemoryRouter>
    );

    fireEvent.change(
      await screen.findByLabelText(/Account Type/i),
      {
        target: {
          value: 'savingsAccount',
        },
      }
    );

    fireEvent.change(await screen.findByLabelText(/Balance/i), {
      target: { value: accounts[0].balance },
    });

    fireEvent.change(
      await screen.findByLabelText(/Customer/i),
      {
        target: {
          value: accounts[0].customerId,
        },
      }
    );

    fireEvent.change(screen.getByLabelText(/Interest Rate/i), {
      target: { value: accounts[0].interestRate },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Add' }));

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        "http://localhost:8080/sprint4Deliverable/account/save",
        accounts[0]
      );

      expect(mockNavigate).toHaveBeenCalledWith('/admin/accounts');
    });
  });

  it('creates checking account', async () => {
    axios.get.mockResolvedValue({
      data: customers,
    });

    axios.post.mockResolvedValue({
      data: accounts[1],
    });

    axios.put.mockResolvedValue({});

    render(
      <MemoryRouter>
        <AddAccount />
      </MemoryRouter>
    );

    fireEvent.change(
      await screen.findByLabelText(/Account Type/i),
      {
        target: {
          value: 'checkingAccount',
        },
      }
    );

    fireEvent.change(
      await screen.findByLabelText(/Customer/i),
      {
        target: {
          value: accounts[1].customerId,
        },
      }
    );

    fireEvent.change(screen.getByLabelText(/Balance/i), {
      target: {
        value: accounts[1].balance
      },
    });

    fireEvent.change(screen.getByLabelText(/Next Check Number/i), {
      target: {
        value: accounts[1].nextCheckNumber
      },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Add' }));

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        "http://localhost:8080/sprint4Deliverable/account/save",
        accounts[1]
      );
    });
  });

  it('redirects when no customers exist', async () => {

    axios.get.mockResolvedValue({
      data: [],
    });

    render(
      <MemoryRouter>
        <AddAccount />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(window.alert).toHaveBeenCalled();
      expect(mockNavigate).toHaveBeenCalledWith('/admin/accounts');
    });
  });
});