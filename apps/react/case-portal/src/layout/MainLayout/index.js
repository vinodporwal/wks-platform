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
import UtilityDetails from 'components/Utilities/UtilityDetails'

const MainLayout = ({ keycloak, authenticated }) => {
  const dispatch = useDispatch()
  const { drawerOpen: open } = useSelector((state) => state.menu)
  const { items: menuItems } = useMenuContext()
  const menu = { items: [...menuItems] }
  const location = useLocation()

  const BG_COLOR = '#ffff'
  const BG_COLOR_FULL = '#ffff'

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
          minWidth: 0,
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          backgroundColor: BG_COLOR_FULL,
          pt: '55px',

          border: '1px solid rgba(0,0,0,0.08)',
          boxShadow: '0 4px 14px rgba(0,0,0,0.10)',
          borderRadius: '6px',
        }}
      >
        {/* Reserve header height */}
        {/* <Toolbar variant='dense' /> */}

        {location.pathname.startsWith('/production-norms-plan') && (
          <Box>
            <StepperNav />
          </Box>
        )}

        {location.pathname.startsWith('/production-norms-plan') && (
          <Box>
            <UtilityDetails navigation={menu} />
          </Box>
        )}

        {/* <Toolbar variant='dense' /> */}
        <Box sx={{ height: 4 }} />
        {/* HIDE Breadcrumbs */}
        {/* <Box
          sx={{
            m: 0,
            p: 0,
            display: 'flex',
            alignItems: 'center',
            ml: '8px',
            mr: '5px',
            mb: '8px',
            backgroundColor: `${BG_COLOR}`,
            border: '1px solid rgba(0,0,0,0.08)',
            boxShadow: '0 4px 14px rgba(0,0,0,0.10)',
            borderRadius: '6px',
          }}
        >
          <Breadcrumbs variant='dense' navigation={menu} divider={false} />
        </Box> */}

        {/* Page content */}
        <Box className='outlet-wrapper'>
          <Box className='outlet-border-box'>
            <Outlet />
          </Box>
        </Box>
      </Box>
    </Box>
  )
}

export default MainLayout
