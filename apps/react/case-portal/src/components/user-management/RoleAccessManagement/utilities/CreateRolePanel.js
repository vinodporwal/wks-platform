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
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline'
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown'
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'

const CreateRolePanel = ({
  roleName,
  setRoleName,
  roleDescription,
  setRoleDescription,
  creatingRole,
  handleCreateRole,
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
        marginBottom: '14px',
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
          <AddCircleOutlineIcon sx={{ color: '#0284c7', fontSize: 20 }} />
          <Typography
            variant="subtitle2"
            sx={{ fontWeight: 800, color: '#0f172a', fontSize: '0.875rem' }}
          >
            Create New Role
          </Typography>
          <Tooltip
            title="Create and add a new custom role with an optional description to the system catalog."
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
        <Box sx={{ pt: 1.5, display: 'flex', gap: 1.5, alignItems: 'center', flexWrap: 'wrap' }}>
          <TextField
            label="Role Name *"
            placeholder="e.g. gms_business_head"
            size="small"
            value={roleName}
            onChange={(e) => setRoleName(e.target.value)}
            sx={{
              width: 250,
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
            label="Description"
            placeholder="Optional role description"
            size="small"
            value={roleDescription}
            onChange={(e) => setRoleDescription(e.target.value)}
            sx={{
              width: 330,
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
          <Button
            variant="contained"
            color="primary"
            size="small"
            disabled={!roleName.trim() || creatingRole}
            onClick={handleCreateRole}
            startIcon={
              creatingRole ? (
                <CircularProgress size={14} color="inherit" />
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
          {(roleName || roleDescription) && (
            <Button
              variant="outlined"
              color="inherit"
              size="small"
              onClick={() => {
                setRoleName('')
                setRoleDescription('')
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
              Clear
            </Button>
          )}
        </Box>
      </Collapse>
    </Paper>
  )
}

export default CreateRolePanel
