import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "axios";

function CustomerAccountsPopUp({ customer, selectCustomer }) {
  const apiUrl = 'http://localhost:8080/sprint4Deliverable'
  const [accounts, setAccounts] = useState([]);

  const navigate = useNavigate();

  useEffect(() => {
    const loadAccounts = async () => {
      axios.get(apiUrl + '/customer/getAccounts?customerId=' + customer?.customerId)
        .then((response) => {setAccounts(response.data)})
        .catch((error) => {console.log(error)})
    }

    loadAccounts();
  }, [customer])

  const deleteAccount = (id) => {
    if (confirm("Are you sure you want to delete this account?")) {
      axios.delete(apiUrl + '/account/delete?accountId=' + id)
        .then(() => navigate('/admin/accounts'))
        .catch((error) => console.log(error))
    }
  };

  return (
    <div id="faded-background">
      <div id="popup-box">
        <h3>
          {customer?.name}
          {customer?.name.endsWith("s") || customer?.name.endsWith("z") ? "'" : "'s"} Accounts
        </h3>

        <table 
          id="accountTable"
          style={{ marginTop: 0 }}
        >
          <thead>
            <tr>
              <th>ID</th>
              <th>Balance</th>
              <th>Type</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            { accounts?.map((account) => (
              <tr key={account.accountId}>
                <td>{account.accountId}</td>
                <td>${account.balance.toLocaleString('en-NZ', {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2
                })}</td>
                <td>{account.accountType == 'savingsAccount' ? 'Savings' : 'Checking'}</td>
                <td className="actions">
                  <Link to={`/admin/accounts/edit/${account.accountId}`}>
                    <button>Update</button>
                  </Link>
                  <button onClick={()=>{deleteAccount(account.accountId)}}>Delete</button>
                </td>
              </tr>
            )) }

          </tbody>
        </table>

        <button onClick={() => {selectCustomer(null)}}>Close</button>
      </div>
    </div>
  )
}

export default CustomerAccountsPopUp;