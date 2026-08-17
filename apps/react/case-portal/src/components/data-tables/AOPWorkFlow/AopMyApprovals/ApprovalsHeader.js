import React from 'react'
import {
  Box,
  IconButton,
  InputAdornment,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import RefreshIcon from '@mui/icons-material/Refresh'
import ClearIcon from '@mui/icons-material/Clear'

/**
 * Top Action Toolbar Header component for Approvals screen
 */
export default function ApprovalsHeader({
  itemCount,
  approvedCount = 0,
  actionCount = 0,
  trackedCount = 0,
  searchTerm,
  setSearchTerm,
  load,
  loading,
}) {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      alignItems={{ xs: 'flex-start', sm: 'center' }}
      justifyContent='space-between'
      spacing={1.5}
      sx={{ mb: 1 }}
    >
      <Stack direction='row' alignItems='center' spacing={1} flexWrap='wrap'>
        <Typography
          className='aop-title-text'
          variant='h6'
          sx={{ fontWeight: 600 }}
        >
          Plant AOP Budget Status
        </Typography>

        <Box
          sx={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 1.5,
            py: 0.5,
            px: 1.25,
            backgroundColor: '#f8fafc',
            border: '1px solid #e2e8f0',
            borderRadius: '6px',
            userSelect: 'none',
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.6 }}>
            <Typography
              sx={{ fontSize: '12.5px', color: '#64748b', fontWeight: 500 }}
            >
              Total:
            </Typography>
            <Typography
              sx={{ fontSize: '12.5px', color: '#0f172a', fontWeight: 700 }}
            >
              {itemCount}
            </Typography>
          </Box>

          {approvedCount > 0 && (
            <>
              <Box
                sx={{
                  width: '1px',
                  height: '12px',
                  backgroundColor: '#cbd5e1',
                }}
              />
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.6 }}>
                <Box
                  sx={{
                    width: 7,
                    height: 7,
                    borderRadius: '50%',
                    backgroundColor: '#16a34a',
                  }}
                />
                <Typography
                  sx={{ fontSize: '12.5px', color: '#15803d', fontWeight: 600 }}
                >
                  {approvedCount} Approved
                </Typography>
              </Box>
            </>
          )}

          {actionCount > 0 && (
            <>
              <Box
                sx={{
                  width: '1px',
                  height: '12px',
                  backgroundColor: '#cbd5e1',
                }}
              />
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.6 }}>
                <Box
                  sx={{
                    width: 7,
                    height: 7,
                    borderRadius: '50%',
                    backgroundColor: '#2563eb',
                  }}
                />
                <Typography
                  sx={{ fontSize: '12.5px', color: '#1d4ed8', fontWeight: 600 }}
                >
                  {actionCount} Approval Pending
                </Typography>
              </Box>
            </>
          )}

          {trackedCount > 0 && (
            <>
              <Box
                sx={{
                  width: '1px',
                  height: '12px',
                  backgroundColor: '#cbd5e1',
                }}
              />
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.6 }}>
                <Box
                  sx={{
                    width: 7,
                    height: 7,
                    borderRadius: '50%',
                    backgroundColor: '#d97706',
                  }}
                />
                <Typography
                  sx={{ fontSize: '12.5px', color: '#b45309', fontWeight: 500 }}
                >
                  {trackedCount} In Progress
                </Typography>
              </Box>
            </>
          )}
        </Box>
      </Stack>

      <Stack direction='row' alignItems='center' spacing={1.5}>
        {/* Quick Search Input Bar */}
        <TextField
          size='small'
          placeholder='Search Plant, Site, Role...'
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className='aop-search-field'
          InputProps={{
            startAdornment: (
              <InputAdornment position='start'>
                <SearchIcon sx={{ fontSize: 18, color: '#64748b' }} />
              </InputAdornment>
            ),
            endAdornment: searchTerm ? (
              <InputAdornment position='end'>
                <IconButton
                  size='small'
                  onClick={() => setSearchTerm('')}
                  edge='end'
                >
                  <ClearIcon sx={{ fontSize: 16 }} />
                </IconButton>
              </InputAdornment>
            ) : null,
          }}
        />

        {/* Refresh Action Button */}
        <Tooltip title='Refresh Inbox'>
          <IconButton
            className='aop-refresh-btn'
            size='small'
            onClick={load}
            disabled={loading}
          >
            <RefreshIcon sx={{ fontSize: 18 }} />
          </IconButton>
        </Tooltip>
      </Stack>
    </Stack>
  )
}
