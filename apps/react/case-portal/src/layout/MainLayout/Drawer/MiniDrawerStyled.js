// material-ui
import { styled } from '@mui/material/styles'
import Drawer from '@mui/material/Drawer'
import { drawerWidth, miniDrawerWidth } from 'config'

const openedMixin = (theme) => ({
  width: drawerWidth,
  // borderRight: `1px solid ${theme.palette.divider}`,
  transition: theme.transitions.create('width', {
    easing: theme.transitions.easing.sharp,
    duration: theme.transitions.duration.enteringScreen,
  }),
  overflowX: 'hidden',
  boxShadow: 'none',
  background: 'linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%)',
  borderRight: '1px solid #DDDEE1 !important',
})

const closedMixin = (theme) => ({
  transition: theme.transitions.create('width', {
    easing: theme.transitions.easing.sharp,
    duration: theme.transitions.duration.leavingScreen,
  }),
  overflowX: 'hidden',
  width: miniDrawerWidth,
  borderRight: '1px solid #DDDEE1 !important',
  // boxShadow: theme.customShadows.z1,
  background: 'linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%)',
})

const MiniDrawerStyled = styled(Drawer, {
  shouldForwardProp: (prop) => prop !== 'open' && prop !== 'hide',
})(({ theme, open, hide }) => ({
  width: drawerWidth,
  flexShrink: 0,
  whiteSpace: 'nowrap',
  boxSizing: 'border-box',
  ...(open && {
    ...openedMixin(theme),
    '& .MuiDrawer-paper': openedMixin(theme),
  }),
  ...(!open && {
    ...closedMixin(theme),
    '& .MuiDrawer-paper': closedMixin(theme),
  }),
  ...(hide && {
    width: 0,
    '& .MuiDrawer-paper': {
      ...closedMixin(theme),
      width: 0,
      borderRight: 'none',
    },
  }),
}))

export default MiniDrawerStyled
