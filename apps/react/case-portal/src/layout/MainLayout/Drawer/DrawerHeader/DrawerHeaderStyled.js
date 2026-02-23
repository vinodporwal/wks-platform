import { styled } from '@mui/material/styles'
import Box from '@mui/material/Box'

const DrawerHeaderStyled = styled(Box)(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  padding: '12px 14px',
  background: '#0f172a', // ?? DARK LIKE MENU
  borderBottom: '1px solid rgba(255,255,255,0.08)',
  flexShrink: 0,
}))

export default DrawerHeaderStyled
