import React from 'react'
import {
  Chip,
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
      <Stack direction='row' alignItems='center' spacing={1.5}>
        <Typography className='aop-title-text' variant='h6'>
          Action Items
        </Typography>
        <Chip
          className='aop-count-chip'
          label={`${itemCount} ${itemCount === 1 ? 'Item' : 'Items'}`}
          size='small'
        />
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
