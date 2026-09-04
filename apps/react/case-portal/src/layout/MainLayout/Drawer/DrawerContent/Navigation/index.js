// Navigation.jsx
import Box from '@mui/material/Box'
import Chip from '@mui/material/Chip'
import Divider from '@mui/material/Divider'
import LinearProgress from '@mui/material/LinearProgress'
import Skeleton from '@mui/material/Skeleton'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import NavGroup from './NavGroup'
import useFilteredMenu from 'hooks/useFilteredMenu'

const MenuLoadingSkeleton = () => (
  <Box sx={{ px: 2, pt: 2, pb: 4, width: '100%' }}>
    <Stack spacing={2.5}>
      {/* Group 1 Skeleton */}
      <Box>
        <Skeleton
          variant='text'
          width={110}
          height={18}
          sx={{ mb: 1.5, bgcolor: 'rgba(0, 0, 0, 0.08)', borderRadius: '3px' }}
        />
        <Stack spacing={1}>
          {[80, 68, 92, 60, 76].map((w, idx) => (
            <Box
              key={`g1-${idx}`}
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1.5,
                px: 1,
                py: 0.75,
                borderRadius: '6px',
                bgcolor: 'rgba(0, 0, 0, 0.02)',
              }}
            >
              <Skeleton
                variant='rounded'
                width={22}
                height={22}
                sx={{ borderRadius: 1, bgcolor: 'rgba(0, 0, 0, 0.08)' }}
              />
              <Skeleton
                variant='text'
                width={`${w}%`}
                height={20}
                sx={{ bgcolor: 'rgba(0, 0, 0, 0.08)', flex: 1 }}
              />
              <Skeleton
                variant='circular'
                width={12}
                height={12}
                sx={{ bgcolor: 'rgba(0, 0, 0, 0.05)' }}
              />
            </Box>
          ))}
        </Stack>
      </Box>

      {/* Divider */}
      <Skeleton
        variant='rectangular'
        height={1}
        sx={{ bgcolor: 'rgba(0, 0, 0, 0.06)', my: 0.5 }}
      />

      {/* Group 2 Skeleton */}
      <Box>
        <Skeleton
          variant='text'
          width={130}
          height={18}
          sx={{ mb: 1.5, bgcolor: 'rgba(0, 0, 0, 0.08)', borderRadius: '3px' }}
        />
        <Stack spacing={1}>
          {[85, 62, 90, 72, 80, 65].map((w, idx) => (
            <Box
              key={`g2-${idx}`}
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1.5,
                px: 1,
                py: 0.75,
                borderRadius: '6px',
                bgcolor: 'rgba(0, 0, 0, 0.02)',
              }}
            >
              <Skeleton
                variant='rounded'
                width={22}
                height={22}
                sx={{ borderRadius: 1, bgcolor: 'rgba(0, 0, 0, 0.08)' }}
              />
              <Skeleton
                variant='text'
                width={`${w}%`}
                height={20}
                sx={{ bgcolor: 'rgba(0, 0, 0, 0.08)', flex: 1 }}
              />
              <Skeleton
                variant='circular'
                width={12}
                height={12}
                sx={{ bgcolor: 'rgba(0, 0, 0, 0.05)' }}
              />
            </Box>
          ))}
        </Stack>
      </Box>
    </Stack>
  </Box>
)

const Navigation = () => {
  const filteredMenu = useFilteredMenu()
  const isMenuLoading = filteredMenu?.isMenuLoading

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
        background: '#ffffff',
        fontFamily: "'Honeywell Sans Web', 'Inter', Arial, sans-serif",
        color: '#cbd5e1',
        fontSize: '0.75rem',
      }}
    >
      {/* Thin divider */}
      <Divider sx={{ borderColor: '#DDDEE1', my: 0.25, borderWidth: '1px' }} />

      {/* Loading progress bar */}
      {isMenuLoading && (
        <LinearProgress
          sx={{
            height: 2,
            bgcolor: '#f1f5f9',
            '& .MuiLinearProgress-bar': {
              bgcolor: '#0284c7',
            },
          }}
        />
      )}

      {/* Scrollable menu */}
      <Box
        sx={{
          flex: 1,
          px: 0,
          pb: '140px',
          pt: 1,
          overflowY: 'auto',
          overflowX: 'hidden',
          '&::-webkit-scrollbar': {
            width: '4px',
          },
          '&::-webkit-scrollbar-thumb': {
            background: '#334155',
            borderRadius: '999px',
          },
        }}
      >
        {isMenuLoading ? (
          <MenuLoadingSkeleton />
        ) : (
          <Box
            sx={{
              display: 'flex',
              flexDirection: 'column',
              gap: 0,
            }}
          >
            {navGroups}
          </Box>
        )}
      </Box>
    </Box>
  )
}

export default Navigation
