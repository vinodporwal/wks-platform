import { useCallback } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Outlet, useLocation } from 'react-router-dom'
import { Box, Toolbar, useTheme } from '@mui/material'
import Breadcrumbs from 'components/@extended/Breadcrumbs'
import Drawer from './Drawer'
import Header from './Header'
import { openDrawer } from 'store/reducers/menu'
import { useMenuContext } from 'menu/menuProvider'
import StepperNav from 'components/Utilities/StepperNav'
import UtilityDetails from 'components/Utilities/UtilityDetails'

const MainLayout = ({ keycloak, authenticated }) => {
  const dispatch = useDispatch()
  const theme = useTheme()
  const isDark = theme.palette.mode === 'dark'
  const { drawerOpen: open } = useSelector((state) => state.menu)
  const { items: menuItems } = useMenuContext()
  const menu = { items: [...menuItems] }
  const location = useLocation()
  const isDashboard = location.pathname === '/dashboard'
  // Routes that should display StepperNav
  const stepperNavRoutes = ['/production-norms-plan', '/tcs', '/utilityPlant']

  const BG_COLOR = isDark ? '#131A2A' : '#ffff'
  const BG_COLOR_FULL = isDark ? '#131A2A' : '#ffff'

  const handleDrawerToggle = useCallback(() => {
    dispatch(openDrawer({ drawerOpen: !open }))
  }, [dispatch, open])

  const hideBreadcrumbs =
    location?.pathname === '/user-management' ||
    location?.pathname === '/user-form' ||
    location?.pathname === '/dashboard'

  if (!keycloak || !authenticated) return null

  return (
    <Box sx={{ display: 'flex', width: '100%', height: '100vh' }}>
      {/* Sidebar LEFT */}
      <Drawer
        open={open}
        handleDrawerToggle={handleDrawerToggle}
        isDashboard={isDashboard}
        keycloak={keycloak}
      />

      {/* RIGHT SIDE CONTENT */}
      <Box
        component='main'
        sx={{
          flex: 1,
          minWidth: 0,
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          backgroundColor: BG_COLOR_FULL,

          // border: '1px solid rgba(0,0,0,0.08)',
          // boxShadow: '0 4px 14px rgba(0,0,0,0.10)',
          // borderRadius: '6px',
        }}
      >
        {/* Header ONLY for right content */}
          <Header
            open={open}
            handleDrawerToggle={handleDrawerToggle}
            keycloak={keycloak}
            isDashboard={isDashboard}
            navigation={menu}
          />

        {/* Push content below header */}
        <Box sx={{ pt: '46px' }} />

        {/* {location.pathname.startsWith('/production-norms-plan') && (
          <Box>
            <StepperNav />
          </Box>
        )} */}

        {stepperNavRoutes.some((route) =>
          location?.pathname.startsWith(route),
        ) && (
            <Box>
              <StepperNav />
            </Box>
        )}

        {/* HIDE AS OF NOW - 16 APRIL 2026 */}
        {/* {location.pathname.startsWith('/production-norms-plan') && (
          <Box>
            <UtilityDetails navigation={menu} />
          </Box>
        )} */}

        {/* <Box className='outlet-wrapper'>
          <Box className='outlet-border-box'>
            {stepperNavRoutes.some((route) =>
              location?.pathname.startsWith(route),
            ) && (
                <Box className="breadcrumbs-box">
                  <Breadcrumbs variant='dense' navigation={menu} divider={false} />
                </Box>)}
            <Outlet />
          </Box>
        </Box> */}

        <Box className={isDark ? 'outlet-wrapper-dark' : 'outlet-wrapper'}>
          <Box className='outlet-border-box'>
            {!hideBreadcrumbs && (
                <Breadcrumbs
                  variant='dense'
                  navigation={menu}
                  divider={false}
                />
            )}

            <Outlet />
          </Box>
        </Box>
      </Box>
    </Box>
  )
}

export default MainLayout
