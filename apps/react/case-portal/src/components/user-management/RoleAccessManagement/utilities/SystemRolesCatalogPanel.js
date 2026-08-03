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
import PersonAddIcon from '@mui/icons-material/PersonAdd'
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

  const rows = useMemo(() => formatGridRows(filteredRolesList), [filteredRolesList])

  // Extract unique distinct values for each column
  const uniqueColumnValues = useMemo(() => {
    const values = {}
    const fields = ['name', 'description']
    fields.forEach((field) => {
      const set = new Set()
      rows.forEach((r) => {
        if (r[field]) set.add(r[field])
      })
      values[field] = Array.from(set).sort()
    })
    return values
  }, [rows])

  // Multi-column filtering + sorting
  const filteredAndSortedRows = useMemo(() => {
    // 1. Multi-column filtering
    let result = rows.filter((row) => {
      for (const field of Object.keys(columnFilters)) {
        const selected = columnFilters[field]
        if (selected && Array.isArray(selected)) {
          if (!selected.includes(row[field])) {
            return false
          }
        }
      }
      return true
    })

    // 2. Sorting
    result.sort((a, b) => {
      const valueA = (a[orderBy] || '').toString().toLowerCase()
      const valueB = (b[orderBy] || '').toString().toLowerCase()
      if (valueA < valueB) return order === 'asc' ? -1 : 1
      if (valueA > valueB) return order === 'asc' ? 1 : -1
      return 0
    })

    return result
  }, [rows, columnFilters, orderBy, order])

  // Sorting Handler
  const handleRequestSort = (property) => {
    const isAsc = orderBy === property && order === 'asc'
    setOrder(isAsc ? 'desc' : 'asc')
    setOrderBy(property)
  }

  // Open Column Filter Popover
  const handleOpenFilterPopover = (event, field) => {
    event.stopPropagation()
    setFilterPopover({ field, anchorEl: event.currentTarget })
  }

  // Close Popover
  const handleCloseFilterPopover = () => {
    setFilterPopover(null)
  }

  // Apply column filter values
  const handleApplyColumnFilter = (field, selectedVals) => {
    setColumnFilters((prev) => ({
      ...prev,
      [field]: selectedVals,
    }))
  }

  // Clear column filter for a specific column
  const handleClearColumnFilter = (field) => {
    setColumnFilters((prev) => {
      const next = { ...prev }
      delete next[field]
      return next
    })
  }

  // Clear all column filters
  const handleClearAllColumnFilters = () => {
    setColumnFilters({})
  }

  const hasActiveColumnFilters = Object.keys(columnFilters).length > 0

  const handleAssignClick = (rawRole, rName) => {
    setSelectedRoles([rawRole])
    showNotification(`Role "${rName}" selected for assignment below.`, 'info')
  }

  const handleDeleteClick = (rName) => {
    setRoleToDelete(rName)
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
        marginBottom: '14px',
        maxWidth: '920px',
      }}
    >
      {/* Panel Header */}
      <Box
        onClick={() => setExpanded(!expanded)}
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: expanded ? '14px' : 0,
          flexWrap: 'wrap',
          gap: '10px',
          cursor: 'pointer',
          userSelect: 'none',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <SecurityIcon sx={{ color: '#0284c7', fontSize: 20 }} />
          <Typography
            variant="subtitle2"
            sx={{ fontWeight: 800, color: '#0f172a', fontSize: '0.875rem' }}
          >
            System Roles Catalog
          </Typography>
          <Tooltip
            title="View, search, filter, sort, and manage all existing system roles in the catalog."
            arrow
            placement="top"
          >
            <IconButton
              size="small"
              onClick={(e) => e.stopPropagation()}
              sx={{ p: 0.2, color: '#0284c7', '&:hover': { color: '#0369a1' } }}
            >
              <InfoOutlinedIcon sx={{ fontSize: 16 }} />
            </IconButton>
          </Tooltip>
          <Chip
            label={filteredAndSortedRows.length}
            size="small"
            sx={{
              backgroundColor: '#0284c7',
              color: '#ffffff',
              fontWeight: 800,
              fontSize: '0.72rem',
              height: '20px',
              px: 0.5,
            }}
          />

          {/* Active Filters Clear Badge Button */}
          {hasActiveColumnFilters && (
            <Button
              size="small"
              onClick={(e) => {
                e.stopPropagation()
                handleClearAllColumnFilters()
              }}
              startIcon={<FilterAltOffIcon style={{ fontSize: 13 }} />}
              sx={{
                fontSize: '0.7rem',
                fontWeight: 700,
                textTransform: 'none',
                color: '#ef4444',
                backgroundColor: '#fef2f2',
                borderRadius: '6px',
                py: 0.2,
                px: 1,
                border: '1px solid #fca5a5',
                '&:hover': { backgroundColor: '#fee2e2' },
              }}
            >
              Reset Column Filters
            </Button>
          )}
        </Box>

        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          <Box onClick={(e) => e.stopPropagation()}>
            <TextField
              size="small"
              placeholder="Filter roles..."
              value={roleSearchQuery}
              onChange={(e) => setRoleSearchQuery(e.target.value)}
              InputProps={{
                startAdornment: (
                  <SearchIcon sx={{ color: '#94a3b8', mr: 0.8, fontSize: 16 }} />
                ),
              }}
              sx={{
                width: 210,
                '& .MuiOutlinedInput-root': {
                  borderRadius: '8px',
                  backgroundColor: '#f8fafc',
                  '& fieldset': { borderColor: '#cbd5e1' },
                  '&:hover fieldset': { borderColor: '#94a3b8' },
                  '&.Mui-focused': {
                    backgroundColor: '#ffffff',
                    '& fieldset': { borderColor: '#0284c7', borderWidth: '1.5px' },
                    boxShadow: '0 0 0 3px rgba(2, 132, 199, 0.12)',
                  },
                },
                '& .MuiInputBase-input': {
                  fontSize: '0.78rem',
                  py: 0.75,
                  color: '#0f172a !important',
                  fontWeight: 600,
                  '&::placeholder': {
                    color: '#64748b',
                    opacity: 1,
                  },
                },
              }}
            />
          </Box>
          <Button
            variant="outlined"
            color="inherit"
            size="small"
            startIcon={<RefreshIcon style={{ fontSize: 14 }} />}
            onClick={(e) => {
              e.stopPropagation()
              fetchRoles(roleSearchQuery)
            }}
            sx={{
              textTransform: 'none',
              borderRadius: '8px',
              fontWeight: 700,
              fontSize: '0.75rem',
              color: '#475569',
              borderColor: '#cbd5e1',
              height: '34px',
              '&:hover': { backgroundColor: '#f1f5f9', borderColor: '#94a3b8' },
            }}
          >
            Refresh
          </Button>

          <IconButton
            size="small"
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
      </Box>

      {/* CUSTOM MODERN DATA TABLE WITH STICKY HEADER, MULTI-COLUMN FILTERS & SCROLL */}
      <Collapse in={expanded} timeout="auto" unmountOnExit={false}>
        <TableContainer
          sx={{
            maxHeight: 380,
            overflowY: 'auto',
            borderRadius: '8px',
            border: '1px solid #cbd5e1',
            backgroundColor: '#ffffff',
            position: 'relative',

            // Custom modern scrollbar styling
            '&::-webkit-scrollbar': {
              width: '6px',
              height: '6px',
            },
            '&::-webkit-scrollbar-track': {
              backgroundColor: '#f1f5f9',
              borderRadius: '4px',
            },
            '&::-webkit-scrollbar-thumb': {
              backgroundColor: '#cbd5e1',
              borderRadius: '4px',
              '&:hover': {
                backgroundColor: '#0284c7',
              },
            },
          }}
        >
          {rolesLoading && (
            <Box
              sx={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                backgroundColor: 'rgba(255, 255, 255, 0.7)',
                zIndex: 10,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 1.5,
              }}
            >
              <CircularProgress size={22} sx={{ color: '#0284c7' }} />
              <Typography variant="body2" sx={{ fontWeight: 700, color: '#0284c7', fontSize: '0.8rem' }}>
                Loading roles...
              </Typography>
            </Box>
          )}

          <Table size="small" stickyHeader sx={{ minWidth: 600 }}>
            {/* Table Header - Frozen/Sticky on scroll with Column Filter Funnels */}
            <TableHead>
              <TableRow>
                {/* Role Name Header */}
                {(() => {
                  const isFiltered = Boolean(columnFilters.name)
                  const filterCount = columnFilters.name ? columnFilters.name.length : 0
                  return (
                    <TableCell
                      sx={{
                        position: 'sticky',
                        top: 0,
                        zIndex: 5,
                        backgroundColor: isFiltered ? '#e0f2fe' : '#f1f5f9',
                        fontWeight: 800,
                        color: isFiltered ? '#0369a1' : '#0f172a',
                        fontSize: '0.8rem',
                        py: 1.2,
                        px: 2,
                        borderBottom: isFiltered ? '2px solid #0284c7' : '2px solid #0284c7',
                        borderRight: '1px solid #cbd5e1',
                        width: '35%',
                        transition: 'background-color 0.2s ease',
                      }}
                    >
                      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <TableSortLabel
                          active={orderBy === 'name'}
                          direction={orderBy === 'name' ? order : 'asc'}
                          onClick={() => handleRequestSort('name')}
                          sx={{
                            fontWeight: 800,
                            color: isFiltered ? '#0369a1 !important' : '#0f172a !important',
                            '& .MuiTableSortLabel-icon': {
                              color: '#0284c7 !important',
                            },
                          }}
                        >
                          Role Name
                        </TableSortLabel>

                        {/* Filter Funnel Icon with Badge */}
                        <IconButton
                          size="small"
                          onClick={(e) => handleOpenFilterPopover(e, 'name')}
                          sx={{
                            p: 0.4,
                            color: isFiltered ? '#0284c7' : '#94a3b8',
                            backgroundColor: isFiltered ? '#bae6fd' : 'transparent',
                            '&:hover': {
                              color: '#0284c7',
                              backgroundColor: '#e0f2fe',
                            },
                          }}
                        >
                          <Badge
                            badgeContent={isFiltered ? filterCount : 0}
                            color="primary"
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
                  const filterCount = columnFilters.description ? columnFilters.description.length : 0
                  return (
                    <TableCell
                      sx={{
                        position: 'sticky',
                        top: 0,
                        zIndex: 5,
                        backgroundColor: isFiltered ? '#e0f2fe' : '#f1f5f9',
                        fontWeight: 800,
                        color: isFiltered ? '#0369a1' : '#0f172a',
                        fontSize: '0.8rem',
                        py: 1.2,
                        px: 2,
                        borderBottom: isFiltered ? '2px solid #0284c7' : '2px solid #0284c7',
                        borderRight: '1px solid #cbd5e1',
                        width: '45%',
                        transition: 'background-color 0.2s ease',
                      }}
                    >
                      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <TableSortLabel
                          active={orderBy === 'description'}
                          direction={orderBy === 'description' ? order : 'asc'}
                          onClick={() => handleRequestSort('description')}
                          sx={{
                            fontWeight: 800,
                            color: isFiltered ? '#0369a1 !important' : '#0f172a !important',
                            '& .MuiTableSortLabel-icon': {
                              color: '#0284c7 !important',
                            },
                          }}
                        >
                          Description
                        </TableSortLabel>

                        {/* Filter Funnel Icon with Badge */}
                        <IconButton
                          size="small"
                          onClick={(e) => handleOpenFilterPopover(e, 'description')}
                          sx={{
                            p: 0.4,
                            color: isFiltered ? '#0284c7' : '#94a3b8',
                            backgroundColor: isFiltered ? '#bae6fd' : 'transparent',
                            '&:hover': {
                              color: '#0284c7',
                              backgroundColor: '#e0f2fe',
                            },
                          }}
                        >
                          <Badge
                            badgeContent={isFiltered ? filterCount : 0}
                            color="primary"
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
                  align="center"
                  sx={{
                    position: 'sticky',
                    top: 0,
                    zIndex: 5,
                    backgroundColor: '#f1f5f9',
                    fontWeight: 800,
                    color: '#0f172a',
                    fontSize: '0.8rem',
                    py: 1.2,
                    px: 2,
                    borderBottom: '2px solid #0284c7',
                    width: '20%',
                  }}
                >
                  Actions
                </TableCell>
              </TableRow>
            </TableHead>

            {/* Table Body */}
            <TableBody>
              {filteredAndSortedRows.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={3} align="center" sx={{ py: 4 }}>
                    <Typography
                      variant="body2"
                      sx={{ color: '#94a3b8', fontStyle: 'italic', fontSize: '0.82rem' }}
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
                      {/* Role Name Cell with Tooltip */}
                      <TableCell
                        sx={{
                          py: 0.9,
                          px: 2,
                          borderRight: '1px solid #e2e8f0',
                          borderBottom: '1px solid #e2e8f0',
                        }}
                      >
                        <Tooltip title={`System Role: ${row.name}`} arrow placement="top">
                          <Chip
                            label={row.name}
                            size="small"
                            icon={<SecurityIcon style={{ fontSize: 13, color: '#0369a1' }} />}
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

                      {/* Description Cell with Tooltip */}
                      <TableCell
                        sx={{
                          py: 0.9,
                          px: 2,
                          borderRight: '1px solid #e2e8f0',
                          borderBottom: '1px solid #e2e8f0',
                        }}
                      >
                        <Tooltip title={row.description || 'System Realm Role'} arrow placement="top">
                          <Typography
                            variant="body2"
                            sx={{
                              color: '#475569',
                              fontSize: '0.8rem',
                              fontWeight: 500,
                              whiteSpace: 'nowrap',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              maxWidth: 320,
                              cursor: 'default',
                            }}
                          >
                            {row.description}
                          </Typography>
                        </Tooltip>
                      </TableCell>

                      {/* Actions Cell with Tooltips */}
                      <TableCell
                        align="center"
                        sx={{
                          py: 0.9,
                          px: 1.5,
                          borderBottom: '1px solid #e2e8f0',
                        }}
                      >
                        <Box sx={{ display: 'flex', gap: 0.8, justifyContent: 'center', alignItems: 'center' }}>
                          <Tooltip title={`Select "${row.name}" for assignment`} arrow placement="top">
                            <Button
                              size="small"
                              variant="outlined"
                              color="primary"
                              startIcon={<PersonAddIcon style={{ fontSize: 13 }} />}
                              onClick={() => handleAssignClick(row.rawRole, row.name)}
                              sx={{
                                textTransform: 'none',
                                fontWeight: 700,
                                borderRadius: '5px',
                                fontSize: '0.7rem',
                                py: 0.3,
                                px: 1,
                                minWidth: 'auto',
                                borderColor: '#38bdf8',
                                color: '#0284c7',
                                backgroundColor: '#f0f9ff',
                                '&:hover': {
                                  backgroundColor: '#e0f2fe',
                                  borderColor: '#0284c7',
                                },
                              }}
                            >
                              Assign
                            </Button>
                          </Tooltip>

                          <Tooltip title={`Delete "${row.name}" role from catalog`} arrow placement="top">
                            <Button
                              size="small"
                              variant="outlined"
                              color="error"
                              startIcon={<DeleteOutlineIcon style={{ fontSize: 13 }} />}
                              onClick={() => handleDeleteClick(row.name)}
                              sx={{
                                textTransform: 'none',
                                fontWeight: 700,
                                borderRadius: '5px',
                                fontSize: '0.7rem',
                                py: 0.3,
                                px: 1,
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
      </Collapse>

      {/* Column Filter Popover Instance */}
      {filterPopover && (
        <ColumnFilterPopover
          anchorEl={filterPopover.anchorEl}
          onClose={handleCloseFilterPopover}
          columnTitle={filterPopover.field === 'name' ? 'Role Name' : 'Description'}
          allValues={uniqueColumnValues[filterPopover.field] || []}
          selectedValues={columnFilters[filterPopover.field] || null}
          onApplyFilter={(vals) => handleApplyColumnFilter(filterPopover.field, vals)}
          onClearFilter={() => handleClearColumnFilter(filterPopover.field)}
        />
      )}
    </Paper>
  )
}

export default SystemRolesCatalogPanel
