import PropTypes from 'prop-types'
import { Box, Typography, IconButton } from '@mui/material'
import DrawerHeaderStyled from './DrawerHeaderStyled'
import logo from 'assets/images/ril-logo2.png'
import MenuOpenIcon from '@mui/icons-material/MenuOpen'
import MenuIcon from '@mui/icons-material/Menu'
import HomeIcon from '@mui/icons-material/Home'
import { useNavigate } from 'react-router-dom'

const DrawerHeader = ({ open, handleDrawerToggle }) => {
  const navigate = useNavigate()
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
              '&:hover': { color: '#6a7b92' },
            }}
          >
            <HomeIcon sx={{ width: 28, height: 28 }} />
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
                sx={{ fontWeight: 700, fontSize: 17, color: '#04140f' }}
              >
                Reliance
              </Typography>
              <Typography sx={{ fontSize: 12, color: '#6a7b92' }}>
                Monitoring Dashboard
              </Typography>
            </Box>
          )}
        </Box>

        {/* RIGHT COLLAPSE BUTTON */}
        {open && (
          <IconButton
            onClick={handleDrawerToggle}
            size='small'
            sx={{
              color: '#6a7b92',
              ml: open ? 0 : 0,
              '&:hover': { color: '#6a7b92' },
            }}
          >
            {open ? <MenuOpenIcon /> : <MenuIcon />}
          </IconButton>
        )}
      </Box>
    </DrawerHeaderStyled>
  )
}

DrawerHeader.propTypes = {
  open: PropTypes.bool,
  handleDrawerToggle: PropTypes.func,
}

export default DrawerHeader
