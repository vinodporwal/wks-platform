import React, { memo, useMemo, useState } from 'react'
import {
  Box,
  Button,
  Checkbox,
  Divider,
  FormControlLabel,
  IconButton,
  InputAdornment,
  Popover,
  TextField,
  Typography,
} from '@mui/material'
import MoreVertIcon from '@mui/icons-material/MoreVert'
import SearchIcon from '@mui/icons-material/Search'
import ClearIcon from '@mui/icons-material/Clear'
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp'

export const CustomColumnHeader = memo(
  ({
    field,
    title,
    getUniqueValues,
    selectedValues, // Set of selected values, or null if all selected
    onApplyFilter,
    onClearFilter,
    isNumeric,
  }) => {
    const [anchorEl, setAnchorEl] = useState(null)
    const [searchTerm, setSearchTerm] = useState('')
    const [tempSelected, setTempSelected] = useState(new Set())
    const [uniqueValues, setUniqueValues] = useState([])

    const isFiltered = Boolean(
      selectedValues != null && selectedValues.size > 0,
    )

    const handleOpen = (e) => {
      e.stopPropagation()
      setAnchorEl(e.currentTarget)
      setSearchTerm('')
      const vals = getUniqueValues ? getUniqueValues(field) : []
      setUniqueValues(vals)
      setTempSelected(new Set(selectedValues != null ? selectedValues : []))
    }

    const handleClose = () => {
      setAnchorEl(null)
    }

    const visibleValues = useMemo(() => {
      if (!searchTerm.trim()) return uniqueValues
      const q = searchTerm.toLowerCase().trim()
      return uniqueValues.filter((v) =>
        String(v ?? '')
          .toLowerCase()
          .includes(q),
      )
    }, [uniqueValues, searchTerm])

    const renderedValues = useMemo(() => {
      return visibleValues.slice(0, 100)
    }, [visibleValues])

    const isAllChecked =
      visibleValues.length > 0 &&
      visibleValues.every((v) => tempSelected.has(v))
    const isIndeterminate =
      visibleValues.some((v) => tempSelected.has(v)) && !isAllChecked

    const handleToggleAll = () => {
      setTempSelected((prev) => {
        const next = new Set(prev)
        if (isAllChecked) {
          visibleValues.forEach((v) => next.delete(v))
        } else {
          visibleValues.forEach((v) => next.add(v))
        }
        return next
      })
    }

    const handleToggleItem = (val) => {
      setTempSelected((prev) => {
        const next = new Set(prev)
        if (next.has(val)) {
          next.delete(val)
        } else {
          next.add(val)
        }
        return next
      })
    }

    const handleApply = () => {
      if (
        tempSelected.size === 0 ||
        tempSelected.size === uniqueValues.length
      ) {
        onClearFilter(field)
      } else {
        onApplyFilter(field, tempSelected)
      }
      handleClose()
    }

    const handleClear = () => {
      setTempSelected(new Set())
      onClearFilter(field)
      handleClose()
    }

    return (
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: isNumeric ? 'flex-end' : 'space-between',
          width: '100%',
          height: '100%',
          px: 1.2,
          backgroundColor: isFiltered ? '#BAE6FD' : 'inherit',
          color: isFiltered ? '#0369A1' : '#1E293B',
          transition: 'background-color 0.2s',
        }}
      >
        <Typography
          sx={{
            fontWeight: isFiltered ? 700 : 600,
            fontSize: '0.82rem',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
            fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
            textAlign: isNumeric ? 'right' : 'left',
          }}
          title={title}
        >
          {title}
        </Typography>

        <IconButton
          size='small'
          onClick={handleOpen}
          sx={{
            p: '2px',
            ml: 0.5,
            color: isFiltered ? '#0284C7' : '#94A3B8',
            '&:hover': {
              backgroundColor: isFiltered
                ? 'rgba(2, 132, 199, 0.15)'
                : 'rgba(0,0,0,0.06)',
            },
          }}
        >
          <MoreVertIcon sx={{ fontSize: 16 }} />
        </IconButton>

        {/* Checkbox Filter Popover Menu */}
        <Popover
          open={Boolean(anchorEl)}
          anchorEl={anchorEl}
          onClose={handleClose}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
          transformOrigin={{ vertical: 'top', horizontal: 'left' }}
          PaperProps={{
            sx: {
              width: 270,
              p: 1.5,
              borderRadius: '6px',
              border: '1px solid #CBD5E1',
              boxShadow: '0 10px 25px rgba(0,0,0,0.15)',
              fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
            },
          }}
        >
          {/* Header Title with Collapse */}
          <Box
            display='flex'
            alignItems='center'
            justifyContent='space-between'
            mb={1}
            pb={0.5}
            borderBottom='1px solid #F1F5F9'
          >
            <Typography
              variant='caption'
              sx={{
                fontWeight: 700,
                color: '#334155',
                textTransform: 'uppercase',
                fontSize: '0.75rem',
              }}
            >
              {title}
            </Typography>
            <IconButton
              size='small'
              onClick={handleClose}
              sx={{ p: '2px', color: '#64748B' }}
            >
              <KeyboardArrowUpIcon sx={{ fontSize: 18 }} />
            </IconButton>
          </Box>

          {/* Search Input Box */}
          <TextField
            placeholder='Search'
            size='small'
            fullWidth
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position='start'>
                  <SearchIcon sx={{ fontSize: 16, color: '#64748B' }} />
                </InputAdornment>
              ),
              endAdornment: searchTerm ? (
                <InputAdornment position='end'>
                  <IconButton size='small' onClick={() => setSearchTerm('')}>
                    <ClearIcon sx={{ fontSize: 14 }} />
                  </IconButton>
                </InputAdornment>
              ) : null,
              sx: { height: 28, fontSize: '0.8rem', borderRadius: '4px' },
            }}
            sx={{ mb: 1 }}
          />

          <Divider sx={{ mb: 0.5 }} />

          {/* Check All Option */}
          <FormControlLabel
            control={
              <Checkbox
                size='small'
                checked={isAllChecked}
                indeterminate={isIndeterminate}
                onChange={handleToggleAll}
                sx={{ p: '3px 6px' }}
              />
            }
            label={
              <Typography sx={{ fontSize: '0.82rem', fontWeight: 600 }}>
                Check All
              </Typography>
            }
            sx={{ width: '100%', m: 0 }}
          />

          {/* Scrollable Checkbox List for Unique Values */}
          <Box
            sx={{
              maxHeight: 180,
              overflowY: 'auto',
              border: '1px solid #F1F5F9',
              borderRadius: '4px',
              p: 0.5,
              my: 0.5,
              backgroundColor: '#FAFAFA',
            }}
          >
            {renderedValues.length === 0 ? (
              <Typography
                variant='caption'
                sx={{
                  p: 1,
                  display: 'block',
                  color: '#94A3B8',
                  textAlign: 'center',
                }}
              >
                No matching values
              </Typography>
            ) : (
              renderedValues.map((val, idx) => {
                const isChecked = tempSelected.has(val)
                const displayLabel =
                  val == null || val === '' ? '(Blank)' : String(val)
                return (
                  <FormControlLabel
                    key={idx}
                    control={
                      <Checkbox
                        size='small'
                        checked={isChecked}
                        onChange={() => handleToggleItem(val)}
                        sx={{ p: '2px 6px' }}
                      />
                    }
                    label={
                      <Typography
                        sx={{
                          fontSize: '0.8rem',
                          color: '#334155',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                          maxWidth: 190,
                        }}
                        title={displayLabel}
                      >
                        {displayLabel}
                      </Typography>
                    }
                    sx={{
                      width: '100%',
                      m: 0,
                      '&:hover': { backgroundColor: '#F1F5F9' },
                    }}
                  />
                )
              })
            )}
          </Box>

          {/* Summary Count of Selected Items */}
          <Box
            display='flex'
            justifyContent='space-between'
            alignItems='center'
            mb={1.5}
          >
            <Typography
              variant='caption'
              sx={{ color: '#64748B', fontSize: '0.72rem' }}
            >
              {tempSelected.size} selected items
            </Typography>
            {visibleValues.length > 100 && (
              <Typography
                variant='caption'
                sx={{ color: '#94A3B8', fontSize: '0.68rem' }}
              >
                (Showing top 100)
              </Typography>
            )}
          </Box>

          {/* Action Buttons: Filter & Clear */}
          <Box display='flex' gap={1}>
            <Button
              variant='contained'
              size='small'
              fullWidth
              onClick={handleApply}
              sx={{
                textTransform: 'none',
                height: 28,
                fontSize: '0.78rem',
                backgroundColor: '#0284C7',
                '&:hover': { backgroundColor: '#0369A1' },
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
                textTransform: 'none',
                height: 28,
                fontSize: '0.78rem',
                borderColor: '#CBD5E1',
                color: '#475569',
              }}
            >
              Clear
            </Button>
          </Box>
        </Popover>
      </Box>
    )
  },
)

export default CustomColumnHeader
