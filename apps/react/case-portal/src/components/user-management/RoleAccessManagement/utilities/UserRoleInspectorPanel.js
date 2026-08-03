import React, { useState } from 'react'
import {
  Box,
  Typography,
  Button,
  Chip,
  TextField,
  Autocomplete,
  Paper,
  CircularProgress,
  Collapse,
  IconButton,
  Tooltip,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import ManageAccountsIcon from '@mui/icons-material/ManageAccounts'
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown'
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'

const UserRoleInspectorPanel = ({
  lookupUser,
  lookupUserOptions,
  handleUserSearchForLookup,
  lookupUserLoading,
  handleRetrieveUserRoles,
  retrievedUserRoles,
  retrievingRoles,
  handleOpenUnassignDialog,
}) => {
  const [expanded, setExpanded] = useState(true)

  return (
    <Paper
      elevation={0}
      sx={{
        borderRadius: '10px',
        padding: '14px 16px',
        border: '1px solid #e2e8f0',
        backgroundColor: '#ffffff',
        maxWidth: '920px',
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
          <ManageAccountsIcon sx={{ color: '#0284c7', fontSize: 20 }} />
          <Typography
            variant="subtitle2"
            sx={{ fontWeight: 800, color: '#0f172a', fontSize: '0.875rem' }}
          >
            User Role Inspector
          </Typography>
          <Tooltip
            title="Inspect active assigned roles for a specific user and unassign roles if needed."
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
          <Box
            sx={{
              display: 'flex',
              gap: '10px',
              alignItems: 'center',
              marginBottom: '12px',
              maxWidth: '500px',
            }}
          >
            <Autocomplete
              options={lookupUserOptions}
              value={lookupUser}
              size="small"
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
              sx={{ flexGrow: 1 }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Select User to Inspect"
                  placeholder="Search username..."
                  size="small"
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
                />
              )}
            />
            {lookupUser && (
              <Button
                variant="outlined"
                color="primary"
                size="small"
                onClick={() => handleRetrieveUserRoles(lookupUser)}
                disabled={retrievingRoles}
                startIcon={<RefreshIcon style={{ fontSize: 14 }} />}
                sx={{
                  textTransform: 'none',
                  fontWeight: 600,
                  borderRadius: '6px',
                  height: '36px',
                  fontSize: '0.78rem',
                }}
              >
                Refresh
              </Button>
            )}
          </Box>

          {lookupUser ? (
            <Paper
              variant="outlined"
              sx={{
                padding: '12px 14px',
                borderRadius: '6px',
                backgroundColor: '#f8fafc',
                border: '1px solid #e2e8f0',
              }}
            >
              <Typography
                variant="body2"
                sx={{ fontWeight: 700, color: '#0f172a', marginBottom: '8px', fontSize: '0.8rem' }}
              >
                Assigned Roles for{' '}
                <span style={{ color: '#0284c7', fontWeight: 800 }}>
                  {lookupUser.username}
                </span>
                :
              </Typography>

              {retrievingRoles ? (
                <Box
                  sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 0.5 }}
                >
                  <CircularProgress size={14} />
                  <Typography variant="caption" sx={{ color: '#64748b' }}>
                    Fetching roles...
                  </Typography>
                </Box>
              ) : !Array.isArray(retrievedUserRoles) ||
                retrievedUserRoles.length === 0 ? (
                <Typography
                  variant="caption"
                  sx={{ color: '#94a3b8', fontStyle: 'italic', fontSize: '0.75rem' }}
                >
                  No active roles assigned to this user.
                </Typography>
              ) : (
                <Box
                  sx={{
                    display: 'flex',
                    flexWrap: 'wrap',
                    gap: '6px',
                    alignItems: 'center',
                  }}
                >
                  {retrievedUserRoles.map((r, idx) => {
                    const rName =
                      typeof r === 'string'
                        ? r
                        : r?.name || r?.code || String(r)
                    return (
                      <Chip
                        key={idx}
                        label={rName}
                        size="small"
                        onDelete={() => handleOpenUnassignDialog(rName)}
                        sx={{
                          fontWeight: 700,
                          backgroundColor: '#e0f2fe',
                          color: '#0369a1',
                          borderRadius: '4px',
                          fontSize: '0.75rem',
                          height: '24px',
                          border: '1px solid #bae6fd',
                          '& .MuiChip-deleteIcon': {
                            color: '#0284c7',
                            fontSize: 14,
                            '&:hover': {
                              color: '#ef4444',
                            },
                          },
                        }}
                      />
                    )
                  })}
                </Box>
              )}
            </Paper>
          ) : (
            <Typography
              variant="caption"
              sx={{ color: '#94a3b8', fontStyle: 'italic', fontSize: '0.75rem' }}
            >
              Select a user above to inspect their active assigned roles.
            </Typography>
          )}
        </Box>
      </Collapse>
    </Paper>
  )
}

export default UserRoleInspectorPanel
