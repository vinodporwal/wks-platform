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
import PageSkeleton from 'components/PageSkeleton'

const MainLayout = ({ keycloak, authenticated }) => {
  const dispatch = useDispatch()
  const { drawerOpen: open } = useSelector((state) => state.menu)
  const { items: menuItems, isMenuLoading } = useMenuContext()
  const menu = { items: [...menuItems] }
  const location = useLocation()
  const isDashboard = location.pathname === '/dashboard'
  // Routes that should display StepperNav
  const stepperNavRoutes = ['/production-norms-plan', '/tcs', '/utilityPlant']

  const BG_COLOR = '#ffff'
  const BG_COLOR_FULL = '#ffff'

  const handleDrawerToggle = useCallback(() => {
    dispatch(openDrawer({ drawerOpen: !open }))
  }, [dispatch, open])

  const hideBreadcrumbs =
    location?.pathname === '/user-management' ||
    location?.pathname === '/user-form' ||
    location?.pathname === '/dashboard' ||
    [
      '/refinery-aop-budget/plant-capacities',
      '/refinery-aop-budget/shutdown',
      '/refinery-aop-budget/slowdown',
      '/refinery-aop-budget/other-document-upload',
      '/jw-budget/jw-unit',
      '/jw-budget/throughput-norms',
      '/jw-budget/jw-budget-source-dta',
      '/jw-budget/jw-budget-source',
      '/jw-budget/fixed-bed-and-lab-cost',
    ].includes(location?.pathname)

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
          borderRadius: '6px',
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

        {isMenuLoading ? (
          <PageSkeleton />
        ) : (
          <>
            {stepperNavRoutes.some((route) =>
              location?.pathname.startsWith(route),
            ) && (
              <Box>
                <StepperNav />
              </Box>
            )}

            <Box className='outlet-wrapper'>
              <Box className='outlet-border-box'>
                {!hideBreadcrumbs && (
                  <Box className='breadcrumbs-box'>
                    <Breadcrumbs
                      variant='dense'
                      navigation={menu}
                      divider={false}
                    />
                  </Box>
                )}

                <Outlet />
              </Box>
            </Box>
          </>
        )}
      </Box>
    </Box>
  )
}

export default MainLayout
