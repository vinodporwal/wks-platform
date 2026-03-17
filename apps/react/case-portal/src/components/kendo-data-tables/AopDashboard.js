import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  IconMapPin,
  IconBriefcase,
  IconBuildingFactory,
  IconChevronDown,
  IconChevronUp,
  IconChevronRight,
} from '@tabler/icons-react'
import { Card, Box, Typography } from '@mui/material'
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

  const [expandedSites, setExpandedSites] = useState({})
  const [expandedSubSites, setExpandedSubSites] = useState({})

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
    setExpandedSites((prev) => ({
      ...prev,
      [siteName]: !prev[siteName],
    }))
  }

  const toggleSubSite = (siteName, subCategory) => {
    const key = `${siteName}-${subCategory}`
    setExpandedSubSites((prev) => ({
      ...prev,
      [key]: !prev[key],
    }))
  }

  // ------------------ event handlers ------------------

  const handlePlantClick = useCallback(
    (event, vid, sid) => {
      setLoading(true)
      const vertical = verticals.find((v) => v.vid === vid)
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
        setVerticalChangeFromDashboard({ vid, trigger: Date.now(), sid }),
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
      const apiRows = res?.data?.data || []

      setStatusData(apiRows)

      let idx = 0
      const grouped = Object.values(
        apiRows.reduce((acc, item) => {
          const site = item.site_name || 'Unknown Site'
          if (!acc[site]) acc[site] = { site, rows: [] }

          acc[site].rows.push({
            idx: idx++,
            id: idMap[item.vertical_name] ?? item.vertical_id,
            sId: item.site_id,
            verticalName: item.vertical_name,
            status: item.status,
            status_color: item.status_color,
            status_text_color: item.status_text_color,
            business_category: item.business_category,
            display_order: item.display_order,
          })

          return acc
        }, {}),
      )

      setSiteGroupedRows(grouped)
      // Expand the first site by default
      if (grouped.length > 0) {
        const firstSite = grouped[0].site
        setExpandedSites((prev) => ({ ...prev, [firstSite]: true }))
        setExpandedSubSites((prev) => ({
          ...prev,
          [`${firstSite}-Refining`]: true,
          [`${firstSite}-Gasification`]: true,
          [`${firstSite}-Aromatics`]: true,
        }))
      }
    } catch (error) {
      console.error('Error fetching dashboard data', error)
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, SITE_ID, VERTICAL_ID, AOP_YEAR, idMap, keycloak])

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
  return (
    <Box className='dashboard-root-v3'>
      <LoaderBackdrop open={!!loading} />

      <Card className='dashboard-main-card'>
        {/* Business Units List - Each Site is a Summary Bar Accordion */}
        {siteGroupedRows.map((site) => {
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
                    <IconMapPin size={20} />
                  </Box>
                  <Typography className='summary-label'>{site.site}</Typography>
                </Box>

                <Box className='summary-divider' />

                <Box className='summary-item business'>
                  <Box className='summary-icon-box'>
                    <IconBriefcase size={20} />
                  </Box>
                  <Box>
                    <Typography
                      component='span'
                      className='summary-label-total-business'
                    >
                      Total Business
                    </Typography>
                    <Typography component='span' className='summary-count'>
                      3
                    </Typography>
                  </Box>
                </Box>

                <Box className='summary-divider' />

                <Box className='summary-item plants'>
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
                      className={`status-summary-chip ${getStatusClass(status)}`}
                    >
                      {siteStatusSummary[status]} {status}
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
                  {[
                    { name: 'Refining', icon: IconBriefcase },
                    { name: 'Gasification', icon: IconBriefcase },
                    { name: 'Aromatics', icon: IconBriefcase },
                  ].map((sub) => {
                    const subKey = `${site.site}-${sub.name}`
                    const isSubExpanded = expandedSubSites[subKey]

                    return (
                      <Box key={sub.name} className='sub-accordion-wrapper'>
                        <Box
                          className='sub-header-row'
                          onClick={() => toggleSubSite(site.site, sub.name)}
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
                                <sub.icon size={18} className='sub-icon' />
                                <Typography className='sub-category-name'>
                                  {sub.name}
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
                                {
                                  site?.rows?.filter(
                                    (i) => i?.business_category === sub.name,
                                  )?.length
                                }
                              </Typography>
                            </Box>
                          </Box>
                        </Box>

                        {isSubExpanded && (
                          <Box className='sub-accordion-content'>
                            <Box className='plant-grid'>
                              {site?.rows
                                ?.filter(
                                  (i) => i?.business_category === sub.name,
                                )
                                ?.map((plant) => (
                                  <Box
                                    key={`${sub.name}-${plant.idx}`}
                                    className='plant-item-card'
                                    // onClick={(e) =>
                                    //   handlePlantClick(e, plant.id, plant.sId)
                                    // }
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

      <Notification
        open={snackbar.open}
        message={snackbar.message || ''}
        severity={snackbar.severity || 'info'}
        onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
      />
    </Box>
  )
}
