import MenuOpenIcon from '@mui/icons-material/MenuOpen'
import MenuIcon from '@mui/icons-material/Menu'

import { Toolbar, IconButton } from '@mui/material'
import { useMediaQuery } from '@mui/material'
import { useTheme } from '@mui/material/styles'
import AppBarStyled from './AppBarStyled'
import HeaderContent from './HeaderContent/index'
import logo from 'assets/images/ril-logo2.png'
import { Box } from '@mui/material'

const Header = ({ open, handleDrawerToggle, keycloak, isDashboard }) => {
  const theme = useTheme()
  const matchDownMD = useMediaQuery(theme.breakpoints.down('lg'))

  const mainHeader = (
    <Toolbar
      sx={{
        minHeight: '38px !important',
        py: 0,
      }}
    >
      {/* Show icon ONLY when drawer is closed */}
      {!open && (
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          {isDashboard && (
            <Box
              component='img'
              src={logo}
              alt='Logo'
              sx={{ width: 28, height: 28, mr: 3 }}
            />
          )}
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
          
        </Box>
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

      <HeaderContent keycloak={keycloak} />
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
