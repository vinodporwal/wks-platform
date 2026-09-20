import React, { memo, useMemo, useState, useEffect } from 'react'
import {
  Box,
  Button,
  Checkbox,
  Divider,
  FormControl,
  FormControlLabel,
  IconButton,
  InputAdornment,
  MenuItem,
  Popover,
  Select,
  TextField,
  Typography,
} from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import ClearIcon from '@mui/icons-material/Clear'
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward'
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward'
import {
  TEXT_OPERATORS,
  NUMERIC_OPERATORS,
  DATE_OPERATORS,
} from './filterConstants'

const ColumnFilterPopover = memo(
  ({
    anchorEl,
    onClose,
    field,
    isNum,
    isDate,
    uniqueValues = [],
    activeFilter,
    onFilterChange,
    sortDirection,
    onSortChange,
  }) => {
    const [searchTerm, setSearchTerm] = useState('')
    const [tempSelected, setTempSelected] = useState(new Set())
    const [tempOperator, setTempOperator] = useState(
      isDate ? 'equals' : isNum ? 'equals' : 'contains',
    )
    const [tempCondValue, setTempCondValue] = useState('')

    // Reset / sync local staging state whenever popover opens
    useEffect(() => {
      if (anchorEl) {
        setTempSelected(
          activeFilter?.selected ? new Set(activeFilter.selected) : new Set(),
        )
        if (activeFilter?.condition) {
          setTempOperator(
            activeFilter.condition.operator ||
              (isDate ? 'equals' : isNum ? 'equals' : 'contains'),
          )
          setTempCondValue(activeFilter.condition.value || '')
        } else {
          setTempOperator(isDate ? 'equals' : isNum ? 'equals' : 'contains')
          setTempCondValue('')
        }
        setSearchTerm('')
      }
    }, [anchorEl, activeFilter, isDate, isNum])

    const availableOperators = useMemo(() => {
      if (isDate) return DATE_OPERATORS
      if (isNum) return NUMERIC_OPERATORS
      return TEXT_OPERATORS
    }, [isDate, isNum])

    const handleSetSort = (dir) => {
      if (onSortChange) {
        onSortChange(field, sortDirection === dir ? null : dir)
      }
    }

    const handleToggleAll = () => {
      if (tempSelected.size === uniqueValues.length) {
        setTempSelected(new Set())
      } else {
        setTempSelected(new Set(uniqueValues))
      }
    }

    const handleToggleItem = (val) => {
      const next = new Set(tempSelected)
      if (next.has(val)) {
        next.delete(val)
      } else {
        next.add(val)
      }
      setTempSelected(next)
    }

    const handleApply = () => {
      const isCustomCondition =
        tempOperator === 'isEmpty' ||
        tempOperator === 'isNotEmpty' ||
        tempCondValue.trim() !== ''

      const isCheckboxFiltered =
        tempSelected.size > 0 && tempSelected.size < uniqueValues.length
          ? tempSelected
          : tempSelected.size === uniqueValues.length
            ? null
            : tempSelected.size > 0
              ? tempSelected
              : null

      if (!isCustomCondition && !isCheckboxFiltered) {
        onFilterChange(field, null)
      } else {
        onFilterChange(field, {
          selected: isCheckboxFiltered,
          condition: isCustomCondition
            ? { operator: tempOperator, value: tempCondValue.trim() }
            : null,
        })
      }
      onClose()
    }

    const handleClear = () => {
      setTempSelected(new Set())
      setTempOperator(isDate ? 'equals' : isNum ? 'equals' : 'contains')
      setTempCondValue('')
      onFilterChange(field, null)
      onClose()
    }

    const visibleValues = useMemo(() => {
      if (!searchTerm) return uniqueValues
      const lower = searchTerm.toLowerCase()
      return uniqueValues.filter((v) => String(v).toLowerCase().includes(lower))
    }, [uniqueValues, searchTerm])

    const renderedValues = useMemo(
      () => visibleValues.slice(0, 100),
      [visibleValues],
    )

    const isAllChecked =
      uniqueValues.length > 0 && tempSelected.size === uniqueValues.length
    const isIndeterminate =
      tempSelected.size > 0 && tempSelected.size < uniqueValues.length

    const isNoValOperator =
      tempOperator === 'isEmpty' || tempOperator === 'isNotEmpty'

    const sortAscLabel = isDate
      ? 'Sort Oldest to Newest'
      : isNum
        ? 'Sort Smallest to Largest (1 → 9)'
        : 'Sort A to Z'

    const sortDescLabel = isDate
      ? 'Sort Newest to Oldest'
      : isNum
        ? 'Sort Largest to Smallest (9 → 1)'
        : 'Sort Z to A'

    return (
      <Popover
        open={Boolean(anchorEl)}
        anchorEl={anchorEl}
        onClose={onClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        transformOrigin={{ vertical: 'top', horizontal: 'left' }}
        PaperProps={{
          sx: {
            p: 1.5,
            width: 290,
            boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
            borderRadius: '8px',
          },
        }}
      >
        {/* Section 1: Quick Sort */}
        <Box mb={1}>
          <Typography
            variant='caption'
            sx={{
              fontWeight: 600,
              color: '#64748B',
              display: 'block',
              mb: 0.5,
              fontSize: '0.72rem',
            }}
          >
            SORT COLUMN
          </Typography>
          <Button
            variant={sortDirection === 'asc' ? 'contained' : 'outlined'}
            size='small'
            fullWidth
            startIcon={<ArrowUpwardIcon sx={{ fontSize: 14 }} />}
            onClick={() => handleSetSort('asc')}
            sx={{
              textTransform: 'none',
              height: 26,
              fontSize: '0.72rem',
              mb: 0.5,
              borderColor: '#CBD5E1',
              backgroundColor: sortDirection === 'asc' ? '#0284C7' : '#FFFFFF',
              color: sortDirection === 'asc' ? '#FFFFFF' : '#334155',
              '&:hover': {
                backgroundColor:
                  sortDirection === 'asc' ? '#0369A1' : '#F1F5F9',
              },
            }}
          >
            {sortAscLabel}
          </Button>
          <Button
            variant={sortDirection === 'desc' ? 'contained' : 'outlined'}
            size='small'
            fullWidth
            startIcon={<ArrowDownwardIcon sx={{ fontSize: 14 }} />}
            onClick={() => handleSetSort('desc')}
            sx={{
              textTransform: 'none',
              height: 26,
              fontSize: '0.72rem',
              borderColor: '#CBD5E1',
              backgroundColor: sortDirection === 'desc' ? '#0284C7' : '#FFFFFF',
              color: sortDirection === 'desc' ? '#FFFFFF' : '#334155',
              '&:hover': {
                backgroundColor:
                  sortDirection === 'desc' ? '#0369A1' : '#F1F5F9',
              },
            }}
          >
            {sortDescLabel}
          </Button>
        </Box>

        <Divider sx={{ my: 1 }} />

        {/* Section 2: Condition Filter */}
        <Box mb={1.2}>
          <Typography
            variant='caption'
            sx={{
              fontWeight: 600,
              color: '#64748B',
              display: 'block',
              mb: 0.5,
              fontSize: '0.72rem',
            }}
          >
            FILTER BY CONDITION
          </Typography>

          <FormControl size='small' fullWidth sx={{ mb: 0.8 }}>
            <Select
              value={tempOperator}
              onChange={(e) => setTempOperator(e.target.value)}
              sx={{
                height: 30,
                fontSize: '0.82rem',
                borderRadius: '4px',
                backgroundColor: '#FFFFFF',
              }}
            >
              {availableOperators.map((op) => (
                <MenuItem
                  key={op.value}
                  value={op.value}
                  sx={{ fontSize: '0.82rem' }}
                >
                  {op.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          {!isNoValOperator && (
            <TextField
              placeholder={isDate ? 'YYYY-MM-DD' : 'Value...'}
              type={
                isDate &&
                (tempOperator === 'isAfter' ||
                  tempOperator === 'isAfterOrEqual' ||
                  tempOperator === 'isBefore' ||
                  tempOperator === 'isBeforeOrEqual' ||
                  tempOperator === 'equals' ||
                  tempOperator === 'doesNotEqual')
                  ? 'date'
                  : 'text'
              }
              size='small'
              fullWidth
              value={tempCondValue}
              onChange={(e) => setTempCondValue(e.target.value)}
              inputProps={{
                sx: {
                  padding: '4px 8px !important',
                  caretColor: '#0284C7 !important',
                  color: '#1E293B',
                  fontSize: '0.82rem',
                  cursor: 'text',
                },
              }}
              InputProps={{
                endAdornment: tempCondValue ? (
                  <InputAdornment position='end' sx={{ ml: 0.5 }}>
                    <IconButton
                      size='small'
                      onClick={() => setTempCondValue('')}
                      sx={{ p: '2px' }}
                    >
                      <ClearIcon sx={{ fontSize: 14 }} />
                    </IconButton>
                  </InputAdornment>
                ) : null,
                sx: {
                  height: 30,
                  borderRadius: '4px',
                  backgroundColor: '#FFFFFF',
                },
              }}
            />
          )}
        </Box>

        <Divider sx={{ my: 1 }}>
          <Typography
            variant='caption'
            sx={{ color: '#94A3B8', fontSize: '0.68rem', fontWeight: 600 }}
          >
            OR FILTER BY VALUE
          </Typography>
        </Divider>

        {/* Section 3: Search Values Input */}
        <TextField
          placeholder='Search values...'
          size='small'
          fullWidth
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          inputProps={{
            sx: {
              padding: '4px 0 !important',
              caretColor: '#0284C7 !important',
              color: '#1E293B',
              fontSize: '0.82rem',
              cursor: 'text',
            },
          }}
          InputProps={{
            startAdornment: (
              <InputAdornment position='start' sx={{ mr: 0.8 }}>
                <SearchIcon sx={{ fontSize: 16, color: '#64748B' }} />
              </InputAdornment>
            ),
            endAdornment: searchTerm ? (
              <InputAdornment position='end' sx={{ ml: 0.5 }}>
                <IconButton
                  size='small'
                  onClick={() => setSearchTerm('')}
                  sx={{ p: '2px' }}
                >
                  <ClearIcon sx={{ fontSize: 14 }} />
                </IconButton>
              </InputAdornment>
            ) : null,
            sx: {
              height: 30,
              fontSize: '0.82rem',
              borderRadius: '4px',
              backgroundColor: '#FFFFFF',
              '& .MuiOutlinedInput-input': {
                padding: '4px 0 !important',
                caretColor: '#0284C7 !important',
              },
            },
          }}
          sx={{ mb: 0.8 }}
        />

        {/* Check All Option */}
        <FormControlLabel
          control={
            <Checkbox
              size='small'
              checked={isAllChecked}
              indeterminate={isIndeterminate}
              onChange={handleToggleAll}
              sx={{ p: '2px 6px' }}
            />
          }
          label={
            <Typography sx={{ fontSize: '0.8rem', fontWeight: 600 }}>
              Check All
            </Typography>
          }
          sx={{ width: '100%', m: 0 }}
        />

        {/* Scrollable Checkbox List for Unique Values */}
        <Box
          sx={{
            maxHeight: 150,
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
                        fontSize: '0.78rem',
                        color: '#334155',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                        maxWidth: 210,
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
    )
  },
)

export default ColumnFilterPopover
