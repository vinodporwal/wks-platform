import PropTypes from 'prop-types'
import { Box, Typography, IconButton, useTheme } from '@mui/material'
import DrawerHeaderStyled from './DrawerHeaderStyled'
import logo from 'assets/images/ril-logo2.png'
import MenuOpenIcon from '@mui/icons-material/MenuOpen'
import MenuIcon from '@mui/icons-material/Menu'
import HomeIcon from '@mui/icons-material/Home'
import { useNavigate } from 'react-router-dom'
import { DrawerCloseIcon, DrawerOpenIcon, DrawerOpenDarkIcon, DrawerCloseDarkIcon } from 'assets/images/icons/index'

const DrawerHeader = ({ open, handleDrawerToggle }) => {
  const navigate = useNavigate()
  const theme = useTheme()
  const isDark = theme.palette.mode === 'dark'

  return (
    <DrawerHeaderStyled open={open}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          width: '100%',
          justifyContent: open ? 'space-between' : 'center',
          flexDirection: open ? 'row' : 'column',
          gap: open ? 0 : 1.5,
        }}
      >
        {/* LEFT LOGO + TEXT */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <IconButton
            onClick={() => navigate('/dashboard')}
            size='small'
            sx={{
              // color: '#6a7b92',
              '&:hover': { color: '#6a7b92ff' },
            }}
          >
            <HomeIcon sx={{ width: 28, height: 28, color: '#bfa161ff' }} />
          </IconButton>
          {/* <Box
            component='img'
            src={logo}
            alt='Logo'
            sx={{ width: 28, height: 28, cursor: 'pointer' }}
            onClick={() => navigate('/dashboard')}
          /> */}

          {open && (
            <Box>
              <Typography
                sx={{
                  fontWeight: 800,
                  fontSize: 20,
                  color: isDark ? '#fff' : '#303030',
                  fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
                }}
              >
                Reliance
              </Typography>
              <Typography
                sx={{
                  fontSize: 13,
                  color: isDark ? 'rgba(255,255,255,0.7)' : '#606060',
                  fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
                }}
              >
                AOP Dashboard
              </Typography>
            </Box>
          )}
        </Box>

        {/* RIGHT COLLAPSE BUTTON */}
        {/* {open && ( */}
        <IconButton
          onClick={handleDrawerToggle}
          size='small'
          sx={{
            // color: '#6a7b92',
            ml: open ? 0 : 0,
            '&:hover': { color: '#6a7b92' },
          }}
        >
          <Box
            component='img'
            src={open ? (isDark ? DrawerCloseDarkIcon : DrawerCloseIcon) : (isDark ? DrawerOpenDarkIcon : DrawerOpenIcon)}
            alt='Drawer Toggle'
            sx={{
              width: 20,
              height: 20,
              cursor: 'pointer',
              filter: 'none',
            }}
          />
        </IconButton>
        {/* )} */}
      </Box>
    </DrawerHeaderStyled>
  )
}

DrawerHeader.propTypes = {
  open: PropTypes.bool,
  handleDrawerToggle: PropTypes.func,
}

export default DrawerHeader
