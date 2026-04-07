// HeaderContent.jsx
import {
  Box,
  FormControl,
  MenuItem,
  Select,
  Stack,
  Typography,
  useMediaQuery,
  SvgIcon,
} from '@mui/material'
import ArrowDropDownIcon from '@mui/icons-material/ArrowDropDown'
import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { DataService } from 'services/DataService'
import {
  setAopYear,
  setCurrentYear,
  setIsReleased,
  setOldYear,
  setPlantID,
  setPlantObject,
  setSiteID,
  setSiteObject,
  setSitePlantChange,
  setVerticalChange,
  setVerticalObject,
  setYearChange,
} from 'store/reducers/dataGridStore'
import MobileSection from './MobileSection'
import Profile from './Profile/index'
import { HIDE_VERTICAL_PATHS, HIDE_SITE_PATHS, HIDE_PLANT_PATHS } from './utils'

import Logo from 'assets/images/ril-logo2.png'
import DropdownSkeleton from 'utils/DropdownSkeleton'
import { useLocation, useNavigate } from 'react-router-dom'
import { openDrawer } from 'store/reducers/menu'
import CalendarToday from '@mui/icons-material/CalendarToday'
import Business from '@mui/icons-material/Business'
import Domain from '@mui/icons-material/Domain'
import Factory from '@mui/icons-material/Factory'
import CorporateFare from '@mui/icons-material/CorporateFare'

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

export default function HeaderContent({ keycloak }) {
  const [headerLoading, setHeaderLoading] = useState(false)
  const getSelectedVerticalStorage = localStorage.getItem('selectedVertical')
    ? JSON.parse(localStorage.getItem('selectedVertical'))
    : null
  const dispatch = useDispatch()
  const matchesXs = useMediaQuery((theme) => theme.breakpoints.down('md'))

  const location = useLocation()

  const [aopYears, setAopYears] = useState([])
  const [selectedYear, setSelectedYear] = useState('')

  const screenTitle = useSelector((s) => s.dataGridStore.screenTitle)
  const screenTitleName = screenTitle?.title

  const [allowedMap, setAllowedMap] = useState({})
  const [fullDetails, setFullDetails] = useState([])

  const [verticals, setVerticals] = useState([])
  const [sites, setSites] = useState([])
  const [plants, setPlants] = useState([])

  const [selectedVertical, setSelectedVertical] = useState(
    getSelectedVerticalStorage ? getSelectedVerticalStorage.id : '',
  )
  const [selectedSite, setSelectedSite] = useState('')
  const [selectedPlant, setSelectedPlant] = useState('')

  const verticalFromDashboard = useSelector(
    (state) => state.dataGridStore.verticalChangeFromDashboard,
  )

  const HIDE_VERTICAL_DROPDOWN =
    keycloak?.realmAccess?.roles?.includes('maintenance_users')

  const HIDE_DASHBOARD_DROPDOWN = [
    '/dashboard',
    '/user-management',
    '/user-form',
  ].includes(location.pathname)

  // Individual dropdown visibility ? extends HIDE_DASHBOARD_DROPDOWN with utils.js config
  const hideVertical =
    HIDE_DASHBOARD_DROPDOWN ||
    HIDE_VERTICAL_PATHS.some((s) => location.pathname.includes(s))
  const hideSite =
    HIDE_DASHBOARD_DROPDOWN ||
    HIDE_SITE_PATHS.some((s) => location.pathname.includes(s))
  const hidePlant =
    HIDE_DASHBOARD_DROPDOWN ||
    HIDE_PLANT_PATHS.some((s) => location.pathname.includes(s))

  if (['/dashboard'].includes(location.pathname))
    dispatch(openDrawer({ drawerOpen: false }))

  useEffect(() => {
    let parsed = []
    try {
      parsed = JSON.parse(keycloak.idTokenParsed.plants)
    } catch (e) {
      console.error('Token parse error', e)
    }
    setAllowedMap(parseAllowed(parsed))
  }, [keycloak])

  const getIsReleased = async () => {
    if (!selectedPlant || !selectedYear) return

    try {
      const response = await DataService.getReleaseAOPStatus(
        keycloak,
        selectedPlant,
        selectedYear,
      )

      // If response has data, disable the button (already released)
      // If no data, enable the button (not yet released)
      if (response?.data && Object.keys(response.data).length > 0) {
        // setIsReleaseDisabled(true)

        let isReleased = 1
        dispatch(setIsReleased({ isReleased }))
      } else {
        // setIsReleaseDisabled(false)
        let isReleased = 0
        dispatch(setIsReleased({ isReleased }))
      }
    } catch (error) {
      console.error('Error fetching release status:', error)
    }
  }

  // 2?? fetch full details once

  const fetchAllSites = async () => {
    setHeaderLoading(true)
    try {
      const data = await DataService.getAllSites(keycloak)
      setFullDetails(data || [])
    } catch (error) {
      console.error('Error fetching data', error)
      setFullDetails([])
    } finally {
      setHeaderLoading(false)
    }
  }

  useEffect(() => {
    fetchAllSites()
    getIsReleased()
    // }, [keycloak, verticalFromDashboard])
  }, [keycloak])

  useEffect(() => {
    getIsReleased()
  }, [keycloak, selectedYear, selectedPlant])

  useEffect(() => {
    if (!fullDetails.length || !Object.keys(allowedMap).length) return

    const avail = fullDetails
      .filter((v) => allowedMap[v.id])
      .map((v) => ({ id: v.id, name: v.displayName }))

    setVerticals(avail)

    if (
      selectedVertical &&
      avail.length &&
      !avail.some((v) => v.id === selectedVertical)
    ) {
      const defV = avail[0]
      setSelectedVertical(defV.id)

      localStorage.setItem('verticalId', defV.id)
      localStorage.setItem(
        'selectedVertical',
        JSON.stringify({ id: defV.id, name: defV.name }),
      )

      dispatch(
        setVerticalChange({
          selectedVertical: defV.name,
          selectedSite: '',
          selectedPlant: '',
        }),
      )

      dispatch(setVerticalObject({ id: defV.id, name: defV.name }))
    }

    if (!selectedVertical && avail.length) {
      const defV = avail[0]
      setSelectedVertical(defV.id)

      localStorage.setItem('verticalId', defV.id)
      localStorage.setItem(
        'selectedVertical',
        JSON.stringify({ id: defV.id, name: defV.name }),
      )

      dispatch(
        setVerticalChange({
          selectedVertical: defV.name,
          selectedSite: '',
          selectedPlant: '',
        }),
      )

      dispatch(setVerticalObject({ id: defV.id, name: defV.name }))
    }
  }, [fullDetails, allowedMap, selectedVertical, dispatch])

  useEffect(() => {
    if (!selectedVertical) {
      setSites([])
      setSelectedSite('')
      return
    }
    const vertObj = fullDetails.find((v) => v.id === selectedVertical)
    const allowedSites = allowedMap[selectedVertical] || {}
    const list = (vertObj?.sites || [])
      .filter((s) => allowedSites[s.id])
      .map((s) => ({ id: s.id, name: s.displayName }))
    setSites(list)

    if (list.length) {
      const defS = list[0]
      setSelectedSite(defS.id)

      localStorage.setItem(
        'selectedSite',
        JSON.stringify({ id: defS.id, name: defS.name }),
      )
      localStorage.setItem('selectedSiteId', JSON.stringify({ id: defS.id }))

      dispatch(
        setSiteObject({
          id: defS.id,
          name: defS.displayName ?? defS.name ?? '',
        }),
      )

      dispatch(setSiteID({ siteId: defS.id }))

      dispatch(setSitePlantChange({ sitePlantChange: true }))
    }
  }, [selectedVertical, fullDetails, allowedMap, dispatch])

  useEffect(() => {
    if (!selectedSite) {
      setPlants([])
      setSelectedPlant('')
      return
    }
    const vertObj = fullDetails.find((v) => v.id === selectedVertical)
    const siteObj = vertObj?.sites.find((s) => s.id === selectedSite)
    const allowedPlants = allowedMap[selectedVertical]?.[selectedSite] || []

    const list = (siteObj?.plants || [])
      .filter((p) => allowedPlants.includes(p.id))
      .map((p) => ({ id: p.id, name: p.displayName }))
    setPlants(list)

    if (list.length) {
      const defP = list[0]
      setSelectedPlant(defP.id)
      localStorage.setItem(
        'selectedPlant',
        JSON.stringify({ id: defP.id, name: defP.name }),
      )

      dispatch(
        setPlantObject({
          id: defP.id,
          name: defP.displayName ?? defP.name ?? '',
        }),
      )

      dispatch(setSitePlantChange({ sitePlantChange: true }))
      dispatch(setPlantID({ plantId: defP.id, plantName: defP.name }))
    }
  }, [selectedSite, selectedVertical, fullDetails, allowedMap, dispatch])

  useEffect(() => {
    async function fetchYears() {
      try {
        var resp = await DataService.getAopyears(keycloak)
        if (resp?.length) {
          setAopYears(resp)

          const currentYear = resp.find(
            (item) => item.currentYear == 1,
          )?.AOPYear

          if (currentYear) {
            setSelectedYear(currentYear)
            localStorage.setItem('year', currentYear)
            dispatch(setAopYear({ selectedYear: currentYear }))
            dispatch(setOldYear({ oldYear: 0 }))
          }
        }
      } catch (err) {
        console.error('Error fetching data', err)
      } finally {
        // setHeaderLoading(false)
      }
    }
    fetchYears()
  }, [keycloak, dispatch])

  const handleYearChange = (e) => {
    const newYear = e.target.value
    setSelectedYear(newYear)

    localStorage.setItem('year', newYear)
    dispatch(setYearChange({ yearChanged: true }))
    dispatch(setAopYear({ selectedYear: newYear }))

    const selectedYearObj = aopYears.find((y) => y.AOPYear === newYear)
    const isCurrentYear = selectedYearObj?.currentYear == 1

    const currentYear = aopYears.find((y) => y.currentYear == 1)
    dispatch(setCurrentYear({ currentYear: isCurrentYear ? 1 : 0 }))
    let isOldYear = 0
    let currentYear1 = currentYear?.AOPYear
    const [currentStartYear] = currentYear1.split('-').map(Number)
    const [selectedStartYear] = newYear.split('-').map(Number)
    if (selectedStartYear < currentStartYear) {
      isOldYear = 1
    }
    dispatch(setOldYear({ oldYear: isOldYear }))
  }

  const handlePlantChange = (e) => {
    const newPlantId = e.target.value
    setSelectedPlant(newPlantId)

    const plantObj = plants.find((p) => p.id === newPlantId)
    if (plantObj) {
      localStorage.setItem(
        'selectedPlant',
        JSON.stringify({ id: plantObj.id, name: plantObj.name }),
      )

      dispatch(
        setPlantObject({
          id: plantObj.id,
          name: plantObj.name ?? plantObj.name ?? '',
        }),
      )

      dispatch(setSitePlantChange({ sitePlantChange: true }))
      dispatch(setPlantID({ plantId: plantObj.id, plantName: plantObj.name }))
    }
  }

  const handleVertChange = (e) => {
    const newVId = e.target.value

    setSelectedSite('')
    setSelectedPlant('')

    setSelectedVertical(newVId)

    const vert = verticals.find((v) => v.id === newVId)
    if (vert) {
      localStorage.setItem('verticalId', vert.id)
      localStorage.setItem('selectedVertical', JSON.stringify(vert))

      dispatch(setVerticalObject({ id: vert.id, name: vert.name }))

      dispatch(
        setVerticalChange({
          selectedVertical: vert.name,
          selectedSite: '',
          selectedPlant: '',
        }),
      )
    }
  }

  useEffect(() => {
    if (!selectedVertical) return
    const vert = verticals.find((v) => v.id === selectedVertical)
    if (!vert) return

    localStorage.setItem('verticalId', vert.id)
    localStorage.setItem('selectedVertical', JSON.stringify(vert))

    dispatch(setVerticalObject({ id: vert.id, name: vert.name }))

    dispatch(
      setVerticalChange({
        selectedVertical: vert.name,
        selectedSite: '',
        selectedPlant: '',
      }),
    )
  }, [selectedVertical, verticals, dispatch])

  const handleSiteChange = (e) => {
    const newSiteId = e.target.value
    setSelectedSite(newSiteId)
    const site = sites.find((s) => s.id === newSiteId)
    if (site) {
      localStorage.setItem(
        'selectedSite',
        JSON.stringify({ id: site.id, name: site.name }),
      )

      dispatch(
        setSiteObject({ id: site.id, name: site.name ?? site.name ?? '' }),
      )

      localStorage.setItem('selectedSiteId', JSON.stringify({ id: site?.id }))

      dispatch(
        setSiteObject({ id: site.id, name: site.name ?? site.name ?? '' }),
      )

      setSelectedPlant('')
      dispatch(
        setSitePlantChange({
          selectedSite: site.name,
          selectedPlant: '',
          sitePlantChange: true,
        }),
      )
    }
  }

  const navigate = useNavigate()

  useEffect(() => {
    // console.log(1)

    if (!verticalFromDashboard?.v_id || !verticalFromDashboard?.sid) return

    if (verticalFromDashboard?.v_id === selectedVertical) return

    setSelectedVertical(verticalFromDashboard?.v_id)
  }, [verticalFromDashboard?.v_id])

  useEffect(() => {
    if (!verticalFromDashboard?.sid || !sites.length) return

    const site = sites.find((s) => s?.id === verticalFromDashboard?.sid)

    if (!site) {
      // console.log('Site not found:', verticalFromDashboard?.sid)
      return
    }

    setSelectedSite(site?.id)

    dispatch(
      setSiteObject({
        id: site?.id,
        name: site?.displayName ?? site?.name ?? '',
      }),
    )
  }, [verticalFromDashboard?.sid, sites, dispatch])

  useEffect(() => {
    // console.log(2)

    if (!verticalFromDashboard?.v_id || !verticalFromDashboard?.sid) {
      return
    }
    // setTimeout(() => {
    //   dispatch(openDrawer({ drawerOpen: true }))
    // }, 1500)
    navigate('/production-norms-plan/configuration', { replace: true })
  }, [verticalFromDashboard?.trigger])

  useEffect(() => {
    // console.log('--- Plant Effect Triggered ---')
    // console.log('Dashboard PID:', verticalFromDashboard?.pid)
    // console.log('Plants List:', plants)

    if (!verticalFromDashboard?.pid) {
      // console.log('? No PID from dashboard')
      return
    }

    if (!plants.length) {
      // console.log('? Plants not loaded yet')
      return
    }

    const plant = plants.find((p) => p?.id === verticalFromDashboard?.pid)

    // console.log('Matched Plant:', plant)

    if (!plant) {
      // console.log('? Plant not found for PID:', verticalFromDashboard?.pid)
      return
    }

    // console.log('? Setting Plant:', plant.id, plant.name)

    setSelectedPlant(plant.id)

    localStorage.setItem(
      'selectedPlant',
      JSON.stringify({
        id: plant.id,
        name: plant.displayName ?? plant.name ?? '',
      }),
    )

    dispatch(
      setPlantObject({
        id: plant.id,
        name: plant.displayName ?? plant.name ?? '',
      }),
    )

    dispatch(setSitePlantChange({ sitePlantChange: true }))

    dispatch(
      setPlantID({
        plantId: plant.id,
        plantName: plant.displayName ?? plant.name ?? '',
      }),
    )

    // console.log('?? Dispatched plant to Redux')
  }, [verticalFromDashboard?.pid, plants, dispatch])
  // 4. Navigation Effect (Existing)
  useEffect(() => {
    if (!verticalFromDashboard?.v_id || !verticalFromDashboard?.sid) {
      return
    }
    navigate('/production-norms-plan/configuration', { replace: true })
  }, [verticalFromDashboard?.trigger])

  // -------------------------
  // Visual styles (pills & menus)
  // -------------------------
  // menuPropsStyle (replace your existing)
  const menuPropsStyle = {
    PaperProps: {
      style: {
        maxHeight: 240,
        borderRadius: 8, // menu panel radius
        marginTop: 6,
        background: '#f2f2f2',

        boxShadow: '0 8px 24px rgba(15,23,42,0.08)',
        border: '1px solid rgba(15,23,42,0.06)',
        color: '#0f172a',
        padding: '4px',
      },
      sx: {
        // also add sx for runtime override (some MUI versions prefer sx)
        borderRadius: 8,
      },
    },
    disableScrollLock: true,
  }
  const menuItemStyle = {
    transition: 'all 0.12s ease',
    borderRadius: 6, //  smaller
    mx: 0.5,
    my: 0.25,
    color: '#0f172a',
    fontWeight: 600,
    fontSize: '0.85rem',
    '&:hover': {
      background: 'rgba(59,130,246,0.06)',
    },
    '&.Mui-selected': {
      background: 'rgba(59,130,246,0.12)',
      fontWeight: 700,
    },
  }

  const dropdownContainerStyle = {
    display: 'flex',
    alignItems: 'center',
    background: '#e7eaee',
  }

  const selectStyle = {
    height: 34,
    minWidth: 150,

    '& .MuiOutlinedInput-root': {
      borderRadius: '6px !important', // ?? THIS is the real border
      background: '#eef3f8',
      height: 34,
    },

    '& .MuiOutlinedInput-notchedOutline': {
      border: '1px solid rgba(15,23,42,0.06)',
      borderRadius: '6px !important', // ?? must repeat here
    },

    '&:hover .MuiOutlinedInput-notchedOutline': {
      borderColor: 'rgba(15,23,42,0.1)',
    },

    '& .MuiSelect-select': {
      display: 'flex',
      alignItems: 'center',
      gap: 6,
      padding: '6px 10px !important',
      fontSize: '0.85rem',
      fontWeight: 600,
    },

    '& .MuiSvgIcon-root': {
      fontSize: 18,
      color: '#6b7786',
    },
  }

  return (
    <>
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr auto',
          alignItems: 'center',
          width: '100%',
          position: 'relative',
          pr: 2,
          py: 0.75,
          background: '#fff',
          borderBottom: '1px solid rgba(15,23,42,0.04)',
        }}
      >
        {/* LEFT SIDE: Title */}
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 2,
            minWidth: 0,
            overflow: 'hidden',
          }}
        >
          {!HIDE_DASHBOARD_DROPDOWN && (
            <Typography
              variant='h6'
              sx={{
                fontWeight: 700,
                fontSize: '1.05rem',
                color: '#0f172a',
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
              }}
            >
              {screenTitleName}
            </Typography>
          )}
        </Box>

        {/* CENTER: Dropdown Pills */}
        <Stack
          direction='row'
          spacing={2}
          alignItems='center'
          sx={{
            justifySelf: 'end',
            width: '100%',
            justifyContent: 'flex-end',
            mr: 1,
          }}
        >
          {/* Year */}
          <Box sx={dropdownContainerStyle}>
            {headerLoading ? (
              <DropdownSkeleton />
            ) : (
              <FormControl size='small' variant='outlined'>
                {' '}
                <Select
                  IconComponent={ArrowDropDownIcon}
                  value={selectedYear}
                  onChange={handleYearChange}
                  sx={selectStyle}
                  MenuProps={menuPropsStyle}
                  renderValue={(value) => {
                    const yearObj = aopYears.find((y) => y.AOPYear === value)
                    return (
                      <Box
                        sx={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 1,
                        }}
                      >
                        <CalendarToday
                          sx={{ fontSize: 16, color: '#97751d !important' }}
                        />

                        <Box
                          component='span'
                          sx={{
                            color: '#6b7c93',
                            fontWeight: 600,
                            fontSize: '0.85rem',
                          }}
                        >
                          Year:
                        </Box>
                        <Box
                          component='strong'
                          sx={{ fontWeight: 800, color: '#0f172a', ml: 0.5 }}
                        >
                          {yearObj?.AOPDisplayYear}
                        </Box>
                      </Box>
                    )
                  }}
                >
                  {aopYears.map((y) => (
                    <MenuItem
                      key={y.AOPYear}
                      value={y.AOPYear}
                      sx={menuItemStyle}
                    >
                      {y.AOPDisplayYear}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}
          </Box>

          {/* Vertical */}
          {!(HIDE_VERTICAL_DROPDOWN || HIDE_DASHBOARD_DROPDOWN) && (
            <Box sx={dropdownContainerStyle}>
              {headerLoading ? (
                <DropdownSkeleton />
              ) : (
                <FormControl size='small' variant='outlined'>
                  {' '}
                  <Select
                    IconComponent={ArrowDropDownIcon}
                    value={selectedVertical}
                    onChange={handleVertChange}
                    sx={selectStyle}
                    MenuProps={menuPropsStyle}
                    renderValue={(value) => {
                      const vert = verticals.find((v) => v.id === value)
                      return (
                        <Box
                          sx={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 1,
                          }}
                        >
                          <Business
                            sx={{ fontSize: 16, color: '#d539d3 !important' }}
                          />
                          <Box
                            component='span'
                            sx={{
                              color: '#6b7c93',
                              fontWeight: 600,
                              fontSize: '0.85rem',
                            }}
                          >
                            Vertical:
                          </Box>
                          <Box
                            component='strong'
                            sx={{ fontWeight: 800, color: '#0f172a', ml: 0.5 }}
                          >
                            {vert?.name}
                          </Box>
                        </Box>
                      )
                    }}
                  >
                    {verticals.map((v) => (
                      <MenuItem key={v.id} value={v.id} sx={menuItemStyle}>
                        {v.name}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              )}
            </Box>
          )}

          {/* Site */}
          {!HIDE_DASHBOARD_DROPDOWN && (
            <Box sx={dropdownContainerStyle}>
              {headerLoading ? (
                <DropdownSkeleton />
              ) : (
                <FormControl size='small' variant='outlined'>
                  {' '}
                  <Select
                    IconComponent={ArrowDropDownIcon}
                    value={selectedSite}
                    onChange={handleSiteChange}
                    disabled={!sites.length}
                    sx={selectStyle}
                    MenuProps={menuPropsStyle}
                    renderValue={(value) => {
                      const site = sites.find((s) => s.id === value)
                      return (
                        <Box
                          sx={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 1,
                          }}
                        >
                          <CorporateFare
                            sx={{ fontSize: 16, color: '#005bdb !important' }}
                          />
                          <Box
                            component='span'
                            sx={{
                              color: '#6b7c93',
                              fontWeight: 600,
                              fontSize: '0.85rem',
                            }}
                          >
                            Site:
                          </Box>
                          <Box
                            component='strong'
                            sx={{ fontWeight: 800, color: '#0f172a', ml: 0.5 }}
                          >
                            {site?.name}
                          </Box>
                        </Box>
                      )
                    }}
                  >
                    {sites.map((s) => (
                      <MenuItem key={s.id} value={s.id} sx={menuItemStyle}>
                        {s.name}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              )}
            </Box>
          )}

          {/* Plant */}
          {!HIDE_DASHBOARD_DROPDOWN && (
            <Box sx={dropdownContainerStyle}>
              {headerLoading ? (
                <DropdownSkeleton />
              ) : (
                <FormControl size='small'>
                  <Select
                    IconComponent={ArrowDropDownIcon}
                    value={selectedPlant}
                    onChange={handlePlantChange}
                    disabled={!plants.length}
                    sx={selectStyle}
                    MenuProps={menuPropsStyle}
                    renderValue={(value) => {
                      const plant = plants.find((p) => p.id === value)
                      return (
                        <Box
                          sx={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 1,
                          }}
                        >
                          <Factory
                            sx={{ fontSize: 16, color: '#638f11 !important' }}
                          />
                          <Box
                            component='span'
                            sx={{
                              color: '#6b7c93',
                              fontWeight: 600,
                              fontSize: '0.85rem',
                            }}
                          >
                            Plant:
                          </Box>
                          <Box
                            component='strong'
                            sx={{ fontWeight: 800, color: '#0f172a', ml: 0.5 }}
                          >
                            {plant?.name}
                          </Box>
                        </Box>
                      )
                    }}
                  >
                    {plants.map((p) => (
                      <MenuItem key={p.id} value={p.id} sx={menuItemStyle}>
                        {p.name}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              )}
            </Box>
          )}
        </Stack>

        {/* RIGHT: Profile */}
        <Box sx={{ justifySelf: 'end' }}>
          {!matchesXs ? <Profile keycloak={keycloak} /> : <MobileSection />}
        </Box>
      </Box>
    </>
  )
}
