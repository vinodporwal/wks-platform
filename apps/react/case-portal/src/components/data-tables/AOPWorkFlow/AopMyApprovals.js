import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  Button,
  Chip,
  IconButton,
  InputAdornment,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import ArrowForwardIcon from '@mui/icons-material/ArrowForward'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import SearchIcon from '@mui/icons-material/Search'
import RefreshIcon from '@mui/icons-material/Refresh'
import ClearIcon from '@mui/icons-material/Clear'
import FactoryIcon from '@mui/icons-material/Factory'
import LocationOnIcon from '@mui/icons-material/LocationOn'
import AccountTreeIcon from '@mui/icons-material/AccountTree'
import CalendarTodayIcon from '@mui/icons-material/CalendarToday'
import BadgeIcon from '@mui/icons-material/Badge'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import HourglassTopIcon from '@mui/icons-material/HourglassTop'
import RuleIcon from '@mui/icons-material/Rule'
import { useDispatch } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { AopApprovalService } from 'services/AopApprovalService'
import { DataService } from 'services/DataService'
import { setVerticalChangeFromDashboard } from 'store/reducers/dataGridStore'
import KendoDataTables from 'components/kendo-data-tables/index'
import './AopMyApprovals.css'

/**
 * "My Approvals" inbox — displays pending AOP workflows using Kendo React Data Grid wrapper.
 */
const AopMyApprovals = ({ onClose }) => {
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const keycloak = useSession()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [sitesData, setSitesData] = useState(null)
  const [modifiedCells, setModifiedCells] = useState({})
  const [navigatingId, setNavigatingId] = useState(null)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const load = async () => {
    setLoading(true)
    try {
      const data = await AopApprovalService.getMyPending(keycloak)
      setItems(Array.isArray(data) ? data : [])
    } catch (e) {
      setSnackbarOpen(true)
      setSnackbarData({
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

  // Pre-fetch sites hierarchy to map plant -> site -> vertical IDs accurately
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

  const filteredItems = useMemo(() => {
    if (!searchTerm.trim()) return items || []
    const term = searchTerm.toLowerCase()
    return (items || []).filter((item) => {
      const pName = String(item.plantName || item.plant || '').toLowerCase()
      const sName = String(item.siteName || item.site || '').toLowerCase()
      const vName = String(item.verticalName || item.vertical || '').toLowerCase()
      const year = String(item.year || '').toLowerCase()
      const stage = String(item.gateDisplayName || item.gateName || '').toLowerCase()
      const role = String(item.assignedRole || '').toLowerCase()
      return (
        pName.includes(term) ||
        sName.includes(term) ||
        vName.includes(term) ||
        year.includes(term) ||
        stage.includes(term) ||
        role.includes(term)
      )
    })
  }, [items, searchTerm])

  const handleGoToPlant = useCallback(
    async (row) => {
      const rowId = row.id || row.plantId || row.plantName
      setNavigatingId(rowId)

      let pid = row.plantId || row.pid || row.plant_id || row.id
      let sid = row.siteId || row.sid || row.sId || row.site_id
      let v_id = row.verticalId || row.v_id || row.vid || row.vertical_id

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
          const verticalMatch =
            !row.verticalName ||
            (v_id && String(vertical.id).toUpperCase() === String(v_id).toUpperCase()) ||
            String(vertical.id).toUpperCase() === String(row.verticalName).toUpperCase() ||
            String(vertical.displayName || vertical.name || '').toUpperCase() === String(row.verticalName).toUpperCase()

          if (!verticalMatch) continue

          for (const site of vertical.sites || []) {
            const siteMatch =
              !row.siteName ||
              (sid && String(site.id).toUpperCase() === String(sid).toUpperCase()) ||
              String(site.id).toUpperCase() === String(row.siteName).toUpperCase() ||
              String(site.displayName || site.name || '').toUpperCase() === String(row.siteName).toUpperCase()

            if (!siteMatch) continue

            for (const plant of site.plants || []) {
              const matchesId =
                pid && String(plant.id).toUpperCase() === String(pid).toUpperCase()
              const matchesName =
                row.plantName &&
                (String(plant.id).toUpperCase() === String(row.plantName).toUpperCase() ||
                  String(plant.displayName || plant.name || '').toUpperCase() === String(row.plantName).toUpperCase())

              if (matchesId || matchesName) {
                pid = plant.id
                sid = site.id
                v_id = vertical.id
                break
              }
            }
            if (pid && sid && v_id) break
          }
          if (pid && sid && v_id) break
        }
      }

      const missingParams = []
      if (!pid) missingParams.push('Plant ID (pid)')
      if (!sid) missingParams.push('Site ID (sid)')
      if (!v_id) missingParams.push('Vertical ID (v_id)')

      if (missingParams.length > 0) {
        setNavigatingId(null)
        setSnackbarOpen(true)
        setSnackbarData({
          message: `Cannot navigate: Missing required parameters (${missingParams.join(', ')}).`,
          severity: 'error',
        })
        return
      }

      // Update Redux state with active context
      dispatch(
        setVerticalChangeFromDashboard({
          v_id,
          sid,
          pid,
          trigger: Date.now(),
        }),
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: `Redirecting to AOP Report for ${row.plantName || pid}...`,
        severity: 'success',
      })

      if (typeof onClose === 'function') {
        onClose()
      }

      setTimeout(() => {
        navigate('/workflow')
      }, 400)
    },
    [sitesData, keycloak, dispatch, navigate, onClose],
  )

  const columns = useMemo(
    () => [
      {
        field: 'plantName',
        title: 'Plant',
        minWidth: 160,
        editable: false,
        cell: (props) => {
          const val = props.dataItem?.plantName || props.dataItem?.plant || '-'
          return (
            <td style={{ padding: '6px 12px' }}>
              <Chip
                className='aop-chip aop-chip-plant'
                size='small'
                icon={<FactoryIcon style={{ fontSize: 14 }} />}
                label={val}
              />
            </td>
          )
        },
      },
      {
        field: 'siteName',
        title: 'Site',
        minWidth: 160,
        editable: false,
        cell: (props) => {
          const val = props.dataItem?.siteName || props.dataItem?.site || '-'
          return (
            <td style={{ padding: '6px 12px' }}>
              <Chip
                className='aop-chip aop-chip-site'
                size='small'
                icon={<LocationOnIcon style={{ fontSize: 14 }} />}
                label={val}
              />
            </td>
          )
        },
      },
      {
        field: 'verticalName',
        title: 'Vertical',
        minWidth: 160,
        editable: false,
        cell: (props) => {
          const val = props.dataItem?.verticalName || props.dataItem?.vertical || '-'
          return (
            <td style={{ padding: '6px 12px' }}>
              <Chip
                className='aop-chip aop-chip-vertical'
                size='small'
                icon={<AccountTreeIcon style={{ fontSize: 14 }} />}
                label={val}
              />
            </td>
          )
        },
      },
      {
        field: 'year',
        title: 'AOP Year',
        minWidth: 140,
        editable: false,
        cell: (props) => {
          const val = props.dataItem?.year || '-'
          return (
            <td style={{ padding: '6px 12px' }}>
              <Chip
                className='aop-chip aop-chip-year'
                size='small'
                icon={<CalendarTodayIcon style={{ fontSize: 13 }} />}
                label={val}
              />
            </td>
          )
        },
      },
      {
        field: 'gateDisplayName',
        title: 'Stage',
        minWidth: 190,
        editable: false,
        cell: (props) => {
          const row = props.dataItem || {}
          const label = row.gateDisplayName || row.gateName || 'Pending'
          let stageClass = 'aop-chip-stage-default'
          let StageIcon = RuleIcon

          const lowerLabel = String(label).toLowerCase()
          if (lowerLabel.includes('approved') || lowerLabel.includes('completed')) {
            stageClass = 'aop-chip-stage-approved'
            StageIcon = CheckCircleOutlineIcon
          } else if (lowerLabel.includes('pending') || lowerLabel.includes('review')) {
            stageClass = 'aop-chip-stage-pending'
            StageIcon = HourglassTopIcon
          } else if (lowerLabel.includes('gate') || lowerLabel.includes('l1') || lowerLabel.includes('l2')) {
            stageClass = 'aop-chip-stage-gate'
            StageIcon = RuleIcon
          }

          return (
            <td style={{ textAlign: 'left', padding: '6px 12px' }}>
              <Chip
                className={`aop-chip ${stageClass}`}
                size='small'
                icon={<StageIcon style={{ fontSize: 14 }} />}
                label={label}
              />
            </td>
          )
        },
      },
      {
        field: 'assignedRole',
        title: 'As Role',
        minWidth: 170,
        editable: false,
        cell: (props) => {
          const val = props.dataItem?.assignedRole || '-'
          return (
            <td style={{ padding: '6px 12px' }}>
              <Chip
                className='aop-chip aop-chip-role'
                size='small'
                icon={<BadgeIcon style={{ fontSize: 14 }} />}
                label={val}
              />
            </td>
          )
        },
      },
      {
        field: 'action',
        title: 'Action',
        minWidth: 170,
        editable: false,
        filterable: false,
        sortable: false,
        cell: (props) => {
          const row = props.dataItem || {}
          const rowId = row.id || row.plantId || row.plantName
          const isNavigating = navigatingId === rowId

          return (
            <td style={{ textAlign: 'center', padding: '6px 12px' }}>
              <Button
                className='aop-go-to-plant-btn'
                variant='contained'
                size='small'
                onClick={() => handleGoToPlant(row)}
                disabled={Boolean(navigatingId)}
                endIcon={<ArrowForwardIcon sx={{ fontSize: '14px !important' }} />}
              >
                {isNavigating ? 'Opening...' : 'View AOP Report'}
              </Button>
            </td>
          )
        },
      },
    ],
    [handleGoToPlant, navigatingId],
  )

  const permissions = useMemo(
    () => ({
      hideUploadExcel: true,
      hideDownloadExcel: false,
      ExcelName: 'AOP_My_Pending_Approvals',
      hideCalculateButton: true,
      hideSaveButton: true,
      deleteButton: false,
      makePagable: true,
    }),
    [],
  )

  return (
    <Box className='aop-my-approvals-container' sx={{ width: '100%', pt: 1.5 }}>
      {/* Top Header & Quick Action Toolbar */}
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        alignItems={{ xs: 'flex-start', sm: 'center' }}
        justifyContent='space-between'
        spacing={2}
        sx={{ mb: 2 }}
      >
        <Stack direction='row' alignItems='center' spacing={1.5}>
          <Tooltip title='Back to Dashboard'>
            <IconButton
              className='aop-back-btn'
              size='small'
              onClick={() => navigate('/dashboard')}
            >
              <ArrowBackIcon sx={{ fontSize: 18 }} />
            </IconButton>
          </Tooltip>
          <Typography className='aop-title-text' variant='h6'>
            Pending Action Items
          </Typography>
          <Chip
            className='aop-count-chip'
            label={`${filteredItems.length} ${filteredItems.length === 1 ? 'Item' : 'Items'}`}
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

      {/* Kendo React Data Grid Wrapper (Scoped Styling) */}
      <Box className='aop-my-approvals-kendo-wrapper'>
        <KendoDataTables
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          setRows={setItems}
          columns={columns}
          rows={filteredItems}
          title='Pending Approvals'
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          fetchData={load}
          permissions={permissions}
        />
      </Box>
    </Box>
  )
}

export default AopMyApprovals
