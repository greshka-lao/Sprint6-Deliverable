import '../../App.css'
import axios from 'axios'
import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Sidebar from '../../components/Sidebar'
import Header from '../../components/Header'

function AddAccount() {
  const apiUrl = 'http://localhost:8080/sprint4Deliverable'
  
  const [customers, setCustomers] = useState([])
  const [message, setMessage] = useState('')

  const [accountType, setAccountType] = useState('savingsAccount')

  const navigate = useNavigate()

  useEffect(() => {
    const loadCustomers = async () => {
      axios.get(apiUrl + '/customer/getAll')
        .then((response) => {
          const customersData = response.data
          if (customersData.length == 0) {
            alert("There are no customers.")
            navigate("/account")
          }
          setCustomers(customersData)
        })
        .catch((error) => {console.log(error)})
    }
    
    loadCustomers()
  }, [])

  const sendAccountData = async (accountData) => {
    try {
      const response = await axios.post(apiUrl + '/account/save', accountData)

      await axios.put(apiUrl + `/account/setCustomer?accountId=${response.data.accountId}&customerId=${accountData.customerId}`);

      navigate('/account')
    } catch (error) {
      console.error('Error adding account:', error)
      setMessage('Error adding account. Please try again.')
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    
    setMessage("");
    
    const formData = new FormData(e.target)
    const rawData = Object.fromEntries(formData);

    if (parseFloat(rawData.balance) < 0) {
      setMessage('Balance cannot be negative.');
      return;
    }

    if (rawData.accountType === 'savingsAccount' && parseFloat(rawData.interestRate) < 0) {
      setMessage('Interest rate cannot be negative.');
      return;
    }

    if (rawData.accountType === 'checkingAccount' && parseFloat(rawData.nextCheckNumber) < 0) {
      setMessage('Next check number cannot be negative.');
      return;
    }

    const account = {
      accountType: rawData.accountType,
      balance: parseFloat(rawData.balance),
      customerId: parseInt(rawData.customerId),
      ...(rawData.accountType === 'savingsAccount' && { interestRate: parseFloat(rawData.interestRate) }),
      ...(rawData.accountType === 'checkingAccount' && { nextCheckNumber: parseInt(rawData.nextCheckNumber) }),
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
          <h2>Add Account</h2>
        </div>

        <form id="accountForm" onSubmit={handleSubmit}>

            <label for="accountType">Account Type:</label>
            <select name="accountType" id="accountType" onChange={(e) => setAccountType(e.target.value)}>
              <option value="savingsAccount">Savings</option>
              <option value="checkingAccount">Checking</option>
            </select>

            <label for="balance">Balance:</label>
            <input type="number" id="balance" name="balance" step="0.01" required/>
          
            <label for="customerId">Customer*:</label>
            <select name="customerId" id="customerId" required>
              {customers.map((customer) => (
                <option key={customer.customerId} value={customer.customerId}>
                  {customer.name}
                </option>
              ))}
            </select>
          
            {accountType == 'savingsAccount' && (
              <>
                <label for="interestRate">Interest Rate*:</label>
                <input type="number" id="interestRate" name="interestRate" step="0.01" required/>
              </>
            )}
          
            {accountType == 'checkingAccount' && (
              <>
                <label for="nextCheckNumber">Next Check Number*:</label>
                <input type="number" id="nextCheckNumber" name="nextCheckNumber" step="1" required/>
              </>
            )}

            <div id="formActions">
              <button type="submit" id="submit">Add</button>
              <Link to="/account">
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

export default AddAccount;