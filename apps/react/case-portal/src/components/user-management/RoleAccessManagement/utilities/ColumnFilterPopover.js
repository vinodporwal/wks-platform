import React, { useState, useMemo, useEffect } from 'react'
import {
  Box,
  Typography,
  Button,
  TextField,
  Popover,
  Checkbox,
  FormControlLabel,
  IconButton,
  InputAdornment,
} from '@mui/material'
import FilterAltIcon from '@mui/icons-material/FilterAlt'
import SearchIcon from '@mui/icons-material/Search'
import CloseIcon from '@mui/icons-material/Close'
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp'

const ColumnFilterPopover = ({
  anchorEl,
  onClose,
  columnTitle,
  allValues = [],
  selectedValues = null, // null means no active filter (all selected)
  onApplyFilter,
  onClearFilter,
}) => {
  const open = Boolean(anchorEl)

  // Internal search text inside filter popup
  const [filterSearchQuery, setFilterSearchQuery] = useState('')

  // Internal temporary selection state before user clicks "Filter" button
  const [tempSelected, setTempSelected] = useState(new Set())

  // Initialize temp state when popup opens
  useEffect(() => {
    if (open) {
      setFilterSearchQuery('')
      if (selectedValues === null) {
        // If no filter applied yet, check all by default
        setTempSelected(new Set(allValues))
      } else {
        setTempSelected(new Set(selectedValues))
      }
    }
  }, [open, selectedValues, allValues])

  // Filter distinct values matching the search query inside popup
  const matchingValues = useMemo(() => {
    if (!filterSearchQuery.trim()) return allValues
    const q = filterSearchQuery.toLowerCase()
    return allValues.filter((val) => val.toLowerCase().includes(q))
  }, [allValues, filterSearchQuery])

  // Is Check All checked
  const isCheckAllChecked =
    allValues.length > 0 && tempSelected.size === allValues.length

  // Toggle Check All
  const handleToggleCheckAll = () => {
    if (isCheckAllChecked) {
      setTempSelected(new Set())
    } else {
      setTempSelected(new Set(allValues))
    }
  }

  // Toggle individual item
  const handleToggleItem = (val) => {
    const next = new Set(tempSelected)
    if (next.has(val)) {
      next.delete(val)
    } else {
      next.add(val)
    }
    setTempSelected(next)
  }

  // Handle Apply Filter button
  const handleApply = () => {
    if (tempSelected.size === allValues.length) {
      // If everything is selected, clear filter
      onClearFilter()
    } else {
      onApplyFilter(Array.from(tempSelected))
    }
    onClose()
  }

  // Handle Clear button
  const handleClear = () => {
    setTempSelected(new Set(allValues))
    onClearFilter()
    onClose()
  }

  return (
    <Popover
      open={open}
      anchorEl={anchorEl}
      onClose={onClose}
      anchorOrigin={{
        vertical: 'bottom',
        horizontal: 'left',
      }}
      transformOrigin={{
        vertical: 'top',
        horizontal: 'left',
      }}
      PaperProps={{
        sx: {
          width: 250,
          borderRadius: '8px',
          boxShadow:
            '0 10px 25px -5px rgba(15, 23, 42, 0.2), 0 8px 10px -6px rgba(15, 23, 42, 0.1)',
          border: '1px solid #94a3b8',
          p: 0,
          overflow: 'hidden',
          backgroundColor: '#ffffff',
          fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
          '& *': {
            fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif !important",
          },
        },
      }}
    >
      {/* 1. Header Bar: Filter funnel + "Filter" + chevron */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          px: 1.5,
          py: 1,
          borderBottom: '1px solid #cbd5e1',
          backgroundColor: '#ffffff',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <FilterAltIcon sx={{ color: '#0284c7', fontSize: 18 }} />
          <Typography
            variant='subtitle2'
            sx={{ fontWeight: 700, color: '#0f172a', fontSize: '0.875rem' }}
          >
            Filter
          </Typography>
        </Box>
        <IconButton
          size='small'
          onClick={onClose}
          sx={{ p: 0.3, color: '#0284c7' }}
        >
          <KeyboardArrowUpIcon sx={{ fontSize: 20 }} />
        </IconButton>
      </Box>

      <Box sx={{ p: 1.5 }}>
        {/* 2. Search Input Box */}
        <TextField
          size='small'
          placeholder='Search'
          value={filterSearchQuery}
          onChange={(e) => setFilterSearchQuery(e.target.value)}
          fullWidth
          InputProps={{
            startAdornment: (
              <InputAdornment position='start' sx={{ mr: 0.5 }}>
                <SearchIcon sx={{ color: '#0284c7', fontSize: 18 }} />
              </InputAdornment>
            ),
            endAdornment: filterSearchQuery ? (
              <InputAdornment position='end'>
                <IconButton
                  size='small'
                  onClick={() => setFilterSearchQuery('')}
                  sx={{ p: 0.2 }}
                >
                  <CloseIcon sx={{ fontSize: 14, color: '#64748b' }} />
                </IconButton>
              </InputAdornment>
            ) : null,
          }}
          sx={{
            mb: 1.2,
            '& .MuiOutlinedInput-root': {
              borderRadius: '6px',
              backgroundColor: '#ffffff',
              height: '34px',
              '& fieldset': { borderColor: '#64748b' },
              '&:hover fieldset': { borderColor: '#334155' },
              '&.Mui-focused fieldset': {
                borderColor: '#0284c7',
                borderWidth: '1.5px',
              },
            },
            '& .MuiInputBase-input': {
              fontSize: '0.8rem',
              py: 0.5,
              color: '#0f172a !important',
              fontWeight: 500,
              '&::placeholder': { color: '#64748b', opacity: 1 },
            },
          }}
        />

        {/* 3. Checkbox Items List */}
        <Box
          sx={{
            maxHeight: 150,
            overflowY: 'auto',
            border: '1px solid #e2e8f0',
            borderRadius: '4px',
            p: 0.5,
            mb: 1.2,
            backgroundColor: '#ffffff',
            '&::-webkit-scrollbar': { width: '5px' },
            '&::-webkit-scrollbar-thumb': {
              backgroundColor: '#cbd5e1',
              borderRadius: '3px',
            },
          }}
        >
          {/* Check All item */}
          <FormControlLabel
            control={
              <Checkbox
                size='small'
                checked={isCheckAllChecked}
                onChange={handleToggleCheckAll}
                sx={{
                  py: 0.3,
                  px: 0.8,
                  color: '#64748b',
                  '&.Mui-checked': { color: '#0284c7' },
                }}
              />
            }
            label={
              <Typography
                sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#0f172a' }}
              >
                Check All
              </Typography>
            }
            sx={{ width: '100%', m: 0, py: 0.2 }}
          />

          {/* List of distinct matching column values */}
          {matchingValues.map((val, idx) => {
            const isChecked = tempSelected.has(val)
            return (
              <FormControlLabel
                key={idx}
                control={
                  <Checkbox
                    size='small'
                    checked={isChecked}
                    onChange={() => handleToggleItem(val)}
                    sx={{
                      py: 0.3,
                      px: 0.8,
                      color: '#64748b',
                      '&.Mui-checked': { color: '#0284c7' },
                    }}
                  />
                }
                label={
                  <Typography
                    sx={{
                      fontSize: '0.8rem',
                      fontWeight: isChecked ? 600 : 400,
                      color: isChecked ? '#0f172a' : '#475569',
                      wordBreak: 'break-word',
                    }}
                  >
                    {val || '(Blank)'}
                  </Typography>
                }
                sx={{ width: '100%', m: 0, py: 0.1 }}
              />
            )
          })}

          {matchingValues.length === 0 && (
            <Typography
              variant='caption'
              sx={{
                color: '#94a3b8',
                p: 1,
                display: 'block',
                fontStyle: 'italic',
              }}
            >
              No matching values.
            </Typography>
          )}
        </Box>

        {/* 4. Selection Count Indicator */}
        <Typography
          variant='body2'
          sx={{
            fontSize: '0.78rem',
            fontWeight: 700,
            color: '#0f172a',
            mb: 1.5,
          }}
        >
          {tempSelected.size} selected items
        </Typography>

        {/* 5. Action Buttons (Filter & Clear) */}
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button
            variant='contained'
            size='small'
            fullWidth
            onClick={handleApply}
            sx={{
              backgroundColor: '#0284c7',
              color: '#ffffff',
              fontWeight: 700,
              textTransform: 'none',
              borderRadius: '6px',
              fontSize: '0.8rem',
              py: 0.6,
              boxShadow: 'none',
              '&:hover': { backgroundColor: '#0369a1' },
            }}
          >
            Filter
          </Button>

          <Button
            variant='outlined'
            size='small'
            fullWidth
            onClick={handleClear}
            sx={{
              borderColor: '#94a3b8',
              color: '#0f172a',
              fontWeight: 700,
              textTransform: 'none',
              borderRadius: '6px',
              fontSize: '0.8rem',
              py: 0.6,
              '&:hover': { backgroundColor: '#f1f5f9', borderColor: '#64748b' },
            }}
          >
            Clear
          </Button>
        </Box>
      </Box>
    </Popover>
  )
}

export default ColumnFilterPopover
