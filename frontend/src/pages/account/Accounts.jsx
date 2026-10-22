import "../../App.css";
import axios from "axios";
import { useState, useEffect } from "react";
import Sidebar from "../../components/Sidebar";
import Header from "../../components/Header";
import AccountPopUp from "../../components/AccountPopUp"
import { Link } from "react-router-dom";

function Accounts() {
  const apiUrl = 'http://localhost:8080/sprint4Deliverable'
  const [accounts, setAccounts] = useState([])
  const [customerNames, setCustomerNames] = useState([])

  const [selectedAccountId, selectAccountId] = useState(null)
  const [selectedCity, selectCity] = useState("")

  useEffect(() => {
    loadAccounts();
  }, [selectedCity])

  useEffect(() => {
    const loadCustomerNames = async () => {
      if (!accounts.length) return;
  
      const names = {};
  
      for (const account of accounts) {
        try {
          const response = await axios.get(
            `${apiUrl}/customer/get?customerId=${account.customerId}`
          );
          names[account.customerId] = response.data?.name || "";
        } catch (error) {
          console.log(error);
          names[account.customerId] = "";
        }
      }
  
      setCustomerNames(names);
    };
    
    loadCustomerNames();
  }, [accounts])

  const loadAccounts = async () => {
    if (selectedCity == "") {
      axios.get(apiUrl + '/account/getAll')
         .then((response) => {setAccounts(response.data)})
         .catch((error) => {console.log(error)})
    } else {
      axios.get(apiUrl + '/account/getByCity?city=' + selectedCity)
         .then((response) => {setAccounts(response.data)})
         .catch((error) => {console.log(error)})
    }
  }

  const deleteAccount = (id) => {
    if (confirm("Are you sure you want to delete this account?")) {
      axios.delete(apiUrl + '/account/delete?accountId=' + id)
        .then(() => loadAccounts())
        .catch((error) => console.log(error))
    }
  };
  
  return (
    <div style={{ position: "relative" }}>
      {selectedAccountId && <AccountPopUp accountId={selectedAccountId} selectAccountId={selectAccountId}/>}

      <Header/>

      <div id="layout"> 
        <Sidebar/>

        <div id="content">
          <div id="header">
            <h2>Accounts</h2>

            <Link to="/account/add">
              <button id="add">Add</button>
            </Link>
            
          </div>

          <div style={{ display: "flex", alignItems: "center", justifyContent: "center" }}>
            <div id="filter">
              <p>Filter by city:</p>
              <form>
                <select 
                  name="city" 
                  id="city" 
                  value={selectedCity} 
                  onChange={
                    (e) => {selectCity(e.target.value)
                      console.log(e.target.value)
                    }
                  }
                >
                  <option value="">Select</option>
                  <option value="Adelaide">Adelaide</option>
                  <option value="Auckland">Auckland</option>
                  <option value="Brisbane">Brisbane</option>
                  <option value="Calgary">Calgary</option>
                  <option value="Canberra">Canberra</option>
                  <option value="Christchurch">Christchurch</option>
                  <option value="Darwin">Darwin</option>
                  <option value="Dunedin">Dunedin</option>
                  <option value="Geelong">Geelong</option>
                  <option value="Hamilton">Hamilton</option>
                  <option value="Hobart">Hobart</option>
                  <option value="Melbourne">Melbourne</option>
                  <option value="Montreal">Montreal</option>
                  <option value="Napier">Napier</option>
                  <option value="Nelson">Nelson</option>
                  <option value="Newcastle">Newcastle</option>
                  <option value="Palmerston North">Palmerston North</option>
                  <option value="Perth">Perth</option>
                  <option value="Sydney">Sydney</option>
                  <option value="Tauranga">Tauranga</option>
                  <option value="Toronto">Toronto</option>
                  <option value="Vancouver">Vancouver</option>
                  <option value="Wellington">Wellington</option>
                  <option value="Whangarei">Whangarei</option>
                  <option value="Winnipeg">Winnipeg</option>
                </select>
              </form>
            </div>
          </div>

          <table id="accountTable">
            <thead>
              <tr>
                <th>ID</th>
                <th>Customer</th>
                <th>Balance</th>
                <th>Type</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              { accounts.map((account) => (
                <tr key={account.accountId}>
                  <td 
                    onClick={() => selectAccountId(account.accountId)}
                  >
                    {account.accountId}
                  </td>
                  <td 
                    onClick={() => selectAccountId(account.accountId)}
                  >
                    {customerNames[account.customerId] || ""}
                  </td>
                  <td
                    onClick={() => selectAccountId(account.accountId)}
                  >
                    ${account.balance.toLocaleString('en-NZ', {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2
                    })}
                  </td>
                  <td
                    onClick={() => selectAccountId(account.accountId)}
                  >
                    {account.accountType == 'savingsAccount' ? 'Savings' : 'Checking'}
                  </td>
                  <td className="actions">
                    <Link to={`/account/edit/${account.accountId}`}>
                      <button>Update</button>
                    </Link>
                    <button onClick={()=>{deleteAccount(account.accountId)}}>Delete</button>
                  </td>
                </tr>
              )) }

            </tbody>
          </table>
        </div>

      </div>
    </div>
  )
}

export default Accounts