import Box from '@mui/material/Box'
import Chip from '@mui/material/Chip'
import Divider from '@mui/material/Divider'
import { useTheme } from '@mui/material/styles'
import NavGroup from './NavGroup'
import useFilteredMenu from 'hooks/useFilteredMenu'

const Navigation = () => {
  const theme = useTheme()
  const isDark = theme.palette.mode === 'dark'
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
            my: 0.25,
            fontSize: '0.6rem',
            bgcolor: '#7f1d1d',
            color: '#ffffff',
          }}
        />
      )
    }

    return (
      <Box
        key={`${item.id}-${index}`}
        sx={{
          position: 'relative',
          backgroundColor: 'transparent',
          border: 'none',
          overflow: 'hidden',
          px: 0,
          py: 0,
        }}
      >
        {item.badge && (
          <Chip
            label={item.badge}
            size='small'
            sx={{
              position: 'absolute',
              top: 4,
              right: 6,
              height: 16,
              fontSize: '0.55rem',
              fontWeight: 700,
              bgcolor: '#16a34a',
              color: '#ffffff',
              borderRadius: '4px',
              zIndex: 2,
              px: 0.5,
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

        // ? Dark navy gradient background (like your screenshot)
        background: isDark ? 'transparent' : '#ffffff',

        // borderRight: '1px solid #ffffff',
        fontFamily: "'Honeywell Sans Web', 'Inter', Arial, sans-serif",
        color: '#cbd5e1',
        fontSize: '0.75rem',
      }}
    >
      {/* Thin divider */}
      <Divider
        sx={{
          borderColor: isDark ? 'rgba(255,255,255,0.1)' : '#DDDEE1',
          my: 0.25,
          borderWidth: '1px',
        }}
      />

      {/* Scrollable menu */}
      <Box
        sx={{
          flex: 1,
          px: 0,
          pb: '200px',
          pt: 1,
          overflowY: 'auto',
          overflowX: 'hidden',
          '&::-webkit-scrollbar': {
            width: '4px',
          },
          '&::-webkit-scrollbar-thumb': {
            background: isDark ? 'rgba(255,255,255,0.2)' : '#334155',
            borderRadius: '999px',
          },
        }}
      >
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            gap: 0,
          }}
        >
          {navGroups}
        </Box>
      </Box>
    </Box>
  )
}

export default Navigation
