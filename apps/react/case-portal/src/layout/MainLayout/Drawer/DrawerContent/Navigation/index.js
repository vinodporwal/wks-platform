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
          borderRadius: '8px',
          backgroundColor: 'transparent',
          border: 'none',
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
              fontSize: '0.6rem',
              fontWeight: 700,
              bgcolor: '#16a34a', // green ACTIVE badge
              color: '#ffffff',
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
        background: '#f9fafb', // light sidebar bg
        borderRight: '1px solid #e5e7eb',
        fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
        color: '#111827', // dark text
      }}
    >
      {/* Thin top divider */}
      <Divider sx={{ borderColor: '#e5e7eb', my: 0.5 }} />

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
            width: '6px',
          },
          '&::-webkit-scrollbar-track': {
            background: 'transparent',
          },
          '&::-webkit-scrollbar-thumb': {
            background: '#c7d2fe',
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
