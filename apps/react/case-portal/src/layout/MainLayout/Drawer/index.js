import PropTypes from 'prop-types'
import { useMemo } from 'react'
import Box from '@mui/material/Box'
import Avatar from '@mui/material/Avatar'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import Divider from '@mui/material/Divider'
import DrawerHeader from './DrawerHeader'
import DrawerContent from './DrawerContent'
import MiniDrawerStyled from './MiniDrawerStyled'

const MainDrawer = ({ open, handleDrawerToggle, isDashboard, keycloak }) => {
  const drawerContent = useMemo(() => <DrawerContent />, [])
  const drawerHeader = useMemo(
    () => <DrawerHeader open={open} handleDrawerToggle={handleDrawerToggle} />,
    [open, handleDrawerToggle],
  )

  function stringToColor(string) {
    let hash = 0;
    let i;

    /* eslint-disable no-bitwise */
    for (i = 0; i < string.length; i += 1) {
      hash = string.charCodeAt(i) + ((hash << 5) - hash);
    }

    let color = '#';

    for (i = 0; i < 3; i += 1) {
      const value = (hash >> (i * 8)) & 0xff;
      color += `00${value.toString(16)}`.slice(-2);
    }
    /* eslint-enable no-bitwise */

    return color;
  }

  function stringAvatar(name) {
    return {
      sx: {
        bgcolor: stringToColor(name),
      },
      children: `${name.split(' ')[0][0]}${name.split(' ')[1][0]}`,
    };
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
        {/* USER DETAILS AT BOTTOM */}
        <Box
          sx={{
            p: open ? 2 : 1.25,
            borderTop: '1px solid #F0F1F2',
            background: open ? '#F0F1F2' : '#F0F1F2',
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
              variant="square"
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
                textTransform: 'uppercase'
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
                    color: '#303030',
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
                    color: '#303030',
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
