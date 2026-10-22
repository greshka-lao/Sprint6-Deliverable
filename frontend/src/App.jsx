import { Routes, Route } from 'react-router-dom'
import Customers from './pages/customer/Customers'
import Accounts from './pages/account/Accounts'
import AddCustomer from './pages/customer/AddCustomer'
import AddAccount from './pages/account/AddAccount'
import UpdateCustomer from './pages/customer/UpdateCustomer'
import UpdateAccount from './pages/account/UpdateAccount'
import './App.css'

function App() {

  return (
    <div>
      <Routes>
        <Route path="/" element={<Customers/>}/>
        <Route path="/customer" element={<Customers/>}/>
        <Route path="/customer/add" element={<AddCustomer/>}/>
        <Route path="/account/add" element={<AddAccount/>}/>
        <Route path="/account" element={<Accounts/>}/>
        <Route path="/customer/edit/:customerId" element={<UpdateCustomer/>} />
        <Route path="/account/edit/:accountId" element={<UpdateAccount/>} />
      </Routes>
    </div>
  )
}

export default App;
