import React, { useState } from 'react'
import {
  Box,
  Typography,
  Button,
  TextField,
  Paper,
  CircularProgress,
  Collapse,
  IconButton,
  Tooltip,
  Autocomplete,
  Chip,
  Checkbox,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline'
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown'
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import DesktopWindowsIcon from '@mui/icons-material/DesktopWindows'
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank'
import CheckBoxIcon from '@mui/icons-material/CheckBox'
import SelectAllIcon from '@mui/icons-material/SelectAll'
import DeselectIcon from '@mui/icons-material/Deselect'

const CreateRolePanel = ({
  roleName,
  setRoleName,
  roleDescription,
  setRoleDescription,
  availableScreens = [],
  screensLoading = false,
  selectedScreens = [],
  setSelectedScreens,
  creatingRole,
  handleCreateRole,
}) => {
  const [expanded, setExpanded] = useState(true)

  const handleSelectAllScreens = () => {
    const allCodes = availableScreens.map((s) =>
      typeof s === 'string' ? s : s.screenCode || s.screenValue,
    )
    setSelectedScreens(allCodes)
  }

  const handleClearScreens = () => {
    setSelectedScreens([])
  }

  return (
    <Paper
      elevation={0}
      sx={{
        borderRadius: '10px',
        padding: '16px 18px',
        border: '1px solid #e2e8f0',
        backgroundColor: '#ffffff',
        marginBottom: '14px',
        width: '100%',
      }}
    >
      {/* Header Bar with Front Icon, Info Tooltip & Expand/Collapse Controls */}
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
          <AddCircleOutlineIcon sx={{ color: '#0284c7', fontSize: 20 }} />
          <Typography
            variant='subtitle2'
            sx={{ fontWeight: 800, color: '#0f172a', fontSize: '0.875rem' }}
          >
            Create New Role
          </Typography>
          <Tooltip
            title='Create a new custom role, set optional description, and assign specific screen access permissions.'
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

      {/* Collapsible Body Content */}
      <Collapse in={expanded} timeout='auto' unmountOnExit={false}>
        <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
          {/* Row 1: Role Name & Description */}
          <Box
            sx={{
              display: 'flex',
              gap: 2,
              alignItems: 'center',
              flexWrap: 'wrap',
            }}
          >
            <TextField
              label='Role Name *'
              placeholder='e.g. gms_business_head'
              size='small'
              value={roleName}
              onChange={(e) => setRoleName(e.target.value)}
              sx={{
                width: 260,
                '& .MuiOutlinedInput-root': {
                  borderRadius: '8px',
                  backgroundColor: '#f8fafc',
                  transition: 'all 0.2s ease-in-out',
                  '& fieldset': {
                    borderColor: '#cbd5e1',
                  },
                  '&:hover fieldset': {
                    borderColor: '#94a3b8',
                  },
                  '&.Mui-focused': {
                    backgroundColor: '#ffffff',
                    '& fieldset': {
                      borderColor: '#0284c7',
                      borderWidth: '1.5px',
                    },
                    boxShadow: '0 0 0 3px rgba(2, 132, 199, 0.12)',
                  },
                },
                '& .MuiInputBase-input': {
                  fontSize: '0.8rem',
                  py: 0.9,
                  fontWeight: 600,
                  color: '#0f172a !important',
                  '&::placeholder': {
                    color: '#64748b',
                    opacity: 1,
                  },
                },
                '& .MuiInputLabel-root': {
                  fontSize: '0.8rem',
                  '&.Mui-focused': {
                    color: '#0284c7',
                    fontWeight: 700,
                  },
                },
              }}
            />
            <TextField
              label='Description'
              placeholder='Optional role description'
              size='small'
              value={roleDescription}
              onChange={(e) => setRoleDescription(e.target.value)}
              sx={{
                flex: 1,
                minWidth: 280,
                '& .MuiOutlinedInput-root': {
                  borderRadius: '8px',
                  backgroundColor: '#f8fafc',
                  transition: 'all 0.2s ease-in-out',
                  '& fieldset': {
                    borderColor: '#cbd5e1',
                  },
                  '&:hover fieldset': {
                    borderColor: '#94a3b8',
                  },
                  '&.Mui-focused': {
                    backgroundColor: '#ffffff',
                    '& fieldset': {
                      borderColor: '#0284c7',
                      borderWidth: '1.5px',
                    },
                    boxShadow: '0 0 0 3px rgba(2, 132, 199, 0.12)',
                  },
                },
                '& .MuiInputBase-input': {
                  fontSize: '0.8rem',
                  py: 0.9,
                  fontWeight: 600,
                  color: '#0f172a !important',
                  '&::placeholder': {
                    color: '#64748b',
                    opacity: 1,
                  },
                },
                '& .MuiInputLabel-root': {
                  fontSize: '0.8rem',
                  '&.Mui-focused': {
                    color: '#0284c7',
                    fontWeight: 700,
                  },
                },
              }}
            />
          </Box>

          {/* Row 2: Screen Assignment Picker */}
          <Box
            sx={{
              p: 1.5,
              borderRadius: '8px',
              border: '1px solid #e2e8f0',
              backgroundColor: '#f8fafc',
              display: 'flex',
              flexDirection: 'column',
              gap: 1.5,
            }}
          >
            <Box
              sx={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: 1,
              }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <DesktopWindowsIcon sx={{ color: '#0284c7', fontSize: 18 }} />
                <Typography
                  variant='caption'
                  sx={{
                    fontWeight: 700,
                    color: '#0f172a',
                    fontSize: '0.8rem',
                  }}
                >
                  Assign Accessible Screens
                </Typography>
                {selectedScreens.length > 0 && (
                  <Chip
                    label={`${selectedScreens.length} selected`}
                    size='small'
                    sx={{
                      fontWeight: 700,
                      backgroundColor: '#e0f2fe',
                      color: '#0369a1',
                      height: '20px',
                      fontSize: '0.68rem',
                    }}
                  />
                )}
              </Box>

              {/* Quick Screen Selector Controls */}
              {availableScreens.length > 0 && (
                <Box sx={{ display: 'flex', gap: 1 }}>
                  <Button
                    size='small'
                    onClick={handleSelectAllScreens}
                    startIcon={<SelectAllIcon sx={{ fontSize: 14 }} />}
                    sx={{
                      fontSize: '0.7rem',
                      fontWeight: 700,
                      textTransform: 'none',
                      color: '#0284c7',
                      py: 0.2,
                      px: 1,
                      minWidth: 'auto',
                    }}
                  >
                    Select All
                  </Button>
                  {selectedScreens.length > 0 && (
                    <Button
                      size='small'
                      onClick={handleClearScreens}
                      startIcon={<DeselectIcon sx={{ fontSize: 14 }} />}
                      sx={{
                        fontSize: '0.7rem',
                        fontWeight: 700,
                        textTransform: 'none',
                        color: '#64748b',
                        py: 0.2,
                        px: 1,
                        minWidth: 'auto',
                      }}
                    >
                      Clear Screens
                    </Button>
                  )}
                </Box>
              )}
            </Box>

            {/* Screen Multi-Select Autocomplete */}
            <Autocomplete
              multiple
              disableCloseOnSelect
              size='small'
              options={availableScreens}
              loading={screensLoading}
              getOptionLabel={(option) => {
                if (typeof option === 'string') {
                  const found = availableScreens.find(
                    (s) => s.screenCode === option || s.screenValue === option,
                  )
                  return found
                    ? found.screenValue || found.screenDisplayName || option
                    : option
                }
                return option.screenValue || option.screenDisplayName || ''
              }}
              isOptionEqualToValue={(option, val) => {
                const optCode =
                  typeof option === 'string'
                    ? option
                    : option.screenCode || option.screenValue
                const valCode =
                  typeof val === 'string'
                    ? val
                    : val.screenCode || val.screenValue
                return optCode === valCode
              }}
              value={availableScreens.filter((s) => {
                const code =
                  typeof s === 'string' ? s : s.screenCode || s.screenValue
                return selectedScreens.includes(code)
              })}
              onChange={(event, newValue) => {
                const selectedCodes = newValue.map((item) =>
                  typeof item === 'string'
                    ? item
                    : item.screenCode || item.screenValue,
                )
                if (typeof setSelectedScreens === 'function') {
                  setSelectedScreens(selectedCodes)
                }
              }}
              renderOption={(props, option, { selected }) => {
                const displayLabel =
                  typeof option === 'string'
                    ? availableScreens.find((s) => s.screenCode === option)
                        ?.screenValue || option
                    : option.screenValue || option.screenDisplayName
                return (
                  <li
                    {...props}
                    key={
                      typeof option === 'string'
                        ? option
                        : option.screenCode || option.screenValue
                    }
                  >
                    <Checkbox
                      icon={<CheckBoxOutlineBlankIcon fontSize='small' />}
                      checkedIcon={<CheckBoxIcon fontSize='small' />}
                      style={{ marginRight: 8 }}
                      checked={selected}
                    />
                    <Typography
                      variant='body2'
                      sx={{
                        fontWeight: 600,
                        fontSize: '0.8rem',
                        color: '#0f172a',
                      }}
                    >
                      {displayLabel}
                    </Typography>
                  </li>
                )
              }}
              renderTags={(value, getTagProps) =>
                value.map((option, index) => {
                  const label =
                    typeof option === 'string'
                      ? availableScreens.find((s) => s.screenCode === option)
                          ?.screenValue || option
                      : option.screenValue || option.screenDisplayName
                  const key =
                    typeof option === 'string'
                      ? option
                      : option.screenCode || option.screenValue
                  return (
                    <Chip
                      {...getTagProps({ index })}
                      key={key}
                      label={label}
                      size='small'
                      sx={{
                        fontWeight: 600,
                        fontSize: '0.72rem',
                        backgroundColor: '#e0f2fe',
                        color: '#0369a1',
                        borderRadius: '6px',
                        border: '1px solid #bae6fd',
                        height: '24px',
                      }}
                    />
                  )
                })
              }
              renderInput={(params) => (
                <TextField
                  {...params}
                  placeholder={
                    selectedScreens.length === 0
                      ? 'Search and select screens to assign...'
                      : ''
                  }
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '8px',
                      backgroundColor: '#ffffff',
                      '& fieldset': { borderColor: '#cbd5e1' },
                      '&:hover fieldset': { borderColor: '#94a3b8' },
                      '&.Mui-focused': {
                        backgroundColor: '#ffffff',
                        '& fieldset': {
                          borderColor: '#0284c7',
                          borderWidth: '1.5px',
                        },
                        boxShadow: '0 0 0 3px rgba(2, 132, 199, 0.12)',
                      },
                    },
                  }}
                />
              )}
              sx={{ width: '100%' }}
            />
          </Box>

          {/* Row 3: Action Buttons */}
          <Box
            sx={{
              display: 'flex',
              gap: 1.5,
              alignItems: 'center',
              justifyContent: 'flex-start',
            }}
          >
            <Button
              variant='contained'
              color='primary'
              size='small'
              disabled={!roleName.trim() || creatingRole}
              onClick={handleCreateRole}
              startIcon={
                creatingRole ? (
                  <CircularProgress size={14} color='inherit' />
                ) : (
                  <AddIcon style={{ fontSize: 16 }} />
                )
              }
              sx={{
                fontWeight: 700,
                borderRadius: '8px',
                textTransform: 'none',
                px: 2.5,
                height: '38px',
                fontSize: '0.78rem',
                boxShadow: 'none',
                backgroundColor: '#0284c7',
                '&:hover': {
                  backgroundColor: '#0369a1',
                },
              }}
            >
              {creatingRole ? 'Creating...' : 'Create Role'}
            </Button>
            {(roleName || roleDescription || selectedScreens.length > 0) && (
              <Button
                variant='outlined'
                color='inherit'
                size='small'
                onClick={() => {
                  setRoleName('')
                  setRoleDescription('')
                  if (typeof setSelectedScreens === 'function') {
                    setSelectedScreens([])
                  }
                }}
                sx={{
                  borderRadius: '8px',
                  textTransform: 'none',
                  height: '38px',
                  fontSize: '0.78rem',
                  color: '#64748b',
                  borderColor: '#cbd5e1',
                }}
              >
                Clear Form
              </Button>
            )}
          </Box>
        </Box>
      </Collapse>
    </Paper>
  )
}

export default CreateRolePanel
