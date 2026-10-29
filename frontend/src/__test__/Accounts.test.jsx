import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import axios from 'axios';
import Accounts from '../pages/account/Accounts';

/* ===== MOCKS ===== */

vi.mock('axios');

vi.mock('../../components/AccountPopUp', () => ({
  default: ({ accountId }) => (
    <div>Popup {accountId}</div>
  ),
}));

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

describe('Accounts', () => {

  /* ===== BEFORE TEST ===== */

  beforeEach(() => {
    vi.clearAllMocks();
  });

  /* ===== TESTS =====*/

  it('renders accounts and customer names', async () => {
    axios.get.mockImplementation((url) => {
      if (url.endsWith('/account/getAll')) {
        return Promise.resolve({ data: accounts })
      }
      if (url.endsWith('/customer/get?customerId=1')) {
        return Promise.resolve({ data: customers[0] })
      }
      if (url.endsWith('/customer/get?customerId=2')) {
        return Promise.resolve({ data: customers[1] })
      }
      return Promise.resolve({ data: [] })
    })

    render(
      <MemoryRouter>
        <Accounts />
      </MemoryRouter>
    );

    expect(await screen.findByText(customers[0].name)).toBeInTheDocument();

    expect(screen.getByText(customers[1].name)).toBeInTheDocument();
    expect(screen.getByText('Savings')).toBeInTheDocument();
    expect(screen.getByText('Checking')).toBeInTheDocument();
  });

  it('filters by city', async () => {
    axios.get.mockResolvedValue({ data: [] });

    render(
      <MemoryRouter>
        <Accounts />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByRole('combobox'), {
      target: {
        value: 'Auckland',
      },
    });

    await waitFor(() => {
      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining(
          '/account/getByCity?city=Auckland'
        )
      );
    });
  });

  it('opens popup when savings account ID clicked', async () => {
    axios.get
      .mockResolvedValueOnce({
        data: accounts,
      })
      .mockResolvedValueOnce({
        data: customers[0],
      })
      .mockResolvedValueOnce({
        data: customers[1],
      })
      .mockResolvedValueOnce({
        data: accounts[0],
      });

    render(
      <MemoryRouter>
        <Accounts />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByText(accounts[0].accountId));

    await waitFor(() => {
      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining(
          '/account/get?accountId=' + accounts[0].accountId
        )
      );
      expect(screen.getByText(/Interest Rate/i)).toBeInTheDocument();
      expect(screen.getByText(accounts[0].interestRate*100 + "%")).toBeInTheDocument();
    })
  });

  it('opens popup when savings account ID clicked', async () => {
    axios.get
      .mockResolvedValueOnce({
        data: accounts,
      })
      .mockResolvedValueOnce({
        data: customers[0],
      })
      .mockResolvedValueOnce({
        data: customers[1],
      })
      .mockResolvedValueOnce({
        data: accounts[1],
      });

    render(
      <MemoryRouter>
        <Accounts />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByText(accounts[1].accountId));

    await waitFor(() => {
      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining(
          '/account/get?accountId=' + accounts[1].accountId
        )
      );
      expect(screen.getByText(/Next Check Number/i)).toBeInTheDocument();
      expect(screen.getByText(accounts[1].nextCheckNumber)).toBeInTheDocument();
    })
  });

  it('opens popup when account customer name clicked', async () => {
    axios.get
      .mockResolvedValueOnce({
        data: accounts,
      })
      .mockResolvedValueOnce({
        data: customers[0],
      })
      .mockResolvedValueOnce({
        data: customers[1],
      })
      .mockResolvedValueOnce({
        data: accounts[0],
      });

    render(
      <MemoryRouter>
        <Accounts />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByText(customers[0].name));

    await waitFor(() => {
      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining(
          '/account/get?accountId=' + accounts[0].accountId
        )
      );
    })
  });

  it('opens popup when account balance clicked', async () => {
    axios.get
      .mockResolvedValueOnce({
        data: accounts,
      })
      .mockResolvedValueOnce({
        data: customers[0],
      })
      .mockResolvedValueOnce({
        data: customers[1],
      })
      .mockResolvedValueOnce({
        data: accounts[0],
      });

    render(
      <MemoryRouter>
        <Accounts />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByText("$1,500.00"));

    await waitFor(() => {
      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining(
          '/account/get?accountId=' + accounts[0].accountId
        )
      );
    })
  });

  it('opens popup when account type clicked', async () => {
    axios.get
      .mockResolvedValueOnce({
        data: accounts,
      })
      .mockResolvedValueOnce({
        data: customers[0],
      })
      .mockResolvedValueOnce({
        data: customers[1],
      })
      .mockResolvedValueOnce({
        data: accounts[0],
      });

    render(
      <MemoryRouter>
        <Accounts />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByText('Savings'));

    await waitFor(() => {
      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining(
          '/account/get?accountId=' + accounts[0].accountId
        )
      );
    })
  });

  it('deletes account when confirmed', async () => {
    window.confirm = vi.fn(() => true);

    axios.get
      .mockResolvedValueOnce({
        data: accounts,
      })
      .mockResolvedValueOnce({
        data: customers[0],
      })
      .mockResolvedValueOnce({
        data: customers[1],
      });

    axios.delete.mockResolvedValue({});

    render(
      <MemoryRouter>
        <Accounts />
      </MemoryRouter>
    );

    const deleteButtons = await screen.findAllByText('Delete');

    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      expect(axios.delete).toHaveBeenCalledWith(
        expect.stringContaining(
          '/account/delete?accountId=' + accounts[0].accountId
        )
      );
    });
  });

  it('does not delete when confirmation cancelled', async () => {
    window.confirm = vi.fn(() => false);

    axios.get.mockResolvedValue({
      data: accounts,
    });

    render(
      <MemoryRouter>
        <Accounts />
      </MemoryRouter>
    );

    const deleteButtons = await screen.findAllByText('Delete');

    fireEvent.click(deleteButtons[0]);

    expect(axios.delete).not.toHaveBeenCalled();
  });
});