import '../../App.css'
import axios from 'axios'
import { useState, useEffect } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Sidebar from '../../components/Sidebar'
import Header from '../../components/Header'

function UpdateCustomer() {
  const { customerId } = useParams();
  const apiUrl = 'http://localhost:8080/sprint4Deliverable'

  const [currentCustomer, setCurrentCustomer] = useState(null)

  const [message, setMessage] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    const loadCustomer = async () => {
      axios.get(apiUrl + '/customer/get?customerId=' + customerId)
        .then((response) => {
          setCurrentCustomer(response.data);
        })
        .catch((error) => {console.log(error)})
    }
    
    loadCustomer()
  }, []);

  const sendCustomerData = async (customerData) => {
    try {

      // Edit current customer
      await axios.put(apiUrl + '/customer/edit', customerData)
      navigate('/customer')

    } catch (error) {

      if (error.response?.status == 400 && error.response?.data == "Postal code not found") {
        setMessage('Invalid postal code. Please enter a valid postal code.')

      } else if (error.response?.status == 400 && error.response?.data == "Incorrect city or province entered") {
        setMessage('Invalid city/province. Please enter the valid city/province or leave it blank to auto-fill.')

      } else {
        setMessage('Error updating customer. Please try again.')

      }

      console.error('Error updating customer:', error)
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()

    setMessage("");

    const customer = {
      customerId: currentCustomer.customerId,
      customerType: currentCustomer.customerType,
      name: currentCustomer.name,
      address: {
        addressId: currentCustomer.address.addressId,
        streetNumber: currentCustomer.address.streetNumber,
        city: currentCustomer.address.city,
        province: currentCustomer.address.province,
        postalCode: currentCustomer.address.postalCode
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
            <h2>Update Customer</h2>
          </div>

          <form id="customerForm" onSubmit={handleSubmit}>

              <label for="customerId">ID:</label>
              <input 
                type="text" 
                id="customerId" 
                name="customerId" 
                value={currentCustomer?.customerId} 
                class="read-only"
                readOnly
              />

              <label for="customerType">Customer Type:</label>
              <input 
                type="text" 
                id="customerType" 
                name="customerType" 
                value={currentCustomer?.customerType == "person" ? "Person" : "Company" } 
                class="read-only"
                readOnly
              />

              <label for="name">Name*:</label>
              <input 
                type="text" 
                id="name" 
                name="name" 
                value={currentCustomer?.name} 
                onChange={(e) =>
                  setCurrentCustomer({
                      ...currentCustomer,
                      name: e.target.value
                  })
                }
                required
              />
            
              <label for="streetNumber">Street*:</label>
              <input 
                type="text" 
                id="streetNumber" 
                name="streetNumber" 
                value={currentCustomer?.address.streetNumber} 
                onChange={(e) =>
                  setCurrentCustomer({
                      ...currentCustomer,
                      address: {
                          ...currentCustomer.address,
                          streetNumber: e.target.value
                      }
                  })
                }
                required
              />
            
              <label for="city">City:</label>
              <input 
                type="text" 
                id="city" 
                name="city" 
                value={currentCustomer?.address.city}
                onChange={(e) =>
                  setCurrentCustomer({
                      ...currentCustomer,
                      address: {
                          ...currentCustomer.address,
                          city: e.target.value
                      }
                  })
                }
              />
            
              <label for="province">Province:</label>
              <input 
                type="text" 
                id="province" 
                name="province" 
                value={currentCustomer?.address.province}
                onChange={(e) =>
                  setCurrentCustomer({
                      ...currentCustomer,
                      address: {
                          ...currentCustomer.address,
                          province: e.target.value
                      }
                  })
                }
              />
            
              <label for="postalCode">Postal Code*:</label>
              <input 
                type="text" 
                id="postalCode" 
                name="postalCode" 
                value={currentCustomer?.address.postalCode} 
                onChange={(e) =>
                  setCurrentCustomer({
                      ...currentCustomer,
                      address: {
                          ...currentCustomer.address,
                          postalCode: e.target.value
                      }
                  })
                }
                required
              />

              <div id="formActions">
                <button type="submit" id="submit">Update</button>
                <Link to="/customer">
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

export default UpdateCustomer;