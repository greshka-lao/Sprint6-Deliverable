import { useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import '../App.css'

function Header() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    const handleOutsideClick = (event) => {
      const sidebar = document.querySelector('.sidebar')
      const menuIcon = document.querySelector('img[alt="Menu"]')

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

    return () => {
      document.removeEventListener('click', handleOutsideClick)
    }
  }, [])

  const handleLogout = async () => {
    await logout()
    if (location.pathname !== '/login') {
      navigate('/login')
    }
  }

  return (
    <header>
      <img 
        src="menu-icon.svg" 
        alt="Menu" 
        width="24" 
        height="24" 
        onClick={() => {
          const sidebar = document.querySelector('.sidebar')
          if (sidebar) {
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
        }}
      />
      <h1>Hooper Bank</h1>

      {user ? (
        <div className="header-actions">
          <span className="header-user">{user.username}</span>
          <button type="button" onClick={handleLogout}>Logout</button>
        </div>
      ) : (
        <Link to="/login" className="header-login-link">Login</Link>
      )}
    </header> 
  )
}

export default Header