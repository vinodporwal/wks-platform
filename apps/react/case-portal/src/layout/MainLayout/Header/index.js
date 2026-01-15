import CloseOutlined from '@ant-design/icons/CloseOutlined'
import MenuOutlined from '@ant-design/icons/MenuOutlined'

import MenuRoundedIcon from '@mui/icons-material/MenuRounded'
import CloseRoundedIcon from '@mui/icons-material/CloseRounded'
import DragHandleIcon from '@mui/icons-material/DragHandle'
import MenuOpenIcon from '@mui/icons-material/MenuOpen'
import MenuIcon from '@mui/icons-material/Menu'

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
          borderRight: '1px solid #000', // ? black right border
          borderRadius: 0, // ? keeps it straight
          pr: 1, // ? little space from border (optional)
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
          <Zoom in timeout={300}>
            <MenuIcon
              sx={{
                position: 'absolute',
                fontSize: '2rem',

                color: open ? '#00F5E1' : 'rgba(255, 255, 255, 0.55)',

                opacity: open ? 1 : 0.85,
                filter: open
                  ? 'drop-shadow(0 0 6px rgba(0, 245, 225, 0.55))'
                  : 'none',

                transition:
                  'color 0.25s ease-in-out, filter 0.25s ease-in-out, opacity 0.25s ease-in-out',
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
    // sx: {
    //   borderBottom: `1px solid ${theme.palette.divider}`,
    // },
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
