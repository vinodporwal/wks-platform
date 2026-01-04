import Box from '@mui/material/Box'
import Chip from '@mui/material/Chip'
import NavGroup from './NavGroup'
import useFilteredMenu from 'hooks/useFilteredMenu'

const Navigation = () => {
  const filteredMenu = useFilteredMenu()

  const navGroups = filteredMenu?.items?.map((item, index) => {
    switch (item.type) {
      case 'group':
        return (
          <Box
            key={`${item.id}-${index}`}
            sx={{
              position: 'relative',
              borderRadius: '12px',
              background: 'linear-gradient(135deg, #fafbfc 0%, #f1f5f9 100%)',
              border: '1px solid rgba(148, 163, 184, 0.15)',
              overflow: 'hidden',
            }}
          >
            {item.badge && (
              <Chip
                label={item.badge}
                size='small'
                sx={{
                  position: 'absolute',
                  top: 0,
                  right: 8,
                  height: '20px',
                  fontSize: '0.7rem',
                  fontFamily:
                    '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
                  fontWeight: 600,
                  background:
                    'linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%)',
                  color: 'white',
                  border: '1px solid rgba(255, 255, 255, 0.3)',
                  boxShadow: '0 4px 12px rgba(59, 130, 246, 0.3)',
                  zIndex: 1,
                }}
              />
            )}
            <NavGroup item={item} />
          </Box>
        )

      default:
        return (
          <Chip
            key={`${item.id}-${index}`}
            label='Fix - Navigation Group'
            color='error'
            size='small'
            icon={
              <Box
                sx={{
                  width: 6,
                  height: 6,
                  borderRadius: '50%',
                  bgcolor: '#ef4444',
                }}
              />
            }
            sx={{
              mx: 'auto',
              fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
              fontWeight: 500,
              boxShadow: '0 2px 8px rgba(239, 68, 68, 0.2)',
            }}
          />
        )
    }
  })

  return (
    <Box
      sx={{
        height: '100vh',
        display: 'flex',
        // pt: '60px', // ?? THIS is the fix
        flexDirection: 'column',
        background: 'linear-gradient(180deg, #f8fafc 0%, #e2e8f0 100%)',
        borderRight: '1px solid rgba(148, 163, 184, 0.2)',
        position: 'relative',
        fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
      }}
    >
      {/* Accent bar */}
      <Box
        sx={{
          height: '3px',
          background:
            'linear-gradient(90deg, #3b82f6, #8b5cf6, #ec4899, #f59e0b)',
        }}
      />

      <Box
        sx={{
          flex: 1,
          p: 0,
          overflowY: 'auto',
          overflowX: 'hidden',
          position: 'relative',
          zIndex: 1,
          '&::-webkit-scrollbar': { width: '6px' },
          '&::-webkit-scrollbar-track': {
            background: 'rgba(148, 163, 184, 0.05)',
            borderRadius: '10px',
          },
          '&::-webkit-scrollbar-thumb': {
            background: 'linear-gradient(180deg, #3b82f6, #8b5cf6)',
            borderRadius: '10px',
          },
        }}
      >
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            gap: 1.5,
            p: 0,
            borderRadius: '16px',
            background: 'rgba(255, 255, 255, 0.6)',
            border: '1px solid rgba(255, 255, 255, 0.8)',
            backdropFilter: 'blur(16px) saturate(180%)',
            boxShadow:
              'inset 0 1px 0 0 rgba(255, 255, 255, 0.8), 0 4px 24px rgba(148, 163, 184, 0.12)',
          }}
        >
          {navGroups}
        </Box>
      </Box>
    </Box>
  )
}

export default Navigation
