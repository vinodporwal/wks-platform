import MenuOpenIcon from '@mui/icons-material/MenuOpen'
import MenuIcon from '@mui/icons-material/Menu'

import { Toolbar, IconButton, Typography } from '@mui/material'
import { useMediaQuery } from '@mui/material'
import { useTheme } from '@mui/material/styles'
import AppBarStyled from './AppBarStyled'
import HeaderContent from './HeaderContent/index'
import { DashboardColors } from 'themes/colors'

const Header = ({
  open,
  handleDrawerToggle,
  keycloak,
  isDashboard,
  navigation,
}) => {
  const theme = useTheme()
  const matchDownMD = useMediaQuery(theme.breakpoints.down('lg'))

  const mainHeader = (
    <Toolbar
      sx={{
        minHeight: '38px !important',
        py: 0,
        pl: '16px !important',
        pr: '0px !important',
      }}
    >
      {/* Show icon ONLY when drawer is closed */}
      {!open && isDashboard && (
        <IconButton
          disableRipple
          aria-label='open drawer'
          onClick={handleDrawerToggle}
          edge='start'
          sx={{
            p: 0,
            mr: 1,
            width: 40,
            height: 40,
            color: '#6a7b92',
            pr: 1,
            '&:hover': { color: '#6a7b92' },
          }}
        >
          <MenuIcon
            sx={{
              // fontSize: '1.8rem',
              color: '#6a7b92',
              transition: 'all 0.25s ease',
            }}
          />
        </IconButton>
      )}

      {!open && isDashboard && (
        <Typography
          sx={{
            fontWeight: 800,
            fontSize: 14,
            color: theme.palette.mode === 'dark' ? '#F0F0F0' : DashboardColors.text.heading,
            fontFamily:
              "'Hiragino Sans', 'Honeywell Sans Web', 'Inter', sans-serif",
          }}
        >
          Reliance
        </Typography>
      )}
      {/* {open && (<IconButton
          onClick={handleDrawerToggle}
          size="small"
          sx={{
            color: '#6a7b92',
            ml: open ? 0 : 0,
            '&:hover': { color: '#6a7b92' },
          }}
        >
          {open ? <MenuOpenIcon /> : <MenuIcon />}
        </IconButton>)} */}

      <HeaderContent keycloak={keycloak} navigation={navigation} />
    </Toolbar>
  )

  const appBar = {
    position: 'fixed',
    color: 'inherit',
    elevation: 0,
  }

  return (
    <AppBarStyled open={open} isDashboard={isDashboard} {...appBar}>
      {mainHeader}
    </AppBarStyled>
  )
}

export default Header
