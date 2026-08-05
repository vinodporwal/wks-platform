import React, { useState } from 'react'
import {
  Box,
  Typography,
  Button,
  Chip,
  TextField,
  Autocomplete,
  Paper,
  Grid,
  CircularProgress,
  IconButton,
  Tooltip,
  Collapse,
  Avatar,
  InputAdornment,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import ManageAccountsIcon from '@mui/icons-material/ManageAccounts'
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown'
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import SecurityIcon from '@mui/icons-material/Security'
import SearchIcon from '@mui/icons-material/Search'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import PersonIcon from '@mui/icons-material/Person'
import ShieldIcon from '@mui/icons-material/Shield'
import PersonAddIcon from '@mui/icons-material/PersonAdd'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import FilterAltIcon from '@mui/icons-material/FilterAlt'
import { roleAccessApiService } from 'services/roleAccessApiService'

const UserRoleInspectorPanel = ({
  lookupUser,
  lookupUserOptions,
  handleUserSearchForLookup,
  lookupUserLoading,
  handleRetrieveUserRoles,
  retrievedUserRoles,
  retrievingRoles,
  handleOpenUnassignDialog,
  rolesFormattedForSelect = [],
  showNotification,
  keycloak,
}) => {
  const [expanded, setExpanded] = useState(true)
  const [roleFilterText, setRoleFilterText] = useState('')
  const [filterMode, setFilterMode] = useState('ALL') // 'ALL' | 'ASSIGNED' | 'UNASSIGNED'
  const [assigningRoleName, setAssigningRoleName] = useState(null)

  // Extract names of roles currently assigned to lookupUser
  const activeRoleNames = Array.isArray(retrievedUserRoles)
    ? retrievedUserRoles.map((r) => (typeof r === 'string' ? r : r?.name || r?.code || String(r)))
    : []

  // Combine and filter all system roles with assigned status
  const allRolesWithStatus = rolesFormattedForSelect.map((roleObj) => {
    const rName = typeof roleObj === 'string' ? roleObj : roleObj.name || roleObj.id
    const rDesc = typeof roleObj === 'object' ? roleObj.description : ''
    const isAssigned = activeRoleNames.includes(rName)
    return { roleObj, rName, rDesc, isAssigned }
  })

  // Filter roles by text search and filterMode
  const filteredRoles = allRolesWithStatus.filter(({ rName, isAssigned }) => {
    const matchesSearch = rName.toLowerCase().includes(roleFilterText.trim().toLowerCase())
    if (!matchesSearch) return false
    if (filterMode === 'ASSIGNED') return isAssigned
    if (filterMode === 'UNASSIGNED') return !isAssigned
    return true
  })

  // Direct single role assignment for inspected user
  const handleAssignSingleRole = async (roleName) => {
    if (!lookupUser || !roleName) return
    setAssigningRoleName(roleName)
    try {
      const assignments = [
        {
          userId: lookupUser.id || lookupUser.username,
          roles: [roleName],
        },
      ]
      const res = await roleAccessApiService.assignRoles(keycloak, assignments)
      const successMsg = res?.message || `Role "${roleName}" assigned to ${lookupUser.username} successfully!`
      if (showNotification) {
        showNotification(successMsg, 'success')
      }
      handleRetrieveUserRoles(lookupUser)
    } catch (err) {
      console.error('Error assigning role:', err)
      if (showNotification) {
        showNotification(err.message || `Failed to assign role "${roleName}"`, 'error')
      }
    } finally {
      setAssigningRoleName(null)
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
        display: 'flex',
        flexDirection: 'column',
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
          pb: expanded ? 1.5 : 0,
          borderBottom: expanded ? '1px solid #f1f5f9' : 'none',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <ManageAccountsIcon sx={{ color: '#0284c7', fontSize: 20 }} />
          <Typography
            variant='subtitle2'
            sx={{ fontWeight: 800, color: '#0f172a', fontSize: '0.875rem' }}
          >
            User Role Inspector
          </Typography>
          <Tooltip
            title='Inspect active roles for any user and assign or unassign roles directly on the same panel.'
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
        <Box sx={{ pt: 2 }}>
          <Grid container spacing={2.5}>
            {/* LEFT COLUMN: TARGET USER LOOKUP */}
            <Grid item xs={12} md={4.5} sx={{ display: 'flex', flexDirection: 'column' }}>
              <Paper
                variant='outlined'
                sx={{
                  p: 2,
                  borderRadius: '8px',
                  borderColor: '#cbd5e1',
                  backgroundColor: '#ffffff',
                  display: 'flex',
                  flexDirection: 'column',
                  height: '100%',
                }}
              >
                <Typography variant='subtitle2' sx={{ fontWeight: 700, color: '#0f172a', mb: 1.5, fontSize: '0.8rem' }}>
                  1. Select User to Inspect
                </Typography>

                <Autocomplete
                  options={lookupUserOptions}
                  value={lookupUser}
                  size='small'
                  filterOptions={(options) => options}
                  getOptionLabel={(option) =>
                    typeof option === 'string' ? option : option?.username || ''
                  }
                  isOptionEqualToValue={(option, value) =>
                    option?.id === value?.id || option?.username === value?.username
                  }
                  onChange={(event, newValue) => {
                    handleRetrieveUserRoles(newValue)
                  }}
                  onInputChange={(event, newInputValue, reason) => {
                    if (!newInputValue || reason === 'clear') {
                      handleRetrieveUserRoles(null)
                    } else {
                      handleUserSearchForLookup(newInputValue)
                    }
                  }}
                  loading={lookupUserLoading}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      placeholder='Search username...'
                      size='small'
                      sx={{
                        '& .MuiOutlinedInput-root': {
                          borderRadius: '8px',
                          backgroundColor: '#f8fafc',
                          '& fieldset': { borderColor: '#cbd5e1' },
                          '&:hover fieldset': { borderColor: '#94a3b8' },
                          '&.Mui-focused': {
                            backgroundColor: '#ffffff',
                            '& fieldset': { borderColor: '#0284c7', borderWidth: '1.5px' },
                            boxShadow: '0 0 0 3px rgba(2, 132, 199, 0.12)',
                          },
                        },
                        '& .MuiInputBase-input': {
                          fontSize: '0.8rem',
                          color: '#0f172a !important',
                          fontWeight: 600,
                        },
                      }}
                      InputProps={{
                        ...params.InputProps,
                        startAdornment: (
                          <InputAdornment position='start'>
                            <SearchIcon sx={{ color: '#0284c7', fontSize: 18 }} />
                          </InputAdornment>
                        ),
                        endAdornment: (
                          <React.Fragment>
                            {lookupUserLoading ? <CircularProgress color='inherit' size={14} /> : null}
                            {params.InputProps.endAdornment}
                          </React.Fragment>
                        ),
                      }}
                    />
                  )}
                />

                {/* Inspected User Profile Card */}
                <Box
                  sx={{
                    mt: 2,
                    p: 2,
                    border: '1px solid #e2e8f0',
                    borderRadius: '8px',
                    backgroundColor: '#f8fafc',
                    flex: 1,
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center',
                    alignItems: 'center',
                    minHeight: '260px',
                  }}
                >
                  {lookupUser ? (
                    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', width: '100%' }}>
                      <Avatar
                        sx={{
                          width: 56,
                          height: 56,
                          backgroundColor: '#0284c7',
                          fontWeight: 800,
                          fontSize: '1.3rem',
                          mb: 1.5,
                          boxShadow: '0 3px 8px rgba(2,132,199,0.3)',
                        }}
                      >
                        {lookupUser.username.charAt(0).toUpperCase()}
                      </Avatar>
                      <Typography variant='subtitle1' sx={{ fontWeight: 800, color: '#0f172a', fontSize: '1rem' }}>
                        {lookupUser.username}
                      </Typography>
                      {lookupUser.email && (
                        <Typography variant='caption' sx={{ color: '#64748b', fontSize: '0.78rem', mb: 1.5 }}>
                          {lookupUser.email}
                        </Typography>
                      )}

                      <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
                        <Chip
                          label={`${activeRoleNames.length} Assigned Role(s)`}
                          size='small'
                          sx={{
                            backgroundColor: '#e0f2fe',
                            color: '#0369a1',
                            fontWeight: 800,
                            fontSize: '0.75rem',
                          }}
                        />
                      </Box>

                      <Button
                        variant='outlined'
                        size='small'
                        onClick={() => handleRetrieveUserRoles(lookupUser)}
                        disabled={retrievingRoles}
                        startIcon={
                          retrievingRoles ? (
                            <CircularProgress size={14} color='inherit' />
                          ) : (
                            <RefreshIcon style={{ fontSize: 14 }} />
                          )
                        }
                        sx={{
                          textTransform: 'none',
                          fontWeight: 700,
                          borderRadius: '8px',
                          fontSize: '0.75rem',
                          color: '#0284c7',
                          borderColor: '#bae6fd',
                          '&:hover': { backgroundColor: '#e0f2fe' },
                        }}
                      >
                        {retrievingRoles ? 'Refreshing...' : 'Refresh User Roles'}
                      </Button>
                    </Box>
                  ) : (
                    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', color: '#94a3b8' }}>
                      <PersonIcon sx={{ fontSize: 48, mb: 1, opacity: 0.5 }} />
                      <Typography variant='body2' sx={{ fontWeight: 600, fontSize: '0.85rem' }}>
                        No user selected
                      </Typography>
                      <Typography variant='caption' sx={{ fontSize: '0.75rem', color: '#94a3b8', textAlign: 'center', mt: 0.5 }}>
                        Search and select a user above to inspect and manage their roles
                      </Typography>
                    </Box>
                  )}
                </Box>
              </Paper>
            </Grid>

            {/* RIGHT COLUMN: UNIFIED ROLES WORKBENCH (ASSIGN & UNASSIGN ON SAME PANEL) */}
            <Grid item xs={12} md={7.5} sx={{ display: 'flex', flexDirection: 'column' }}>
              <Paper
                variant='outlined'
                sx={{
                  p: 2,
                  borderRadius: '8px',
                  borderColor: '#cbd5e1',
                  backgroundColor: '#ffffff',
                  display: 'flex',
                  flexDirection: 'column',
                  height: '100%',
                }}
              >
                {/* Header & Filter Controls */}
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5, flexWrap: 'wrap', gap: 1 }}>
                  <Typography variant='subtitle2' sx={{ fontWeight: 700, color: '#0f172a', fontSize: '0.8rem' }}>
                    2. Roles Management {lookupUser ? `for ${lookupUser.username}` : ''}
                  </Typography>

                  {/* Filter Mode Chips: ALL | ASSIGNED | UNASSIGNED */}
                  <Box sx={{ display: 'flex', gap: 0.5 }}>
                    <Chip
                      label={`All (${rolesFormattedForSelect.length})`}
                      size='small'
                      onClick={() => setFilterMode('ALL')}
                      sx={{
                        fontSize: '0.7rem',
                        fontWeight: 700,
                        cursor: 'pointer',
                        backgroundColor: filterMode === 'ALL' ? '#0284c7' : '#f1f5f9',
                        color: filterMode === 'ALL' ? '#ffffff' : '#64748b',
                        '&:hover': { backgroundColor: filterMode === 'ALL' ? '#0369a1' : '#e2e8f0' },
                      }}
                    />
                    <Chip
                      label={`Assigned (${activeRoleNames.length})`}
                      size='small'
                      onClick={() => setFilterMode('ASSIGNED')}
                      sx={{
                        fontSize: '0.7rem',
                        fontWeight: 700,
                        cursor: 'pointer',
                        backgroundColor: filterMode === 'ASSIGNED' ? '#0284c7' : '#f1f5f9',
                        color: filterMode === 'ASSIGNED' ? '#ffffff' : '#64748b',
                        '&:hover': { backgroundColor: filterMode === 'ASSIGNED' ? '#0369a1' : '#e2e8f0' },
                      }}
                    />
                    <Chip
                      label={`Unassigned (${Math.max(0, rolesFormattedForSelect.length - activeRoleNames.length)})`}
                      size='small'
                      onClick={() => setFilterMode('UNASSIGNED')}
                      sx={{
                        fontSize: '0.7rem',
                        fontWeight: 700,
                        cursor: 'pointer',
                        backgroundColor: filterMode === 'UNASSIGNED' ? '#0284c7' : '#f1f5f9',
                        color: filterMode === 'UNASSIGNED' ? '#ffffff' : '#64748b',
                        '&:hover': { backgroundColor: filterMode === 'UNASSIGNED' ? '#0369a1' : '#e2e8f0' },
                      }}
                    />
                  </Box>
                </Box>

                {/* Filter Text Field */}
                {lookupUser && (
                  <TextField
                    size='small'
                    placeholder='Filter roles by name...'
                    value={roleFilterText}
                    onChange={(e) => setRoleFilterText(e.target.value)}
                    InputProps={{
                      startAdornment: (
                        <InputAdornment position='start'>
                          <SearchIcon sx={{ color: '#0284c7', fontSize: 18 }} />
                        </InputAdornment>
                      ),
                    }}
                    sx={{
                      mb: 1.5,
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '8px',
                        backgroundColor: '#f8fafc',
                        '& fieldset': { borderColor: '#cbd5e1' },
                        '&:hover fieldset': { borderColor: '#94a3b8' },
                        '&.Mui-focused': {
                          backgroundColor: '#ffffff',
                          '& fieldset': { borderColor: '#0284c7', borderWidth: '1.5px' },
                        },
                      },
                      '& .MuiInputBase-input': {
                        fontSize: '0.8rem',
                        color: '#0f172a !important',
                        fontWeight: 600,
                      },
                    }}
                  />
                )}

                {/* UNIFIED SCROLLABLE ROLES LIST (ASSIGN & UNASSIGN ON SAME PANEL) */}
                <Box
                  sx={{
                    flex: 1,
                    height: 'calc(100vh - 430px)',
                    minHeight: '260px',
                    maxHeight: 'calc(100vh - 430px)',
                    overflowY: 'auto',
                    border: '1px solid #e2e8f0',
                    borderRadius: '6px',
                    backgroundColor: '#ffffff',
                    p: 1,
                    '&::-webkit-scrollbar': { width: '6px' },
                    '&::-webkit-scrollbar-track': { backgroundColor: '#f1f5f9', borderRadius: '4px' },
                    '&::-webkit-scrollbar-thumb': { backgroundColor: '#cbd5e1', borderRadius: '4px', '&:hover': { backgroundColor: '#0284c7' } },
                  }}
                >
                  {!lookupUser ? (
                    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', py: 4, color: '#94a3b8' }}>
                      <ShieldIcon sx={{ fontSize: 44, mb: 1, opacity: 0.5 }} />
                      <Typography variant='body2' sx={{ fontWeight: 600, fontSize: '0.85rem' }}>
                        Select a user to manage their assigned and available roles
                      </Typography>
                    </Box>
                  ) : retrievingRoles ? (
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', gap: 1.5, py: 4 }}>
                      <CircularProgress size={22} sx={{ color: '#0284c7' }} />
                      <Typography variant='body2' sx={{ fontWeight: 700, color: '#0284c7', fontSize: '0.8rem' }}>
                        Fetching roles for {lookupUser.username}...
                      </Typography>
                    </Box>
                  ) : filteredRoles.length === 0 ? (
                    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', py: 4, color: '#94a3b8' }}>
                      <Typography variant='body2' sx={{ fontWeight: 600, fontSize: '0.8rem' }}>
                        No roles match the filter criteria
                      </Typography>
                    </Box>
                  ) : (
                    <Grid container spacing={1}>
                      {filteredRoles.map(({ rName, rDesc, isAssigned }, idx) => {
                        const isAssigningThis = assigningRoleName === rName
                        return (
                          <Grid item xs={12} key={rName || idx}>
                            <Paper
                              elevation={0}
                              sx={{
                                p: 1.2,
                                px: 1.5,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                border: isAssigned ? '1px solid #bae6fd' : '1px solid #e2e8f0',
                                backgroundColor: isAssigned ? '#f0f9ff' : '#ffffff',
                                borderRadius: '6px',
                                transition: 'all 0.15s ease',
                                '&:hover': { border: '1px solid #0284c7', backgroundColor: isAssigned ? '#e0f2fe' : '#f8fafc' },
                              }}
                            >
                              {/* Left Role Info */}
                              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.2 }}>
                                <SecurityIcon sx={{ fontSize: 18, color: isAssigned ? '#0284c7' : '#94a3b8' }} />
                                <Box>
                                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                    <Typography variant='body2' sx={{ fontWeight: 700, color: isAssigned ? '#0369a1' : '#0f172a', fontSize: '0.825rem' }}>
                                      {rName}
                                    </Typography>
                                    <Chip
                                      label={isAssigned ? 'Assigned' : 'Available'}
                                      size='small'
                                      sx={{
                                        fontSize: '0.65rem',
                                        fontWeight: 800,
                                        height: '18px',
                                        backgroundColor: isAssigned ? '#0284c7' : '#f1f5f9',
                                        color: isAssigned ? '#ffffff' : '#64748b',
                                      }}
                                    />
                                  </Box>
                                  {rDesc && (
                                    <Typography variant='caption' sx={{ color: '#64748b', fontSize: '0.72rem', display: 'block' }}>
                                      {rDesc}
                                    </Typography>
                                  )}
                                </Box>
                              </Box>

                              {/* Right Action Button (Assign vs Unassign) */}
                              {isAssigned ? (
                                <Button
                                  size='small'
                                  color='error'
                                  onClick={() => handleOpenUnassignDialog(rName)}
                                  startIcon={<DeleteOutlineIcon style={{ fontSize: 14 }} />}
                                  sx={{
                                    fontSize: '0.7rem',
                                    fontWeight: 700,
                                    textTransform: 'none',
                                    color: '#ef4444',
                                    backgroundColor: '#fef2f2',
                                    border: '1px solid #fca5a5',
                                    borderRadius: '6px',
                                    py: 0.3,
                                    px: 1.2,
                                    '&:hover': { backgroundColor: '#fee2e2' },
                                  }}
                                >
                                  Unassign
                                </Button>
                              ) : (
                                <Button
                                  size='small'
                                  color='primary'
                                  disabled={isAssigningThis}
                                  onClick={() => handleAssignSingleRole(rName)}
                                  startIcon={
                                    isAssigningThis ? (
                                      <CircularProgress size={12} color='inherit' />
                                    ) : (
                                      <PersonAddIcon style={{ fontSize: 14 }} />
                                    )
                                  }
                                  sx={{
                                    fontSize: '0.7rem',
                                    fontWeight: 700,
                                    textTransform: 'none',
                                    color: '#0284c7',
                                    backgroundColor: '#f0f9ff',
                                    border: '1px solid #bae6fd',
                                    borderRadius: '6px',
                                    py: 0.3,
                                    px: 1.2,
                                    '&:hover': { backgroundColor: '#e0f2fe' },
                                  }}
                                >
                                  {isAssigningThis ? 'Assigning...' : 'Assign'}
                                </Button>
                              )}
                            </Paper>
                          </Grid>
                        )
                      })}
                    </Grid>
                  )}
                </Box>
              </Paper>
            </Grid>
          </Grid>
        </Box>
      </Collapse>
    </Paper>
  )
}

export default UserRoleInspectorPanel
