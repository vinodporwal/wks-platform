import PropTypes from 'prop-types'
import { useTheme } from '@mui/material/styles'
import Stack from '@mui/material/Stack'
import { Box, Typography, IconButton } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import HomeIcon from '@mui/icons-material/Home'
import DrawerHeaderStyled from './DrawerHeaderStyled'

const DrawerHeader = ({ open = false }) => {
  //  default here
  const theme = useTheme()
  const navigate = useNavigate()

  return (
    <DrawerHeaderStyled theme={theme} open={open}>
      <Stack direction='row' spacing={1} alignItems='center'>
        <IconButton
          onClick={() => navigate('/dashboard')}
          size='small'
          sx={{
            '&:hover': { color: '#6a7b92ff' },
          }}
        >
          <HomeIcon sx={{ width: 28, height: 28, color: '#bfa161ff' }} />
        </IconButton>

        {open && (
          <Box>
            <Typography
              sx={{
                fontWeight: 800,
                fontSize: 20,
                color: '#303030',
                fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
              }}
            >
              Reliance
            </Typography>
          </Box>
        )}
      </Stack>
    </DrawerHeaderStyled>
  )
}

DrawerHeader.propTypes = {
  open: PropTypes.bool,
}

// Removed DrawerHeader.defaultProps block

export default DrawerHeader

// import PropTypes from 'prop-types'
// import { useTheme } from '@mui/material/styles'
// import Stack from '@mui/material/Stack'
// import DrawerHeaderStyled from './DrawerHeaderStyled'
// // import Logo from 'components/Logo'

// const DrawerHeader = ({ open }) => {
//   const theme = useTheme()

//   return (
//     <DrawerHeaderStyled theme={theme} open={open}>
//       <Stack direction='row' spacing={1} alignItems='center'>
//         {/* <Logo /> */}
//       </Stack>
//     </DrawerHeaderStyled>
//   )
// }

// DrawerHeader.propTypes = {
//   open: PropTypes.bool,
// }

// DrawerHeader.defaultProps = {
//   open: false,
// }
// export default DrawerHeader
