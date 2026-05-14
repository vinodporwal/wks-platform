import { styled } from '@mui/material/styles'
import Box from '@mui/material/Box'

const DrawerHeaderStyled = styled(Box)(({ theme, open }) => ({
  display: 'flex',
  alignItems: 'center',
  padding: open ? '12px 14px' : '9px 0px',
  background: theme.palette.mode === 'dark' ? 'transparent' : '#ffffff',
  flexShrink: 0,
}))

export default DrawerHeaderStyled
