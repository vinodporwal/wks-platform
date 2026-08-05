import React, { useState, useMemo } from 'react'
import {
  Box,
  Typography,
  Button,
  Chip,
  TextField,
  Autocomplete,
  Paper,
  CircularProgress,
  Collapse,
  IconButton,
  Tooltip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TableSortLabel,
  Select,
  MenuItem,
} from '@mui/material'
import GroupWorkIcon from '@mui/icons-material/GroupWork'
import SearchIcon from '@mui/icons-material/Search'
import FilterAltIcon from '@mui/icons-material/FilterAlt'
import FilterAltOffIcon from '@mui/icons-material/FilterAltOff'
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown'
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp'
import KeyboardArrowLeftIcon from '@mui/icons-material/KeyboardArrowLeft'
import KeyboardArrowRightIcon from '@mui/icons-material/KeyboardArrowRight'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import ColumnFilterPopover from './ColumnFilterPopover'

const UsersByRolesPanel = ({
  rolesFormattedForSelect = [],
  selectedRoles = [],
  setSelectedRoles,
  onFetchUsers,
  onClearUsers,
  usersData = [],
  loading = false,
  totalUsers = 0,
  page = 1,
  pageSize = 20,
  onPageChange,
  onPageSizeChange,
}) => {
  const [expanded, setExpanded] = useState(true)

  // 1. Column Sorting State
  const [orderBy, setOrderBy] = useState('username')
  const [order, setOrder] = useState('asc')

  // 2. Multi-Column Filter State: { [field]: string[] | null }
  const [columnFilters, setColumnFilters] = useState({})

  // 3. Active Filter Popover State: { field: string, anchorEl: HTMLElement } | null
  const [filterPopover, setFilterPopover] = useState(null)

  // Extract unique distinct values for each column for popovers
  const uniqueColumnValues = useMemo(() => {
    const values = {}
    const fields = ['username', 'fullName', 'email', 'matchedRoles', 'status']
    
    fields.forEach((field) => {
      const set = new Set()
      usersData.forEach((item) => {
        const userObj = item?.user || item
        if (field === 'username') {
          const u = userObj?.username || userObj?.id || ''
          if (u) set.add(u)
        } else if (field === 'fullName') {
          const fn = [userObj?.firstName, userObj?.lastName].filter(Boolean).join(' ')
          if (fn) set.add(fn)
        } else if (field === 'email') {
          if (userObj?.email) set.add(userObj.email)
        } else if (field === 'matchedRoles') {
          const roles = item?.matchedRoles || []
          roles.forEach((r) => {
            if (r) set.add(r)
          })
        } else if (field === 'status') {
          set.add(userObj?.enabled !== false ? 'Active' : 'Disabled')
        }
      })
      values[field] = Array.from(set).sort()
    })
    return values
  }, [usersData])

  // Filtered and sorted grid rows
  const filteredAndSortedRows = useMemo(() => {
    let result = usersData.map((item, idx) => {
      const userObj = item?.user || item
      const matchedRoles = item?.matchedRoles || []
      const fullName = [userObj?.firstName, userObj?.lastName].filter(Boolean).join(' ')
      return {
        raw: item,
        id: userObj?.id || userObj?.username || `user-${idx}`,
        username: userObj?.username || userObj?.id || '',
        fullName: fullName || 'N/A',
        email: userObj?.email || 'N/A',
        matchedRoles: matchedRoles,
        matchedRolesStr: matchedRoles.join(', '),
        status: userObj?.enabled !== false ? 'Active' : 'Disabled',
        enabled: userObj?.enabled !== false,
      }
    })

    // Multi-column filtering
    result = result.filter((row) => {
      for (const field of Object.keys(columnFilters)) {
        const selected = columnFilters[field]
        if (selected && Array.isArray(selected) && selected.length > 0) {
          if (field === 'matchedRoles') {
            const hasMatch = row.matchedRoles.some((r) => selected.includes(r))
            if (!hasMatch) return false
          } else {
            if (!selected.includes(row[field])) {
              return false
            }
          }
        }
      }
      return true
    })

    // Column Sorting
    result.sort((a, b) => {
      let valueA = (a[orderBy] || '').toString().toLowerCase()
      let valueB = (b[orderBy] || '').toString().toLowerCase()
      if (orderBy === 'matchedRoles') {
        valueA = a.matchedRolesStr.toLowerCase()
        valueB = b.matchedRolesStr.toLowerCase()
      }
      if (valueA < valueB) return order === 'asc' ? -1 : 1
      if (valueA > valueB) return order === 'asc' ? 1 : -1
      return 0
    })

    return result
  }, [usersData, columnFilters, orderBy, order])

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

  // Apply column filter
  const handleApplyColumnFilter = (field, selectedVals) => {
    setColumnFilters((prev) => ({
      ...prev,
      [field]: selectedVals,
    }))
  }

  // Clear single column filter
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

  const handleSearch = () => {
    if (onFetchUsers) {
      onFetchUsers(selectedRoles, 1, pageSize)
    }
  }

  const handlePaginationChange = (event, newPage) => {
    if (onPageChange) {
      onPageChange(newPage + 1)
    }
  }

  const handleRowsPerPageChange = (event) => {
    const newSize = parseInt(event.target.value, 10)
    if (onPageSizeChange) {
      onPageSizeChange(newSize)
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
        width: '100%',
        mt: '14px',
        mb: '14px',
      }}
    >
      {/* Header Bar */}
      <Box
        onClick={() => setExpanded(!expanded)}
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          cursor: 'pointer',
          userSelect: 'none',
          marginBottom: expanded ? '14px' : 0,
          flexWrap: 'wrap',
          gap: '10px',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <GroupWorkIcon sx={{ color: '#0284c7', fontSize: 20 }} />
          <Typography
            variant='subtitle2'
            sx={{ fontWeight: 800, color: '#0f172a', fontSize: '0.875rem' }}
          >
            Users by Roles Directory
          </Typography>
          <Tooltip
            title='Search and view users assigned to selected system roles.'
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

          {/* Grid Count Badge */}
          <Chip
            label={filteredAndSortedRows.length}
            size='small'
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
              size='small'
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

      {/* Collapsible Content */}
      <Collapse in={expanded} timeout='auto' unmountOnExit={false}>
        <Box sx={{ pt: 0.5 }}>
          {/* Role Selection & Search Bar */}
          <Box
            sx={{
              display: 'flex',
              gap: '12px',
              alignItems: 'flex-start',
              marginBottom: '14px',
              maxWidth: '800px',
              flexWrap: 'wrap',
            }}
          >
            <Autocomplete
              multiple
              options={rolesFormattedForSelect}
              value={selectedRoles}
              size='small'
              getOptionLabel={(option) =>
                typeof option === 'string'
                  ? option
                  : option?.name || option?.label || ''
              }
              isOptionEqualToValue={(option, value) => {
                const optName = typeof option === 'string' ? option : option?.name || option?.value
                const valName = typeof value === 'string' ? value : value?.name || value?.value
                return optName === valName
              }}
              onChange={(event, newValue) => {
                setSelectedRoles(newValue)
                setColumnFilters({})
                if (newValue && newValue.length > 0) {
                  if (onFetchUsers) {
                    onFetchUsers(newValue, 1, pageSize)
                  }
                } else {
                  if (onClearUsers) {
                    onClearUsers()
                  }
                }
              }}
              sx={{ flexGrow: 1, minWidth: '280px' }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label='Select Roles'
                  placeholder='Choose roles...'
                  size='small'
                  sx={{
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
                    },
                    '& .MuiInputLabel-root': {
                      fontSize: '0.8rem',
                      '&.Mui-focused': { color: '#0284c7', fontWeight: 700 },
                    },
                  }}
                />
              )}
              renderTags={(tagValue, getTagProps) =>
                tagValue.map((option, index) => {
                  const tagLabel =
                    typeof option === 'string'
                      ? option
                      : option?.name || option?.label || String(option)
                  return (
                    <Chip
                      {...getTagProps({ index })}
                      key={index}
                      label={tagLabel}
                      size='small'
                      sx={{
                        fontWeight: 700,
                        backgroundColor: '#0284c7',
                        color: '#ffffff',
                        borderRadius: '4px',
                        fontSize: '0.72rem',
                        height: '22px',
                        '& .MuiChip-deleteIcon': {
                          color: 'rgba(255,255,255,0.8)',
                          fontSize: 13,
                          '&:hover': { color: '#ffffff' },
                        },
                      }}
                    />
                  )
                })
              }
            />

            <Button
              variant='contained'
              disableElevation
              size='small'
              onClick={handleSearch}
              disabled={loading || selectedRoles.length === 0}
              startIcon={
                loading ? (
                  <CircularProgress size={14} color='inherit' />
                ) : (
                  <SearchIcon style={{ fontSize: 16 }} />
                )
              }
              sx={{
                textTransform: 'none',
                fontWeight: 700,
                backgroundColor: '#0284c7',
                color: '#ffffff',
                borderRadius: '8px',
                height: '38px',
                px: 2.5,
                fontSize: '0.8rem',
                '&:hover': { backgroundColor: '#0369a1' },
                '&:disabled': { backgroundColor: '#cbd5e1' },
              }}
            >
              {loading ? 'Fetching...' : 'Find Users'}
            </Button>
          </Box>

          {/* User Results Data Grid Container */}
          <Paper
            variant='outlined'
            sx={{
              borderRadius: '8px',
              borderColor: '#cbd5e1',
              overflow: 'hidden',
              backgroundColor: '#ffffff',
            }}
          >
            <TableContainer
              sx={{
                maxHeight: 'calc(100vh - 350px)',
                overflowY: 'auto',
                position: 'relative',
                '&::-webkit-scrollbar': {
                  width: '6px',
                  height: '6px',
                },
                '&::-webkit-scrollbar-track': {
                  backgroundColor: '#f1f5f9',
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
              {loading && (
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
                  <Typography
                    variant='body2'
                    sx={{ fontWeight: 700, color: '#0284c7', fontSize: '0.8rem' }}
                  >
                    Loading users by roles...
                  </Typography>
                </Box>
              )}

              <Table size='small' stickyHeader sx={{ minWidth: 650 }}>
                {/* Sticky Header with Sorting & Column Filters */}
                <TableHead>
                  <TableRow>
                    {[
                      { id: 'username', label: 'Username', width: '22%' },
                      { id: 'fullName', label: 'Full Name', width: '22%' },
                      { id: 'email', label: 'Email', width: '26%' },
                      { id: 'matchedRoles', label: 'Matched Roles', width: '18%' },
                      { id: 'status', label: 'Status', width: '12%', align: 'center' },
                    ].map((col) => {
                      const isFiltered = Boolean(columnFilters[col.id])
                      return (
                        <TableCell
                          key={col.id}
                          align={col.align || 'left'}
                          sx={{
                            position: 'sticky',
                            top: 0,
                            zIndex: 5,
                            backgroundColor: isFiltered ? '#e0f2fe' : '#f1f5f9',
                            fontWeight: 800,
                            color: isFiltered ? '#0369a1' : '#0f172a',
                            fontSize: '0.8rem',
                            py: 1.2,
                            px: 1.5,
                            borderBottom: '2px solid #0284c7',
                            borderRight: '1px solid #cbd5e1',
                            width: col.width,
                            transition: 'background-color 0.2s ease',
                          }}
                        >
                          <Box
                            sx={{
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: col.align === 'center' ? 'center' : 'space-between',
                              gap: 0.5,
                            }}
                          >
                            <TableSortLabel
                              active={orderBy === col.id}
                              direction={orderBy === col.id ? order : 'asc'}
                              onClick={() => handleRequestSort(col.id)}
                              sx={{
                                fontWeight: 800,
                                color: isFiltered ? '#0369a1 !important' : '#0f172a !important',
                                '& .MuiTableSortLabel-icon': {
                                  color: '#0284c7 !important',
                                },
                              }}
                            >
                              {col.label}
                            </TableSortLabel>

                            <IconButton
                              size='small'
                              onClick={(e) => handleOpenFilterPopover(e, col.id)}
                              sx={{
                                p: 0.4,
                                color: isFiltered ? '#0284c7' : '#94a3b8',
                                backgroundColor: isFiltered ? '#bae6fd' : 'transparent',
                                '&:hover': {
                                  color: '#0284c7',
                                  backgroundColor: isFiltered ? '#7dd3fc' : '#e2e8f0',
                                },
                              }}
                            >
                              <FilterAltIcon sx={{ fontSize: 14 }} />
                            </IconButton>
                          </Box>
                        </TableCell>
                      )
                    })}
                  </TableRow>
                </TableHead>

                {/* Table Body */}
                <TableBody>
                  {filteredAndSortedRows.length > 0 ? (
                    filteredAndSortedRows.map((row) => (
                      <TableRow
                        key={row.id}
                        hover
                        sx={{
                          '&:nth-of-type(even)': { backgroundColor: '#f8fafc' },
                          '&:hover': { backgroundColor: '#f1f5f9' },
                        }}
                      >
                        <TableCell
                          sx={{
                            fontWeight: 700,
                            color: '#0f172a',
                            fontSize: '0.78rem',
                            borderRight: '1px solid #e2e8f0',
                          }}
                        >
                          {row.username}
                        </TableCell>

                        <TableCell
                          sx={{
                            color: '#334155',
                            fontSize: '0.78rem',
                            borderRight: '1px solid #e2e8f0',
                          }}
                        >
                          {row.fullName}
                        </TableCell>

                        <TableCell
                          sx={{
                            color: '#64748b',
                            fontSize: '0.78rem',
                            borderRight: '1px solid #e2e8f0',
                          }}
                        >
                          {row.email}
                        </TableCell>

                        <TableCell sx={{ borderRight: '1px solid #e2e8f0' }}>
                          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                            {row.matchedRoles.length > 0 ? (
                              row.matchedRoles.map((r, rIdx) => (
                                <Chip
                                  key={rIdx}
                                  label={r}
                                  size='small'
                                  sx={{
                                    fontWeight: 700,
                                    backgroundColor: '#e0f2fe',
                                    color: '#0369a1',
                                    borderRadius: '4px',
                                    fontSize: '0.7rem',
                                    height: '20px',
                                    border: '1px solid #bae6fd',
                                  }}
                                />
                              ))
                            ) : (
                              <Typography variant='caption' sx={{ color: '#94a3b8' }}>
                                —
                              </Typography>
                            )}
                          </Box>
                        </TableCell>

                        <TableCell align='center'>
                          <Chip
                            label={row.status}
                            size='small'
                            sx={{
                              fontWeight: 700,
                              fontSize: '0.68rem',
                              height: '20px',
                              backgroundColor: row.enabled ? '#dcfce7' : '#fee2e2',
                              color: row.enabled ? '#15803d' : '#b91c1c',
                            }}
                          />
                        </TableCell>
                      </TableRow>
                    ))
                  ) : (
                    <TableRow>
                      <TableCell colSpan={5} align='center' sx={{ py: 3 }}>
                        <Typography
                          variant='caption'
                          sx={{
                            color: '#94a3b8',
                            fontStyle: 'italic',
                            fontSize: '0.78rem',
                          }}
                        >
                          {usersData.length === 0
                            ? 'Select roles to view users.'
                            : 'No matching users found for active column filters.'}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>

            {/* Clean Pagination Footer Bar */}
            {usersData.length > 0 && (
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  px: 2,
                  py: 1,
                  borderTop: '1px solid #e2e8f0',
                  backgroundColor: '#f8fafc',
                  flexWrap: 'wrap',
                  gap: 1,
                }}
              >
                <Typography
                  variant='caption'
                  sx={{ color: '#475569', fontWeight: 700, fontSize: '0.75rem' }}
                >
                  Showing {filteredAndSortedRows.length > 0 ? (page - 1) * pageSize + 1 : 0}–
                  {Math.min(page * pageSize, totalUsers)} of {totalUsers} users
                </Typography>

                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Typography
                      variant='caption'
                      sx={{ color: '#64748b', fontWeight: 600, fontSize: '0.75rem' }}
                    >
                      Rows per page:
                    </Typography>
                    <Select
                      size='small'
                      value={pageSize}
                      onChange={handleRowsPerPageChange}
                      sx={{
                        height: '28px',
                        fontSize: '0.75rem',
                        fontWeight: 700,
                        color: '#0f172a',
                        backgroundColor: '#ffffff',
                        borderRadius: '6px',
                        '& .MuiOutlinedInput-notchedOutline': { borderColor: '#cbd5e1' },
                        '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: '#0284c7' },
                        '& .MuiSelect-select': { py: 0.2, px: 1 },
                      }}
                    >
                      {[5, 10, 20, 50].map((option) => (
                        <MenuItem key={option} value={option} sx={{ fontSize: '0.75rem', fontWeight: 600 }}>
                          {option}
                        </MenuItem>
                      ))}
                    </Select>
                  </Box>

                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <IconButton
                      size='small'
                      onClick={(e) => handlePaginationChange(e, page - 2)}
                      disabled={page <= 1}
                      sx={{
                        p: '4px',
                        borderRadius: '6px',
                        border: '1px solid #cbd5e1',
                        backgroundColor: '#ffffff',
                        color: '#0284c7',
                        '&:hover': { backgroundColor: '#e0f2fe', borderColor: '#0284c7' },
                        '&.Mui-disabled': { color: '#cbd5e1', borderColor: '#e2e8f0', backgroundColor: '#f8fafc' },
                      }}
                    >
                      <KeyboardArrowLeftIcon sx={{ fontSize: 18 }} />
                    </IconButton>

                    <Typography
                      variant='caption'
                      sx={{ color: '#0f172a', fontWeight: 700, fontSize: '0.75rem', px: 1 }}
                    >
                      {page} / {Math.ceil(totalUsers / pageSize) || 1}
                    </Typography>

                    <IconButton
                      size='small'
                      onClick={(e) => handlePaginationChange(e, page)}
                      disabled={page * pageSize >= totalUsers}
                      sx={{
                        p: '4px',
                        borderRadius: '6px',
                        border: '1px solid #cbd5e1',
                        backgroundColor: '#ffffff',
                        color: '#0284c7',
                        '&:hover': { backgroundColor: '#e0f2fe', borderColor: '#0284c7' },
                        '&.Mui-disabled': { color: '#cbd5e1', borderColor: '#e2e8f0', backgroundColor: '#f8fafc' },
                      }}
                    >
                      <KeyboardArrowRightIcon sx={{ fontSize: 18 }} />
                    </IconButton>
                  </Box>
                </Box>
              </Box>
            )}
          </Paper>

          {/* Column Filter Popover Instance */}
          {filterPopover && (
            <ColumnFilterPopover
              anchorEl={filterPopover.anchorEl}
              onClose={handleCloseFilterPopover}
              columnTitle={
                filterPopover.field === 'username'
                  ? 'Username'
                  : filterPopover.field === 'fullName'
                  ? 'Full Name'
                  : filterPopover.field === 'email'
                  ? 'Email'
                  : filterPopover.field === 'matchedRoles'
                  ? 'Matched Roles'
                  : 'Status'
              }
              allValues={uniqueColumnValues[filterPopover.field] || []}
              selectedValues={columnFilters[filterPopover.field] || null}
              onApplyFilter={(vals) => handleApplyColumnFilter(filterPopover.field, vals)}
              onClearFilter={() => handleClearColumnFilter(filterPopover.field)}
            />
          )}
        </Box>
      </Collapse>
    </Paper>
  )
}

export default UsersByRolesPanel
