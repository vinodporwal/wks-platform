import PropTypes from 'prop-types'
import { Box, Typography, IconButton } from '@mui/material'
import DrawerHeaderStyled from './DrawerHeaderStyled'
import logo from 'assets/images/ril-logo2.png'
import MenuOpenIcon from '@mui/icons-material/MenuOpen'
import MenuIcon from '@mui/icons-material/Menu'

const DrawerHeader = ({ open, handleDrawerToggle }) => {
  return (
    <DrawerHeaderStyled open={open}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          width: '100%',
          justifyContent: open ? 'space-between' : 'center',
        }}
      >
        {/* LEFT LOGO + TEXT */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <Box
            component='img'
            src={logo}
            alt='Logo'
            sx={{ width: 28, height: 28 }}
          />

          {open && (
            <Box>
              <Typography sx={{ fontWeight: 700, fontSize: 17, color: '#fff' }}>
                Reliance
              </Typography>
              <Typography sx={{ fontSize: 12, color: '#94a3b8' }}>
                Monitoring Dashboard
              </Typography>
            </Box>
          )}
        </Box>

        {/* RIGHT COLLAPSE BUTTON */}
        <IconButton
          onClick={handleDrawerToggle}
          size='small'
          sx={{
            color: '#cbd5f5',
            ml: open ? 0 : 0,
            '&:hover': { color: '#fff' },
          }}
        >
          {open ? <MenuOpenIcon /> : <MenuIcon />}
        </IconButton>
      </Box>
    </DrawerHeaderStyled>
  )
}

DrawerHeader.propTypes = {
  open: PropTypes.bool,
  handleDrawerToggle: PropTypes.func,
}

export default DrawerHeader
