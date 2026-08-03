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
  Divider,
  Popper,
} from '@mui/material'
import PersonAddIcon from '@mui/icons-material/PersonAdd'
import SearchIcon from '@mui/icons-material/Search'
import SecurityIcon from '@mui/icons-material/Security'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import FilterListIcon from '@mui/icons-material/FilterList'
import SelectAllIcon from '@mui/icons-material/SelectAll'
import ClearIcon from '@mui/icons-material/Clear'

import GroupAddIcon from '@mui/icons-material/GroupAdd'
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown'
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import { Collapse, IconButton, Tooltip } from '@mui/material'

// Custom Popper component to give the dropdown a modern, sleek floating card look
const CustomPopper = (props) => {
  return (
    <Popper
      {...props}
      sx={{
        zIndex: 1400,
        pt: 0.5,
        fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
        '& *': {
          fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif !important",
        },
        '& .MuiPaper-root': {
          borderRadius: '12px',
          boxShadow: '0 10px 25px -5px rgba(15, 23, 42, 0.15), 0 8px 10px -6px rgba(15, 23, 42, 0.1)',
          border: '1px solid #cbd5e1',
          overflow: 'hidden',
          backgroundColor: '#ffffff',
        },
      }}
    />
  )
}

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

  // Handle Select All / Deselect All roles helper
  const handleSelectAllRoles = () => {
    if (selectedRoles.length === rolesFormattedForSelect.length) {
      setSelectedRoles([])
    } else {
      setSelectedRoles([...rolesFormattedForSelect])
    }
  }

  const isAllRolesSelected =
    rolesFormattedForSelect.length > 0 &&
    selectedRoles.length === rolesFormattedForSelect.length

  return (
    <Paper
      elevation={0}
      sx={{
        borderRadius: '10px',
        padding: '16px 18px',
        border: '1px solid #e2e8f0',
        backgroundColor: '#ffffff',
        marginBottom: '14px',
        maxWidth: '920px',
      }}
    >
      {/* Header Bar with Front Icon & Expand/Collapse Controls */}
      <Box
        onClick={() => setExpanded(!expanded)}
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          cursor: 'pointer',
          userSelect: 'none',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <GroupAddIcon sx={{ color: '#0284c7', fontSize: 20 }} />
          <Typography
            variant="subtitle2"
            sx={{ fontWeight: 800, color: '#0f172a', fontSize: '0.875rem' }}
          >
            Assign Roles to Users
          </Typography>
          <Tooltip
            title="Search for target users and select one or multiple roles to assign to them simultaneously."
            arrow
            placement="top"
          >
            <IconButton
              size="small"
              onClick={(e) => e.stopPropagation()}
              sx={{ p: 0.2, color: '#0284c7', '&:hover': { color: '#0369a1' } }}
            >
              <InfoOutlinedIcon sx={{ fontSize: 16 }} />
            </IconButton>
          </Tooltip>
        </Box>

        <IconButton
          size="small"
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

      {/* Collapsible Body Content */}
      <Collapse in={expanded} timeout="auto" unmountOnExit={false}>
        <Box sx={{ pt: 1.5 }}>

      <Grid container spacing={2.5} sx={{ mb: 1.5 }}>
        {/* Target Users Autocomplete */}
        <Grid item xs={12} md={6}>
          <Autocomplete
            multiple
            disableCloseOnSelect
            size="small"
            PopperComponent={CustomPopper}
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
                  size="small"
                  checked={selected}
                  sx={{
                    mr: 1,
                    color: '#94a3b8',
                    p: 0.5,
                    '&.Mui-checked': {
                      color: '#0284c7',
                    },
                  }}
                />
                <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                  <Typography
                    variant="body2"
                    sx={{
                      fontSize: '0.8rem',
                      fontWeight: selected ? 700 : 600,
                      color: selected ? '#0284c7' : '#0f172a',
                    }}
                  >
                    {typeof option === 'string' ? option : option.username}
                  </Typography>
                  {option.email && (
                    <Typography
                      variant="caption"
                      sx={{ fontSize: '0.72rem', color: '#64748b' }}
                    >
                      {option.email}
                    </Typography>
                  )}
                </Box>
              </li>
            )}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Select Users"
                placeholder={selectedUsers.length > 0 ? '' : 'Type username to search...'}
                size="small"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: '8px',
                    backgroundColor: '#f8fafc',
                    transition: 'all 0.2s ease-in-out',
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
                    '&::placeholder': {
                      color: '#64748b',
                      opacity: 1,
                    },
                  },
                  '& .MuiInputLabel-root': {
                    fontSize: '0.8rem',
                    '&.Mui-focused': { color: '#0284c7', fontWeight: 700 },
                  },
                }}
                InputProps={{
                  ...params.InputProps,
                  endAdornment: (
                    <React.Fragment>
                      {userSearchLoading ? (
                        <CircularProgress color="inherit" size={14} />
                      ) : null}
                      {params.InputProps.endAdornment}
                    </React.Fragment>
                  ),
                }}
              />
            )}
            renderTags={(value, getTagProps) =>
              value.map((option, index) => (
                <Chip
                  size="small"
                  variant="outlined"
                  color="primary"
                  label={
                    typeof option === 'string' ? option : option.username
                  }
                  {...getTagProps({ index })}
                  key={index}
                  sx={{
                    borderRadius: '5px',
                    fontWeight: 700,
                    fontSize: '0.72rem',
                    height: '22px',
                    backgroundColor: '#e0f2fe',
                    borderColor: '#0284c7',
                    color: '#0369a1',
                  }}
                />
              ))
            }
          />
        </Grid>

        {/* Target Roles Autocomplete - ENHANCED SEARCH & BEAUTIFUL UI */}
        <Grid item xs={12} md={6}>
          <Autocomplete
            multiple
            disableCloseOnSelect
            size="small"
            PopperComponent={CustomPopper}
            options={rolesFormattedForSelect}
            value={selectedRoles}
            inputValue={roleSearchText}
            onInputChange={(event, newInputValue) => {
              setRoleSearchText(newInputValue)
            }}
            onChange={(event, newValue) => setSelectedRoles(newValue)}
            getOptionLabel={(option) =>
              typeof option === 'string' ? option : option.name || option.id || ''
            }
            isOptionEqualToValue={(option, value) =>
              (option.id && value.id && option.id === value.id) ||
              option.name === value.name
            }
            filterOptions={(options, state) => {
              const query = state.inputValue.trim().toLowerCase()
              if (!query) return options
              return options.filter((opt) => {
                const nameStr = typeof opt === 'string' ? opt : opt.name || opt.id || ''
                return nameStr.toLowerCase().includes(query)
              })
            }}
            PaperComponent={({ children }) => (
              <Paper>
                {/* Search Bar & Header Action Controls inside Dropdown Menu */}
                <Box
                  sx={{
                    p: '8px 12px',
                    backgroundColor: '#f8fafc',
                    borderBottom: '1px solid #e2e8f0',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.8 }}>
                    <SecurityIcon sx={{ color: '#0284c7', fontSize: 16 }} />
                    <Typography
                      variant="caption"
                      sx={{ fontWeight: 800, color: '#0f172a', fontSize: '0.75rem' }}
                    >
                      Available Roles ({rolesFormattedForSelect.length})
                    </Typography>
                  </Box>

                  <Box sx={{ display: 'flex', gap: 0.5 }}>
                    <Button
                      size="small"
                      onClick={handleSelectAllRoles}
                      startIcon={
                        isAllRolesSelected ? (
                          <ClearIcon style={{ fontSize: 12 }} />
                        ) : (
                          <SelectAllIcon style={{ fontSize: 12 }} />
                        )
                      }
                      sx={{
                        fontSize: '0.68rem',
                        fontWeight: 700,
                        textTransform: 'none',
                        py: 0.2,
                        px: 0.8,
                        borderRadius: '4px',
                        color: isAllRolesSelected ? '#ef4444' : '#0284c7',
                        backgroundColor: isAllRolesSelected ? '#fef2f2' : '#f0f9ff',
                        '&:hover': {
                          backgroundColor: isAllRolesSelected ? '#fee2e2' : '#e0f2fe',
                        },
                      }}
                    >
                      {isAllRolesSelected ? 'Clear All' : 'Select All'}
                    </Button>
                  </Box>
                </Box>

                {children}

                {/* Dropdown Footer Status Indicator */}
                {selectedRoles.length > 0 && (
                  <Box
                    sx={{
                      p: '6px 12px',
                      backgroundColor: '#f0f9ff',
                      borderTop: '1px solid #bae6fd',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                    }}
                  >
                    <Typography
                      variant="caption"
                      sx={{ fontWeight: 700, color: '#0369a1', fontSize: '0.72rem' }}
                    >
                      {selectedRoles.length} role(s) selected
                    </Typography>
                    <Button
                      size="small"
                      onClick={() => setSelectedRoles([])}
                      sx={{
                        fontSize: '0.65rem',
                        fontWeight: 700,
                        textTransform: 'none',
                        p: 0,
                        minWidth: 'auto',
                        color: '#0284c7',
                        '&:hover': { textDecoration: 'underline' },
                      }}
                    >
                      Deselect
                    </Button>
                  </Box>
                )}
              </Paper>
            )}
            renderOption={(props, option, { selected }) => {
              const rName = typeof option === 'string' ? option : option.name
              return (
                <li
                  {...props}
                  style={{
                    padding: '6px 12px',
                    cursor: 'pointer',
                    backgroundColor: selected ? '#f0f9ff' : 'transparent',
                    transition: 'background-color 0.15s ease',
                  }}
                >
                  <Checkbox
                    size="small"
                    checked={selected}
                    sx={{
                      mr: 1,
                      color: '#94a3b8',
                      p: 0.3,
                      '&.Mui-checked': {
                        color: '#0284c7',
                      },
                    }}
                  />
                  <Box
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      width: '100%',
                    }}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <SecurityIcon
                        sx={{
                          fontSize: 14,
                          color: selected ? '#0284c7' : '#94a3b8',
                        }}
                      />
                      <Typography
                        variant="body2"
                        sx={{
                          fontSize: '0.8rem',
                          fontWeight: selected ? 700 : 600,
                          color: selected ? '#0284c7' : '#1e293b',
                        }}
                      >
                        {rName}
                      </Typography>
                    </Box>

                    {selected && (
                      <CheckCircleIcon
                        sx={{ fontSize: 14, color: '#0284c7', ml: 1 }}
                      />
                    )}
                  </Box>
                </li>
              )
            }}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Select Roles"
                placeholder={
                  selectedRoles.length > 0
                    ? `${selectedRoles.length} selected...`
                    : 'Search & select roles...'
                }
                size="small"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: '8px',
                    backgroundColor: '#f8fafc',
                    transition: 'all 0.2s ease-in-out',
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
                    '&::placeholder': {
                      color: '#64748b',
                      opacity: 1,
                    },
                  },
                  '& .MuiInputLabel-root': {
                    fontSize: '0.8rem',
                    '&.Mui-focused': { color: '#0284c7', fontWeight: 700 },
                  },
                }}
                InputProps={{
                  ...params.InputProps,
                  startAdornment: (
                    <React.Fragment>
                      <InputAdornment position="start" sx={{ mr: 0.5 }}>
                        <SearchIcon sx={{ color: '#0284c7', fontSize: 18 }} />
                      </InputAdornment>
                      {params.InputProps.startAdornment}
                    </React.Fragment>
                  ),
                }}
              />
            )}
            renderTags={(value, getTagProps) =>
              value.map((option, index) => {
                const rName = typeof option === 'string' ? option : option.name
                return (
                  <Chip
                    size="small"
                    color="info"
                    variant="soft"
                    label={rName}
                    icon={<SecurityIcon style={{ fontSize: 12, color: '#0284c7' }} />}
                    {...getTagProps({ index })}
                    key={index}
                    sx={{
                      borderRadius: '5px',
                      fontWeight: 700,
                      backgroundColor: '#e0f2fe',
                      borderColor: '#bae6fd',
                      border: '1px solid #bae6fd',
                      color: '#0369a1',
                      fontSize: '0.72rem',
                      height: '22px',
                      '& .MuiChip-deleteIcon': {
                        color: '#0284c7',
                        fontSize: 13,
                        '&:hover': {
                          color: '#ef4444',
                        },
                      },
                    }}
                  />
                )
              })
            }
          />
        </Grid>
      </Grid>

      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 1 }}>
        <Button
          variant="contained"
          color="primary"
          size="small"
          disabled={
            selectedUsers.length === 0 ||
            selectedRoles.length === 0 ||
            assigningRoles
          }
          onClick={handleAssignRoles}
          startIcon={
            assigningRoles ? (
              <CircularProgress size={14} color="inherit" />
            ) : (
              <PersonAddIcon style={{ fontSize: 16 }} />
            )
          }
          sx={{
            fontWeight: 700,
            padding: '6px 18px',
            borderRadius: '8px',
            textTransform: 'none',
            fontSize: '0.78rem',
            boxShadow: 'none',
            backgroundColor: '#0284c7',
            '&:hover': { backgroundColor: '#0369a1' },
          }}
        >
          {assigningRoles ? 'Assigning...' : 'Assign Selected Roles'}
        </Button>
      </Box>
    </Box>
  </Collapse>
</Paper>
  )
}

export default AssignRolesPanel
