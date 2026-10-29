import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "axios";

function AccountPopUp({ accountId, selectAccountId }) {
  const apiUrl = 'http://localhost:8080/sprint4Deliverable'
  const [account, setAccount] = useState(null);
  const [customer, setCustomer] = useState(null);
  const navigate = useNavigate();

  const deleteAccount = (id) => {
    if (confirm("Are you sure you want to delete this account?")) {
      axios.delete(apiUrl + '/account/delete?accountId=' + id)
        .then(() => {
          navigate("/admin/accounts");
          selectAccountId(null);
        })
        .catch((error) => console.log(error))
    }
  };

  useEffect(() => {

    axios.get(apiUrl + '/account/get?accountId=' + accountId)
      .then((response) => setAccount(response.data))
      .catch((error) => console.log(error));

  }, [accountId]);

  useEffect(() => {
    if (!account) return;

    axios.get(apiUrl + '/customer/get?customerId=' + account.customerId)
      .then((response) => setCustomer(response.data))
      .catch((error) => console.log(error));
  }, [account])

  return (
    <div id="faded-background">
      <div id="popup-box">

        <div id="accountForm">
          <p className="left-column">ID:</p> 
          <p className="right-column">{account?.accountId}</p>
          <p className="left-column">Customer:</p> 
          <p className="right-column">{customer?.name}</p>
          <p className="left-column">Account Type:</p> 
          <p className="right-column">{account?.accountType == "savingsAccount" ? "Savings" : "Checking" }</p>
          <p className="left-column">Balance:</p>
          <p className="right-column">
            ${account?.balance.toLocaleString ('en-NZ', {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2
            })}
          </p>
          {account?.accountType == 'savingsAccount' && (
            <p className="left-column">Interest Rate:</p>
          )}
          {account?.accountType == 'savingsAccount' && (
            <p className="right-column">{account?.interestRate*100}%</p>
          )}
          {account?.accountType == 'checkingAccount' && (
            <p className="left-column">Next Check Number:</p>
          )}
          {account?.accountType == 'checkingAccount' && (
            <p className="right-column">{account?.nextCheckNumber}</p>
          )}
        </div>

        <div className="actions">
          <Link to={`/admin/accounts/edit/${account?.accountId}`}>
            <button>Update</button>
          </Link>
          <button onClick={()=>{deleteAccount(account?.accountId)}}>Delete</button>
          <button onClick={() => {selectAccountId(null)}}>Close</button>
        </div>
        
      </div>
    </div>
  )
}

export default AccountPopUp;