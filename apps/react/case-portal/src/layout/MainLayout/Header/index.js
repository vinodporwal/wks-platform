import CloseOutlined from '@ant-design/icons/CloseOutlined'
import MenuOutlined from '@ant-design/icons/MenuOutlined'

import MenuRoundedIcon from '@mui/icons-material/MenuRounded'
import CloseRoundedIcon from '@mui/icons-material/CloseRounded'
import DragHandleIcon from '@mui/icons-material/DragHandle'
import MenuOpenIcon from '@mui/icons-material/MenuOpen'

import { Toolbar, IconButton, Box, Zoom } from '@mui/material'

import { useMediaQuery } from '@mui/material'
import { useTheme } from '@mui/material/styles'
import AppBarStyled from './AppBarStyled'
import HeaderContent from './HeaderContent/index'

{
  /* <MenuOutlined /> */
  // <CloseOutlined />
}

const Header = ({ open, handleDrawerToggle, keycloak }) => {
  const theme = useTheme()
  const matchDownMD = useMediaQuery(theme.breakpoints.down('lg'))
  const iconBackColor = 'grey.100'
  const iconBackColorOpen = 'grey.200'

  const mainHeader = (
    <Toolbar
      sx={{
        minHeight: '38px !important',
        py: 0,
      }}
    >
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
        }}
      >
        <Box
          sx={{
            position: 'relative',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          {/* Menu Icon Transition */}
          <Zoom in={!open} timeout={300} unmountOnExit>
            <DragHandleIcon
              sx={{
                position: 'absolute',
                color: 'rgba(255, 255, 255, 0.9)',
                fontSize: '2rem',
                transition: 'color 0.2s ease-in-out',
                '&:hover': {
                  color: '#00F5E1',
                },
              }}
            />
          </Zoom>

          {/* Close Icon Transition */}
          <Zoom in={open} timeout={300} unmountOnExit>
            <CloseRoundedIcon
              sx={{
                position: 'absolute',
                color: 'rgba(255, 255, 255, 0.9)',
                fontSize: '2rem',
                // Adds a subtle rotation as it zooms in
                transform: open ? 'rotate(0deg)' : 'rotate(-90deg)',
                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                '&:hover': {
                  color: '#00F5E1',
                },
              }}
            />
          </Zoom>
        </Box>
      </IconButton>

      <HeaderContent keycloak={keycloak} />
    </Toolbar>
  )
  const appBar = {
    position: 'fixed',
    color: 'inherit',
    elevation: 0,
    sx: {
      borderBottom: `1px solid ${theme.palette.divider}`,
    },
  }

  return (
    <>
      {/* {!matchDownMD ? (
        <AppBarStyled open={open} {...appBar}>
          {mainHeader}
        </AppBarStyled>
      ) : (
        <AppBar {...appBar}>{mainHeader}</AppBar>
      )} */}
      <AppBarStyled open={open} {...appBar}>
        {mainHeader}
      </AppBarStyled>
    </>
  )
}

// Header.propTypes = {
//   open: PropTypes.bool,
//   handleDrawerToggle: PropTypes.func,
// }

export default Header
