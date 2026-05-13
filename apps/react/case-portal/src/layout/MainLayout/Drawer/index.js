import PropTypes from 'prop-types'
import { useMemo } from 'react'
import Box from '@mui/material/Box'
import Avatar from '@mui/material/Avatar'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import Divider from '@mui/material/Divider'
import { useDispatch, useSelector } from 'react-redux'
import Switch from '@mui/material/Switch'
import IconButton from '@mui/material/IconButton'
import Tooltip from '@mui/material/Tooltip'
import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined'
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined'
import DrawerHeader from './DrawerHeader'
import DrawerContent from './DrawerContent'
import MiniDrawerStyled from './MiniDrawerStyled'
import { setMode } from 'store/reducers/theme'

const MainDrawer = ({ open, handleDrawerToggle, isDashboard, keycloak }) => {
  const dispatch = useDispatch()
  const { mode } = useSelector((state) => state.theme)

  const handleModeToggle = () => {
    dispatch(setMode({ mode: mode === 'light' ? 'dark' : 'light' }))
  }

  const drawerContent = useMemo(() => <DrawerContent />, [])
  const drawerHeader = useMemo(
    () => <DrawerHeader open={open} handleDrawerToggle={handleDrawerToggle} />,
    [open, handleDrawerToggle],
  )

  function stringToColor(str = 'User') {
    let hash = 0

    /* eslint-disable no-bitwise */
    for (let i = 0; i < str.length; i += 1) {
      hash = str.charCodeAt(i) + ((hash << 5) - hash)
    }

    let color = '#'

    for (let i = 0; i < 3; i += 1) {
      const value = (hash >> (i * 8)) & 0xff
      color += `00${value.toString(16)}`.slice(-2)
    }
    /* eslint-enable no-bitwise */

    return color
  }

  function stringAvatar(name = 'User') {
    const safeName = name?.trim() || 'User'
    const nameParts = safeName.split(' ').filter(Boolean)

    const firstLetter = nameParts?.[0]?.[0] || 'U'
    const secondLetter = nameParts?.[1]?.[0] || ''

    return {
      sx: {
        bgcolor: stringToColor(safeName),
      },
      children: `${firstLetter}${secondLetter}`.toUpperCase(),
    }
  }

  return (
    <MiniDrawerStyled
      variant='permanent'
      open={open}
      hide={isDashboard && !open}
    >
      <Box
        role='presentation'
        sx={{
          height: '100vh',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        {/* HEADER FIXED */}
        {drawerHeader}

        {/* MENU SCROLL AREA */}
        <Box
          sx={{
            flex: 1, // TAKE REMAINING HEIGHT
            overflowY: 'auto',
            overflowX: 'hidden',

            '&::-webkit-scrollbar': {
              width: '6px',
            },
            '&::-webkit-scrollbar-track': {
              background: 'transparent',
            },
            '&::-webkit-scrollbar-thumb': {
              background: 'rgba(100,116,139,0.35)',
              borderRadius: '10px',
            },
            '&::-webkit-scrollbar-thumb:hover': {
              background: 'rgba(100,116,139,0.55)',
            },
          }}
        >
          {drawerContent}
        </Box>

        {/* DARK MODE TOGGLE */}
        <Box sx={{ p: open ? 2 : 0, py: open ? 1 : 1 }}>
          {open ? (
            <Stack
              direction='row'
              alignItems='center'
              justifyContent='space-between'
              sx={{
                bgcolor:
                  mode === 'dark'
                    ? 'rgba(255,255,255,0.05)'
                    : 'rgba(0,0,0,0.02)',
                borderRadius: '8px',
                p: 1,
                mx: 0.5,
                border: '1px solid',
                borderColor:
                  mode === 'dark'
                    ? 'rgba(255,255,255,0.1)'
                    : 'rgba(0,0,0,0.05)',
              }}
            >
              <Stack direction='row' alignItems='center' spacing={1}>
                {mode === 'dark' ? (
                  <DarkModeOutlinedIcon
                    sx={{ fontSize: '1.2rem', color: '#6366f1' }}
                  />
                ) : (
                  <LightModeOutlinedIcon
                    sx={{ fontSize: '1.2rem', color: '#f59e0b' }}
                  />
                )}
                <Typography
                  variant='body2'
                  sx={{
                    fontWeight: 500,
                    color: mode === 'dark' ? '#fff' : '#303030',
                  }}
                >
                  {mode === 'dark' ? 'Light' : 'Dark'} Mode
                </Typography>
              </Stack>
              <Switch
                size='small'
                checked={mode === 'dark'}
                onChange={handleModeToggle}
              />
            </Stack>
          ) : (
            <Tooltip
              title={
                mode === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'
              }
              placement='right'
            >
              <IconButton
                onClick={handleModeToggle}
                sx={{
                  mx: 'auto',
                  display: 'flex',
                  color: mode === 'dark' ? '#6366f1' : '#64748b',
                }}
              >
                {mode === 'dark' ? (
                  <DarkModeOutlinedIcon />
                ) : (
                  <LightModeOutlinedIcon />
                )}
              </IconButton>
            </Tooltip>
          )}
        </Box>

        {/* USER DETAILS AT BOTTOM */}
        <Box
          sx={{
            p: open ? 2 : 1.25,
            borderTop: '1px solid',
            borderColor: mode === 'dark' ? 'rgba(255,255,255,0.1)' : '#F0F1F2',
            background: mode === 'dark' ? 'rgba(255,255,255,0.02)' : '#F0F1F2',
            transition: 'all 0.3s ease',
          }}
        >
          <Stack
            direction='row'
            alignItems='center'
            spacing={open ? 1.5 : 0}
            justifyContent={open ? 'flex-start' : 'center'}
          >
            <Avatar
              alt={keycloak?.idTokenParsed?.name}
              // src={keycloak?.idTokenParsed?.picture}
              {...stringAvatar(keycloak?.idTokenParsed?.name)}
              variant='square'
              sx={{
                width: 28,
                height: 28,
                bgcolor: '#AE4787',
                fontSize: '11px',
                fontWeight: 800,
                color: '#F0F0F0',
                boxShadow: '0 2px 8px rgba(174, 71, 135, 0.25)',
                fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
                borderRadius: '4px',
                textTransform: 'uppercase',
              }}
            >
              {/* {keycloak?.idTokenParsed?.name?.charAt(0) || 'U'} */}
            </Avatar>
            {open && (
              <Box sx={{ overflow: 'hidden', flex: 1 }}>
                <Typography
                  variant='subtitle2'
                  noWrap
                  sx={{
                    fontWeight: 700,
                    color: mode === 'dark' ? '#fff' : '#303030',
                    fontSize: '14px',
                    fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
                    lineHeight: 1.2,
                  }}
                >
                  {keycloak?.idTokenParsed?.name || 'User'}
                </Typography>
                <Typography
                  variant='caption'
                  noWrap
                  display='block'
                  sx={{
                    color:
                      mode === 'dark' ? 'rgba(255,255,255,0.7)' : '#303030',
                    fontSize: '12px',
                    fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
                    mt: 0.2,
                    fontWeight: 500,
                  }}
                >
                  {keycloak?.idTokenParsed?.email || 'user@reliance.com'}
                </Typography>
              </Box>
            )}
          </Stack>
        </Box>
      </Box>
    </MiniDrawerStyled>
  )
}

MainDrawer.propTypes = {
  open: PropTypes.bool.isRequired,
  handleDrawerToggle: PropTypes.func,
  isDashboard: PropTypes.bool,
  keycloak: PropTypes.object,
}

export default MainDrawer
