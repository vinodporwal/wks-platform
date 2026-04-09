import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  IconMapPin,
  IconBriefcase,
  IconBuildingFactory,
  IconChevronDown,
  IconChevronUp,
  IconChevronRight,
  IconCircleCheck,
  IconCode,
  IconSearch,
  IconEye,
  IconClock,
  IconAdjustments,
  IconDots,
} from '@tabler/icons-react'
import {
  Card,
  Box,
  Typography,
  Switch,
  FormControlLabel,
  Menu,
  MenuItem,
  Checkbox,
  ListItemText,
  Divider,
} from '@mui/material'
import { useDispatch, useSelector } from 'react-redux'
import Notification from 'components/Utilities/Notification'
import { BusinessDemandDataApiService } from 'services/business-demand-data-api-service'
import { DataService } from 'services/DataService'
import { useSession } from 'SessionStoreContext'
import { setVerticalChangeFromDashboard } from 'store/reducers/dataGridStore'
import '../../dashboard-v2.css'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const ALL_STATUSES = ['Go-Live', 'Development', 'UAT', 'Pre-UAT', 'Not Started']
const STATUS_MAP = {
  'Go Live': 'Go-Live',
  'Pre UAT': 'Pre-UAT',
}

export default function AopDashboardCompact() {
  const dispatch = useDispatch()
  const keycloak = useSession()

  // store slice
  const {
    yearChanged,
    oldYear,
    plantObject = {},
    siteObject = {},
    verticalObject = {},
    year = {},
  } = useSelector((s) => s.dataGridStore || {})

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear

  // local UI state
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'info',
  })
  const [loading, setLoading] = useState(false)
  const [fullDetails, setFullDetails] = useState([])
  const [allowedMap, setAllowedMap] = useState({})
  const [verticals, setVerticals] = useState([])
  const [statusData, setStatusData] = useState([])
  const [siteGroupedRows, setSiteGroupedRows] = useState([])
  const [idMap, setIdMap] = useState({})

  const [selectedStatuses, setSelectedStatuses] = useState(ALL_STATUSES)
  const [statusMenuAnchor, setStatusMenuAnchor] = useState(null)
  const [moreMenuAnchor, setMoreMenuAnchor] = useState(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [viewMode, setViewMode] = useState('sites')

  const handleStatusMenuOpen = (event) =>
    setStatusMenuAnchor(event.currentTarget)
  const handleStatusMenuClose = () => setStatusMenuAnchor(null)
  const toggleStatus = (status) => {
    if (status === 'all') {
      if (selectedStatuses.length === ALL_STATUSES.length)
        setSelectedStatuses([])
      else setSelectedStatuses(ALL_STATUSES)
    } else {
      setSelectedStatuses((prev) =>
        prev.includes(status)
          ? prev.filter((s) => s !== status)
          : [...prev, status],
      )
    }
  }

  const [expandedSites, setExpandedSites] = useState({})
  const [expandedSubSites, setExpandedSubSites] = useState({})
  const [allExpanded, setAllExpanded] = useState(false)

  // ------------------ helpers ------------------
  const showSnackbar = useCallback((message, severity = 'info') => {
    setSnackbar({ open: true, message, severity })
  }, [])

  function parseAllowed(raw) {
    const map = {}
    raw.forEach((vObj) => {
      const vid = Object.keys(vObj)[0]
      map[vid] = {}
      vObj[vid].forEach((siteObj) => {
        const sid = Object.keys(siteObj)[0]
        map[vid][sid] = siteObj[sid]
      })
    })
    return map
  }

  const buildIdMap = useCallback((details = []) => {
    return details.reduce((acc, item) => {
      if (!item?.name || !item?.id) return acc
      const key = item.name.toUpperCase().replace(/\s+/g, '_')
      acc[key] = item.id
      return acc
    }, {})
  }, [])

  const toggleSite = (siteName) => {
    const isExpanding = !expandedSites[siteName]
    setExpandedSites((prev) => ({
      ...prev,
      [siteName]: isExpanding,
    }))

    // Also toggle all sub-sites for this site
    const siteData = siteGroupedRows.find((s) => s.site === siteName)
    if (siteData) {
      setExpandedSubSites((prev) => {
        const next = { ...prev }
        siteData.businessCategories.forEach((cat) => {
          next[`${siteName}-${cat}`] = isExpanding
        })
        return next
      })
    }
  }
  const toggleSubSite = (siteName, subCategory) => {
    const key = `${siteName}-${subCategory}`
    setExpandedSubSites((prev) => ({
      ...prev,
      [key]: !prev[key],
    }))
  }

  const handleExpandCollapseAll = (isExpand) => {
    setAllExpanded(isExpand)

    if (isExpand) {
      const newExpandedSites = {}
      const newExpandedSubSites = {}

      groupedRows.forEach((site) => {
        newExpandedSites[site.site] = true
        site.businessCategories.forEach((cat) => {
          newExpandedSubSites[`${site.site}-${cat}`] = true
        })
      })

      setExpandedSites(newExpandedSites)
      setExpandedSubSites(newExpandedSubSites)
    } else {
      setExpandedSites({})
      setExpandedSubSites({})
    }
    setMoreMenuAnchor(null)
  }

  const handleToggleAll = (event) => {
    handleExpandCollapseAll(event.target.checked)
  }

  // ------------------ event handlers ------------------

  const handlePlantClick = useCallback(
    (event, vid, sid, v_id) => {
      setLoading(true)
      const vertical = verticals.find((v) => v.vid === v_id)
      if (!vertical) {
        showSnackbar('Access Denied!', 'error')
        setLoading(false)
        return
      }

      if (sid && !vertical.sids.includes(sid)) {
        showSnackbar('Access Denied!', 'error')
        setLoading(false)
        return
      }

      dispatch(
        setVerticalChangeFromDashboard({ v_id, trigger: Date.now(), sid }),
      )
    },
    [dispatch, verticals, showSnackbar],
  )

  // ------------------ data fetching ------------------

  const fetchAllSites = useCallback(async () => {
    try {
      let parsedPlants = []
      try {
        parsedPlants = JSON.parse(keycloak?.idTokenParsed?.plants || '[]')
      } catch (e) {
        console.warn('Token parse error', e)
      }

      setAllowedMap(parseAllowed(parsedPlants))

      const allSites = await DataService.getAllSites(keycloak)
      const details = allSites || []
      setFullDetails(details)
      setIdMap(buildIdMap(details))
    } catch (error) {
      console.error('Error fetching all sites', error)
      setFullDetails([])
      setIdMap({})
    }
  }, [buildIdMap, keycloak])

  const fetchDashboardData = useCallback(async () => {
    if (!PLANT_ID || !SITE_ID || !VERTICAL_ID || !AOP_YEAR) return

    setLoading(true)
    setSiteGroupedRows([])

    try {
      const res = await BusinessDemandDataApiService.getDashboardData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      // Robust handling of API response structure
      const apiRows = res?.data?.data || res?.data || res || []

      setStatusData(apiRows)

      // Expand the first site and its sub-categories by default initially
      const tempGrouped = Object.values(
        apiRows.reduce((acc, item) => {
          const site = item.site_name || 'Unknown Site'
          if (!acc[site]) acc[site] = { site, businessCategories: new Set() }
          if (item.business_category)
            acc[site].businessCategories.add(item.business_category)
          return acc
        }, {}),
      )
      if (tempGrouped.length > 0) {
        const firstSite = tempGrouped[0].site
        setExpandedSites((prev) => ({ ...prev, [firstSite]: true }))
        setExpandedSubSites((prev) => {
          const next = { ...prev }
          Array.from(tempGrouped[0].businessCategories).forEach((cat) => {
            next[`${firstSite}-${cat}`] = true
          })
          return next
        })
      }
    } catch (error) {
      console.error('Error fetching dashboard data', error)
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, SITE_ID, VERTICAL_ID, AOP_YEAR, keycloak])

  useEffect(() => {
    if (!fullDetails.length || !Object.keys(allowedMap).length) return

    const result = fullDetails
      .filter((v) => allowedMap[v.id])
      .map((v) => ({
        vid: v.id,
        vname: v.displayName,
        sids: Object.keys(allowedMap[v.id]),
      }))

    setVerticals(result)
  }, [fullDetails, allowedMap])

  useEffect(() => {
    fetchAllSites()
    fetchDashboardData()
  }, [PLANT_ID, AOP_YEAR, yearChanged, keycloak])

  const getStatusClass = (status) => {
    if (!status) return ''
    return status.toLowerCase().replace(/\s+/g, '-')
  }

  const getSiteStatusSummary = (rows) => {
    const counts = {}
    ALL_STATUSES.forEach((s) => {
      counts[s] = 0
    })
    rows.forEach((r) => {
      let s = r.status || 'Other'
      if (STATUS_MAP[s]) {
        s = STATUS_MAP[s]
      }
      if (counts[s] !== undefined) {
        counts[s] += 1
      }
    })
    return counts
  }

  const filteredData = useMemo(() => {
    if (!statusData) return []
    let data = statusData

    if (selectedStatuses.length > 0) {
      data = data.filter((row) => {
        let rs = row.status || 'Other'
        if (STATUS_MAP[rs]) rs = STATUS_MAP[rs]
        return selectedStatuses.includes(rs)
      })
    } else {
      data = []
    }

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase()
      data = data.filter(
        (row) =>
          (row.site_name && row.site_name.toLowerCase().includes(q)) ||
          (row.business_category &&
            row.business_category.toLowerCase().includes(q)) ||
          (row.verticalName && row.verticalName.toLowerCase().includes(q)),
      )
    }

    return data
  }, [statusData, selectedStatuses, searchQuery])

  const groupedRows = useMemo(() => {
    let idx = 0
    if (viewMode === 'sites') {
      const grouped = Object.values(
        filteredData.reduce((acc, item) => {
          const site = item.site_name || 'Unknown Site'
          if (!acc[site])
            acc[site] = { site, rows: [], businessCategories: new Set() }
          const verticalName = item.vertical_name || 'N/A'
          acc[site].rows.push({
            idx: idx++,
            id:
              idMap[verticalName.toUpperCase().replace(/\s+/g, '_')] ??
              item.vertical_id,
            sId: item.site_id,
            verticalName: verticalName,
            status: item.status,
            status_color: item.status_color,
            status_text_color: item.status_text_color,
            business_category: item.business_category || 'Other',
            display_order: item.display_order,
            v_id: item.v_id,
          })
          if (item.business_category)
            acc[site].businessCategories.add(item.business_category)
          return acc
        }, {}),
      ).map((siteGroup) => ({
        ...siteGroup,
        businessCategories: Array.from(siteGroup.businessCategories).sort(),
      }))
      return grouped
    } else {
      const grouped = Object.values(
        filteredData.reduce((acc, item) => {
          const business = item.business_category || 'Other'
          if (!acc[business])
            acc[business] = {
              site: business,
              rows: [],
              businessCategories: new Set(),
            }
          const verticalName = item.vertical_name || 'N/A'
          acc[business].rows.push({
            idx: idx++,
            id:
              idMap[verticalName.toUpperCase().replace(/\s+/g, '_')] ??
              item.vertical_id,
            sId: item.site_id,
            verticalName: verticalName,
            status: item.status,
            status_color: item.status_color,
            status_text_color: item.status_text_color,
            business_category: item.site_name || 'Unknown Site',
            display_order: item.display_order,
            v_id: item.v_id,
          })
          if (item.site_name)
            acc[business].businessCategories.add(item.site_name)
          return acc
        }, {}),
      ).map((bGroup) => ({
        ...bGroup,
        businessCategories: Array.from(bGroup.businessCategories).sort(),
      }))
      return grouped
    }
  }, [filteredData, viewMode, idMap])

  const overallStatusSummary = getSiteStatusSummary(filteredData)
  const totalSites =
    [...new Set(filteredData.map((item) => item.site_name).filter(Boolean))]
      ?.length || 0
  const totalBusinesses =
    [
      ...new Set(
        filteredData.map((item) => item.business_category).filter(Boolean),
      ),
    ]?.length || 0
  const totalPlants = filteredData?.length || 0

  return (
    <Box className='dashboard-root-v3'>
      <LoaderBackdrop open={!!loading} />

      <Box className='dashboard-top-section'>
        <Box className='top-left-summaries'>
          <Box className='top-summary-card-small'>
            <Box className='icon-text-group'>
              <Box className='top-icon-box site'>
                <IconMapPin size={20} />
              </Box>
              <Typography className='label'>Total Sites</Typography>
            </Box>
            <Typography className='value'>{totalSites}</Typography>
          </Box>
          <Box className='top-summary-card-small'>
            <Box className='icon-text-group'>
              <Box className='top-icon-box business'>
                <IconBriefcase size={20} />
              </Box>
              <Typography className='label'>Total Businesses</Typography>
            </Box>
            <Typography className='value'>{totalBusinesses}</Typography>
          </Box>
        </Box>

        <Box className='top-right-summaries'>
          <Box className='top-total-plants-section'>
            <Box className='plants-icon-box'>
              <IconBuildingFactory size={26} />
            </Box>
            <Box className='plants-text-col'>
              <Typography className='label'>Total Plants</Typography>
              <Typography className='value'>{totalPlants}</Typography>
            </Box>
          </Box>

          <Box className='top-summary-divider' />

          <Box className='top-statuses-section'>
            {ALL_STATUSES.map((status) => {
              const count = overallStatusSummary[status] || 0
              let StatusIcon = IconCircleCheck
              if (status === 'Development') StatusIcon = IconCode
              if (status === 'UAT') StatusIcon = IconSearch
              if (status === 'Pre-UAT' || status === 'Pre UAT')
                StatusIcon = IconEye
              if (status === 'Not Started') StatusIcon = IconClock

              return (
                <Box
                  key={status}
                  className={`top-status-box ${getStatusClass(status)}`}
                >
                  <Box className='top-status-header'>
                    <StatusIcon size={18} />
                    <Typography className='status-label-text'>
                      {status.replace('-', ' ')}
                    </Typography>
                  </Box>
                  <Typography className='top-status-value'>{count}</Typography>
                </Box>
              )
            })}
          </Box>
        </Box>
      </Box>

      <Box className='dashboard-filters-row'>
        <Box className='view-toggle-group'>
          <Box
            className={`view-toggle-btn ${viewMode === 'sites' ? 'active' : ''}`}
            onClick={() => setViewMode('sites')}
          >
            <IconMapPin size={16} /> View by Sites
          </Box>
          <Box
            className={`view-toggle-btn ${viewMode === 'businesses' ? 'active' : ''}`}
            onClick={() => setViewMode('businesses')}
          >
            <IconBriefcase size={16} /> View by Businesses
          </Box>
        </Box>
        <Box className='filters-right'>
          <Box className='search-input-wrapper'>
            <IconSearch size={16} className='search-icon' />
            <input
              type='text'
              placeholder='Search for Site or Business name...'
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </Box>
          <Box className='status-dropdown' onClick={handleStatusMenuOpen}>
            <IconAdjustments size={16} className='dropdown-icon' />
            <Typography component='span'>
              Status:{' '}
              <strong>
                {selectedStatuses.length === ALL_STATUSES.length
                  ? 'All'
                  : selectedStatuses.length === 0
                    ? 'None'
                    : selectedStatuses.length === 1
                      ? selectedStatuses[0].replace('-', ' ')
                      : `${selectedStatuses.length} selected`}
              </strong>
            </Typography>
            <IconChevronDown size={14} className='dropdown-chevron' />
          </Box>

          <Menu
            anchorEl={statusMenuAnchor}
            open={Boolean(statusMenuAnchor)}
            onClose={handleStatusMenuClose}
            PaperProps={{
              style: {
                minWidth: 200,
                borderRadius: 8,
                boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                border: '1px solid #e2e8f0',
              },
            }}
            MenuListProps={{
              style: { padding: '4px 0' },
            }}
          >
            <MenuItem onClick={() => toggleStatus('all')} sx={{ py: 0 }}>
              <Checkbox
                checked={selectedStatuses.length === ALL_STATUSES.length}
                size='small'
              />
              <ListItemText
                primary='Select all'
                primaryTypographyProps={{ fontSize: 14, fontWeight: 600 }}
              />
            </MenuItem>
            <Divider sx={{ my: 0, margin: `0px !important` }} />

            {ALL_STATUSES.map((status, index) => (
              <React.Fragment key={status}>
                <MenuItem onClick={() => toggleStatus(status)} sx={{ py: 0 }}>
                  <Checkbox
                    checked={selectedStatuses.includes(status)}
                    size='small'
                  />
                  <ListItemText
                    primary={status.replace('-', ' ')}
                    primaryTypographyProps={{ fontSize: 14, fontWeight: 500 }}
                  />
                </MenuItem>
                {index < ALL_STATUSES.length - 1 && (
                  <Divider sx={{ my: 0, margin: `0px !important` }} />
                )}
              </React.Fragment>
            ))}
          </Menu>

          <Box
            className='more-btn'
            onClick={(e) => setMoreMenuAnchor(e.currentTarget)}
          >
            <IconDots size={20} />
          </Box>

          <Menu
            anchorEl={moreMenuAnchor}
            open={Boolean(moreMenuAnchor)}
            onClose={() => setMoreMenuAnchor(null)}
            PaperProps={{
              style: {
                minWidth: 140,
                borderRadius: 8,
                boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                border: '1px solid #e2e8f0',
                marginTop: 4,
              },
            }}
          >
            <MenuItem onClick={() => handleExpandCollapseAll(true)}>
              <Typography
                sx={{ fontSize: 14, fontWeight: 500, color: '#303030' }}
              >
                Expand All
              </Typography>
            </MenuItem>
            <Divider sx={{ margin: `4px 0px !important` }} />
            <MenuItem onClick={() => handleExpandCollapseAll(false)}>
              <Typography
                sx={{ fontSize: 14, fontWeight: 500, color: '#303030' }}
              >
                Collapse All
              </Typography>
            </MenuItem>
          </Menu>
        </Box>
      </Box>

      <Card className='dashboard-main-card'>
        {/* Business Units List - Each Site is a Summary Bar Accordion */}
        {groupedRows.map((site) => {
          const siteStatusSummary = getSiteStatusSummary(site.rows)
          const isSiteExpanded = expandedSites[site.site]

          return (
            <Box key={site.site} className='site-accordion-container'>
              {/* Site Header Row (Styled as Summary Bar) */}
              <Box
                className='summary-bar'
                onClick={() => toggleSite(site.site)}
              >
                <Box className='summary-item summary-item-site'>
                  <Box className='summary-icon-box'>
                    {viewMode === 'sites' ? (
                      <IconMapPin size={20} />
                    ) : (
                      <IconBriefcase size={20} />
                    )}
                  </Box>
                  <Typography className='summary-label'>{site.site}</Typography>
                </Box>

                <Box className='summary-divider' />

                <Box className='summary-item business'>
                  <Box className='summary-icon-box'>
                    {viewMode === 'sites' ? (
                      <IconBriefcase size={20} />
                    ) : (
                      <IconMapPin size={20} />
                    )}
                  </Box>
                  <Box>
                    <Typography
                      component='span'
                      className='summary-label-total-business'
                    >
                      {viewMode === 'sites' ? 'Total Business' : 'Total Sites'}
                    </Typography>
                    <Typography component='span' className='summary-count'>
                      {site.businessCategories?.length || 0}
                    </Typography>
                  </Box>
                </Box>

                <Box className='summary-divider' />

                <Box className='summary-item plants summary-item-plants'>
                  <Box className='summary-icon-box'>
                    <IconBuildingFactory size={20} />
                  </Box>
                  <Box>
                    <Typography
                      component='span'
                      className='summary-label-total-business'
                    >
                      Plants
                    </Typography>
                    <Typography component='span' className='summary-count'>
                      {site.rows.length}
                    </Typography>
                  </Box>
                </Box>

                {/* Site-Specific Status Breakdown Chips */}
                <Box className='status-chips-summary'>
                  {ALL_STATUSES.map((status) => (
                    <Box
                      key={status}
                      className={`status-summary-chip-width ${getStatusClass(status)}`}
                    >
                      <Box
                        className={`status-summary-chip ${getStatusClass(status)}`}
                      >
                        {siteStatusSummary[status]} {status}
                      </Box>
                    </Box>
                  ))}
                </Box>

                <Box sx={{ ml: 2 }}>
                  {isSiteExpanded ? (
                    <IconChevronUp size={24} className='chevron-arrow' />
                  ) : (
                    <IconChevronDown size={24} className='chevron-arrow' />
                  )}
                </Box>
              </Box>

              {/* Expanded Content: Sub-Accordions */}
              {isSiteExpanded && (
                <Box className='bu-expanded-content'>
                  {site.businessCategories.map((catName) => {
                    const subKey = `${site.site}-${catName}`
                    const isSubExpanded = expandedSubSites[subKey]
                    const catRows = site?.rows?.filter(
                      (i) => i?.business_category === catName,
                    )

                    return (
                      <Box key={catName} className='sub-accordion-wrapper'>
                        <Box
                          className='sub-header-row'
                          onClick={() => toggleSubSite(site.site, catName)}
                        >
                          <Box className='sub-header-left'>
                            <Box className='sub-header-title-box'>
                              {isSubExpanded ? (
                                <IconChevronUp
                                  size={16}
                                  className='chevron-arrow'
                                />
                              ) : (
                                <IconChevronDown
                                  size={16}
                                  className='chevron-arrow'
                                />
                              )}
                              <Box className='sub-header-plants'>
                                {viewMode === 'sites' ? (
                                  <IconBriefcase
                                    size={18}
                                    className='sub-icon'
                                  />
                                ) : (
                                  <IconMapPin size={18} className='sub-icon' />
                                )}
                                <Typography className='sub-category-name'>
                                  {catName}
                                </Typography>
                              </Box>
                            </Box>
                            <Box className='summary-divider' />
                            <Box className='sub-header-plants'>
                              <Box className='summary-icon-box-small'>
                                <IconBuildingFactory size={16} />
                              </Box>
                              <Typography className='sub-label-small'>
                                Plants
                              </Typography>
                              <Typography className='sub-count-small'>
                                {catRows?.length}
                              </Typography>
                            </Box>
                          </Box>
                        </Box>

                        {isSubExpanded && (
                          <Box className='sub-accordion-content'>
                            <Box className='plant-grid'>
                              {catRows?.map((plant) => (
                                <Box
                                  key={`${catName}-${plant.idx}`}
                                  className='plant-item-card'
                                  onClick={(e) =>
                                    handlePlantClick(
                                      e,
                                      plant.id,
                                      plant.sId,
                                      plant.v_id,
                                    )
                                  }
                                >
                                  <Box className='plant-card-left'>
                                    <IconBuildingFactory
                                      size={18}
                                      className='plant-card-icon'
                                    />
                                    <Typography className='plant-name'>
                                      {plant.verticalName}
                                    </Typography>
                                  </Box>

                                  <Box className='plant-card-right'>
                                    <Box
                                      className={`plant-status-chip ${getStatusClass(plant.status)}`}
                                      // style={{
                                      //   backgroundColor: plant.status_color,
                                      //   color: plant.status_text_color,
                                      // }}
                                    >
                                      {plant.status}
                                    </Box>
                                    <IconChevronRight
                                      size={18}
                                      className='chevron-arrow'
                                    />
                                  </Box>
                                </Box>
                              ))}
                            </Box>
                          </Box>
                        )}
                      </Box>
                    )
                  })}
                </Box>
              )}
            </Box>
          )
        })}
      </Card>

      {/* <Box className='floating-switch-container'>
        <FormControlLabel
          control={
            <Switch
              checked={allExpanded}
              onChange={handleToggleAll}
              color='primary'
            />
          }
          label={
            <Typography className='floating-switch-label'>
              {allExpanded ? 'Collapse All' : 'Expand All'}
            </Typography>
          }
          labelPlacement='start'
        />
      </Box> */}

      <Notification
        open={snackbar.open}
        message={snackbar.message || ''}
        severity={snackbar.severity || 'info'}
        onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
      />
    </Box>
  )
}
