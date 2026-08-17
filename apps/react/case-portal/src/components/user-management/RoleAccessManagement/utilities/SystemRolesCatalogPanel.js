import React, { useState, useMemo } from 'react'
import {
  Box,
  Typography,
  Button,
  Chip,
  TextField,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TableSortLabel,
  CircularProgress,
  IconButton,
  Badge,
  Collapse,
  Tooltip,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import SearchIcon from '@mui/icons-material/Search'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import SecurityIcon from '@mui/icons-material/Security'
import FilterAltIcon from '@mui/icons-material/FilterAlt'
import FilterAltOffIcon from '@mui/icons-material/FilterAltOff'
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown'
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import { formatGridRows } from './roleUtils'
import ColumnFilterPopover from './ColumnFilterPopover'

const SystemRolesCatalogPanel = ({
  filteredRolesList,
  roleSearchQuery,
  setRoleSearchQuery,
  fetchRoles,
  rolesLoading,
  setSelectedRoles,
  showNotification,
  setRoleToDelete,
  setDeleteDialogOpen,
}) => {
  const [expanded, setExpanded] = useState(true)

  // Sorting State
  const [orderBy, setOrderBy] = useState('name')
  const [order, setOrder] = useState('asc')

  // Multi-Column Filter State: { [field]: string[] | null }
  const [columnFilters, setColumnFilters] = useState({})

  // Active Popover State: { field: string, anchorEl: HTMLElement } | null
  const [filterPopover, setFilterPopover] = useState(null)

  const rows = useMemo(
    () => formatGridRows(filteredRolesList),
    [filteredRolesList],
  )

  // Extract unique distinct values for each column for popovers
  const uniqueColumnValues = useMemo(() => {
    const values = {}
    const fields = ['name', 'description']
    fields.forEach((field) => {
      const set = new Set()
      rows.forEach((r) => {
        const val = r[field] ? String(r[field]) : '(Blank)'
        set.add(val)
      })
      values[field] = Array.from(set).sort()
    })
    return values
  }, [rows])

  // Filter Popover handlers
  const handleOpenFilterPopover = (event, field) => {
    event.stopPropagation()
    setFilterPopover({ field, anchorEl: event.currentTarget })
  }

  const handleCloseFilterPopover = () => {
    setFilterPopover(null)
  }

  const handleApplyColumnFilter = (field, selectedValues) => {
    setColumnFilters((prev) => ({
      ...prev,
      [field]:
        selectedValues && selectedValues.length > 0 ? selectedValues : null,
    }))
  }

  const handleClearColumnFilter = (field) => {
    setColumnFilters((prev) => {
      const next = { ...prev }
      delete next[field]
      return next
    })
  }

  const handleClearAllFilters = () => {
    setColumnFilters({})
    setRoleSearchQuery('')
  }

  const hasActiveColumnFilters = Object.values(columnFilters).some(
    (v) => Array.isArray(v) && v.length > 0,
  )

  // Filtered & Sorted Rows
  const filteredAndSortedRows = useMemo(() => {
    let result = [...rows]

    // Global Search Filter
    if (roleSearchQuery && roleSearchQuery.trim()) {
      const q = roleSearchQuery.trim().toLowerCase()
      result = result.filter(
        (r) =>
          (r.name && r.name.toLowerCase().includes(q)) ||
          (r.description && r.description.toLowerCase().includes(q)),
      )
    }

    // Per-Column Multi-Select Filters
    Object.keys(columnFilters).forEach((field) => {
      const allowedValues = columnFilters[field]
      if (Array.isArray(allowedValues) && allowedValues.length > 0) {
        result = result.filter((r) => {
          const val = r[field] ? String(r[field]) : '(Blank)'
          return allowedValues.includes(val)
        })
      }
    })

    // Sorting
    result.sort((a, b) => {
      const valA = (a[orderBy] || '').toString().toLowerCase()
      const valB = (b[orderBy] || '').toString().toLowerCase()
      if (valA < valB) return order === 'asc' ? -1 : 1
      if (valA > valB) return order === 'asc' ? 1 : -1
      return 0
    })

    return result
  }, [rows, roleSearchQuery, columnFilters, orderBy, order])

  const handleRequestSort = (property) => {
    const isAsc = orderBy === property && order === 'asc'
    setOrder(isAsc ? 'desc' : 'asc')
    setOrderBy(property)
  }

  const handleDeleteClick = (roleName) => {
    setRoleToDelete(roleName)
    setDeleteDialogOpen(true)
  }

  return (
    <Paper
      elevation={0}
      sx={{
        borderRadius: '10px',
        padding: '16px 18px',
        border: '1px solid #e2e8f0',
        backgroundColor: '#ffffff',
        marginBottom: 0,
        width: '100%',
      }}
    >
      {/* Panel Header */}
      <Box
        onClick={() => setExpanded(!expanded)}
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          cursor: 'pointer',
          userSelect: 'none',
          pb: expanded ? 1.5 : 0,
          borderBottom: expanded ? '1px solid #f1f5f9' : 'none',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <SecurityIcon sx={{ color: '#0284c7', fontSize: 20 }} />
          <Typography
            variant='subtitle2'
            sx={{ fontWeight: 800, color: '#0f172a', fontSize: '0.875rem' }}
          >
            System Roles Catalog
          </Typography>
          <Tooltip
            title='View, filter, sort, and manage system roles.'
            arrow
            placement='top'
          >
            <IconButton
              size='small'
              onClick={(e) => e.stopPropagation()}
              sx={{ p: 0.2, color: '#0284c7', '&:hover': { color: '#0369a1' } }}
            >
              <InfoOutlinedIcon sx={{ fontSize: 16 }} />
            </IconButton>
          </Tooltip>
        </Box>

        <IconButton
          size='small'
          onClick={(e) => {
            e.stopPropagation()
            setExpanded(!expanded)
          }}
          sx={{
            color: '#64748b',
            p: 0.5,
            '&:hover': { backgroundColor: '#f1f5f9', color: '#0284c7' },
          }}
        >
          {expanded ? (
            <KeyboardArrowUpIcon sx={{ fontSize: 20 }} />
          ) : (
            <KeyboardArrowDownIcon sx={{ fontSize: 20 }} />
          )}
        </IconButton>
      </Box>

      {/* Collapsible Body */}
      <Collapse in={expanded} timeout='auto' unmountOnExit={false}>
        <Box sx={{ pt: 1.5 }}>
          {/* Top Search Bar & Actions */}
          <Box
            sx={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: '12px',
              gap: '12px',
              flexWrap: 'wrap',
            }}
          >
            {/* Search Input */}
            <TextField
              size='small'
              placeholder='Search system roles catalog...'
              value={roleSearchQuery}
              onChange={(e) => setRoleSearchQuery(e.target.value)}
              InputProps={{
                startAdornment: (
                  <SearchIcon
                    style={{
                      fontSize: 18,
                      marginRight: 6,
                      color: '#0284c7',
                    }}
                  />
                ),
              }}
              sx={{
                width: '320px',
                '& .MuiOutlinedInput-root': {
                  borderRadius: '8px',
                  backgroundColor: '#f8fafc',
                  '& fieldset': { borderColor: '#cbd5e1' },
                  '&:hover fieldset': { borderColor: '#94a3b8' },
                  '&.Mui-focused': {
                    backgroundColor: '#ffffff',
                    '& fieldset': {
                      borderColor: '#0284c7',
                      borderWidth: '1.5px',
                    },
                    boxShadow: '0 0 0 3px rgba(2, 132, 199, 0.12)',
                  },
                },
                '& .MuiInputBase-input': {
                  fontSize: '0.8rem',
                  color: '#0f172a !important',
                  fontWeight: 600,
                  '&::placeholder': {
                    color: '#64748b',
                    opacity: 1,
                  },
                },
              }}
            />

            {/* Action Bar Right */}
            <Box
              sx={{
                display: 'flex',
                gap: '8px',
                alignItems: 'center',
              }}
            >
              {/* Clear Column Filters Button */}
              {(hasActiveColumnFilters || roleSearchQuery) && (
                <Button
                  size='small'
                  variant='outlined'
                  color='warning'
                  onClick={handleClearAllFilters}
                  startIcon={<FilterAltOffIcon style={{ fontSize: 14 }} />}
                  sx={{
                    fontSize: '0.72rem',
                    fontWeight: 700,
                    textTransform: 'none',
                    borderRadius: '6px',
                    height: '32px',
                    px: 1.2,
                    color: '#d97706',
                    borderColor: '#fcd34d',
                    backgroundColor: '#fffbeb',
                    '&:hover': { backgroundColor: '#fef3c7' },
                  }}
                >
                  Clear Filters
                </Button>
              )}

              {/* Refresh Catalog Button */}
              <Button
                variant='outlined'
                color='primary'
                size='small'
                onClick={() => fetchRoles()}
                disabled={rolesLoading}
                startIcon={
                  rolesLoading ? (
                    <CircularProgress size={14} color='inherit' />
                  ) : (
                    <RefreshIcon style={{ fontSize: 14 }} />
                  )
                }
                sx={{
                  textTransform: 'none',
                  fontWeight: 600,
                  borderRadius: '6px',
                  height: '32px',
                  fontSize: '0.75rem',
                  color: '#0284c7',
                  borderColor: '#bae6fd',
                  backgroundColor: '#f0f9ff',
                  '&:hover': { backgroundColor: '#e0f2fe' },
                }}
              >
                {rolesLoading ? 'Refreshing...' : 'Refresh Catalog'}
              </Button>
            </Box>
          </Box>

          {/* Active Filter Chips Bar */}
          {hasActiveColumnFilters && (
            <Box
              sx={{
                display: 'flex',
                gap: 1,
                alignItems: 'center',
                flexWrap: 'wrap',
                mb: 1.5,
                p: 1,
                borderRadius: '6px',
                backgroundColor: '#f0f9ff',
                border: '1px solid #bae6fd',
              }}
            >
              <Typography
                variant='caption'
                sx={{ fontWeight: 800, color: '#0369a1', fontSize: '0.72rem' }}
              >
                Active Filters:
              </Typography>
              {Object.keys(columnFilters).map((field) => {
                const values = columnFilters[field]
                if (!Array.isArray(values) || values.length === 0) return null
                const label = field === 'name' ? 'Role Name' : 'Description'
                return (
                  <Chip
                    key={field}
                    label={`${label}: ${values.join(', ')}`}
                    size='small'
                    onDelete={() => handleClearColumnFilter(field)}
                    sx={{
                      fontWeight: 700,
                      backgroundColor: '#0284c7',
                      color: '#ffffff',
                      fontSize: '0.7rem',
                      height: '22px',
                      '& .MuiChip-deleteIcon': {
                        color: '#ffffff',
                        fontSize: 13,
                        '&:hover': { color: '#fef2f2' },
                      },
                    }}
                  />
                )
              })}
            </Box>
          )}

          {/* Scrollable Table Container */}
          <TableContainer
            sx={{
              maxHeight: 'calc(100vh - 310px)',
              overflowY: 'auto',
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
              '&::-webkit-scrollbar': { width: '6px', height: '6px' },
              '&::-webkit-scrollbar-track': { backgroundColor: '#f1f5f9' },
              '&::-webkit-scrollbar-thumb': {
                backgroundColor: '#cbd5e1',
                borderRadius: '4px',
                '&:hover': { backgroundColor: '#0284c7' },
              },
            }}
          >
            <Table size='small' stickyHeader sx={{ minWidth: 600 }}>
              {/* Frozen Sticky Table Header */}
              <TableHead>
                <TableRow>
                  {/* Role Name Header */}
                  {(() => {
                    const isFiltered = Boolean(columnFilters.name)
                    const filterCount = columnFilters.name
                      ? columnFilters.name.length
                      : 0
                    return (
                      <TableCell
                        sx={{
                          position: 'sticky',
                          top: 0,
                          zIndex: 10,
                          backgroundColor: isFiltered ? '#e0f2fe' : '#f1f5f9',
                          fontWeight: 800,
                          color: isFiltered ? '#0369a1' : '#0f172a',
                          fontSize: '0.8rem',
                          py: 1.2,
                          px: 2,
                          borderBottom: '2px solid #0284c7',
                          borderRight: '1px solid #cbd5e1',
                          width: '35%',
                          transition: 'background-color 0.2s ease',
                        }}
                      >
                        <Box
                          sx={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                          }}
                        >
                          <TableSortLabel
                            active={orderBy === 'name'}
                            direction={orderBy === 'name' ? order : 'asc'}
                            onClick={() => handleRequestSort('name')}
                            sx={{
                              fontWeight: 800,
                              color: isFiltered
                                ? '#0369a1 !important'
                                : '#0f172a !important',
                              '& .MuiTableSortLabel-icon': {
                                color: '#0284c7 !important',
                              },
                            }}
                          >
                            Role Name
                          </TableSortLabel>

                          {/* Filter Funnel Icon with Badge */}
                          <IconButton
                            size='small'
                            onClick={(e) => handleOpenFilterPopover(e, 'name')}
                            sx={{
                              p: 0.4,
                              color: isFiltered ? '#0284c7' : '#94a3b8',
                              backgroundColor: isFiltered
                                ? '#bae6fd'
                                : 'transparent',
                              '&:hover': {
                                color: '#0284c7',
                                backgroundColor: '#e0f2fe',
                              },
                            }}
                          >
                            <Badge
                              badgeContent={isFiltered ? filterCount : 0}
                              color='primary'
                              sx={{
                                '& .MuiBadge-badge': {
                                  fontSize: '0.65rem',
                                  height: 15,
                                  minWidth: 15,
                                  backgroundColor: '#0284c7',
                                },
                              }}
                            >
                              <FilterAltIcon sx={{ fontSize: 16 }} />
                            </Badge>
                          </IconButton>
                        </Box>
                      </TableCell>
                    )
                  })()}

                  {/* Description Header */}
                  {(() => {
                    const isFiltered = Boolean(columnFilters.description)
                    const filterCount = columnFilters.description
                      ? columnFilters.description.length
                      : 0
                    return (
                      <TableCell
                        sx={{
                          position: 'sticky',
                          top: 0,
                          zIndex: 10,
                          backgroundColor: isFiltered ? '#e0f2fe' : '#f1f5f9',
                          fontWeight: 800,
                          color: isFiltered ? '#0369a1' : '#0f172a',
                          fontSize: '0.8rem',
                          py: 1.2,
                          px: 2,
                          borderBottom: '2px solid #0284c7',
                          borderRight: '1px solid #cbd5e1',
                          width: '50%',
                          transition: 'background-color 0.2s ease',
                        }}
                      >
                        <Box
                          sx={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                          }}
                        >
                          <TableSortLabel
                            active={orderBy === 'description'}
                            direction={orderBy === 'description' ? order : 'asc'}
                            onClick={() => handleRequestSort('description')}
                            sx={{
                              fontWeight: 800,
                              color: isFiltered
                                ? '#0369a1 !important'
                                : '#0f172a !important',
                              '& .MuiTableSortLabel-icon': {
                                color: '#0284c7 !important',
                              },
                            }}
                          >
                            Description
                          </TableSortLabel>

                          {/* Filter Funnel Icon with Badge */}
                          <IconButton
                            size='small'
                            onClick={(e) =>
                              handleOpenFilterPopover(e, 'description')
                            }
                            sx={{
                              p: 0.4,
                              color: isFiltered ? '#0284c7' : '#94a3b8',
                              backgroundColor: isFiltered
                                ? '#bae6fd'
                                : 'transparent',
                              '&:hover': {
                                color: '#0284c7',
                                backgroundColor: '#e0f2fe',
                              },
                            }}
                          >
                            <Badge
                              badgeContent={isFiltered ? filterCount : 0}
                              color='primary'
                              sx={{
                                '& .MuiBadge-badge': {
                                  fontSize: '0.65rem',
                                  height: 15,
                                  minWidth: 15,
                                  backgroundColor: '#0284c7',
                                },
                              }}
                            >
                              <FilterAltIcon sx={{ fontSize: 16 }} />
                            </Badge>
                          </IconButton>
                        </Box>
                      </TableCell>
                    )
                  })()}

                  {/* Actions Header */}
                  <TableCell
                    align='center'
                    sx={{
                      position: 'sticky',
                      top: 0,
                      zIndex: 10,
                      backgroundColor: '#f1f5f9',
                      fontWeight: 800,
                      color: '#0f172a',
                      fontSize: '0.8rem',
                      py: 1.2,
                      px: 2,
                      borderBottom: '2px solid #0284c7',
                      width: '15%',
                    }}
                  >
                    Actions
                  </TableCell>
                </TableRow>
              </TableHead>

              {/* Table Body */}
              <TableBody>
                {rolesLoading ? (
                  <TableRow>
                    <TableCell colSpan={3} align='center' sx={{ py: 6 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1.5 }}>
                        <CircularProgress size={20} sx={{ color: '#0284c7' }} />
                        <Typography variant='body2' sx={{ fontWeight: 700, color: '#0284c7', fontSize: '0.825rem' }}>
                          Refreshing catalog roles...
                        </Typography>
                      </Box>
                    </TableCell>
                  </TableRow>
                ) : filteredAndSortedRows.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={3} align='center' sx={{ py: 4 }}>
                      <Typography
                        variant='body2'
                        sx={{
                          color: '#94a3b8',
                          fontStyle: 'italic',
                          fontSize: '0.82rem',
                        }}
                      >
                        {hasActiveColumnFilters
                          ? 'No roles match the selected column filters.'
                          : roleSearchQuery
                            ? `No roles found matching "${roleSearchQuery}".`
                            : 'No roles available in the catalog.'}
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredAndSortedRows.map((row, index) => {
                    const isEven = index % 2 === 0
                    return (
                      <TableRow
                        key={row.id || index}
                        sx={{
                          backgroundColor: isEven ? '#ffffff' : '#f8fafc',
                          transition: 'background-color 0.15s ease',
                          '&:hover': {
                            backgroundColor: '#e0f2fe !important',
                          },
                        }}
                      >
                        {/* Role Name Cell */}
                        <TableCell
                          sx={{
                            py: 0.9,
                            px: 2,
                            borderRight: '1px solid #e2e8f0',
                            borderBottom: '1px solid #e2e8f0',
                          }}
                        >
                          <Tooltip
                            title={`System Role: ${row.name}`}
                            arrow
                            placement='top'
                          >
                            <Chip
                              label={row.name}
                              size='small'
                              icon={
                                <SecurityIcon
                                  style={{ fontSize: 13, color: '#0369a1' }}
                                />
                              }
                              sx={{
                                fontWeight: 700,
                                backgroundColor: '#e0f2fe',
                                color: '#0369a1',
                                borderRadius: '4px',
                                fontSize: '0.75rem',
                                height: '24px',
                                border: '1px solid #bae6fd',
                                cursor: 'pointer',
                              }}
                            />
                          </Tooltip>
                        </TableCell>

                        {/* Description Cell */}
                        <TableCell
                          sx={{
                            py: 0.9,
                            px: 2,
                            borderRight: '1px solid #e2e8f0',
                            borderBottom: '1px solid #e2e8f0',
                          }}
                        >
                          <Tooltip
                            title={row.description || 'System Realm Role'}
                            arrow
                            placement='top'
                          >
                            <Typography
                              variant='body2'
                              sx={{
                                color: '#475569',
                                fontSize: '0.8rem',
                                fontWeight: 500,
                                whiteSpace: 'nowrap',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                cursor: 'default',
                              }}
                            >
                              {row.description}
                            </Typography>
                          </Tooltip>
                        </TableCell>

                        {/* Actions Cell (Delete Only) */}
                        <TableCell
                          align='center'
                          sx={{
                            py: 0.9,
                            px: 1.5,
                            borderBottom: '1px solid #e2e8f0',
                          }}
                        >
                          <Box
                            sx={{
                              display: 'flex',
                              justifyContent: 'center',
                              alignItems: 'center',
                            }}
                          >
                            <Tooltip
                              title={`Delete "${row.name}" role from catalog`}
                              arrow
                              placement='top'
                            >
                              <Button
                                size='small'
                                variant='outlined'
                                color='error'
                                startIcon={
                                  <DeleteOutlineIcon style={{ fontSize: 13 }} />
                                }
                                onClick={() => handleDeleteClick(row.name)}
                                sx={{
                                  textTransform: 'none',
                                  fontWeight: 700,
                                  borderRadius: '5px',
                                  fontSize: '0.7rem',
                                  py: 0.3,
                                  px: 1.2,
                                  minWidth: 'auto',
                                  borderColor: '#fca5a5',
                                  color: '#ef4444',
                                  backgroundColor: '#fef2f2',
                                  '&:hover': {
                                    backgroundColor: '#fee2e2',
                                    borderColor: '#ef4444',
                                  },
                                }}
                              >
                                Delete
                              </Button>
                            </Tooltip>
                          </Box>
                        </TableCell>
                      </TableRow>
                    )
                  })
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </Box>
      </Collapse>

      {/* Multi-Column Filter Popover */}
      {filterPopover && (
        <ColumnFilterPopover
          anchorEl={filterPopover.anchorEl}
          onClose={handleCloseFilterPopover}
          columnTitle={
            filterPopover.field === 'name' ? 'Role Name' : 'Description'
          }
          allValues={uniqueColumnValues[filterPopover.field] || []}
          selectedValues={columnFilters[filterPopover.field] || null}
          onApplyFilter={(selected) =>
            handleApplyColumnFilter(filterPopover.field, selected)
          }
          onClearFilter={() => handleClearColumnFilter(filterPopover.field)}
        />
      )}
    </Paper>
  )
}

export default SystemRolesCatalogPanel
