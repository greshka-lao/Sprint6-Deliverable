import '../../../App.css'
import axios from 'axios'
import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Sidebar from '../../../components/Sidebar'
import Header from '../../../components/Header'

function AddCustomer() {
  const apiUrl = 'http://localhost:8080/sprint4Deliverable'
  const [message, setMessage] = useState('')
  const navigate = useNavigate()

  const sendCustomerData = async (customerData) => {
    try {
      await axios.post(apiUrl + '/customer/save', customerData)
      navigate('/admin/customers')
    } catch (error) {
      if (error.response?.status == 400 && error.response?.data == "Postal code not found") {
        setMessage('Invalid postal code. Please enter a valid postal code.')
      } else if (error.response?.status == 400 && error.response?.data == "Incorrect city or province entered") {
        setMessage('Invalid city/province. Please enter the valid city/province or leave it blank to auto-fill.')
      } else {
        setMessage('Error adding customer. Please try again.')
      }
      console.error('Error adding customer:', error)
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()

    setMessage("");
    
    const formData = new FormData(e.target)
    const rawData = Object.fromEntries(formData);

    const customer = {
      customerType: rawData.customerType,
      name: rawData.name,
      address: {
        streetNumber: rawData.streetNumber,
        city: rawData.city,
        province: rawData.province,
        postalCode: rawData.postalCode
      }
    };

    sendCustomerData(customer)
  }

  return (
    <>
      <Header/>

      <div id="layout">
        <Sidebar/>

        <div id="content">
          <div id="header">
            <h2>Add Customer</h2>
          </div>

          <form id="customerForm" onSubmit={handleSubmit}>

              <label for="customerType">Customer Type:</label>
              <select name="customerType" id="customerType">
                <option value="person">Person</option>
                <option value="company">Company</option>
              </select>

              <label for="name">Name*:</label>
              <input type="text" id="name" name="name" required/>
            
              <label for="streetNumber">Street*:</label>
              <input type="text" id="streetNumber" name="streetNumber" required/>
            
              <label for="city">City:</label>
              <input type="text" id="city" name="city"/>
            
              <label for="province">Province:</label>
              <input type="text" id="province" name="province"/>
            
              <label for="postalCode">Postal Code*:</label>
              <input type="text" id="postalCode" name="postalCode" required/>

              <div id="formActions">
                <button type="submit" id="submit">Add</button>
                <Link to="/admin/customers">
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

export default AddCustomer;