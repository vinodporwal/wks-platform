// Navigation.jsx
import Box from '@mui/material/Box'
import Chip from '@mui/material/Chip'
import Typography from '@mui/material/Typography'
import IconButton from '@mui/material/IconButton'
import Divider from '@mui/material/Divider'
import NavGroup from './NavGroup'
import useFilteredMenu from 'hooks/useFilteredMenu'
import MenuRoundedIcon from '@mui/icons-material/MenuRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import InputBase from '@mui/material/InputBase'

const NAV_BG = '#041424' // container darkest
const NAV_GROUP_BG = 'transparent'
const NAV_BORDER = 'rgba(255,255,255,0.04)'
const ACCENT = '#39a6ff' // accent for scrollbar / thin strip
const BADGE_BG = '#0f3a5b'
const BADGE_COLOR = '#e9f6ff'

const Navigation = () => {
  const filteredMenu = useFilteredMenu()

  const navGroups = filteredMenu?.items?.map((item, index) => {
    if (item.type !== 'group') {
      return (
        <Chip
          key={`${item.id}-${index}`}
          label='Fix - Navigation Group'
          size='small'
          sx={{
            mx: 'auto',
            my: 0.5,
            fontSize: '0.65rem',
            bgcolor: '#7f1d1d',
            color: '#fff',
          }}
        />
      )
    }

    return (
      <Box
        key={`${item.id}-${index}`}
        sx={{
          position: 'relative',
          borderRadius: '8px',
          backgroundColor: NAV_GROUP_BG,
          border: `1px solid ${NAV_BORDER}`,
          overflow: 'hidden',
          px: 0.5,
          py: 0.25,
        }}
      >
        {item.badge && (
          <Chip
            label={item.badge}
            size='small'
            sx={{
              position: 'absolute',
              top: 6,
              right: 8,
              height: 18,
              fontSize: '0.62rem',
              fontWeight: 700,
              bgcolor: BADGE_BG,
              color: BADGE_COLOR,
              borderRadius: '6px',
              zIndex: 2,
              px: 0.6,
            }}
          />
        )}
        <NavGroup item={item} />
      </Box>
    )
  })

  return (
    <Box
      sx={{
        height: '100vh',
        display: 'flex',
        flexDirection: 'column',
        background: `linear-gradient(180deg, ${NAV_BG} 0%, #170a3bff 100%)`,
        borderRight: `1px solid ${NAV_BORDER}`,
        fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
        color: '#ffffff',
      }}
    >
      {/* Thin top accent for subtle color */}
      <Box sx={{ height: 2, background: ACCENT }} />

      <Divider sx={{ borderColor: 'rgba(255,255,255,0.03)', my: 0.5 }} />

      {/* Scrollable area */}
      <Box
        sx={{
          flex: 1,
          px: 0.5,
          pb: 1,
          overflowY: 'auto',
          overflowX: 'hidden',
          position: 'relative',
          '&::-webkit-scrollbar': {
            width: '8px',
          },
          '&::-webkit-scrollbar-track': {
            background: 'transparent',
          },
          '&::-webkit-scrollbar-thumb': {
            background: `linear-gradient(180deg, ${ACCENT}, #6fb9ff)`,
            borderRadius: '999px',
          },
        }}
      >
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
          {navGroups}
        </Box>
      </Box>
    </Box>
  )
}

export default Navigation
