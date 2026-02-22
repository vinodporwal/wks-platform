import {
  Box,
  FormControl,
  MenuItem,
  Select,
  Stack,
  Typography,
  useMediaQuery,
} from '@mui/material'
import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { DataService } from 'services/DataService'
import {
  setAopYear,
  setCurrentYear,
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

import Logo from 'assets/images/ril-logo2.png'
import DropdownSkeleton from 'utils/DropdownSkeleton'
import {
  useLocation,
  useNavigate,
} from '../../../../../node_modules/react-router-dom/dist/index'
import { openDrawer } from 'store/reducers/menu'
import StepperNav from 'components/Utilities/StepperNav'
import CalendarToday from '@mui/icons-material/CalendarToday'
import Business from '@mui/icons-material/Business'
import Domain from '@mui/icons-material/Domain'
import Factory from '@mui/icons-material/Factory'

// Utility to parse the Keycloak allowed JSON
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
    // }, [keycloak, verticalFromDashboard])
  }, [keycloak])

  useEffect(() => {
    if (!fullDetails.length || !Object.keys(allowedMap).length) return

    const avail = fullDetails
      .filter((v) => allowedMap[v.id])
      .map((v) => ({ id: v.id, name: v.displayName }))

    setVerticals(avail)

    // --- Startt snippet ---
    /* first available vertical so dropdown won't be empty */
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
    // --- end snippet ---

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
      // setHeaderLoading(true)
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

  useEffect(() => {
    if (!verticalFromDashboard?.vid || !verticalFromDashboard?.sid) return

    if (verticalFromDashboard?.vid === selectedVertical) return

    setSelectedVertical(verticalFromDashboard?.vid)
  }, [verticalFromDashboard?.vid])

  useEffect(() => {
    if (!verticalFromDashboard?.sid || !sites.length) return

    const site = sites.find((s) => s?.id === verticalFromDashboard?.sid)

    if (!site) {
      console.log('Site not found:', verticalFromDashboard?.sid)
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

  const navigate = useNavigate()

  useEffect(() => {
    if (!verticalFromDashboard?.vid || !verticalFromDashboard?.sid) {
      return
    }
    setTimeout(() => {
      dispatch(openDrawer({ drawerOpen: true }))
    }, 1500)
    navigate('/production-norms-plan/configuration', { replace: true })
  }, [verticalFromDashboard?.trigger])

  const menuPropsStyle = {
    PaperProps: {
      style: {
        maxHeight: 200,
        borderRadius: '12px',
        marginTop: '4px',
        background: 'rgba(0,0,0,0.75)',
        backdropFilter: 'blur(6px)',
        // boxShadow:
        //   '0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.08)',
        border: '1px solid rgba(255, 255, 255, 0.06)',
        color: '#fff',
      },
    },
    disableScrollLock: true,
  }

  const menuItemStyle = {
    transition: 'all 0.2s ease',
    borderRadius: '6px',
    mx: 0.5,
    my: 0.25,
    color: '#ffffff',
    fontWeight: 700,
    '&:hover': {
      background:
        'linear-gradient(90deg, rgba(1, 0, 203, 0.08) 0%, rgba(91, 89, 255, 0.06) 100%)',
      transform: 'translateX(4px)',
    },
    '&.Mui-selected': {
      background:
        'linear-gradient(90deg, rgba(1, 0, 203, 0.12) 0%, rgba(91, 89, 255, 0.08) 100%)',
      fontWeight: 700,
      '&:hover': {
        background:
          'linear-gradient(90deg, rgba(1, 0, 203, 0.15) 0%, rgba(91, 89, 255, 0.1) 100%)',
      },
    },
  }

  const dropdownContainerStyle = {
    display: 'flex',
    alignItems: 'center',
  }

  const selectStyle = {
    height: 36,
    minWidth: 150,
    borderRadius: '12px',
    background: '#e9edf2',
    fontSize: '0.9rem',
    fontWeight: 600,
    color: '#2c3e50',

    '& .MuiSelect-select': {
      display: 'flex',
      alignItems: 'center',
      gap: 0.75,
      padding: '6px 8px !important',
    },

    '& fieldset': {
      border: 'none',
    },

    '&:hover': {
      background: '#dde3ea',
    },

    '&.Mui-focused': {
      background: '#dde3ea',
    },

    '& .MuiSvgIcon-root': {
      color: '#6b7c93',
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
          px: 1,
          py: 0.125,
          '&::after': {
            content: '""',
            position: 'absolute',
            bottom: '-4px',
            left: 0,
            right: 0,
            height: '1px',
            background:
              'linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.2) 50%, transparent 100%)',
          },
        }}
      >
        {/* LEFT SIDE: Logo + Title (grid column 1) */}
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 2,
            animation: 'fadeInLeft 0.5s ease-out',
            '@keyframes fadeInLeft': {
              from: { opacity: 0, transform: 'translateX(-20px)' },
              to: { opacity: 1, transform: 'translateX(0)' },
            },
            minWidth: 0,
            overflow: 'hidden',
          }}
        >
          {/* Logo Container */}
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              px: 1,
              py: 0.5,
              borderRadius: '8px',
              transition: 'all 0.3s ease',
              background: 'transparent',
              '&:hover': {
                transform: 'translateY(-1px)',
                boxShadow: '0 4px 10px rgba(0,0,0,0.12)',
              },
              flex: '0 0 auto',
            }}
          >
            <Box
              component='img'
              src={Logo}
              alt='RIL Logo'
              sx={{
                height: 32,
                objectFit: 'contain',
              }}
            />
          </Box>

          {/* Title */}
          {!HIDE_DASHBOARD_DROPDOWN && (
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                minWidth: 0,
                overflow: 'hidden',
              }}
            >
              <Typography
                variant='body2'
                className='custom-title-font'
                sx={{
                  fontWeight: 700,
                  fontSize: '1.05rem',
                  letterSpacing: '0.03em',
                  textTransform: 'none',
                  color: '#2c3e50', // dark text for white header
                  lineHeight: 1.2,
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                }}
              >
                {screenTitleName}
              </Typography>
            </Box>
          )}
        </Box>

        {/* CENTERED DROPDOWNS (grid column 2) */}
        <Stack
          direction='row'
          spacing={2}
          alignItems='center'
          sx={{
            justifySelf: 'end',
            width: '100%', // allow stack to fill the middle column
            justifyContent: 'flex-end', // push dropdowns to the right edge of that column
            mr: 1,
          }}
        >
          {/* Year */}
          <Box sx={dropdownContainerStyle}>
            {headerLoading ? (
              <DropdownSkeleton />
            ) : (
              <FormControl>
                <Select
                  value={selectedYear}
                  onChange={handleYearChange}
                  sx={selectStyle}
                  MenuProps={menuPropsStyle}
                  renderValue={(value) => {
                    const yearObj = aopYears.find((y) => y.AOPYear === value)
                    return (
                      <>
                        <CalendarToday sx={{ fontSize: 16 }} />
                        <span>Year:</span>
                        <strong>{yearObj?.AOPDisplayYear}</strong>
                      </>
                    )
                  }}
                >
                  {aopYears.map((y) => (
                    <MenuItem key={y.AOPYear} value={y.AOPYear}>
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
                <FormControl>
                  <Select
                    value={selectedVertical}
                    onChange={handleVertChange}
                    sx={selectStyle}
                    MenuProps={menuPropsStyle}
                    renderValue={(value) => {
                      const vert = verticals.find((v) => v.id === value)
                      return (
                        <>
                          <Business sx={{ fontSize: 16 }} />
                          <span>Vertical:</span>
                          <strong>{vert?.name}</strong>
                        </>
                      )
                    }}
                  >
                    {verticals.map((v) => (
                      <MenuItem key={v.id} value={v.id}>
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
                <FormControl>
                  <Select
                    value={selectedSite}
                    onChange={handleSiteChange}
                    disabled={!sites.length}
                    sx={selectStyle}
                    MenuProps={menuPropsStyle}
                    renderValue={(value) => {
                      const site = sites.find((s) => s.id === value)
                      return (
                        <>
                          <Domain sx={{ fontSize: 16 }} />
                          <span>Site:</span>
                          <strong>{site?.name}</strong>
                        </>
                      )
                    }}
                  >
                    {sites.map((s) => (
                      <MenuItem key={s.id} value={s.id}>
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
                <FormControl>
                  <Select
                    value={selectedPlant}
                    onChange={handlePlantChange}
                    disabled={!plants.length}
                    sx={selectStyle}
                    MenuProps={menuPropsStyle}
                    renderValue={(value) => {
                      const plant = plants.find((p) => p.id === value)
                      return (
                        <>
                          <Factory sx={{ fontSize: 16 }} />
                          <span>Plant:</span>
                          <strong>{plant?.name}</strong>
                        </>
                      )
                    }}
                  >
                    {plants.map((p) => (
                      <MenuItem key={p.id} value={p.id}>
                        {p.name}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              )}
            </Box>
          )}
        </Stack>

        {/* RIGHT SIDE: Profile / Mobile (grid column 3) */}
        <Box sx={{ justifySelf: 'end' }}>
          {!matchesXs ? <Profile keycloak={keycloak} /> : <MobileSection />}
        </Box>
      </Box>
    </>
  )
}
