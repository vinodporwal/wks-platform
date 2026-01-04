import { useCallback } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Outlet } from 'react-router-dom'
import { Box, Toolbar } from '@mui/material'
import Breadcrumbs from 'components/@extended/Breadcrumbs'
import Drawer from './Drawer'
import Header from './Header'
import { openDrawer } from 'store/reducers/menu'
import { useMenuContext } from 'menu/menuProvider'
import StepperNav from 'components/Utilities/StepperNav'
import { useLocation } from '../../../node_modules/react-router-dom/dist/index'

const MainLayout = ({ keycloak, authenticated }) => {
  const dispatch = useDispatch()
  const { drawerOpen: open } = useSelector((state) => state.menu)
  // const menu = useMenu()
  const { items: menuItems } = useMenuContext()
  const menu = { items: [...menuItems] }
  const location = useLocation()

  const handleDrawerToggle = useCallback(() => {
    dispatch(openDrawer({ drawerOpen: !open }))
  }, [dispatch, open])

  return (
    keycloak &&
    authenticated && (
      <Box sx={{ width: '100%' }}>
        {/* Fixed Header */}
        <Header
          open={open}
          handleDrawerToggle={handleDrawerToggle}
          keycloak={keycloak}
        />

        {/* Content BELOW header */}
        <Box
          sx={{
            display: 'flex',
            width: '100%',
            mt: '38px', // ? ONLY header height
          }}
        >
          <Drawer open={open} handleDrawerToggle={handleDrawerToggle} />

          <Box
            component='main'
            sx={{
              flexGrow: 1,
              width: '100%',
              mt: 2.5, // ? only top margin
              p: 2.5, // ? very less padding
            }}
          >
            {/* Stepper is NORMAL FLOW */}
            {location?.pathname.startsWith('/production-norms-plan') && (
              <StepperNav />
            )}

            <Breadcrumbs variant='dense' navigation={menu} divider={false} />
            <Outlet />
          </Box>
        </Box>
      </Box>
    )
  )
}

export default MainLayout
