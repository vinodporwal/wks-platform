// material-ui
import { styled } from '@mui/material/styles'
import AppBar from '@mui/material/AppBar'

// project import
import { drawerWidth, miniDrawerWidth } from 'config'

// ==============================|| HEADER - APP BAR STYLED ||============================== //

const AppBarStyled = styled(AppBar, {
  shouldForwardProp: (prop) => prop !== 'open' && prop !== 'isDashboard',
})(({ theme, open, isDashboard }) => ({
  zIndex: theme.zIndex.drawer + 1,
  transition: theme.transitions.create(['width', 'margin'], {
    easing: theme.transitions.easing.sharp,
    duration: theme.transitions.duration.leavingScreen,
  }),

  ...(!open && {
    marginLeft: isDashboard ? 0 : miniDrawerWidth,
    width: isDashboard ? '100%' : `calc(100% - ${miniDrawerWidth}px)`,
  }),

  ...(open && {
    marginLeft: drawerWidth,
    width: `calc(100% - ${drawerWidth}px)`, // ? FIX HERE

    transition: theme.transitions.create(['width', 'margin'], {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.enteringScreen,
    }),
  }),
}))

export default AppBarStyled
