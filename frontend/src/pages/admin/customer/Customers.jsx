import '../../../App.css'
import { useState, useEffect } from 'react'
import Sidebar from '../../../components/Sidebar'
import Header from '../../../components/Header'
import { Link } from 'react-router-dom'
import { deleteCustomer as removeCustomer, getCustomers } from '../../../services/customerService'

function Customers() {
  const [customers, setCustomers] = useState([]);

  const [searchInput, setSearchInput] = useState("");
  const [searchId, setSearchId] = useState("");

  useEffect(() => {
    let active = true
    getCustomers()
      .then((items) => {
        if (active) setCustomers(items)
      })
      .catch((error) => {
        console.error('Unable to load customers:', error)
      })

    return () => {
      active = false
    }
  }, [])

  const loadCustomers = async () => {
    setCustomers(await getCustomers())
  }

  const deleteCustomer = async (id) => {
    if (confirm("Remove this customer's login access? The record will be marked deleted, and assigned bank accounts will be unassigned.")) {
      try {
        await removeCustomer(id)
        await loadCustomers()
      } catch (error) {
        console.error('Unable to delete customer:', error)
      }
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
      <Header/>

      <div id="layout">
        <Sidebar/>

        <div id="content">
          <div id="header">
            <h2>Customers</h2>
            <Link to="/admin/customers/add">
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
                <th>Username</th>
                <th>Address</th>
                <th>Customer Type</th>
                <th>Phone</th>
                <th>Email</th>
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
                  <td>
                    {customer.username || ''}
                  </td>
                  <td>{customer.address || ''}</td>
                  <td>{customer.customerType || ''}</td>
                  <td>{customer.phoneNumber || ''}</td>
                  <td>{customer.email || ''}</td>
                  <td>
                    <div className="actions">
                      <Link to={`/admin/customers/edit/${customer.customerId}`}>
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