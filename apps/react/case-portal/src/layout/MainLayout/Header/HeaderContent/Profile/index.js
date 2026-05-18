import { useRef, useState } from 'react'
import Avatar from '@mui/material/Avatar'
import Box from '@mui/material/Box'
import Divider from '@mui/material/Divider'
import IconButton from '@mui/material/IconButton'
import ClickAwayListener from '@mui/material/ClickAwayListener'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Popper from '@mui/material/Popper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import LogoutOutlined from '@ant-design/icons/LogoutOutlined'
import avatar2 from 'assets/images/users/new-avatar.jpg'
import logo from 'assets/images/ril-logo2.png'
import { useTheme } from '@mui/material'

const Profile = ({ keycloak }) => {
  const theme = useTheme()
  const isDark = theme.palette.mode === 'dark'
  const anchorRef = useRef(null)
  const [open, setOpen] = useState(false)

  const handleToggle = () => setOpen((prev) => !prev)

  const handleClose = (event) => {
    if (anchorRef.current && anchorRef.current.contains(event.target)) return
    setOpen(false)
  }

  const handleLogout = () => {
    keycloak.logout({ redirectUri: window.location.origin })
  }

  return (
    <Box
      sx={{
        flexShrink: 0,
        ml: 1,
        display: 'flex',
        alignItems: 'center',
        gap: 1,
      }}
    >
      {/* ?? ICON ONLY */}
      <Divider
        orientation='vertical'
        flexItem
        sx={{ border: '2px solid #dfdee3', mr: 1 }}
      />
      <Typography
        fontSize='0.85rem'
        fontWeight={700}
        color='text.primary'
        textTransform='capitalize'
        style={{ color: isDark ? '#F0F0F0' : '#303030' }}
      >
        {keycloak?.idTokenParsed?.name}
      </Typography>
      <IconButton
        ref={anchorRef}
        onClick={handleToggle}
        size='small'
        sx={{
          p: 0.5,
        }}
      >
        <Box
          component='img'
          src={logo}
          alt='Logo'
          sx={{ width: 28, height: 28 }}
        />
      </IconButton>

      {/* Simple Minimal Menu */}
      <Popper
        open={open}
        anchorEl={anchorRef.current}
        placement='bottom-end'
        disablePortal
        modifiers={[{ name: 'offset', options: { offset: [0, 6] } }]}
      >
        {open && (
          <ClickAwayListener onClickAway={handleClose}>
            <Paper
              sx={{
                minWidth: 180,
                borderRadius: 2,
                boxShadow: (theme) => theme.palette.mode === 'dark' ? '0 8px 20px rgba(0,0,0,0.4)' : '0 8px 20px rgba(15,23,42,0.08)',
                border: '1px solid',
                borderColor: (theme) => theme.palette.mode === 'dark' ? 'rgba(255, 255, 255, 0.1)' : 'rgba(15,23,42,0.06)',
                p: 0.5,
                bgcolor: isDark ? '#1C2236' : '#FFFFFF',
              }}
            >
              <MenuItem disabled>
                <Stack spacing={0.3}>
                  <Typography fontSize='0.85rem' fontWeight={600} style={{ color: isDark ? '#F0F0F0' : '#303030' }}>
                    {keycloak?.idTokenParsed?.name}
                  </Typography>
                  <Typography fontSize='0.75rem' style={{ color: isDark ? '#F0F0F0' : '#6b7786' }}>
                    {keycloak?.idTokenParsed?.email}
                  </Typography>
                </Stack>
              </MenuItem>

              {/* <MenuItem onClick={handleLogout}>
                <LogoutOutlined style={{ marginRight: 8 }} />
                Logout
              </MenuItem> */}
            </Paper>
          </ClickAwayListener>
        )}
      </Popper>
    </Box>
  )
}

export default Profile
