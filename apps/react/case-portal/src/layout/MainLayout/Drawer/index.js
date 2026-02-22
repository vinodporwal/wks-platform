import PropTypes from 'prop-types'
import { useMemo } from 'react'
import { useTheme } from '@mui/material/styles'
import Box from '@mui/material/Box'
import Drawer from '@mui/material/Drawer'
import DrawerHeader from './DrawerHeader'
import DrawerContent from './DrawerContent'
import { drawerWidth } from 'config'

const MainDrawer = ({ open }) => {
  const theme = useTheme()

  const drawerContent = useMemo(() => <DrawerContent />, [])
  const drawerHeader = useMemo(() => <DrawerHeader open={open} />, [open])

  return (
    <Drawer
      variant='persistent'
      open={open}
      sx={{
        width: open ? drawerWidth : 0,
        flexShrink: 0,
        whiteSpace: 'nowrap',

        transition: theme.transitions.create(['width'], {
          easing: 'cubic-bezier(0.4, 0, 0.2, 1)', // ?? natural motion
          duration: 380,
        }),

        '& .MuiDrawer-paper': {
          width: open ? drawerWidth : 0,
          overflowX: 'hidden',
          boxSizing: 'border-box',

          /* ?? Modern background */
          background: 'linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%)',

          backdropFilter: 'blur(16px)',
          WebkitBackdropFilter: 'blur(16px)',

          /* ?? Soft edge instead of hard border */
          // borderRight: '1px solid rgba(148,163,184,0.18)',

          /* ?? Premium depth */
          boxShadow:
            '8px 0 24px rgba(15,23,42,0.08), 2px 0 6px rgba(15,23,42,0.04)',

          transition: theme.transitions.create(
            ['width', 'box-shadow', 'background'],
            {
              easing: 'cubic-bezier(0.4, 0, 0.2, 1)',
              duration: 380,
            },
          ),
        },
      }}
    >
      {open && (
        <Box
          role='presentation'
          sx={{
            height: '100vh',
            display: 'flex',
            flexDirection: 'column',
            pt: '0px', // ?? THIS is the fix

            /* ? Smooth scroll */
            overflowY: 'auto',
            overflowX: 'hidden',
            scrollBehavior: 'smooth',

            /* ?? Clean scrollbar */
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
          {drawerHeader}
          {drawerContent}
        </Box>
      )}
    </Drawer>
  )
}

MainDrawer.propTypes = {
  open: PropTypes.bool.isRequired,
}

export default MainDrawer
