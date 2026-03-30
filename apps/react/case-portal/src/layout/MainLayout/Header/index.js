import MenuOpenIcon from '@mui/icons-material/MenuOpen'
import MenuIcon from '@mui/icons-material/Menu'

import { Toolbar, IconButton } from '@mui/material'
import { useMediaQuery } from '@mui/material'
import { useTheme } from '@mui/material/styles'
import AppBarStyled from './AppBarStyled'
import HeaderContent from './HeaderContent/index'

const Header = ({ open, handleDrawerToggle, keycloak }) => {
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
    <AppBarStyled open={open} {...appBar}>
      {mainHeader}
    </AppBarStyled>
  )
}

export default Header
