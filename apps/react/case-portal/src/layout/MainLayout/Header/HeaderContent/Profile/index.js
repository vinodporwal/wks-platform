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
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import LogoutOutlined from '@ant-design/icons/LogoutOutlined'
import avatar2 from 'assets/images/users/new-avatar.jpg'
import logo from 'assets/images/ril-logo2.png'

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
        sx={{
          whiteSpace: 'nowrap',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          maxWidth: 160,
        }}
        title={keycloak?.idTokenParsed?.name}
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

      {/* Quick Logout Button */}
      <Tooltip title='Logout'>
        <IconButton
          onClick={handleLogout}
          size='small'
          sx={{
            p: 0.8,
            color: '#dc2626',
            borderRadius: '6px',
            '&:hover': {
              backgroundColor: '#fee2e2',
            },
          }}
        >
          <LogoutOutlined style={{ fontSize: 16 }} />
        </IconButton>
      </Tooltip>

      {/* Profile Dropdown Menu */}
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

              <Divider sx={{ my: 0.5 }} />

              <MenuItem
                onClick={handleLogout}
                sx={{
                  color: '#dc2626',
                  fontWeight: 600,
                  fontSize: '0.85rem',
                  borderRadius: '4px',
                  '&:hover': { backgroundColor: '#fee2e2' },
                }}
              >
                <LogoutOutlined style={{ marginRight: 8, fontSize: 15 }} />
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
