import React from 'react'
import { Box, Skeleton, Stack, LinearProgress } from '@mui/material'

export const PageSkeleton = () => (
  <Box
    sx={{
      flex: 1,
      width: '100%',
      p: { xs: 1.5, sm: 2.5, md: 3 },
      bgcolor: '#ffffff',
      display: 'flex',
      flexDirection: 'column',
      gap: 2.5,
      height: '100%',
      minHeight: '400px',
      overflow: 'hidden',
    }}
  >
    {/* Top Subtle Progress Bar */}
    <LinearProgress
      sx={{
        height: 2,
        borderRadius: 1,
        bgcolor: '#f1f5f9',
        '& .MuiLinearProgress-bar': {
          bgcolor: '#0284c7',
        },
      }}
    />

    {/* Header & Action Buttons Row */}
    <Box
      sx={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        pt: 0.5,
      }}
    >
      <Skeleton
        variant='text'
        width={220}
        height={32}
        sx={{ bgcolor: 'rgba(0, 0, 0, 0.07)', borderRadius: '4px' }}
      />
      <Box sx={{ display: 'flex', gap: 1 }}>
        <Skeleton
          variant='rounded'
          width={85}
          height={32}
          sx={{ borderRadius: 1.5, bgcolor: 'rgba(0, 0, 0, 0.05)' }}
        />
        <Skeleton
          variant='rounded'
          width={105}
          height={32}
          sx={{ borderRadius: 1.5, bgcolor: 'rgba(0, 0, 0, 0.05)' }}
        />
      </Box>
    </Box>

    {/* Table / Grid Container Skeleton */}
    <Box
      sx={{
        flex: 1,
        border: '1px solid #e2e8f0',
        borderRadius: '8px',
        p: 2,
        display: 'flex',
        flexDirection: 'column',
        gap: 1.5,
        bgcolor: '#ffffff',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.02)',
      }}
    >
      {/* Table Header Row Skeleton */}
      <Box
        sx={{
          display: 'flex',
          gap: 2,
          pb: 1.5,
          borderBottom: '1px solid #e2e8f0',
        }}
      >
        {[20, 25, 18, 15, 22].map((flex, idx) => (
          <Skeleton
            key={idx}
            variant='rectangular'
            height={26}
            sx={{
              flex: flex,
              borderRadius: '4px',
              bgcolor: 'rgba(0, 0, 0, 0.07)',
            }}
          />
        ))}
      </Box>

      {/* Table Data Rows Skeleton */}
      {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((row) => (
        <Box key={row} sx={{ display: 'flex', gap: 2, py: 0.75 }}>
          {[20, 25, 18, 15, 22].map((flex, idx) => (
            <Skeleton
              key={idx}
              variant='text'
              height={20}
              sx={{ flex: flex, bgcolor: 'rgba(0, 0, 0, 0.035)' }}
            />
          ))}
        </Box>
      ))}
    </Box>
  </Box>
)

export default PageSkeleton
