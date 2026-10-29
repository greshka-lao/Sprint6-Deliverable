import { useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCustomerAuth } from '../context/useCustomerAuth'
import '../App.css'

function useHeaderMenu() {
  useEffect(() => {
    const handleOutsideClick = (event) => {
      const sidebar = document.querySelector('.sidebar')
      const menuIcon = document.querySelector('.header-menu-toggle')

      if (!sidebar || !menuIcon) return

      const isLargeScreen = window.matchMedia('(min-width: 768px)').matches
      if (isLargeScreen) return

      const clickedInsideSidebar = sidebar.contains(event.target)
      const clickedMenuIcon = menuIcon.contains(event.target)

      if (!clickedInsideSidebar && !clickedMenuIcon && sidebar.dataset.open === 'true') {
        sidebar.dataset.open = 'false'
        sidebar.style.transform = 'translateX(-100%)'
      }
    }

    document.addEventListener('click', handleOutsideClick)
    return () => document.removeEventListener('click', handleOutsideClick)
  }, [])

  return () => {
    const sidebar = document.querySelector('.sidebar')
    if (!sidebar) return

    const isOpen = sidebar.dataset.open === 'true'
    sidebar.dataset.open = String(!isOpen)
    sidebar.style.display = 'grid'
    sidebar.style.position = 'fixed'
    sidebar.style.top = '0'
    sidebar.style.left = '0'
    sidebar.style.height = '100vh'
    sidebar.style.transition = 'transform 0.3s ease-in-out'
    sidebar.style.transform = isOpen ? 'translateX(-100%)' : 'translateX(0)'
  }
}

function HeaderBar({ username, onLogout, menuLabel = 'Menu', logoutLabel = 'Logout' }) {
  const toggleSidebar = useHeaderMenu()

  return (
    <header>
      <img className="header-menu-toggle" src="menu-icon.svg" alt={menuLabel} width="24" height="24" onClick={toggleSidebar} />
      <h1>Hooper Bank</h1>
      <div className="header-actions">
        <span className="header-user">{username}</span>
        <button type="button" onClick={onLogout}>{logoutLabel}</button>
      </div>
    </header>
  )
}

function AdminHeader() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const handleLogout = async () => {
    await logout()
    if (location.pathname !== '/admin/login') {
      navigate('/admin/login')
    }
  }

  return (
    user
      ? <HeaderBar username={user.username} onLogout={handleLogout} />
      : <header><h1>Hooper Bank</h1><Link to="/" className="header-login-link"> Login</Link></header>
  )
}

function CustomerHeader() {
  const { customer, logout } = useCustomerAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/customer/login')
  }

  return (
    <HeaderBar
      username={customer?.username || 'Customer'}
      onLogout={handleLogout}
      menuLabel="Customer menu"
      logoutLabel="Sign out"
    />
  )
}

function Header({ variant = 'admin' }) {
  return variant === 'customer' ? <CustomerHeader /> : <AdminHeader />
}

export default Header