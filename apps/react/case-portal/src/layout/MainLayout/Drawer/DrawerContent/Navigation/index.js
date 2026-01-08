// Navigation.jsx
import Box from '@mui/material/Box'
import Chip from '@mui/material/Chip'
import Divider from '@mui/material/Divider'
import NavGroup from './NavGroup'
import useFilteredMenu from 'hooks/useFilteredMenu'

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
        backgroundColor: '#ffffffff',
        borderRight: '1px solid #e5e7eb',
        fontFamily: '"Inter", "Open Sans", "Helvetica", "Arial", sans-serif',
        color: '#374151',
        fontSize: '0.75rem',
      }}
    >
      {/* Thin divider */}
      <Divider sx={{ borderColor: '#e5e7eb', my: 0.25 }} />

      {/* Scrollable menu */}
      <Box
        sx={{
          flex: 1,
          px: 0, // ?? zero left padding
          pb: 0.5,
          overflowY: 'auto',
          overflowX: 'hidden',
          '&::-webkit-scrollbar': {
            width: '4px',
          },
          '&::-webkit-scrollbar-thumb': {
            background: '#d1d5db',
            borderRadius: '999px',
          },
        }}
      >
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            gap: 0, // ?? no gap between items
          }}
        >
          {navGroups}
        </Box>
      </Box>
    </Box>
  )
}

export default Navigation
