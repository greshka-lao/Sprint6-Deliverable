import '../../App.css'
import axios from 'axios'
import { useState, useEffect } from 'react'
import Sidebar from '../../components/Sidebar'
import Header from '../../components/Header'
import CustomerAccountsPopUp from '../../components/CustomerAccountsPopUp'
import { Link } from 'react-router-dom'

function Customers() {
  const apiUrl = 'http://localhost:8080/sprint4Deliverable'
  const [customers, setCustomers] = useState([]);

  const [selectedCustomer, selectCustomer] = useState(null);

  const [searchInput, setSearchInput] = useState("");
  const [searchId, setSearchId] = useState("");

  useEffect(() => {
    loadCustomers()
  }, [])

  const loadCustomers = async () => {
    axios.get(apiUrl + '/customer/getAll')
      .then((response) => {setCustomers(response.data)})
      .catch((error) => {console.log(error)})
  }

  const deleteCustomer = (id) => {
    if (confirm("Deleting this customer will also delete all associated accounts. Are you sure you want to continue?")) {
      axios.delete(apiUrl + '/customer/delete?customerId=' + id)
        .then(() => loadCustomers())
        .catch((error) => console.log(error))
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!(searchInput === '' || (Number.isInteger(Number(searchInput)) && Number(searchInput) > 0))) {
      alert("Please enter a valid ID number.");
    } else {
      setSearchId(searchInput);
    }
    
  }

  return (
    <div style={{ position: "relative" }}>
      {selectedCustomer && <CustomerAccountsPopUp customer={selectedCustomer} selectCustomer={selectCustomer}/>}

      <Header/>

      <div id="layout">
        <Sidebar/>

        <div id="content">
          <div id="header">
            <h2>Customers</h2>
            <Link to="/customer/add">
              <button id="add">Add</button>
            </Link>
          </div>

          <form id="searchCustomersForm" onSubmit={handleSubmit}>
            <input 
              type="text" 
              value={searchInput}
              id="search"
              placeholder="Search by ID"
              onChange={(e)=>{setSearchInput(e.target.value)}}
            />
            <button type="submit">Search</button>
            <button 
              onClick={()=>{setSearchInput("")}} 
              id="clearButton"
              style={{ opacity: searchInput != "" ? 1 : 0 }}
            >
              x
            </button>
          </form>

          <table id="customerTable">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Address</th>
                <th>Accounts</th>
                <th>Actions</th>
              </tr>
            </thead>
            
            <tbody>
            {customers
              .filter(customer => searchId === '' || customer.customerId === Number(searchId))
              .map(customer => (
                <tr key={customer.customerId}>
                  <td>{customer.customerId}</td>
                  <td>{customer.name}</td>
                  <td>{customer.address.streetNumber}, {customer.address.city}, {customer.address.province} {customer.address.postalCode}</td>
                  <td>
                    <button onClick={() => {
                      customer.accountIds.length == 0 ?
                        alert("This customer has no accounts.")
                      : selectCustomer(customer)
                    }}
                    >
                      Accounts
                    </button>
                  </td>
                  <td>
                    <div className="actions">
                      <Link to={`/customer/edit/${customer.customerId}`}>
                        <button>Update</button>
                      </Link>
                      <button onClick={()=>deleteCustomer(customer.customerId)}>Delete</button>
                    </div>
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

export default Customers;