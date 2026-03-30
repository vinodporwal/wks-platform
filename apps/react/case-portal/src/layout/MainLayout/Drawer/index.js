import PropTypes from 'prop-types'
import { useMemo } from 'react'
import Box from '@mui/material/Box'
import DrawerHeader from './DrawerHeader'
import DrawerContent from './DrawerContent'
import MiniDrawerStyled from './MiniDrawerStyled'

const MainDrawer = ({ open, handleDrawerToggle, isDashboard }) => {
  const drawerContent = useMemo(() => <DrawerContent />, [])
  const drawerHeader = useMemo(
    () => <DrawerHeader open={open} handleDrawerToggle={handleDrawerToggle} />,
    [open, handleDrawerToggle],
  )

  return (
    <MiniDrawerStyled
      variant='permanent'
      open={open}
      hide={isDashboard && !open}
    >
      <Box
        role='presentation'
        sx={{
          height: '100vh',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        {/* HEADER FIXED */}
        {drawerHeader}

        {/* MENU SCROLL AREA */}
        <Box
          sx={{
            flex: 1, // TAKE REMAINING HEIGHT
            overflowY: 'auto',
            overflowX: 'hidden',

            '&::-webkit-scrollbar': {
              width: '6px',
            },
            '&::-webkit-scrollbar-track': {
              background: 'transparent',
            },
            '&::-webkit-scrollbar-thumb': {
              background: 'rgba(100,116,139,0.35)',
              borderRadius: '10px',
            },
            '&::-webkit-scrollbar-thumb:hover': {
              background: 'rgba(100,116,139,0.55)',
            },
          }}
        >
          {drawerContent}
        </Box>
      </Box>
    </MiniDrawerStyled>
  )
}

MainDrawer.propTypes = {
  open: PropTypes.bool.isRequired,
  handleDrawerToggle: PropTypes.func,
  isDashboard: PropTypes.bool,
}

export default MainDrawer
