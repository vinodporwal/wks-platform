import { useCallback } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Outlet, useLocation } from 'react-router-dom'
import { Box, Toolbar } from '@mui/material'
import Breadcrumbs from 'components/@extended/Breadcrumbs'
import Drawer from './Drawer'
import Header from './Header'
import { openDrawer } from 'store/reducers/menu'
import { useMenuContext } from 'menu/menuProvider'
import StepperNav from 'components/Utilities/StepperNav'

const MainLayout = ({ keycloak, authenticated }) => {
  const dispatch = useDispatch()
  const { drawerOpen: open } = useSelector((state) => state.menu)
  const { items: menuItems } = useMenuContext()
  const menu = { items: [...menuItems] }
  const location = useLocation()

  const handleDrawerToggle = useCallback(() => {
    dispatch(openDrawer({ drawerOpen: !open }))
  }, [dispatch, open])

  if (!keycloak || !authenticated) return null

  return (
    <Box sx={{ display: 'flex', width: '100%', height: '100vh' }}>
      {/* Fixed Header */}
      <Header
        open={open}
        handleDrawerToggle={handleDrawerToggle}
        keycloak={keycloak}
      />

      {/* Sidebar */}
      <Drawer open={open} handleDrawerToggle={handleDrawerToggle} />

      {/* Main Content */}
      <Box
        component='main'
        sx={{
          flex: 1,
          minWidth: 0, // ? CRITICAL
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden', // grid must scroll internally
        }}
      >
        {/* Reserve header height */}
        <Toolbar variant='dense' />

        {location.pathname.startsWith('/production-norms-plan') && (
          <StepperNav />
        )}

        <Breadcrumbs variant='dense' navigation={menu} divider={false} />

        {/* Page content */}
        <Box sx={{ flex: 1, overflow: 'auto', p: 1 }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  )
}

export default MainLayout
