import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import axios from 'axios';
import Accounts from '../pages/admin/account/Accounts';

/* ===== MOCKS ===== */

vi.mock('axios');
vi.stubGlobal('localStorage', {
  getItem: vi.fn(() => 'admin-test-token'),
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

const mockAccountRequests = () => {
  axios.get.mockImplementation((url) => {
    if (url.endsWith('/account/getAll')) {
      return Promise.resolve({ data: accounts });
    }
    if (url.endsWith('/account/get?accountId=1')) {
      return Promise.resolve({ data: accounts[0] });
    }
    if (url.endsWith('/account/get?accountId=2')) {
      return Promise.resolve({ data: accounts[1] });
    }
    if (url.endsWith('/customer/get?customerId=1')) {
      return Promise.resolve({ data: customers[0] });
    }
    if (url.endsWith('/customer/get?customerId=2')) {
      return Promise.resolve({ data: customers[1] });
    }
    return Promise.resolve({ data: [] });
  });
};

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

    await waitFor(() => {
      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining('/customer/get?customerId=1'),
        { headers: { Authorization: 'Bearer admin-test-token' } }
      );
    });
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
        ),
        { headers: { Authorization: 'Bearer admin-test-token' } }
      );
    });
  });

  it('opens popup when savings account ID clicked', async () => {
    mockAccountRequests();

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
        ),
        { headers: { Authorization: 'Bearer admin-test-token' } }
      );
      expect(screen.getByText(/Interest Rate/i)).toBeInTheDocument();
      expect(screen.getByText(accounts[0].interestRate*100 + "%")).toBeInTheDocument();
    })
  });

  it('opens popup when savings account ID clicked', async () => {
    mockAccountRequests();

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
        ),
        { headers: { Authorization: 'Bearer admin-test-token' } }
      );
      expect(screen.getByText(/Next Check Number/i)).toBeInTheDocument();
      expect(screen.getByText(accounts[1].nextCheckNumber)).toBeInTheDocument();
    })
  });

  it('opens popup when account customer name clicked', async () => {
    mockAccountRequests();

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
        ),
        { headers: { Authorization: 'Bearer admin-test-token' } }
      );
    })
  });

  it('opens popup when account balance clicked', async () => {
    mockAccountRequests();

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
        ),
        { headers: { Authorization: 'Bearer admin-test-token' } }
      );
    })
  });

  it('opens popup when account type clicked', async () => {
    mockAccountRequests();

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
        ),
        { headers: { Authorization: 'Bearer admin-test-token' } }
      );
    })
  });

  it('deletes account when confirmed', async () => {
    window.confirm = vi.fn(() => true);

    mockAccountRequests();

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
        ),
        { headers: { Authorization: 'Bearer admin-test-token' } }
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