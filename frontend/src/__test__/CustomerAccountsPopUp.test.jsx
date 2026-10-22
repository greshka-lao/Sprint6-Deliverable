import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event"
import axios from "axios";
import { vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import CustomerAccountsPopUp from "../components/CustomerAccountsPopUp";

/* ===== MOCKS ===== */

const mockNavigate = vi.fn();

vi.mock("axios");

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");

  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

/* ===== PROPS ===== */

const selectCustomerProp = vi.fn();

const customerProp = {
  customerId: 1,
  name: "Greshka Lao",
  accountIds: [1, 2]
};

const defaultProps = {
  customer: customerProp,
  selectCustomer: selectCustomerProp
}

const accounts = [
  {
    accountId: 1,
    customerId: 1,
    accountType: "savingsAccount",
    balance: 1234.56,
    interestRate: 0.5
  },
  {
    accountId: 2,
    customerId: 1,
    accountType: "checkingAccount",
    balance: 987.65,
    nextCheckNumber: 45
  }
]

describe("CustomerAccountPopUp", () => {

  /* ===== BEFORE TEST ===== */

  beforeEach(() => {
    vi.clearAllMocks();
  });

  /* ===== TESTS =====*/

  it("renders the accounts details", async () => {
    axios.get.mockResolvedValue({
      data: accounts,
    });

    render(
      <MemoryRouter>
        <CustomerAccountsPopUp {...defaultProps} />
      </MemoryRouter>
    );

    expect(axios.get).toHaveBeenCalledWith(
      "http://localhost:8080/sprint4Deliverable/customer/getAccounts?customerId=1"
    );

    expect(await screen.getByText(/Greshka Lao's Accounts/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText(accounts[0].accountId)).toBeInTheDocument();
      expect(screen.getByText("$1,234.56")).toBeInTheDocument();
      expect(screen.getByText("Savings")).toBeInTheDocument();
  
      expect(screen.getByText(accounts[1].accountId)).toBeInTheDocument();
      expect(screen.getByText("$" + accounts[1].balance)).toBeInTheDocument();
      expect(screen.getByText("Checking")).toBeInTheDocument();
    })
    
  });

  it("deletes account when confirmed", async () => {
    window.confirm = vi.fn(() => true);

    axios.get.mockResolvedValue({
      data: [
        {
          accountId: 1,
          customerId: 1,
          accountType: "savingsAccount",
          balance: 1234.56,
          interestRate: 0.5
        },
      ],
    });

    axios.delete.mockResolvedValue({});

    render(
      <MemoryRouter>
        <CustomerAccountsPopUp {...defaultProps} />
      </MemoryRouter>
    );

    const deleteButton = await screen.findAllByRole("button", {
      name: /delete/i,
    });

    await userEvent.click(deleteButton[0]);

    expect(window.confirm).toHaveBeenCalledWith(
      "Are you sure you want to delete this account?"
    );

    await waitFor(() => {
      expect(axios.delete).toHaveBeenCalledWith(
        "http://localhost:8080/sprint4Deliverable/account/delete?accountId=1"
      );
    });

    expect(mockNavigate).toHaveBeenCalledWith("/account");
  });

  it("does not delete account when cancelled", async () => {
    window.confirm = vi.fn(() => false);

    axios.get.mockResolvedValue({
      data: accounts,
    });

    render(
      <MemoryRouter>
        <CustomerAccountsPopUp {...defaultProps} />
      </MemoryRouter>
    );

    const deleteButton = await screen.findAllByRole("button", {
      name: /delete/i,
    });

    await userEvent.click(deleteButton[0]);

    expect(axios.delete).not.toHaveBeenCalled();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it("changes customer to null when closed", async () => {
    render(
      <MemoryRouter>
        <CustomerAccountsPopUp {...defaultProps} />
      </MemoryRouter>
    );

    const closeButton = await screen.findByRole("button", {
      name: /close/i,
    });

    await userEvent.click(closeButton);

    expect(selectCustomerProp).toHaveBeenCalledWith(null);
  })
});

