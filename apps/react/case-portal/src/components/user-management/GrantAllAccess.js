import AdminPanelSettingsOutlinedIcon from '@mui/icons-material/AdminPanelSettingsOutlined'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked'
import RestartAltIcon from '@mui/icons-material/RestartAlt'
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined'
import SearchIcon from '@mui/icons-material/Search'
import VerifiedUserOutlinedIcon from '@mui/icons-material/VerifiedUserOutlined'
import {
  Autocomplete,
  Avatar,
  Box,
  Button,
  Chip,
  CircularProgress,
  InputAdornment,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import Notification from 'components/Utilities/Notification'
import Config from 'consts/index'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useSelector } from 'react-redux'
import { DataService } from 'services/DataService'

const extractScreens = (nodes) => {
  const result = []

  const recurse = (list) => {
    if (!Array.isArray(list)) return
    for (const node of list) {
      if (node.type === 'item' && node.url) {
        result.push({
          id: node.id,
          title: node.title,
          url: node.url,
          icon: node.icon || '',
        })
      }
      if (Array.isArray(node.children)) {
        recurse(node.children)
      }
    }
  }

  recurse(nodes)
  return result
}

const collectPlantIdsForVerticals = (selectedVerticalIds, plantSiteData) => {
  const plantIds = new Set()
  selectedVerticalIds.forEach((verticalId) => {
    const vertical = plantSiteData.find((v) => v.id === verticalId)
    const sites = vertical?.sites || []
    sites.forEach((siteObj) => {
      const plants = siteObj.plants || []
      plants.forEach((plantObj) => {
        if (plantObj?.id) {
          plantIds.add(plantObj.id)
        }
      })
    })
  })
  return Array.from(plantIds)
}

const fetchPlantScreensMap = async ({
  plantIds,
  existingScreensByPlant = {},
  keycloak,
  aopYear,
  concurrency = 8,
}) => {
  const missingPlantIds = Array.from(new Set(plantIds)).filter(
    (pid) => pid && !existingScreensByPlant[pid],
  )
  if (missingPlantIds.length === 0) {
    return {}
  }

  const fetched = {}
  let nextIndex = 0
  const workerCount = Math.min(concurrency, missingPlantIds.length)

  const worker = async () => {
    while (nextIndex < missingPlantIds.length) {
      const pid = missingPlantIds[nextIndex++]
      const response = await DataService.getPlantScreenMapping(
        keycloak,
        pid,
        aopYear,
      )
      const groups = response?.data || []
      fetched[pid] = Array.from(
        new Set(extractScreens(groups).map((s) => s.title)),
      )
    }
  }

  await Promise.all(Array.from({ length: workerCount }, () => worker()))
  return fetched
}

const getInitials = (user) => {
  const first = user?.firstName?.[0] || ''
  const last = user?.lastName?.[0] || ''
  if (first || last) return `${first}${last}`.toUpperCase()
  return (user?.username?.slice(0, 2) || 'U').toUpperCase()
}

const getUserSubtitle = (user) => {
  const fullName = [user?.firstName, user?.lastName].filter(Boolean).join(' ')
  if (fullName && user?.email) return `${fullName} • ${user.email}`
  return fullName || user?.email || ''
}

const GrantAllAccess = ({ keycloak, plantSiteData = [] }) => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const aopYear =
    dataGridStore?.year?.selectedYear || localStorage.getItem('year') || ''

  const [selectedUsers, setSelectedUsers] = useState([])
  const [inputValue, setInputValue] = useState('')
  const [searchOptions, setSearchOptions] = useState([])
  const [searchLoading, setSearchLoading] = useState(false)
  const [open, setOpen] = useState(false)

  const [selectedVerticals, setSelectedVerticals] = useState([])
  const [verticalFilter, setVerticalFilter] = useState('')
  const [screensByPlant, setScreensByPlant] = useState({})
  const [saving, setSaving] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const searchAbortControllerRef = useRef(null)
  const searchDebounceRef = useRef(null)
  const latestSearchQueryRef = useRef('')

  const cancelPendingUserSearch = () => {
    if (searchDebounceRef.current) {
      clearTimeout(searchDebounceRef.current)
      searchDebounceRef.current = null
    }
    if (searchAbortControllerRef.current) {
      searchAbortControllerRef.current.abort()
      searchAbortControllerRef.current = null
    }
  }

  useEffect(() => {
    return () => cancelPendingUserSearch()
  }, [])

  const verticalMetaMap = useMemo(() => {
    const map = {}
    plantSiteData.forEach((vertical) => {
      const validSites = (vertical?.sites || []).filter(
        (s) => (s.plants || []).length > 0,
      )
      const plantCount = validSites.reduce(
        (sum, s) => sum + (s.plants || []).length,
        0,
      )
      map[vertical.id] = {
        siteCount: validSites.length,
        plantCount,
      }
    })
    return map
  }, [plantSiteData])

  const filteredVerticals = useMemo(() => {
    const query = verticalFilter.trim().toLowerCase()
    if (!query) return plantSiteData
    return plantSiteData.filter((v) =>
      (v.displayName || v.name || '').toLowerCase().includes(query),
    )
  }, [plantSiteData, verticalFilter])

  const selectedSummary = useMemo(() => {
    let siteCount = 0
    let plantCount = 0
    selectedVerticals.forEach((verticalId) => {
      const meta = verticalMetaMap[verticalId]
      if (meta) {
        siteCount += meta.siteCount
        plantCount += meta.plantCount
      }
    })
    return { siteCount, plantCount }
  }, [selectedVerticals, verticalMetaMap])

  const allVerticalsSelected =
    plantSiteData.length > 0 &&
    selectedVerticals.length === plantSiteData.length

  const handleSearchChange = (value) => {
    latestSearchQueryRef.current = value
    cancelPendingUserSearch()

    if (value.trim().length <= 2) {
      setSearchOptions([])
      setSearchLoading(false)
      return
    }

    setSearchLoading(true)
    searchDebounceRef.current = setTimeout(async () => {
      const controller = new AbortController()
      searchAbortControllerRef.current = controller

      try {
        const url = `${Config.CaseEngineUrl}/task/users/search?search=${encodeURIComponent(value)}`
        const resp = await fetch(url, {
          method: 'GET',
          headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json',
            Authorization: `Bearer ${keycloak.token}`,
          },
          signal: controller.signal,
        })

        if (!resp.ok) {
          throw new Error(`API Error: ${resp.status} ${resp.statusText}`)
        }

        const res = await resp.json()
        if (
          !controller.signal.aborted &&
          latestSearchQueryRef.current === value
        ) {
          setSearchOptions(res?.data || [])
        }
      } catch (err) {
        if (err?.name === 'AbortError') {
          return
        }
        console.error('Search user API failed:', err)
      } finally {
        if (searchAbortControllerRef.current === controller) {
          searchAbortControllerRef.current = null
          setSearchLoading(false)
        }
      }
    }, 250)
  }

  const toggleVertical = (verticalId) => {
    setSelectedVerticals((prev) =>
      prev.includes(verticalId)
        ? prev.filter((id) => id !== verticalId)
        : [...prev, verticalId],
    )
  }

  const handleToggleAllVerticals = () => {
    setSelectedVerticals(
      allVerticalsSelected ? [] : plantSiteData.map((v) => v.id),
    )
  }

  const handleReset = () => {
    cancelPendingUserSearch()
    setSelectedUsers([])
    setInputValue('')
    setSearchOptions([])
    setSearchLoading(false)
    setSelectedVerticals([])
    setVerticalFilter('')
  }

  const handleSave = async () => {
    if (selectedUsers.length === 0 || selectedVerticals.length === 0) {
      setSnackbarData({
        message: 'Please select at least one user and one vertical.',
        severity: 'warning',
      })
      setSnackbarOpen(true)
      return
    }

    setSaving(true)
    try {
      let effectiveYear = aopYear
      if (!effectiveYear) {
        const years = await DataService.getAopyears(keycloak)
        effectiveYear =
          years?.find((item) => item.currentYear == 1)?.AOPYear || ''
      }

      if (!effectiveYear) {
        throw new Error('AOP Year is not available')
      }

      const plantIds = collectPlantIdsForVerticals(
        selectedVerticals,
        plantSiteData,
      )
      const cachedForYear = screensByPlant[effectiveYear] || {}
      const newlyFetched = await fetchPlantScreensMap({
        plantIds,
        existingScreensByPlant: cachedForYear,
        keycloak,
        aopYear: effectiveYear,
      })

      const mergedScreensByPlant = {
        ...cachedForYear,
        ...newlyFetched,
      }

      if (Object.keys(newlyFetched).length > 0) {
        setScreensByPlant((prev) => ({
          ...prev,
          [effectiveYear]: {
            ...(prev[effectiveYear] || {}),
            ...newlyFetched,
          },
        }))
      }

      const userIds = Array.from(
        new Set(selectedUsers.map((user) => user.id).filter(Boolean)),
      )

      const payload = {
        role: 'cts_head',
        userIds,
        attributes: {
          plants: selectedVerticals.map((verticalId) => {
            const vertical = plantSiteData.find((v) => v.id === verticalId)
            const sites = (vertical?.sites || [])
              .map((siteObj) => {
                const plants = (siteObj.plants || []).map((plantObj) => ({
                  plantId: plantObj.id,
                  screens: mergedScreensByPlant[plantObj.id] || [],
                  permission: [],
                }))
                return {
                  siteId: siteObj.id,
                  plants,
                }
              })
              .filter((s) => s.plants.length > 0)

            return {
              verticalId,
              sites,
            }
          }),
        },
      }

      const res = await DataService.updateUserPlants(keycloak, payload)
      if (res?.status !== 200) {
        throw new Error('Failed to update user')
      }

      setSnackbarData({
        message: 'User Access Granted successfully!',
        severity: 'success',
      })
      setSnackbarOpen(true)
      handleReset()
    } catch (error) {
      console.error('Grant all access failed:', error)
      setSnackbarData({
        message: 'Failed to update user. Please try again.',
        severity: 'error',
      })
      setSnackbarOpen(true)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Paper
      elevation={0}
      sx={{
        borderRadius: '14px',
        border: '1px solid #e2e8f0',
        bgcolor: '#ffffff',
        boxShadow: '0 4px 20px rgba(15, 23, 42, 0.04)',
        overflow: 'hidden',
      }}
    >
      <LoaderBackdrop open={saving} />

      {/* Header */}
      <Box
        sx={{
          px: 3,
          py: 2,
          background: 'linear-gradient(135deg, #f8fafc 0%, #eff6ff 100%)',
          borderBottom: '1px solid #e2e8f0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: 2,
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <Box
            sx={{
              width: 40,
              height: 40,
              borderRadius: '10px',
              bgcolor: '#dbeafe',
              color: '#2563eb',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <AdminPanelSettingsOutlinedIcon fontSize='small' />
          </Box>
          <Box>
            <Typography
              sx={{
                fontWeight: 700,
                fontSize: '1rem',
                color: '#0f172a',
                lineHeight: 1.25,
              }}
            >
              Grant All Access
            </Typography>
            <Typography
              sx={{ fontSize: '0.78rem', color: '#64748b', mt: 0.25 }}
            >
              Assign all sites, plants, and mapped plant screens for selected
              verticals in one click.
            </Typography>
          </Box>
        </Box>

        <Stack direction='row' spacing={1} flexWrap='wrap' useFlexGap>
          <Chip
            size='small'
            label={`${selectedUsers.length} User${selectedUsers.length === 1 ? '' : 's'}`}
            sx={{
              fontWeight: 600,
              fontSize: '0.75rem',
              bgcolor: selectedUsers.length > 0 ? '#dbeafe' : '#f1f5f9',
              color: selectedUsers.length > 0 ? '#1d4ed8' : '#64748b',
            }}
          />
          <Chip
            size='small'
            label={`${selectedVerticals.length} Vertical${selectedVerticals.length === 1 ? '' : 's'}`}
            sx={{
              fontWeight: 600,
              fontSize: '0.75rem',
              bgcolor: selectedVerticals.length > 0 ? '#dbeafe' : '#f1f5f9',
              color: selectedVerticals.length > 0 ? '#1d4ed8' : '#64748b',
            }}
          />
          {selectedSummary.plantCount > 0 && (
            <Chip
              size='small'
              label={`${selectedSummary.siteCount} Sites • ${selectedSummary.plantCount} Plants`}
              sx={{
                fontWeight: 600,
                fontSize: '0.75rem',
                bgcolor: '#ecfdf5',
                color: '#047857',
              }}
            />
          )}
        </Stack>
      </Box>

      {/* Main 2-step body */}
      <Box
        sx={{
          p: 3,
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', md: '1fr 1.25fr' },
          gap: 2.5,
          alignItems: 'stretch',
        }}
      >
        {/* Step 1: Select Users */}
        <Box
          sx={{
            p: 2.25,
            borderRadius: '12px',
            border: '1px solid #e2e8f0',
            bgcolor: '#fafbfc',
            display: 'flex',
            flexDirection: 'column',
            gap: 1.75,
          }}
        >
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Box
                sx={{
                  width: 24,
                  height: 24,
                  borderRadius: '50%',
                  bgcolor: '#2563eb',
                  color: '#ffffff',
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                1
              </Box>
              <Typography
                sx={{ fontWeight: 700, fontSize: '0.9rem', color: '#1e293b' }}
              >
                Select Users
              </Typography>
            </Box>

            {selectedUsers.length > 0 && (
              <Button
                size='small'
                onClick={() => setSelectedUsers([])}
                sx={{
                  textTransform: 'none',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  color: '#64748b',
                  minWidth: 'auto',
                  px: 1,
                }}
              >
                Clear
              </Button>
            )}
          </Box>

          <Autocomplete
            multiple
            disableCloseOnSelect
            open={open}
            onOpen={() => setOpen(true)}
            onClose={() => setOpen(false)}
            options={searchOptions}
            getOptionLabel={(opt) => opt?.username || ''}
            isOptionEqualToValue={(option, value) => option?.id === value?.id}
            filterOptions={(options) => {
              const selectedIds = new Set(selectedUsers.map((u) => u.id))
              return options.filter((opt) => !selectedIds.has(opt.id))
            }}
            value={selectedUsers}
            inputValue={inputValue}
            onInputChange={(e, newVal, reason) => {
              if (reason === 'input') {
                setInputValue(newVal)
                handleSearchChange(newVal)
              } else if (reason === 'clear') {
                cancelPendingUserSearch()
                setInputValue('')
                setSearchOptions([])
                setSearchLoading(false)
              }
            }}
            onChange={(e, newSel) => {
              cancelPendingUserSearch()
              setSelectedUsers(newSel)
              setInputValue('')
              setSearchLoading(false)
            }}
            loading={searchLoading}
            noOptionsText={
              inputValue.trim().length <= 2
                ? 'Type at least 3 characters to search'
                : 'No users found'
            }
            renderTags={() => null}
            renderOption={(props, option) => {
              const subtitle = getUserSubtitle(option)
              return (
                <li key={option.id} {...props}>
                  <Box
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 1.25,
                      py: 0.25,
                    }}
                  >
                    <Avatar
                      sx={{
                        width: 28,
                        height: 28,
                        fontSize: '0.72rem',
                        fontWeight: 700,
                        bgcolor: '#eff6ff',
                        color: '#2563eb',
                      }}
                    >
                      {getInitials(option)}
                    </Avatar>
                    <Box>
                      <Typography
                        sx={{
                          fontSize: '0.84rem',
                          fontWeight: 600,
                          color: '#0f172a',
                          lineHeight: 1.2,
                        }}
                      >
                        {option.username}
                      </Typography>
                      {subtitle && (
                        <Typography
                          sx={{ fontSize: '0.72rem', color: '#64748b' }}
                        >
                          {subtitle}
                        </Typography>
                      )}
                    </Box>
                  </Box>
                </li>
              )
            }}
            renderInput={(params) => (
              <TextField
                {...params}
                size='small'
                placeholder='Search by username, name, or email...'
                variant='outlined'
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: '10px',
                    bgcolor: '#ffffff',
                    fontSize: '0.85rem',
                  },
                }}
                InputProps={{
                  ...params.InputProps,
                  startAdornment: (
                    <InputAdornment position='start' sx={{ ml: 0.5 }}>
                      <SearchIcon sx={{ fontSize: 18, color: '#94a3b8' }} />
                    </InputAdornment>
                  ),
                  endAdornment: (
                    <>
                      {searchLoading && (
                        <CircularProgress color='primary' size={16} />
                      )}
                      {params.InputProps.endAdornment}
                    </>
                  ),
                }}
              />
            )}
          />

          {/* Selected users tray */}
          <Box
            sx={{
              flex: 1,
              minHeight: 150,
              maxHeight: 240,
              overflowY: 'auto',
              p: 1.5,
              borderRadius: '10px',
              border: '1px dashed #cbd5e1',
              bgcolor: '#ffffff',
            }}
          >
            {selectedUsers.length === 0 ? (
              <Box
                sx={{
                  height: '100%',
                  minHeight: 120,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  textAlign: 'center',
                  px: 2,
                }}
              >
                <Typography sx={{ fontSize: '0.8rem', color: '#94a3b8' }}>
                  No users selected yet. Search above to add one or more users.
                </Typography>
              </Box>
            ) : (
              <Stack direction='row' flexWrap='wrap' gap={1}>
                {selectedUsers.map((user) => (
                  <Chip
                    key={user.id}
                    avatar={
                      <Avatar
                        sx={{
                          bgcolor: '#2563eb !important',
                          color: '#ffffff !important',
                          fontSize: '0.68rem !important',
                          fontWeight: 700,
                        }}
                      >
                        {getInitials(user)}
                      </Avatar>
                    }
                    label={user.username}
                    onDelete={() =>
                      setSelectedUsers((prev) =>
                        prev.filter((u) => u.id !== user.id),
                      )
                    }
                    sx={{
                      borderRadius: '8px',
                      bgcolor: '#eff6ff',
                      color: '#1e3a8a',
                      fontWeight: 600,
                      fontSize: '0.78rem',
                      border: '1px solid #bfdbfe',
                      '& .MuiChip-deleteIcon': {
                        color: '#60a5fa',
                        '&:hover': { color: '#1d4ed8' },
                      },
                    }}
                  />
                ))}
              </Stack>
            )}
          </Box>
        </Box>

        {/* Step 2: Select Verticals */}
        <Box
          sx={{
            p: 2.25,
            borderRadius: '12px',
            border: '1px solid #e2e8f0',
            bgcolor: '#fafbfc',
            display: 'flex',
            flexDirection: 'column',
            gap: 1.75,
          }}
        >
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 1,
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Box
                sx={{
                  width: 24,
                  height: 24,
                  borderRadius: '50%',
                  bgcolor: '#2563eb',
                  color: '#ffffff',
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                2
              </Box>
              <Typography
                sx={{ fontWeight: 700, fontSize: '0.9rem', color: '#1e293b' }}
              >
                Select Verticals
              </Typography>
            </Box>

            <Button
              size='small'
              variant={allVerticalsSelected ? 'outlined' : 'text'}
              disabled={plantSiteData.length === 0}
              onClick={handleToggleAllVerticals}
              sx={{
                textTransform: 'none',
                fontSize: '0.76rem',
                fontWeight: 600,
                borderRadius: '8px',
                px: 1.25,
                py: 0.4,
              }}
            >
              {allVerticalsSelected ? 'Clear All' : 'Select All'}
            </Button>
          </Box>

          {plantSiteData.length > 6 && (
            <TextField
              size='small'
              value={verticalFilter}
              onChange={(e) => setVerticalFilter(e.target.value)}
              placeholder='Filter verticals...'
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: '10px',
                  bgcolor: '#ffffff',
                  fontSize: '0.84rem',
                },
              }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position='start'>
                    <SearchIcon sx={{ fontSize: 17, color: '#94a3b8' }} />
                  </InputAdornment>
                ),
              }}
            />
          )}

          <Box
            sx={{
              flex: 1,
              minHeight: 150,
              maxHeight: 240,
              overflowY: 'auto',
              pr: 0.5,
            }}
          >
            {plantSiteData.length === 0 ? (
              <Box
                sx={{
                  height: 150,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 1,
                }}
              >
                <CircularProgress size={18} />
                <Typography sx={{ fontSize: '0.82rem', color: '#64748b' }}>
                  Loading verticals...
                </Typography>
              </Box>
            ) : filteredVerticals.length === 0 ? (
              <Box
                sx={{
                  height: 150,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <Typography sx={{ fontSize: '0.82rem', color: '#94a3b8' }}>
                  No matching verticals found.
                </Typography>
              </Box>
            ) : (
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
                  gap: 1.25,
                }}
              >
                {filteredVerticals.map((vertical) => {
                  const isSelected = selectedVerticals.includes(vertical.id)
                  const meta = verticalMetaMap[vertical.id] || {
                    siteCount: 0,
                    plantCount: 0,
                  }

                  return (
                    <Box
                      key={vertical.id}
                      onClick={() => toggleVertical(vertical.id)}
                      role='button'
                      tabIndex={0}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault()
                          toggleVertical(vertical.id)
                        }
                      }}
                      sx={{
                        p: 1.35,
                        borderRadius: '10px',
                        border: '1px solid',
                        borderColor: isSelected ? '#2563eb' : '#e2e8f0',
                        bgcolor: isSelected ? '#eff6ff' : '#ffffff',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: 1.1,
                        transition: 'all 150ms ease',
                        userSelect: 'none',
                        '&:hover': {
                          borderColor: isSelected ? '#1d4ed8' : '#94a3b8',
                          bgcolor: isSelected ? '#eff6ff' : '#f8fafc',
                        },
                      }}
                    >
                      {isSelected ? (
                        <CheckCircleIcon
                          sx={{ fontSize: 19, color: '#2563eb', mt: 0.15 }}
                        />
                      ) : (
                        <RadioButtonUncheckedIcon
                          sx={{ fontSize: 19, color: '#cbd5e1', mt: 0.15 }}
                        />
                      )}
                      <Box sx={{ minWidth: 0 }}>
                        <Typography
                          sx={{
                            fontSize: '0.84rem',
                            fontWeight: 600,
                            color: isSelected ? '#1e3a8a' : '#1e293b',
                            lineHeight: 1.25,
                            wordBreak: 'break-word',
                          }}
                        >
                          {vertical.displayName}
                        </Typography>
                        <Typography
                          sx={{
                            fontSize: '0.72rem',
                            color: isSelected ? '#3b82f6' : '#64748b',
                            mt: 0.25,
                          }}
                        >
                          {`${meta.siteCount} Site${meta.siteCount === 1 ? '' : 's'} • ${meta.plantCount} Plant${meta.plantCount === 1 ? '' : 's'}`}
                        </Typography>
                      </Box>
                    </Box>
                  )
                })}
              </Box>
            )}
          </Box>
        </Box>
      </Box>

      {/* Scope preview bar */}
      {selectedUsers.length > 0 && selectedVerticals.length > 0 && (
        <Box
          sx={{
            mx: 3,
            mb: 2.5,
            px: 2,
            py: 1.25,
            borderRadius: '10px',
            bgcolor: '#f0fdf4',
            border: '1px solid #bbf7d0',
            display: 'flex',
            alignItems: 'center',
            gap: 1.25,
          }}
        >
          <VerifiedUserOutlinedIcon sx={{ fontSize: 19, color: '#16a34a' }} />
          <Typography
            sx={{ fontSize: '0.81rem', color: '#166534', fontWeight: 500 }}
          >
            Ready to grant access for{' '}
            <strong>
              {selectedUsers.length} user{selectedUsers.length === 1 ? '' : 's'}
            </strong>{' '}
            across{' '}
            <strong>
              {selectedVerticals.length} vertical
              {selectedVerticals.length === 1 ? '' : 's'}
            </strong>{' '}
            ({selectedSummary.siteCount} site
            {selectedSummary.siteCount === 1 ? '' : 's'},{' '}
            {selectedSummary.plantCount} plant
            {selectedSummary.plantCount === 1 ? '' : 's'}, and all mapped plant
            screens).
          </Typography>
        </Box>
      )}

      {/* Footer */}
      <Box
        sx={{
          px: 3,
          py: 1.75,
          bgcolor: '#f8fafc',
          borderTop: '1px solid #e2e8f0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'flex-end',
          gap: 1.5,
        }}
      >
        <Button
          variant='outlined'
          startIcon={<RestartAltIcon sx={{ fontSize: 17 }} />}
          onClick={handleReset}
          disabled={
            saving || (!selectedUsers.length && !selectedVerticals.length)
          }
          sx={{
            textTransform: 'none',
            borderRadius: '8px',
            borderColor: '#cbd5e1',
            color: '#475569',
            fontWeight: 600,
            fontSize: '0.82rem',
            px: 2,
            '&:hover': {
              borderColor: '#94a3b8',
              bgcolor: '#f1f5f9',
            },
          }}
        >
          Reset
        </Button>
        <Button
          variant='contained'
          startIcon={<SaveOutlinedIcon sx={{ fontSize: 17 }} />}
          onClick={handleSave}
          disabled={
            saving || !selectedUsers.length || !selectedVerticals.length
          }
          sx={{
            textTransform: 'none',
            borderRadius: '8px',
            bgcolor: '#2563eb',
            fontWeight: 600,
            fontSize: '0.82rem',
            px: 2.75,
            boxShadow: '0 2px 6px rgba(37, 99, 235, 0.25)',
            '&:hover': {
              bgcolor: '#1d4ed8',
              boxShadow: '0 4px 10px rgba(37, 99, 235, 0.3)',
            },
          }}
        >
          Save
        </Button>
      </Box>

      <Notification
        open={snackbarOpen}
        message={snackbarData?.message || ''}
        severity={snackbarData?.severity || 'info'}
        onClose={() => setSnackbarOpen(false)}
      />
    </Paper>
  )
}

export default GrantAllAccess
