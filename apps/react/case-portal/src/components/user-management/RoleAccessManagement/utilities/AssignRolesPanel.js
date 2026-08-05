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
  Checkbox,
  InputAdornment,
  IconButton,
  Tooltip,
  Collapse,
  Avatar,
  Divider,
} from '@mui/material'
import PersonAddIcon from '@mui/icons-material/PersonAdd'
import SearchIcon from '@mui/icons-material/Search'
import SecurityIcon from '@mui/icons-material/Security'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import SelectAllIcon from '@mui/icons-material/SelectAll'
import ClearIcon from '@mui/icons-material/Clear'
import GroupAddIcon from '@mui/icons-material/GroupAdd'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown'
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp'
import PersonIcon from '@mui/icons-material/Person'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'

const AssignRolesPanel = ({
  userSearchOptions,
  selectedUsers,
  setSelectedUsers,
  handleUserSearchForAssign,
  userSearchLoading,
  rolesFormattedForSelect,
  selectedRoles,
  setSelectedRoles,
  assigningRoles,
  handleAssignRoles,
}) => {
  const [expanded, setExpanded] = useState(true)
  const [roleSearchText, setRoleSearchText] = useState('')

  // Filter available roles by roleSearchText
  const filteredRoles = rolesFormattedForSelect.filter((r) => {
    const rName = typeof r === 'string' ? r : r.name || r.id || ''
    return rName.toLowerCase().includes(roleSearchText.trim().toLowerCase())
  })

  // Select all / Clear all roles handlers
  const isAllRolesSelected =
    rolesFormattedForSelect.length > 0 &&
    selectedRoles.length === rolesFormattedForSelect.length

  const handleToggleSelectAllRoles = () => {
    if (isAllRolesSelected) {
      setSelectedRoles([])
    } else {
      setSelectedRoles([...rolesFormattedForSelect])
    }
  }

  const handleRemoveUser = (userToRemove) => {
    setSelectedUsers(
      selectedUsers.filter(
        (u) =>
          (u.id && userToRemove.id ? u.id !== userToRemove.id : u.username !== userToRemove.username),
      ),
    )
  }

  const handleToggleRole = (roleObj) => {
    const roleName = typeof roleObj === 'string' ? roleObj : roleObj.name || roleObj.id
    const isSelected = selectedRoles.some(
      (r) => (typeof r === 'string' ? r : r.name || r.id) === roleName,
    )

    if (isSelected) {
      setSelectedRoles(
        selectedRoles.filter(
          (r) => (typeof r === 'string' ? r : r.name || r.id) !== roleName,
        ),
      )
    } else {
      setSelectedRoles([...selectedRoles, roleObj])
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
          <GroupAddIcon sx={{ color: '#0284c7', fontSize: 20 }} />
          <Typography
            variant='subtitle2'
            sx={{ fontWeight: 800, color: '#0f172a', fontSize: '0.875rem' }}
          >
            Assign Roles to Users
          </Typography>
          <Tooltip
            title='Search for target users and select security roles to assign to them.'
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

      {/* Collapsible Full Vertical Body */}
      <Collapse in={expanded} timeout='auto' unmountOnExit={false} sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <Box sx={{ pt: 2, flex: 1, display: 'flex', flexDirection: 'column' }}>
          <Grid container spacing={2.5}>
            
            {/* LEFT COLUMN: TARGET USERS WORKBENCH */}
            <Grid item xs={12} md={6} sx={{ display: 'flex', flexDirection: 'column' }}>
              <Paper
                variant='outlined'
                sx={{
                  p: 2,
                  borderRadius: '8px',
                  borderColor: '#cbd5e1',
                  backgroundColor: '#ffffff',
                  display: 'flex',
                  flexDirection: 'column',
                  flex: 1,
                  height: '100%',
                }}
              >
                {/* User Search Autocomplete */}
                <Typography variant='subtitle2' sx={{ fontWeight: 700, color: '#0f172a', mb: 1, fontSize: '0.8rem' }}>
                  1. Search & Add Target Users
                </Typography>
                <Autocomplete
                  multiple
                  disableCloseOnSelect
                  size='small'
                  options={userSearchOptions}
                  value={selectedUsers}
                  onChange={(event, newValue) => setSelectedUsers(newValue)}
                  onInputChange={(event, newInputValue) =>
                    handleUserSearchForAssign(newInputValue)
                  }
                  filterOptions={(options) => options}
                  getOptionLabel={(option) =>
                    typeof option === 'string'
                      ? option
                      : option.email
                        ? `${option.username} (${option.email})`
                        : option.username
                  }
                  isOptionEqualToValue={(option, value) =>
                    option.id === value.id || option.username === value.username
                  }
                  loading={userSearchLoading}
                  renderOption={(props, option, { selected }) => (
                    <li {...props} style={{ padding: '6px 12px', cursor: 'pointer' }}>
                      <Checkbox
                        size='small'
                        checked={selected}
                        sx={{ mr: 1, color: '#94a3b8', p: 0.3, '&.Mui-checked': { color: '#0284c7' } }}
                      />
                      <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                        <Typography variant='body2' sx={{ fontSize: '0.8rem', fontWeight: selected ? 700 : 600, color: selected ? '#0284c7' : '#0f172a' }}>
                          {typeof option === 'string' ? option : option.username}
                        </Typography>
                        {option.email && (
                          <Typography variant='caption' sx={{ fontSize: '0.72rem', color: '#64748b' }}>
                            {option.email}
                          </Typography>
                        )}
                      </Box>
                    </li>
                  )}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      placeholder='Type username or email to search...'
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
                        '& .MuiInputBase-input': { fontSize: '0.8rem', fontWeight: 600, color: '#0f172a !important' },
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
                            {userSearchLoading ? <CircularProgress color='inherit' size={14} /> : null}
                            {params.InputProps.endAdornment}
                          </React.Fragment>
                        ),
                      }}
                    />
                  )}
                  renderTags={() => null} // Hide inline tag input; rendered below in vertical workbench
                />

                {/* Selected Users Header Badge & Clear Button */}
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mt: 2, mb: 1 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <PersonIcon sx={{ color: '#0284c7', fontSize: 18 }} />
                    <Typography variant='caption' sx={{ fontWeight: 800, color: '#334155', fontSize: '0.78rem' }}>
                      Selected Users
                    </Typography>
                    <Chip
                      label={selectedUsers.length}
                      size='small'
                      sx={{
                        backgroundColor: '#0284c7',
                        color: '#ffffff',
                        fontWeight: 800,
                        fontSize: '0.72rem',
                        height: '20px',
                      }}
                    />
                  </Box>
                  {selectedUsers.length > 0 && (
                    <Button
                      size='small'
                      onClick={() => setSelectedUsers([])}
                      startIcon={<ClearIcon style={{ fontSize: 12 }} />}
                      sx={{
                        fontSize: '0.7rem',
                        fontWeight: 700,
                        color: '#ef4444',
                        textTransform: 'none',
                        p: 0,
                        minWidth: 'auto',
                        '&:hover': { backgroundColor: 'transparent', textDecoration: 'underline' },
                      }}
                    >
                      Clear All
                    </Button>
                  )}
                </Box>

                {/* Scrollable Selected Users List View */}
                <Box
                  sx={{
                    flex: 1,
                    height: 'calc(100vh - 540px)',
                    minHeight: '180px',
                    maxHeight: '320px',
                    overflowY: 'auto',
                    border: '1px solid #e2e8f0',
                    borderRadius: '6px',
                    backgroundColor: '#f8fafc',
                    p: 1,
                    '&::-webkit-scrollbar': { width: '5px' },
                    '&::-webkit-scrollbar-thumb': { backgroundColor: '#cbd5e1', borderRadius: '4px' },
                  }}
                >
                  {selectedUsers.length === 0 ? (
                    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', py: 4, color: '#94a3b8' }}>
                      <PersonIcon sx={{ fontSize: 36, mb: 1, opacity: 0.5 }} />
                      <Typography variant='body2' sx={{ fontWeight: 600, fontSize: '0.8rem' }}>
                        No users selected yet
                      </Typography>
                      <Typography variant='caption' sx={{ fontSize: '0.72rem', color: '#94a3b8', textAlign: 'center', mt: 0.5 }}>
                        Search and select users above to add them to assignment queue
                      </Typography>
                    </Box>
                  ) : (
                    <Grid container spacing={1}>
                      {selectedUsers.map((user, idx) => {
                        const uName = typeof user === 'string' ? user : user.username
                        const uEmail = typeof user === 'object' ? user.email : ''
                        return (
                          <Grid item xs={12} key={user.id || uName || idx}>
                            <Paper
                              elevation={0}
                              sx={{
                                p: 1.2,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                border: '1px solid #bae6fd',
                                backgroundColor: '#ffffff',
                                borderRadius: '6px',
                                '&:hover': { border: '1px solid #0284c7', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' },
                              }}
                            >
                              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.2 }}>
                                <Avatar sx={{ width: 28, height: 28, backgroundColor: '#0284c7', fontSize: '0.75rem', fontWeight: 800 }}>
                                  {uName.charAt(0).toUpperCase()}
                                </Avatar>
                                <Box>
                                  <Typography variant='body2' sx={{ fontWeight: 700, color: '#0f172a', fontSize: '0.8rem' }}>
                                    {uName}
                                  </Typography>
                                  {uEmail && (
                                    <Typography variant='caption' sx={{ color: '#64748b', fontSize: '0.72rem', display: 'block' }}>
                                      {uEmail}
                                    </Typography>
                                  )}
                                </Box>
                              </Box>
                              <IconButton
                                size='small'
                                onClick={() => handleRemoveUser(user)}
                                sx={{ color: '#94a3b8', '&:hover': { color: '#ef4444', backgroundColor: '#fef2f2' } }}
                              >
                                <ClearIcon sx={{ fontSize: 16 }} />
                              </IconButton>
                            </Paper>
                          </Grid>
                        )
                      })}
                    </Grid>
                  )}
                </Box>
              </Paper>
            </Grid>

            {/* RIGHT COLUMN: SECURITY ROLES SELECTION WORKBENCH */}
            <Grid item xs={12} md={6} sx={{ display: 'flex', flexDirection: 'column' }}>
              <Paper
                variant='outlined'
                sx={{
                  p: 2,
                  borderRadius: '8px',
                  borderColor: '#cbd5e1',
                  backgroundColor: '#ffffff',
                  display: 'flex',
                  flexDirection: 'column',
                  flex: 1,
                  height: '100%',
                }}
              >
                {/* Roles Search & Header */}
                <Typography variant='subtitle2' sx={{ fontWeight: 700, color: '#0f172a', mb: 1, fontSize: '0.8rem' }}>
                  2. Select Roles to Assign
                </Typography>
                
                <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                  <TextField
                    size='small'
                    fullWidth
                    placeholder='Filter roles...'
                    value={roleSearchText}
                    onChange={(e) => setRoleSearchText(e.target.value)}
                    InputProps={{
                      startAdornment: (
                        <InputAdornment position='start'>
                          <SearchIcon sx={{ color: '#0284c7', fontSize: 18 }} />
                        </InputAdornment>
                      ),
                    }}
                    sx={{
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
                      '& .MuiInputBase-input': { fontSize: '0.8rem', fontWeight: 600, color: '#0f172a !important' },
                    }}
                  />
                  <Button
                    size='small'
                    onClick={handleToggleSelectAllRoles}
                    startIcon={isAllRolesSelected ? <ClearIcon style={{ fontSize: 14 }} /> : <SelectAllIcon style={{ fontSize: 14 }} />}
                    sx={{
                      fontSize: '0.72rem',
                      fontWeight: 700,
                      textTransform: 'none',
                      whiteSpace: 'nowrap',
                      px: 1.5,
                      height: '38px',
                      borderRadius: '8px',
                      color: isAllRolesSelected ? '#ef4444' : '#0284c7',
                      backgroundColor: isAllRolesSelected ? '#fef2f2' : '#f0f9ff',
                      border: isAllRolesSelected ? '1px solid #fca5a5' : '1px solid #bae6fd',
                      '&:hover': { backgroundColor: isAllRolesSelected ? '#fee2e2' : '#e0f2fe' },
                    }}
                  >
                    {isAllRolesSelected ? 'Clear All' : 'Select All'}
                  </Button>
                </Box>

                {/* Selected Roles Count Header */}
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mt: 2, mb: 1 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <SecurityIcon sx={{ color: '#0284c7', fontSize: 18 }} />
                    <Typography variant='caption' sx={{ fontWeight: 800, color: '#334155', fontSize: '0.78rem' }}>
                      Available Roles ({filteredRoles.length})
                    </Typography>
                    <Chip
                      label={`${selectedRoles.length} selected`}
                      size='small'
                      sx={{
                        backgroundColor: '#0284c7',
                        color: '#ffffff',
                        fontWeight: 800,
                        fontSize: '0.72rem',
                        height: '20px',
                      }}
                    />
                  </Box>
                  {selectedRoles.length > 0 && (
                    <Button
                      size='small'
                      onClick={() => setSelectedRoles([])}
                      startIcon={<ClearIcon style={{ fontSize: 12 }} />}
                      sx={{
                        fontSize: '0.7rem',
                        fontWeight: 700,
                        color: '#ef4444',
                        textTransform: 'none',
                        p: 0,
                        minWidth: 'auto',
                        '&:hover': { backgroundColor: 'transparent', textDecoration: 'underline' },
                      }}
                    >
                      Deselect
                    </Button>
                  )}
                </Box>

                {/* Scrollable Roles Checklist */}
                <Box
                  sx={{
                    flex: 1,
                    height: 'calc(100vh - 540px)',
                    minHeight: '180px',
                    maxHeight: '320px',
                    overflowY: 'auto',
                    border: '1px solid #e2e8f0',
                    borderRadius: '6px',
                    backgroundColor: '#ffffff',
                    p: 1,
                    '&::-webkit-scrollbar': { width: '5px' },
                    '&::-webkit-scrollbar-thumb': { backgroundColor: '#cbd5e1', borderRadius: '4px' },
                  }}
                >
                  {filteredRoles.length === 0 ? (
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', py: 4, color: '#94a3b8' }}>
                      <Typography variant='body2' sx={{ fontWeight: 600, fontSize: '0.8rem' }}>
                        No matching roles found
                      </Typography>
                    </Box>
                  ) : (
                    <Grid container spacing={0.8}>
                      {filteredRoles.map((roleObj, idx) => {
                        const rName = typeof roleObj === 'string' ? roleObj : roleObj.name || roleObj.id
                        const rDesc = typeof roleObj === 'object' ? roleObj.description : ''
                        const isSelected = selectedRoles.some(
                          (r) => (typeof r === 'string' ? r : r.name || r.id) === rName,
                        )
                        return (
                          <Grid item xs={12} key={rName || idx}>
                            <Paper
                              elevation={0}
                              onClick={() => handleToggleRole(roleObj)}
                              sx={{
                                p: 1,
                                px: 1.2,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                cursor: 'pointer',
                                border: isSelected ? '1px solid #0284c7' : '1px solid #e2e8f0',
                                backgroundColor: isSelected ? '#f0f9ff' : '#ffffff',
                                borderRadius: '6px',
                                transition: 'all 0.15s ease',
                                '&:hover': { border: '1px solid #0284c7', backgroundColor: '#f8fafc' },
                              }}
                            >
                              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                <Checkbox
                                  size='small'
                                  checked={isSelected}
                                  sx={{ p: 0.2, color: '#94a3b8', '&.Mui-checked': { color: '#0284c7' } }}
                                />
                                <SecurityIcon sx={{ fontSize: 16, color: isSelected ? '#0284c7' : '#64748b' }} />
                                <Box>
                                  <Typography variant='body2' sx={{ fontWeight: isSelected ? 700 : 600, color: isSelected ? '#0284c7' : '#0f172a', fontSize: '0.8rem' }}>
                                    {rName}
                                  </Typography>
                                  {rDesc && (
                                    <Typography variant='caption' sx={{ color: '#64748b', fontSize: '0.7rem', display: 'block' }}>
                                      {rDesc}
                                    </Typography>
                                  )}
                                </Box>
                              </Box>

                              {isSelected && <CheckCircleIcon sx={{ fontSize: 16, color: '#0284c7' }} />}
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

          {/* BOTTOM SUMMARY & ASSIGN ACTION FOOTER BAR */}
          <Paper
            elevation={0}
            sx={{
              mt: 2,
              p: 1.5,
              px: 2,
              borderRadius: '8px',
              border: '1px solid #bae6fd',
              backgroundColor: '#f0f9ff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              flexWrap: 'wrap',
              gap: 1.5,
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Typography variant='body2' sx={{ fontWeight: 700, color: '#0369a1', fontSize: '0.825rem' }}>
                Summary:
              </Typography>
              <Chip
                label={`${selectedUsers.length} user(s)`}
                size='small'
                sx={{ fontWeight: 700, backgroundColor: selectedUsers.length > 0 ? '#0284c7' : '#cbd5e1', color: '#ffffff', fontSize: '0.72rem' }}
              />
              <Typography variant='body2' sx={{ color: '#64748b', fontWeight: 700 }}>
                ➔
              </Typography>
              <Chip
                label={`${selectedRoles.length} role(s)`}
                size='small'
                sx={{ fontWeight: 700, backgroundColor: selectedRoles.length > 0 ? '#0284c7' : '#cbd5e1', color: '#ffffff', fontSize: '0.72rem' }}
              />
            </Box>

            <Button
              variant='contained'
              color='primary'
              size='small'
              disabled={
                selectedUsers.length === 0 ||
                selectedRoles.length === 0 ||
                assigningRoles
              }
              onClick={handleAssignRoles}
              startIcon={
                assigningRoles ? (
                  <CircularProgress size={14} color='inherit' />
                ) : (
                  <PersonAddIcon style={{ fontSize: 16 }} />
                )
              }
              sx={{
                fontWeight: 700,
                px: 2.5,
                py: 0.8,
                borderRadius: '8px',
                textTransform: 'none',
                fontSize: '0.8rem',
                boxShadow: '0 2px 4px rgba(2, 132, 199, 0.25)',
                backgroundColor: '#0284c7',
                '&:hover': { backgroundColor: '#0369a1' },
                '&:disabled': { backgroundColor: '#cbd5e1' },
              }}
            >
              {assigningRoles ? 'Assigning Roles...' : 'Assign Selected Roles'}
            </Button>
          </Paper>
        </Box>
      </Collapse>
    </Paper>
  )
}

export default AssignRolesPanel
