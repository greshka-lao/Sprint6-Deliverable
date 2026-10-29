import '../../../App.css'
import axios from 'axios'
import { useState, useEffect } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Sidebar from '../../../components/Sidebar'
import Header from '../../../components/Header'

function UpdateAccount() {
  const { accountId } = useParams();
  const apiUrl = 'http://localhost:8080/sprint4Deliverable'

  const [currentAccount, setCurrentAccount] = useState(null)
  const [customers, setCustomers] = useState([]);
  const [message, setMessage] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    const loadCustomers = async () => {
      axios.get(apiUrl + '/customer/getAll')
            .then((response) => {setCustomers(response.data)})
            .catch((error) => {console.log(error)})
    }

    const loadAccount = async () => {
      axios.get(apiUrl + '/account/get?accountId=' + accountId)
        .then((response) => {
          setCurrentAccount(response.data);
        })
        .catch((error) => {console.log(error)})
    }
    
    loadCustomers()
    loadAccount()
  }, []);

  const sendAccountData = async (accountData) => {
    try {
      // Update account (or create a new one if account type has changed)
      const response = await axios.put(apiUrl + '/account/edit', accountData);

      // Assign customer to account
      await axios.put(apiUrl + `/account/setCustomer?accountId=${response.data.accountId}&customerId=${accountData.customerId}`);
      
      navigate('/admin/accounts')
    } catch (error) {
      setMessage('Error updating account. Please try again.')
      console.error('Error updating account:', error)
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()

    setMessage("");

    const account = {
      accountId: currentAccount.accountId,
      accountType: currentAccount.accountType,
      balance: parseFloat(currentAccount.balance),
      customerId: parseInt(currentAccount.customerId),
      ...(currentAccount.accountType === 'savingsAccount' && { interestRate: parseFloat(currentAccount.interestRate) }),
      ...(currentAccount.accountType === 'checkingAccount' && { nextCheckNumber: parseInt(currentAccount.nextCheckNumber) }),
    };

    sendAccountData(account)
  }

  return (
    <>
      <Header/>

      <div id="layout">
        <Sidebar/>

        <div id="content">
          <div id="header">
            <h2>Update Account</h2>
          </div>

          <form id="accountForm" onSubmit={handleSubmit}>

            <label for="accountId">ID:</label>
            <input 
              type="text" 
              id="accountId" 
              name="accountId" 
              value={currentAccount?.accountId} 
              class="read-only"
              readOnly
            />

            <label for="accountType">Account Type:</label>
            <input 
              type="text" 
              id="accountType" 
              name="accountType" 
              value={currentAccount?.accountType == "savingsAccount" ? "Savings" : "Checking" } 
              class="read-only"
              readOnly
            />

            <label for="balance">Balance:</label>
            <input 
              type="number" 
              id="balance" 
              name="balance" 
              step="0.01"
              value={currentAccount?.balance} 
              onChange={(e) =>
                setCurrentAccount({
                    ...currentAccount,
                    balance: e.target.value
                })
              }
              required
            />
          
            <label for="customerId">Customer*:</label>
            <select 
              name="customerId" 
              id="customerId" 
              value={currentAccount?.customerId}
              onChange={(e) =>
                setCurrentAccount({
                    ...currentAccount,
                    customerId: e.target.value
                })
              }
              required
            >
              {customers.map((customer) => (
                <option key={customer.customerId} value={customer.customerId}>
                  {customer.name}
                </option>
              ))}
            </select>
          
            {currentAccount?.accountType == 'savingsAccount' && (
              <>
                <label for="interestRate">Interest Rate*:</label>
                <input 
                  type="number" 
                  id="interestRate" 
                  name="interestRate" 
                  step="0.01" 
                  value={currentAccount?.interestRate}
                  onChange={(e) =>
                    setCurrentAccount({
                        ...currentAccount,
                        interestRate: e.target.value
                    })
                  }
                  required
                />
              </>
            )}
          
            {currentAccount?.accountType == 'checkingAccount' && (
              <>
                <label for="nextCheckNumber">Next Check Number*:</label>
                <input 
                  type="number" 
                  id="nextCheckNumber" 
                  name="nextCheckNumber" 
                  step="1" 
                  value={currentAccount?.nextCheckNumber}
                  onChange={(e) =>
                    setCurrentAccount({
                        ...currentAccount,
                        nextCheckNumber: e.target.value
                    })
                  }
                  required
                />
              </>
            )}

            <div id="formActions">
              <button type="submit" id="submit">Update</button>
              <Link to="/admin/accounts">
                <button type="button">Cancel</button>
              </Link>
            </div>
          
          </form>
          {message && <p>{message}</p>}
        </div>
      </div>
    </>
  )
}

export default UpdateAccount;