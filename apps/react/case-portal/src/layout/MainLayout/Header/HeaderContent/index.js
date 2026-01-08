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
  }, [keycloak])

  useEffect(() => {
    if (!fullDetails.length || !Object.keys(allowedMap).length) return

    const avail = fullDetails
      .filter((v) => allowedMap[v.id])
      .map((v) => ({ id: v.id, name: v.displayName }))

    setVerticals(avail)

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

  // Common dropdown styles
  const dropdownContainerStyle = {
    display: 'flex',
    alignItems: 'center',
    gap: 1,
    animation: 'fadeInScale 0.4s ease-out backwards',
    '@keyframes fadeInScale': {
      from: { opacity: 0, transform: 'scale(0.95)' },
      to: { opacity: 1, transform: 'scale(1)' },
    },
  }

  const selectStyle = {
    '& .MuiOutlinedInput-notchedOutline': {
      border: '1px solid rgba(255, 255, 255, 0.25)',
      borderRadius: '2px',
      transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
    },
    '&:hover .MuiOutlinedInput-notchedOutline': {
      border: '1px solid rgba(255, 255, 255, 0.4)',
      boxShadow: '0 2px 8px rgba(255, 255, 255, 0.1)',
    },
    '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
      border: '1px solid rgba(255, 255, 255, 0.6)',
      boxShadow: '0 4px 12px rgba(255, 255, 255, 0.15)',
    },
    '& .MuiSelect-select': {
      py: 0.75,
      transition: 'all 0.3s ease',
      color: '#ffffff',
      fontWeight: 700,
    },
    '& .MuiSvgIcon-root': {
      color: '#ffffff',
    },
    '&.Mui-disabled': {
      opacity: 0.5,
    },
    background: 'rgba(255, 255, 255, 0.08)',
    backdropFilter: 'blur(8px)',
    borderRadius: '2px',
    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
    '&:hover': {
      background: 'rgba(255, 255, 255, 0.12)',
    },
  }

  const menuPropsStyle = {
    PaperProps: {
      style: {
        maxHeight: 200,
        borderRadius: '12px',
        marginTop: '4px',
        background: 'rgba(0,0,0,0.75)',
        backdropFilter: 'blur(6px)',
        boxShadow:
          '0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.08)',
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

  return (
    <>
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          width: '100%',
          position: 'relative',
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
        {/* LEFT SIDE: Logo + Title */}
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 1.5,
            animation: 'fadeInLeft 0.5s ease-out',
            '@keyframes fadeInLeft': {
              from: { opacity: 0, transform: 'translateX(-20px)' },
              to: { opacity: 1, transform: 'translateX(0)' },
            },
          }}
        >
          <Box
            sx={{
              ml: 0,
              display: 'flex',
              alignItems: 'center',
              px: 1,
              py: 0.5,
              borderRadius: '10px',
              backdropFilter: 'blur(6px)',
              transition: 'all 0.3s ease',
              '&:hover': {
                transform: 'translateY(-1px) scale(1.05)',
                boxShadow: '0 6px 16px rgba(0,0,0,0.35)',
              },
            }}
          >
            <Box
              component='img'
              src={Logo}
              alt='RIL Logo'
              sx={{
                height: 32,
                transition: 'all 0.3s ease',
              }}
            />
          </Box>

          {!HIDE_DASHBOARD_DROPDOWN && (
            <Box
              sx={{
                ml: 0.75,
                px: 1.5,
                py: 0.5,
                borderRadius: '8px',
                display: 'flex',
                alignItems: 'center',
                // background: 'linear-gradient(135deg, rgba(255,255,255,0.18), rgba(255,255,255,0.08))',
                backdropFilter: 'blur(6px)',
                // boxShadow: '0 4px 12px rgba(0,0,0,0.25)',
              }}
            >
              <Typography
                variant='body2'
                className='custom-title-font'
                sx={{
                  fontWeight: 1000,
                  fontSize: '1rem',
                  letterSpacing: '0.04em',
                  textTransform: 'uppercase',
                  color: '#fff',
                  lineHeight: 1,
                }}
              >
                {screenTitleName}
              </Typography>
            </Box>
          )}
        </Box>

        {/* CENTERED DROPDOWNS */}
        <Stack
          direction='row'
          spacing={1.5}
          alignItems='center'
          sx={{
            position: 'absolute',
            left: '50%',
            top: '50%',
            transform: 'translate(-50%, -50%)',
            animation: 'fadeInRight 0.5s ease-out',
            '@keyframes fadeInRight': {
              from: { opacity: 0, transform: 'translateX(20px)' },
              to: { opacity: 1, transform: 'translateX(0)' },
            },
          }}
        >
          {/* Year */}
          <Box sx={{ ...dropdownContainerStyle, '--animation-delay': '0s' }}>
            <Typography
              variant='body2'
              className='custom-title-dropdown'
              sx={{
                fontSize: '0.875rem',
                textShadow: '0 1px 2px rgba(0, 0, 0, 0.2)',
                color: '#fff',
                fontWeight: 700,
              }}
            >
              Year:
            </Typography>
            {headerLoading ? (
              <DropdownSkeleton />
            ) : (
              <FormControl sx={{ width: 80 }}>
                <Select
                  value={selectedYear}
                  onChange={handleYearChange}
                  className='custom-title-dropdown-content'
                  sx={selectStyle}
                  MenuProps={menuPropsStyle}
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
            <Box
              sx={{ ...dropdownContainerStyle, '--animation-delay': '0.1s' }}
            >
              <Typography
                variant='body2'
                className='custom-title-dropdown'
                sx={{
                  fontSize: '0.875rem',
                  textShadow: '0 1px 2px rgba(0, 0, 0, 0.2)',
                  color: '#ffffffff',
                  fontWeight: 700,
                }}
              >
                Vertical:
              </Typography>

              {headerLoading ? (
                <DropdownSkeleton />
              ) : (
                <FormControl sx={{ width: 100 }}>
                  <Select
                    value={selectedVertical}
                    onChange={handleVertChange}
                    className='custom-title-dropdown-content'
                    sx={selectStyle}
                    MenuProps={menuPropsStyle}
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
            <Box
              sx={{ ...dropdownContainerStyle, '--animation-delay': '0.2s' }}
            >
              <Typography
                variant='body2'
                className='custom-title-dropdown'
                sx={{
                  fontSize: '0.875rem',
                  textShadow: '0 1px 2px rgba(0, 0, 0, 0.2)',
                  color: '#fff',
                  fontWeight: 700,
                }}
              >
                Site:
              </Typography>
              {headerLoading ? (
                <DropdownSkeleton />
              ) : (
                <FormControl sx={{ width: 80 }}>
                  <Select
                    value={selectedSite}
                    onChange={handleSiteChange}
                    disabled={!sites.length}
                    className='custom-title-dropdown-content'
                    sx={selectStyle}
                    MenuProps={menuPropsStyle}
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
            <Box
              sx={{ ...dropdownContainerStyle, '--animation-delay': '0.3s' }}
            >
              <Typography
                variant='body2'
                className='custom-title-dropdown'
                sx={{
                  fontSize: '0.875rem',
                  textShadow: '0 1px 2px rgba(0, 0, 0, 0.2)',
                  color: '#fff',
                  fontWeight: 700,
                }}
              >
                Plant:
              </Typography>
              {headerLoading ? (
                <DropdownSkeleton />
              ) : (
                <FormControl sx={{ width: 110 }}>
                  <Select
                    value={selectedPlant}
                    onChange={handlePlantChange}
                    disabled={!plants.length}
                    className='custom-title-dropdown-content'
                    sx={selectStyle}
                    MenuProps={menuPropsStyle}
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

        {/* RIGHT SIDE: Profile / Mobile */}
        {!matchesXs ? <Profile keycloak={keycloak} /> : <MobileSection />}
      </Box>
    </>
  )
}
