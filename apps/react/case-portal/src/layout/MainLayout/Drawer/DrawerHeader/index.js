import PropTypes from 'prop-types'
import { useTheme } from '@mui/material/styles'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import DrawerHeaderStyled from './DrawerHeaderStyled'

const DrawerHeader = ({ open = false }) => {
  const theme = useTheme()

  return (
    <DrawerHeaderStyled theme={theme} open={open}>
      <Stack
        direction='row'
        spacing={1}
        alignItems='center'
        sx={{
          px: 2,
          py: 1.5,
          overflow: 'hidden',
          // pt: '60px', // ?? THIS is the fix
        }}
      >
        {/* Brand Text */}
        <Typography
          variant='h6'
          noWrap
          sx={{
            fontWeight: 800,
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
            fontSize: '0.95rem',

            /* ?? Gradient text */
            background:
              'linear-gradient(135deg, #2563eb 0%, #7c3aed 50%, #db2777 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',

            /* ? Soft glow */
            textShadow: '0 1px 8px rgba(99,102,241,0.35)',

            /* Smooth collapse */
            opacity: open ? 1 : 0,
            transform: open ? 'translateX(0)' : 'translateX(-8px)',
            transition: 'all 260ms ease',

            whiteSpace: 'nowrap',
          }}
        >
          Digital AOP
        </Typography>
      </Stack>
    </DrawerHeaderStyled>
  )
}

DrawerHeader.propTypes = {
  open: PropTypes.bool,
}

export default DrawerHeader
