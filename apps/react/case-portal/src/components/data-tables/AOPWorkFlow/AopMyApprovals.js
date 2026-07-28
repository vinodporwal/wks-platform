import React, { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  IconButton,
  InputAdornment,
  MenuItem,
  Paper,
  Select,
  Snackbar,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import ArrowForwardIcon from '@mui/icons-material/ArrowForward'
import RefreshIcon from '@mui/icons-material/Refresh'
import FactCheckIcon from '@mui/icons-material/FactCheck'
import SearchIcon from '@mui/icons-material/Search'
import ClearIcon from '@mui/icons-material/Clear'
import FilterListIcon from '@mui/icons-material/FilterList'
import FilterAltOffIcon from '@mui/icons-material/FilterAltOff'
import { useDispatch } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { AopApprovalService } from 'services/AopApprovalService'
import { DataService } from 'services/DataService'
import { setVerticalChangeFromDashboard } from 'store/reducers/dataGridStore'

/**
 * "My Approvals" inbox — displays pending AOP workflows.
 * Renders a high-quality bordered grid with column filters, search bar, and "GO TO PLANT" action button.
 */
const AopMyApprovals = ({ onClose }) => {
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const keycloak = useSession()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [sitesData, setSitesData] = useState(null)
  const [snack, setSnack] = useState({
    open: false,
    message: '',
    severity: 'info',
  })

  // Search & Filter state
  const [searchQuery, setSearchQuery] = useState('')
  const [showFilters, setShowFilters] = useState(true)
  const [columnFilters, setColumnFilters] = useState({
    plant: '',
    site: 'ALL',
    vertical: 'ALL',
    year: 'ALL',
    stage: 'ALL',
    role: 'ALL',
  })

  const load = async () => {
    setLoading(true)
    try {
      const data = await AopApprovalService.getMyPending(keycloak)
      setItems(Array.isArray(data) ? data : [])
    } catch (e) {
      setSnack({
        open: true,
        message: e.message || 'Failed to load approvals',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  // Pre-fetch sites hierarchy to map plant -> site -> vertical IDs if needed
  useEffect(() => {
    let active = true
    const fetchSites = async () => {
      try {
        const res = await DataService.getAllSites(keycloak)
        if (active && Array.isArray(res)) {
          setSitesData(res)
        }
      } catch (err) {
        console.warn('Failed to pre-fetch sites hierarchy:', err)
      }
    }
    fetchSites()
    return () => {
      active = false
    }
  }, [keycloak])

  const handleColumnFilterChange = (field, value) => {
    setColumnFilters((prev) => ({
      ...prev,
      [field]: value,
    }))
  }

  const handleClearAllFilters = () => {
    setSearchQuery('')
    setColumnFilters({
      plant: '',
      site: 'ALL',
      vertical: 'ALL',
      year: 'ALL',
      stage: 'ALL',
      role: 'ALL',
    })
  }

  const isFilterActive = useMemo(() => {
    return (
      searchQuery.trim() !== '' ||
      columnFilters.plant.trim() !== '' ||
      columnFilters.site !== 'ALL' ||
      columnFilters.vertical !== 'ALL' ||
      columnFilters.year !== 'ALL' ||
      columnFilters.stage !== 'ALL' ||
      columnFilters.role !== 'ALL'
    )
  }, [searchQuery, columnFilters])

  // Unique options for column filter dropdowns
  const uniqueSites = useMemo(() => {
    const set = new Set(items.map((i) => i.siteName).filter(Boolean))
    return Array.from(set).sort()
  }, [items])

  const uniqueVerticals = useMemo(() => {
    const set = new Set(items.map((i) => i.verticalName).filter(Boolean))
    return Array.from(set).sort()
  }, [items])

  const uniqueYears = useMemo(() => {
    const set = new Set(items.map((i) => String(i.year)).filter(Boolean))
    return Array.from(set).sort()
  }, [items])

  const uniqueStages = useMemo(() => {
    const set = new Set(
      items.map((i) => i.gateDisplayName || i.gateName).filter(Boolean),
    )
    return Array.from(set).sort()
  }, [items])

  const uniqueRoles = useMemo(() => {
    const set = new Set(items.map((i) => i.assignedRole).filter(Boolean))
    return Array.from(set).sort()
  }, [items])

  // Filtered items computation
  const filteredItems = useMemo(() => {
    return items.filter((row) => {
      const plantStr = (row.plantName || row.plantId || '').toString().toLowerCase()
      const siteStr = (row.siteName || '').toString().toLowerCase()
      const verticalStr = (row.verticalName || '').toString().toLowerCase()
      const yearStr = String(row.year || '').toLowerCase()
      const stageStr = (row.gateDisplayName || row.gateName || '')
        .toString()
        .toLowerCase()
      const roleStr = (row.assignedRole || '').toString().toLowerCase()

      // 1. Global search query filter
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim()
        const matchesGlobal =
          plantStr.includes(q) ||
          siteStr.includes(q) ||
          verticalStr.includes(q) ||
          yearStr.includes(q) ||
          stageStr.includes(q) ||
          roleStr.includes(q)

        if (!matchesGlobal) return false
      }

      // 2. Column-specific filters
      if (
        columnFilters.plant.trim() &&
        !plantStr.includes(columnFilters.plant.toLowerCase().trim())
      ) {
        return false
      }

      if (
        columnFilters.site !== 'ALL' &&
        (row.siteName || '') !== columnFilters.site
      ) {
        return false
      }

      if (
        columnFilters.vertical !== 'ALL' &&
        (row.verticalName || '') !== columnFilters.vertical
      ) {
        return false
      }

      if (
        columnFilters.year !== 'ALL' &&
        String(row.year || '') !== columnFilters.year
      ) {
        return false
      }

      if (
        columnFilters.stage !== 'ALL' &&
        (row.gateDisplayName || row.gateName || '') !== columnFilters.stage
      ) {
        return false
      }

      if (
        columnFilters.role !== 'ALL' &&
        (row.assignedRole || '') !== columnFilters.role
      ) {
        return false
      }

      return true
    })
  }, [items, searchQuery, columnFilters])

  /**
   * Navigate to plant by dispatching setVerticalChangeFromDashboard to Redux
   * and calling React Router navigate for pages like /workflow.
   * Checks whether all required parameters (v_id, sid, pid) exist before dispatching.
   */
  const handleGoToPlant = async (row) => {
    let pid = row.plantId || row.pid || row.plant_id || row.id
    let sid = row.siteId || row.sid || row.sId || row.site_id
    let v_id = row.verticalId || row.v_id || row.vid || row.vertical_id

    // If siteId or verticalId are missing directly on the row, search the hierarchy map
    if ((!sid || !v_id) && (pid || row.plantName)) {
      let hierarchy = sitesData
      if (!hierarchy) {
        try {
          hierarchy = await DataService.getAllSites(keycloak)
          if (Array.isArray(hierarchy)) setSitesData(hierarchy)
        } catch (e) {
          console.error('Error fetching sites hierarchy for plant lookup:', e)
        }
      }

      if (Array.isArray(hierarchy)) {
        for (const vertical of hierarchy) {
          const verticalId = vertical.id
          for (const site of vertical.sites || []) {
            const siteId = site.id
            for (const plant of site.plants || []) {
              const matchesId =
                pid && String(plant.id).toUpperCase() === String(pid).toUpperCase()
              const matchesName =
                row.plantName &&
                plant.displayName &&
                plant.displayName.toUpperCase() ===
                  String(row.plantName).toUpperCase()

              if (matchesId || matchesName) {
                if (!pid) pid = plant.id
                if (!sid) sid = siteId
                if (!v_id) v_id = verticalId
                break
              }
            }
            if (pid && sid && v_id) break
          }
          if (pid && sid && v_id) break
        }
      }
    }

    console.log('GO TO PLANT params evaluation:', { pid, sid, v_id, row })

    // Validate required parameters for navigation
    const missingParams = []
    if (!pid) missingParams.push('Plant ID (pid)')
    if (!sid) missingParams.push('Site ID (sid)')
    if (!v_id) missingParams.push('Vertical ID (v_id)')

    if (missingParams.length > 0) {
      setSnack({
        open: true,
        message: `Cannot navigate: Missing required parameters (${missingParams.join(', ')}).`,
        severity: 'error',
      })
      return
    }

    // All required params present -> dispatch Redux action (same as AopDashboard)
    dispatch(
      setVerticalChangeFromDashboard({
        v_id,
        sid,
        pid,
        trigger: Date.now(),
      }),
    )

    setSnack({
      open: true,
      message: `Redirecting to plant ${row.plantName || pid}...`,
      severity: 'success',
    })

    if (typeof onClose === 'function') {
      onClose()
    }

    // Direct navigation to /workflow route after setting plant context in Redux
    setTimeout(() => {
      navigate('/workflow')
    }, 100)
  }

  // Common select style for column filters
  const filterSelectSx = {
    height: '28px',
    fontSize: '0.78rem',
    backgroundColor: '#ffffff',
    borderRadius: '4px',
    '& .MuiSelect-select': {
      py: '3px',
      px: '8px',
    },
    '& .MuiOutlinedInput-notchedOutline': {
      borderColor: '#cbd5e1',
    },
    '&:hover .MuiOutlinedInput-notchedOutline': {
      borderColor: '#005eb8',
    },
  }

  const filterTextFieldSx = {
    '& .MuiOutlinedInput-root': {
      height: '28px',
      fontSize: '0.78rem',
      backgroundColor: '#ffffff',
      borderRadius: '4px',
      px: '6px',
      '& fieldset': {
        borderColor: '#cbd5e1',
      },
      '&:hover fieldset': {
        borderColor: '#005eb8',
      },
    },
    '& .MuiOutlinedInput-input': {
      py: '3px',
      px: '4px',
    },
  }

  return (
    <Box sx={{ width: '100%' }}>
      {/* Top Header & Search Bar */}
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        alignItems={{ xs: 'flex-start', sm: 'center' }}
        justifyContent='space-between'
        spacing={2}
        sx={{ mb: 2 }}
      >
        <Stack direction='row' alignItems='center' spacing={1.5}>
          <Typography
            variant='h6'
            sx={{
              fontWeight: 700,
              color: '#1e293b',
              fontSize: '1.05rem',
              fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
            }}
          >
            Pending Action Items
          </Typography>
          <Chip
            label={
              isFilterActive
                ? `${filteredItems.length} of ${items.length} Items`
                : `${items.length} ${items.length === 1 ? 'Item' : 'Items'}`
            }
            size='small'
            sx={{
              backgroundColor: isFilterActive ? '#fef3c7' : '#eff6ff',
              color: isFilterActive ? '#d97706' : '#0284c7',
              fontWeight: 700,
              fontSize: '0.75rem',
              border: `1px solid ${isFilterActive ? '#fde68a' : '#bae6fd'}`,
            }}
          />
        </Stack>

        <Stack direction='row' alignItems='center' spacing={1}>
          {/* Global Search Bar */}
          <TextField
            size='small'
            placeholder='Search all columns...'
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position='start'>
                  <SearchIcon sx={{ fontSize: 18, color: '#94a3b8' }} />
                </InputAdornment>
              ),
              endAdornment: searchQuery && (
                <InputAdornment position='end'>
                  <IconButton
                    size='small'
                    onClick={() => setSearchQuery('')}
                    edge='end'
                  >
                    <ClearIcon sx={{ fontSize: 15 }} />
                  </IconButton>
                </InputAdornment>
              ),
              sx: {
                height: '34px',
                fontSize: '0.82rem',
                borderRadius: '6px',
                backgroundColor: '#ffffff',
                width: { xs: '100%', sm: '220px' },
                fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
              },
            }}
          />

          {/* Toggle Column Filters */}
          <Tooltip title={showFilters ? 'Hide Column Filters' : 'Show Column Filters'}>
            <Button
              variant={showFilters ? 'contained' : 'outlined'}
              size='small'
              onClick={() => setShowFilters((prev) => !prev)}
              startIcon={<FilterListIcon sx={{ fontSize: '16px !important' }} />}
              sx={{
                height: '34px',
                textTransform: 'none',
                fontWeight: 600,
                fontSize: '0.8rem',
                borderRadius: '6px',
                backgroundColor: showFilters ? '#e2e8f0' : 'transparent',
                color: showFilters ? '#1e293b' : '#475569',
                borderColor: '#cbd5e1',
                boxShadow: 'none',
                '&:hover': {
                  backgroundColor: showFilters ? '#cbd5e1' : '#f1f5f9',
                  borderColor: '#94a3b8',
                  boxShadow: 'none',
                },
              }}
            >
              Filters
            </Button>
          </Tooltip>

          {/* Clear Filters Button */}
          {isFilterActive && (
            <Tooltip title='Reset all filters'>
              <IconButton
                size='small'
                onClick={handleClearAllFilters}
                sx={{
                  backgroundColor: '#fee2e2',
                  color: '#dc2626',
                  '&:hover': { backgroundColor: '#fca5a5' },
                  width: '34px',
                  height: '34px',
                  borderRadius: '6px',
                }}
              >
                <FilterAltOffIcon sx={{ fontSize: 18 }} />
              </IconButton>
            </Tooltip>
          )}

          {/* Refresh Button */}
          <Button
            variant='outlined'
            size='small'
            onClick={load}
            disabled={loading}
            startIcon={<RefreshIcon sx={{ fontSize: '16px !important' }} />}
            sx={{
              height: '34px',
              textTransform: 'none',
              fontWeight: 600,
              fontSize: '0.8rem',
              borderRadius: '6px',
              borderColor: '#cbd5e1',
              color: '#475569',
              fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
              '&:hover': {
                borderColor: '#005eb8',
                color: '#005eb8',
                backgroundColor: '#f0f7ff',
              },
            }}
          >
            Refresh
          </Button>
        </Stack>
      </Stack>

      {/* Grid Content */}
      {loading ? (
        <Stack alignItems='center' justifyContent='center' sx={{ py: 8 }}>
          <CircularProgress size={36} sx={{ color: '#005eb8' }} />
          <Typography
            variant='body2'
            sx={{ mt: 1.5, color: '#64748b', fontWeight: 500 }}
          >
            Loading approvals...
          </Typography>
        </Stack>
      ) : items.length === 0 ? (
        <Paper
          elevation={0}
          sx={{
            p: 5,
            textAlign: 'center',
            border: '1px solid #e2e8f0',
            borderRadius: '10px',
            backgroundColor: '#f8fafc',
          }}
        >
          <FactCheckIcon sx={{ fontSize: 40, color: '#94a3b8', mb: 1 }} />
          <Typography
            variant='subtitle1'
            sx={{ fontWeight: 600, color: '#475569' }}
          >
            No Pending Approvals
          </Typography>
          <Typography variant='body2' sx={{ color: '#94a3b8', mt: 0.5 }}>
            There are currently no items requiring your approval.
          </Typography>
        </Paper>
      ) : (
        <TableContainer
          component={Paper}
          elevation={0}
          sx={{
            border: '1px solid #cbd5e1',
            borderRadius: '8px',
            overflow: 'hidden',
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
          }}
        >
          <Table size='small' sx={{ borderCollapse: 'collapse' }}>
            <TableHead>
              {/* Column Titles */}
              <TableRow sx={{ backgroundColor: '#f1f5f9' }}>
                <TableCell
                  sx={{
                    fontWeight: 700,
                    fontSize: '0.78rem',
                    color: '#334155',
                    textTransform: 'uppercase',
                    letterSpacing: '0.04em',
                    borderBottom: showFilters ? '1px solid #e2e8f0' : '2px solid #cbd5e1',
                    borderRight: '1px solid #e2e8f0',
                    py: 1.2,
                    px: 1.5,
                  }}
                >
                  Plant
                </TableCell>
                <TableCell
                  sx={{
                    fontWeight: 700,
                    fontSize: '0.78rem',
                    color: '#334155',
                    textTransform: 'uppercase',
                    letterSpacing: '0.04em',
                    borderBottom: showFilters ? '1px solid #e2e8f0' : '2px solid #cbd5e1',
                    borderRight: '1px solid #e2e8f0',
                    py: 1.2,
                    px: 1.5,
                  }}
                >
                  Site
                </TableCell>
                <TableCell
                  sx={{
                    fontWeight: 700,
                    fontSize: '0.78rem',
                    color: '#334155',
                    textTransform: 'uppercase',
                    letterSpacing: '0.04em',
                    borderBottom: showFilters ? '1px solid #e2e8f0' : '2px solid #cbd5e1',
                    borderRight: '1px solid #e2e8f0',
                    py: 1.2,
                    px: 1.5,
                  }}
                >
                  Vertical
                </TableCell>
                <TableCell
                  align='center'
                  sx={{
                    fontWeight: 700,
                    fontSize: '0.78rem',
                    color: '#334155',
                    textTransform: 'uppercase',
                    letterSpacing: '0.04em',
                    borderBottom: showFilters ? '1px solid #e2e8f0' : '2px solid #cbd5e1',
                    borderRight: '1px solid #e2e8f0',
                    py: 1.2,
                    px: 1.5,
                  }}
                >
                  AOP Year
                </TableCell>
                <TableCell
                  sx={{
                    fontWeight: 700,
                    fontSize: '0.78rem',
                    color: '#334155',
                    textTransform: 'uppercase',
                    letterSpacing: '0.04em',
                    borderBottom: showFilters ? '1px solid #e2e8f0' : '2px solid #cbd5e1',
                    borderRight: '1px solid #e2e8f0',
                    py: 1.2,
                    px: 1.5,
                  }}
                >
                  Stage
                </TableCell>
                <TableCell
                  sx={{
                    fontWeight: 700,
                    fontSize: '0.78rem',
                    color: '#334155',
                    textTransform: 'uppercase',
                    letterSpacing: '0.04em',
                    borderBottom: showFilters ? '1px solid #e2e8f0' : '2px solid #cbd5e1',
                    borderRight: '1px solid #e2e8f0',
                    py: 1.2,
                    px: 1.5,
                  }}
                >
                  As Role
                </TableCell>
                <TableCell
                  align='center'
                  sx={{
                    fontWeight: 700,
                    fontSize: '0.78rem',
                    color: '#334155',
                    textTransform: 'uppercase',
                    letterSpacing: '0.04em',
                    borderBottom: showFilters ? '1px solid #e2e8f0' : '2px solid #cbd5e1',
                    py: 1.2,
                    px: 1.5,
                  }}
                >
                  Action
                </TableCell>
              </TableRow>

              {/* Column Filters Row */}
              {showFilters && (
                <TableRow sx={{ backgroundColor: '#f8fafc' }}>
                  {/* Plant Search Filter */}
                  <TableCell
                    sx={{
                      p: 0.75,
                      borderBottom: '2px solid #cbd5e1',
                      borderRight: '1px solid #e2e8f0',
                    }}
                  >
                    <TextField
                      fullWidth
                      placeholder='Filter plant...'
                      value={columnFilters.plant}
                      onChange={(e) =>
                        handleColumnFilterChange('plant', e.target.value)
                      }
                      sx={filterTextFieldSx}
                    />
                  </TableCell>

                  {/* Site Dropdown Filter */}
                  <TableCell
                    sx={{
                      p: 0.75,
                      borderBottom: '2px solid #cbd5e1',
                      borderRight: '1px solid #e2e8f0',
                    }}
                  >
                    <Select
                      fullWidth
                      value={columnFilters.site}
                      onChange={(e) =>
                        handleColumnFilterChange('site', e.target.value)
                      }
                      sx={filterSelectSx}
                    >
                      <MenuItem value='ALL'>All Sites</MenuItem>
                      {uniqueSites.map((site) => (
                        <MenuItem key={site} value={site}>
                          {site}
                        </MenuItem>
                      ))}
                    </Select>
                  </TableCell>

                  {/* Vertical Dropdown Filter */}
                  <TableCell
                    sx={{
                      p: 0.75,
                      borderBottom: '2px solid #cbd5e1',
                      borderRight: '1px solid #e2e8f0',
                    }}
                  >
                    <Select
                      fullWidth
                      value={columnFilters.vertical}
                      onChange={(e) =>
                        handleColumnFilterChange('vertical', e.target.value)
                      }
                      sx={filterSelectSx}
                    >
                      <MenuItem value='ALL'>All Verticals</MenuItem>
                      {uniqueVerticals.map((vert) => (
                        <MenuItem key={vert} value={vert}>
                          {vert}
                        </MenuItem>
                      ))}
                    </Select>
                  </TableCell>

                  {/* AOP Year Filter */}
                  <TableCell
                    sx={{
                      p: 0.75,
                      borderBottom: '2px solid #cbd5e1',
                      borderRight: '1px solid #e2e8f0',
                    }}
                  >
                    <Select
                      fullWidth
                      value={columnFilters.year}
                      onChange={(e) =>
                        handleColumnFilterChange('year', e.target.value)
                      }
                      sx={filterSelectSx}
                    >
                      <MenuItem value='ALL'>All Years</MenuItem>
                      {uniqueYears.map((yr) => (
                        <MenuItem key={yr} value={yr}>
                          {yr}
                        </MenuItem>
                      ))}
                    </Select>
                  </TableCell>

                  {/* Stage Dropdown Filter */}
                  <TableCell
                    sx={{
                      p: 0.75,
                      borderBottom: '2px solid #cbd5e1',
                      borderRight: '1px solid #e2e8f0',
                    }}
                  >
                    <Select
                      fullWidth
                      value={columnFilters.stage}
                      onChange={(e) =>
                        handleColumnFilterChange('stage', e.target.value)
                      }
                      sx={filterSelectSx}
                    >
                      <MenuItem value='ALL'>All Stages</MenuItem>
                      {uniqueStages.map((stg) => (
                        <MenuItem key={stg} value={stg}>
                          {stg}
                        </MenuItem>
                      ))}
                    </Select>
                  </TableCell>

                  {/* As Role Dropdown Filter */}
                  <TableCell
                    sx={{
                      p: 0.75,
                      borderBottom: '2px solid #cbd5e1',
                      borderRight: '1px solid #e2e8f0',
                    }}
                  >
                    <Select
                      fullWidth
                      value={columnFilters.role}
                      onChange={(e) =>
                        handleColumnFilterChange('role', e.target.value)
                      }
                      sx={filterSelectSx}
                    >
                      <MenuItem value='ALL'>All Roles</MenuItem>
                      {uniqueRoles.map((rl) => (
                        <MenuItem key={rl} value={rl}>
                          {rl}
                        </MenuItem>
                      ))}
                    </Select>
                  </TableCell>

                  {/* Action Column Filter Clear */}
                  <TableCell
                    align='center'
                    sx={{
                      p: 0.75,
                      borderBottom: '2px solid #cbd5e1',
                    }}
                  >
                    {isFilterActive && (
                      <Button
                        size='small'
                        onClick={handleClearAllFilters}
                        sx={{
                          fontSize: '0.72rem',
                          textTransform: 'none',
                          color: '#dc2626',
                          py: '2px',
                          px: '6px',
                          minWidth: 'auto',
                          fontWeight: 600,
                        }}
                      >
                        Clear
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              )}
            </TableHead>
            <TableBody>
              {filteredItems.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align='center' sx={{ py: 4 }}>
                    <Typography
                      variant='body2'
                      sx={{ color: '#64748b', fontWeight: 500 }}
                    >
                      No items match your filter criteria.
                    </Typography>
                    <Button
                      size='small'
                      variant='text'
                      onClick={handleClearAllFilters}
                      sx={{
                        mt: 1,
                        fontSize: '0.8rem',
                        textTransform: 'none',
                        color: '#005eb8',
                        fontWeight: 600,
                      }}
                    >
                      Clear All Filters
                    </Button>
                  </TableCell>
                </TableRow>
              ) : (
                filteredItems.map((row, idx) => (
                  <TableRow
                    key={`${row.plantId}-${row.year}-${idx}`}
                    sx={{
                      backgroundColor: idx % 2 === 0 ? '#ffffff' : '#f8fafc',
                      '&:hover': { backgroundColor: '#f1f5f9' },
                      transition: 'background-color 0.15s ease',
                    }}
                  >
                    <TableCell
                      sx={{
                        fontWeight: 600,
                        color: '#0f172a',
                        fontSize: '0.85rem',
                        borderBottom: '1px solid #e2e8f0',
                        borderRight: '1px solid #e2e8f0',
                        py: 1.2,
                        px: 2,
                      }}
                    >
                      {row.plantName || row.plantId}
                    </TableCell>
                    <TableCell
                      sx={{
                        color: '#475569',
                        fontSize: '0.85rem',
                        borderBottom: '1px solid #e2e8f0',
                        borderRight: '1px solid #e2e8f0',
                        py: 1.2,
                        px: 2,
                      }}
                    >
                      {row.siteName || '-'}
                    </TableCell>
                    <TableCell
                      sx={{
                        color: '#475569',
                        fontSize: '0.85rem',
                        borderBottom: '1px solid #e2e8f0',
                        borderRight: '1px solid #e2e8f0',
                        py: 1.2,
                        px: 2,
                      }}
                    >
                      {row.verticalName || '-'}
                    </TableCell>
                    <TableCell
                      align='center'
                      sx={{
                        fontSize: '0.85rem',
                        borderBottom: '1px solid #e2e8f0',
                        borderRight: '1px solid #e2e8f0',
                        py: 1.2,
                        px: 2,
                      }}
                    >
                      <Box
                        component='span'
                        sx={{
                          px: 1,
                          py: 0.3,
                          borderRadius: '4px',
                          backgroundColor: '#f1f5f9',
                          color: '#334155',
                          fontWeight: 600,
                          fontSize: '0.8rem',
                          border: '1px solid #e2e8f0',
                        }}
                      >
                        {row.year}
                      </Box>
                    </TableCell>
                    <TableCell
                      sx={{
                        borderBottom: '1px solid #e2e8f0',
                        borderRight: '1px solid #e2e8f0',
                        py: 1.2,
                        px: 2,
                      }}
                    >
                      <Chip
                        size='small'
                        label={row.gateDisplayName || row.gateName || 'Pending'}
                        sx={{
                          backgroundColor: '#e0f2fe',
                          color: '#0369a1',
                          border: '1px solid #bae6fd',
                          fontWeight: 600,
                          fontSize: '0.78rem',
                          height: '24px',
                        }}
                      />
                    </TableCell>
                    <TableCell
                      sx={{
                        borderBottom: '1px solid #e2e8f0',
                        borderRight: '1px solid #e2e8f0',
                        py: 1.2,
                        px: 2,
                      }}
                    >
                      <Chip
                        size='small'
                        label={row.assignedRole || 'Approver'}
                        sx={{
                          backgroundColor: '#f3e8ff',
                          color: '#6b21a8',
                          border: '1px solid #e9d5ff',
                          fontWeight: 600,
                          fontSize: '0.78rem',
                          height: '24px',
                        }}
                      />
                    </TableCell>
                    <TableCell
                      align='center'
                      sx={{
                        borderBottom: '1px solid #e2e8f0',
                        py: 1.2,
                        px: 2,
                      }}
                    >
                      <Button
                        size='small'
                        variant='contained'
                        endIcon={
                          <ArrowForwardIcon sx={{ fontSize: '15px !important' }} />
                        }
                        onClick={() => handleGoToPlant(row)}
                        sx={{
                          backgroundColor: '#005eb8',
                          color: '#ffffff',
                          fontWeight: 600,
                          fontSize: '0.78rem',
                          textTransform: 'none',
                          borderRadius: '6px',
                          px: 2,
                          py: 0.5,
                          fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
                          boxShadow: '0 2px 4px rgba(0, 94, 184, 0.2)',
                          whiteSpace: 'nowrap',
                          transition: 'all 0.2s ease',
                          '&:hover': {
                            backgroundColor: '#004b93',
                            boxShadow: '0 4px 8px rgba(0, 94, 184, 0.3)',
                            transform: 'translateY(-1px)',
                          },
                        }}
                      >
                        GO TO PLANT
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Snackbar
        open={snack.open}
        autoHideDuration={4000}
        onClose={() => setSnack((s) => ({ ...s, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          severity={snack.severity}
          onClose={() => setSnack((s) => ({ ...s, open: false }))}
        >
          {snack.message}
        </Alert>
      </Snackbar>
    </Box>
  )
}

export default AopMyApprovals
