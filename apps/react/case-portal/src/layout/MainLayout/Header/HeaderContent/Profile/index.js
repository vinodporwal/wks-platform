import { useRef, useState } from 'react'
import Avatar from '@mui/material/Avatar'
import Box from '@mui/material/Box'
import IconButton from '@mui/material/IconButton'
import ClickAwayListener from '@mui/material/ClickAwayListener'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Popper from '@mui/material/Popper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import LogoutOutlined from '@ant-design/icons/LogoutOutlined'
import avatar2 from 'assets/images/users/new-avatar.jpg'

const Profile = ({ keycloak }) => {
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
    <Box sx={{ flexShrink: 0, ml: 1 }}>
      {/* ?? ICON ONLY */}
      <IconButton
        ref={anchorRef}
        onClick={handleToggle}
        size='small'
        sx={{
          p: 0.5,
        }}
      >
        <Avatar
          src={avatar2}
          sx={{
            width: 32,
            height: 32,
          }}
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
                boxShadow: '0 8px 20px rgba(15,23,42,0.08)',
                border: '1px solid rgba(15,23,42,0.06)',
                p: 0.5,
              }}
            >
              <MenuItem disabled>
                <Stack spacing={0.3}>
                  <Typography fontSize='0.85rem' fontWeight={600}>
                    {keycloak?.idTokenParsed?.name}
                  </Typography>
                  <Typography fontSize='0.75rem' color='#6b7786'>
                    {keycloak?.idTokenParsed?.email}
                  </Typography>
                </Stack>
              </MenuItem>

              <MenuItem onClick={handleLogout}>
                <LogoutOutlined style={{ marginRight: 8 }} />
                Logout
              </MenuItem>
            </Paper>
          </ClickAwayListener>
        )}
      </Popper>
    </Box>
  )
}

export default Profile
