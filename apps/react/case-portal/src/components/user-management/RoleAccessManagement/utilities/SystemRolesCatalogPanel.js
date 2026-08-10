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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Autocomplete,
  Checkbox,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import SearchIcon from '@mui/icons-material/Search'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import EditIcon from '@mui/icons-material/Edit'
import SecurityIcon from '@mui/icons-material/Security'
import FilterAltIcon from '@mui/icons-material/FilterAlt'
import FilterAltOffIcon from '@mui/icons-material/FilterAltOff'
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown'
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import DesktopWindowsIcon from '@mui/icons-material/DesktopWindows'
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank'
import CheckBoxIcon from '@mui/icons-material/CheckBox'
import SelectAllIcon from '@mui/icons-material/SelectAll'
import DeselectIcon from '@mui/icons-material/Deselect'
import { formatGridRows } from './roleUtils'
import ColumnFilterPopover from './ColumnFilterPopover'

const SystemRolesCatalogPanel = ({
  filteredRolesList,
  roleSearchQuery,
  setRoleSearchQuery,
  fetchRoles,
  rolesLoading,
  availableScreens = [],
  handleUpdateRole,
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

  // Edit Role & Screens Modal State
  const [editDialogOpen, setEditDialogOpen] = useState(false)
  const [editingRoleName, setEditingRoleName] = useState('')
  const [editingDescription, setEditingDescription] = useState('')
  const [editingScreens, setEditingScreens] = useState([])
  const [updatingRole, setUpdatingRole] = useState(false)

  const rows = useMemo(
    () => formatGridRows(filteredRolesList),
    [filteredRolesList],
  )

  // Screen display name resolver lookup (returns screenValue for display)
  const getScreenDisplayName = (screenCode) => {
    const found = availableScreens.find(
      (s) =>
        (typeof s === 'string' ? s : s.screenCode) === screenCode ||
        (typeof s !== 'string' && s.screenValue === screenCode),
    )
    if (found) {
      return typeof found === 'string'
        ? found
        : found.screenValue || found.screenDisplayName || found.screenCode
    }
    return screenCode
  }

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

  const handleEditClick = (row) => {
    setEditingRoleName(row.name)
    setEditingDescription(
      row.description === 'System Realm Role' ? '' : row.description || '',
    )
    setEditingScreens(Array.isArray(row.screens) ? row.screens : [])
    setEditDialogOpen(true)
  }

  const handleSaveEditRole = async () => {
    if (!editingRoleName || typeof handleUpdateRole !== 'function') return
    setUpdatingRole(true)
    try {
      await handleUpdateRole(editingRoleName, {
        description: editingDescription.trim(),
        screens: editingScreens,
      })
      setEditDialogOpen(false)
    } catch (err) {
      console.error('Failed to update role:', err)
    } finally {
      setUpdatingRole(false)
    }
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
            title='View, filter, sort, and manage system roles and their assigned screen permissions.'
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
            <Table size='small' stickyHeader sx={{ minWidth: 700 }}>
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
                          width: '25%',
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

                  {/* Assigned Screens Header */}
                  <TableCell
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
                      borderRight: '1px solid #cbd5e1',
                      width: '25%',
                    }}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.8 }}>
                      <DesktopWindowsIcon sx={{ color: '#0284c7', fontSize: 16 }} />
                      <Typography variant='caption' sx={{ fontWeight: 800, color: '#0f172a', fontSize: '0.8rem' }}>
                        Assigned Screens
                      </Typography>
                    </Box>
                  </TableCell>

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
                    <TableCell colSpan={4} align='center' sx={{ py: 6 }}>
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
                    <TableCell colSpan={4} align='center' sx={{ py: 4 }}>
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
                    const screensList = Array.isArray(row.screens) ? row.screens : []
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

                        {/* Assigned Screens Cell */}
                        <TableCell
                          sx={{
                            py: 0.9,
                            px: 1.5,
                            borderRight: '1px solid #e2e8f0',
                            borderBottom: '1px solid #e2e8f0',
                          }}
                        >
                          {screensList.length === 0 ? (
                            <Typography
                              variant='caption'
                              sx={{ color: '#94a3b8', fontStyle: 'italic', fontSize: '0.72rem' }}
                            >
                              No screens assigned
                            </Typography>
                          ) : (
                            <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', alignItems: 'center' }}>
                              {screensList.slice(0, 3).map((screenVal) => (
                                <Chip
                                  key={screenVal}
                                  label={getScreenDisplayName(screenVal)}
                                  size='small'
                                  sx={{
                                    fontSize: '0.68rem',
                                    fontWeight: 600,
                                    height: '20px',
                                    backgroundColor: '#f1f5f9',
                                    color: '#334155',
                                    border: '1px solid #cbd5e1',
                                    borderRadius: '4px',
                                  }}
                                />
                              ))}
                              {screensList.length > 3 && (
                                <Tooltip
                                  title={`Assigned screens (${screensList.length}): ${screensList
                                    .map(getScreenDisplayName)
                                    .join(', ')}`}
                                  arrow
                                  placement='top'
                                >
                                  <Chip
                                    label={`+${screensList.length - 3} more`}
                                    size='small'
                                    sx={{
                                      fontSize: '0.68rem',
                                      fontWeight: 700,
                                      height: '20px',
                                      backgroundColor: '#e0f2fe',
                                      color: '#0369a1',
                                      border: '1px solid #bae6fd',
                                      borderRadius: '4px',
                                      cursor: 'pointer',
                                    }}
                                  />
                                </Tooltip>
                              )}
                            </Box>
                          )}
                        </TableCell>

                        {/* Actions Cell (Edit & Delete) */}
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
                              gap: 0.8,
                            }}
                          >
                            <Tooltip
                              title={`Edit description & assigned screens for "${row.name}"`}
                              arrow
                              placement='top'
                            >
                              <Button
                                size='small'
                                variant='outlined'
                                color='primary'
                                startIcon={
                                  <EditIcon style={{ fontSize: 13 }} />
                                }
                                onClick={() => handleEditClick(row)}
                                sx={{
                                  textTransform: 'none',
                                  fontWeight: 700,
                                  borderRadius: '5px',
                                  fontSize: '0.7rem',
                                  py: 0.3,
                                  px: 1,
                                  minWidth: 'auto',
                                  borderColor: '#bae6fd',
                                  color: '#0284c7',
                                  backgroundColor: '#f0f9ff',
                                  '&:hover': {
                                    backgroundColor: '#e0f2fe',
                                    borderColor: '#0284c7',
                                  },
                                }}
                              >
                                Edit
                              </Button>
                            </Tooltip>

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

      {/* Edit Role & Screens Modal Dialog */}
      <Dialog
        open={editDialogOpen}
        onClose={() => !updatingRole && setEditDialogOpen(false)}
        maxWidth='sm'
        fullWidth
        PaperProps={{
          sx: { borderRadius: '12px', padding: '8px' },
        }}
      >
        <DialogTitle sx={{ fontWeight: 800, color: '#0f172a', fontSize: '1rem', pb: 1 }}>
          Edit Role: <span style={{ color: '#0284c7' }}>{editingRoleName}</span>
        </DialogTitle>

        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1.5 }}>
          <TextField
            label='Role Description'
            placeholder='Optional description'
            size='small'
            fullWidth
            value={editingDescription}
            onChange={(e) => setEditingDescription(e.target.value)}
            sx={{
              '& .MuiOutlinedInput-root': {
                borderRadius: '8px',
                backgroundColor: '#f8fafc',
                '& fieldset': { borderColor: '#cbd5e1' },
                '&:hover fieldset': { borderColor: '#94a3b8' },
                '&.Mui-focused fieldset': { borderColor: '#0284c7' },
              },
            }}
          />

          <Box
            sx={{
              p: 1.5,
              borderRadius: '8px',
              border: '1px solid #e2e8f0',
              backgroundColor: '#f8fafc',
              display: 'flex',
              flexDirection: 'column',
              gap: 1.5,
            }}
          >
            <Box
              sx={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <Typography variant='caption' sx={{ fontWeight: 700, color: '#0f172a' }}>
                Assigned Accessible Screens ({editingScreens.length})
              </Typography>
              {availableScreens.length > 0 && (
                <Box sx={{ display: 'flex', gap: 1 }}>
                  <Button
                    size='small'
                    onClick={() => {
                      const allCodes = availableScreens.map((s) =>
                        typeof s === 'string' ? s : s.screenCode || s.screenValue,
                      )
                      setEditingScreens(allCodes)
                    }}
                    startIcon={<SelectAllIcon sx={{ fontSize: 14 }} />}
                    sx={{ fontSize: '0.7rem', fontWeight: 700, textTransform: 'none', color: '#0284c7' }}
                  >
                    Select All
                  </Button>
                  {editingScreens.length > 0 && (
                    <Button
                      size='small'
                      onClick={() => setEditingScreens([])}
                      startIcon={<DeselectIcon sx={{ fontSize: 14 }} />}
                      sx={{ fontSize: '0.7rem', fontWeight: 700, textTransform: 'none', color: '#64748b' }}
                    >
                      Clear
                    </Button>
                  )}
                </Box>
              )}
            </Box>

            <Autocomplete
              multiple
              disableCloseOnSelect
              size='small'
              options={availableScreens}
              getOptionLabel={(option) => {
                if (typeof option === 'string') {
                  const found = availableScreens.find(
                    (s) => s.screenCode === option || s.screenValue === option,
                  )
                  return found
                    ? found.screenValue || found.screenDisplayName || option
                    : option
                }
                return option.screenValue || option.screenDisplayName || ''
              }}
              isOptionEqualToValue={(option, val) => {
                const optCode =
                  typeof option === 'string'
                    ? option
                    : option.screenCode || option.screenValue
                const valCode =
                  typeof val === 'string'
                    ? val
                    : val.screenCode || val.screenValue
                return optCode === valCode
              }}
              value={availableScreens.filter((s) => {
                const code =
                  typeof s === 'string' ? s : s.screenCode || s.screenValue
                return editingScreens.includes(code)
              })}
              onChange={(event, newValue) => {
                const selectedCodes = newValue.map((item) =>
                  typeof item === 'string'
                    ? item
                    : item.screenCode || item.screenValue,
                )
                setEditingScreens(selectedCodes)
              }}
              renderOption={(props, option, { selected }) => {
                const displayLabel =
                  typeof option === 'string'
                    ? availableScreens.find((s) => s.screenCode === option)
                        ?.screenValue || option
                    : option.screenValue || option.screenDisplayName
                return (
                  <li
                    {...props}
                    key={
                      typeof option === 'string'
                        ? option
                        : option.screenCode || option.screenValue
                    }
                  >
                    <Checkbox
                      icon={<CheckBoxOutlineBlankIcon fontSize='small' />}
                      checkedIcon={<CheckBoxIcon fontSize='small' />}
                      style={{ marginRight: 8 }}
                      checked={selected}
                    />
                    <Typography
                      variant='body2'
                      sx={{ fontWeight: 600, fontSize: '0.8rem', color: '#0f172a' }}
                    >
                      {displayLabel}
                    </Typography>
                  </li>
                )
              }}
              renderTags={(value, getTagProps) =>
                value.map((option, index) => {
                  const label =
                    typeof option === 'string'
                      ? availableScreens.find((s) => s.screenCode === option)
                          ?.screenValue || option
                      : option.screenValue || option.screenDisplayName
                  const key =
                    typeof option === 'string'
                      ? option
                      : option.screenCode || option.screenValue
                  return (
                    <Chip
                      {...getTagProps({ index })}
                      key={key}
                      label={label}
                      size='small'
                      sx={{
                        fontWeight: 600,
                        fontSize: '0.72rem',
                        backgroundColor: '#e0f2fe',
                        color: '#0369a1',
                        borderRadius: '6px',
                        border: '1px solid #bae6fd',
                        height: '24px',
                      }}
                    />
                  )
                })
              }
              renderInput={(params) => (
                <TextField
                  {...params}
                  placeholder={editingScreens.length === 0 ? 'Select screens...' : ''}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '8px',
                      backgroundColor: '#ffffff',
                      '& fieldset': { borderColor: '#cbd5e1' },
                    },
                  }}
                />
              )}
              sx={{ width: '100%' }}
            />
          </Box>
        </DialogContent>

        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button
            size='small'
            onClick={() => setEditDialogOpen(false)}
            disabled={updatingRole}
            sx={{ textTransform: 'none', color: '#64748b', fontWeight: 600 }}
          >
            Cancel
          </Button>
          <Button
            size='small'
            variant='contained'
            onClick={handleSaveEditRole}
            disabled={updatingRole}
            startIcon={updatingRole ? <CircularProgress size={14} color='inherit' /> : null}
            sx={{
              textTransform: 'none',
              fontWeight: 700,
              borderRadius: '8px',
              backgroundColor: '#0284c7',
              '&:hover': { backgroundColor: '#0369a1' },
            }}
          >
            {updatingRole ? 'Saving...' : 'Save Changes'}
          </Button>
        </DialogActions>
      </Dialog>
    </Paper>
  )
}

export default SystemRolesCatalogPanel
