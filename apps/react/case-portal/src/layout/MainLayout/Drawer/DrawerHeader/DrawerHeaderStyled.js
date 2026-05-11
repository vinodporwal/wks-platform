import { styled } from '@mui/material/styles'
import Box from '@mui/material/Box'

const DrawerHeaderStyled = styled(Box)(({ open }) => ({
  display: 'flex',
  alignItems: 'center',
  padding: open ? '12px 14px' : '9px 0px',
  background: '#ffffff', // ?? DARK LIKE MENU
  // borderBottom: '1px solid #DDDEE1',
  flexShrink: 0,
}))

export default DrawerHeaderStyled
