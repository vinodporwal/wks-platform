import React, { memo, useMemo, useState } from 'react'
import { Box, IconButton, Tooltip, Typography } from '@mui/material'
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward'
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward'
import SwapVertIcon from '@mui/icons-material/SwapVert'
import MoreVertIcon from '@mui/icons-material/MoreVert'
import { isDateColumn } from './reportGridHelpers'
import ColumnFilterPopover from './ColumnFilterPopover'

const CustomColumnHeader = memo(
  ({
    colDef,
    rows = [],
    activeFilter,
    onFilterChange,
    sortConfig,
    onSortChange,
    isAromaticsHmd,
  }) => {
    const [anchorEl, setAnchorEl] = useState(null)

    const field = colDef.field
    const isNum = colDef.type === 'number'
    const isDate = isDateColumn(colDef, rows)

    const isCurrentSorted = sortConfig?.field === field
    const sortDirection = isCurrentSorted ? sortConfig.direction : null

    // Derive distinct values from data rows
    const uniqueValues = useMemo(() => {
      const set = new Set()
      rows.forEach((r) => {
        let val = r[field]
        if (isNum && val != null && val !== '') {
          const num = Number(val)
          val = isNaN(num) ? val : num.toFixed(isAromaticsHmd ? 5 : 3)
        }
        set.add(val ?? '')
      })
      return Array.from(set).sort((a, b) => {
        if (a === '') return 1
        if (b === '') return -1
        if (typeof a === 'number' && typeof b === 'number') return a - b
        return String(a).localeCompare(String(b), undefined, { numeric: true })
      })
    }, [rows, field, isNum, isAromaticsHmd])

    const isFiltered = Boolean(
      (activeFilter?.selected && activeFilter.selected.size > 0) ||
        (activeFilter?.condition &&
          (activeFilter.condition.value ||
            activeFilter.condition.operator === 'isEmpty' ||
            activeFilter.condition.operator === 'isNotEmpty')),
    )

    const handleHeaderClick = (e) => {
      if (e.target.closest('.filter-btn') || anchorEl) return
      if (onSortChange) {
        onSortChange(field)
      }
    }

    return (
      <Box
        display='flex'
        alignItems='center'
        justifyContent='space-between'
        onClick={handleHeaderClick}
        sx={{
          width: '100%',
          height: '100%',
          px: 1,
          backgroundColor: isFiltered ? '#BAE6FD' : 'inherit',
          userSelect: 'none',
          cursor: 'pointer',
          '&:hover .sort-indicator': {
            opacity: 1,
          },
        }}
      >
        <Box
          display='flex'
          alignItems='center'
          gap={0.5}
          sx={{ overflow: 'hidden', flex: 1 }}
        >
          <Tooltip title={colDef.title || colDef.field} placement='top' arrow>
            <Typography
              sx={{
                fontWeight: 600,
                fontSize: '0.82rem',
                color: '#1E293B',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
                fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
              }}
            >
              {colDef.title || colDef.field}
            </Typography>
          </Tooltip>

          {/* Sort Indicator */}
          {sortDirection === 'asc' ? (
            <ArrowUpwardIcon sx={{ fontSize: 14, color: '#0284C7' }} />
          ) : sortDirection === 'desc' ? (
            <ArrowDownwardIcon sx={{ fontSize: 14, color: '#0284C7' }} />
          ) : (
            <SwapVertIcon
              className='sort-indicator'
              sx={{
                fontSize: 14,
                color: '#94A3B8',
                opacity: 0,
                transition: 'opacity 0.2s',
              }}
            />
          )}
        </Box>

        {/* 3 Dots Menu Button */}
        <IconButton
          className='filter-btn'
          size='small'
          onClick={(e) => {
            e.stopPropagation()
            setAnchorEl(e.currentTarget)
          }}
          sx={{
            p: '2px',
            ml: 0.5,
            color: isFiltered ? '#0284C7' : '#94A3B8',
            '&:hover': { color: '#0284C7', backgroundColor: '#E0F2FE' },
          }}
        >
          <MoreVertIcon sx={{ fontSize: 16 }} />
        </IconButton>

        {/* Modular Filter & Sort Popover */}
        <ColumnFilterPopover
          anchorEl={anchorEl}
          onClose={() => setAnchorEl(null)}
          field={field}
          isNum={isNum}
          isDate={isDate}
          uniqueValues={uniqueValues}
          activeFilter={activeFilter}
          onFilterChange={onFilterChange}
          sortDirection={sortDirection}
          onSortChange={onSortChange}
        />
      </Box>
    )
  },
)

CustomColumnHeader.displayName = 'CustomColumnHeader'

export default CustomColumnHeader
