import React, { useState, useEffect } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Typography,
  Box,
  Stack,
  CircularProgress,
} from '@mui/material'
import SendIcon from '@mui/icons-material/Send'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import UndoIcon from '@mui/icons-material/Undo'
import AssignmentIndIcon from '@mui/icons-material/AssignmentInd'
import AccountTreeIcon from '@mui/icons-material/AccountTree'
import LocationOnIcon from '@mui/icons-material/LocationOn'
import CalendarTodayIcon from '@mui/icons-material/CalendarToday'

/**
 * Utility Remarks PopUp for Workflow Actions
 * Provides a clean modal with context metadata (Role, Action, Gate, Plant, Year)
 * and a text area for capturing user remarks before executing workflow API calls.
 */
const WorkflowRemarksDialog = ({
  open,
  onClose,
  onSubmit,
  actionType = 'APPROVE', // 'SUBMIT' | 'APPROVE' | 'REVERT'
  actionLabel = 'Approve',
  role = '',
  siteName = '',
  gateName = '',
  plantName = '',
  year = '',
  isMandatory = true,
  loading = false,
}) => {
  const [remark, setRemark] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (open) {
      setRemark('')
      setError('')
    }
  }, [open])

  const handleConfirm = () => {
    if (isMandatory && !remark.trim()) {
      setError('A remark is required for this action.')
      return
    }
    setError('')
    onSubmit(remark)
  }

  // Visual configuration based on action type
  const getActionConfig = () => {
    switch (actionType?.toUpperCase()) {
      case 'SUBMIT':
        return {
          title: 'Submit',
          color: '#1565c0',
          bgColor: '#e3f2fd',
          borderColor: '#1976d2',
          icon: <SendIcon sx={{ fontSize: 20, color: '#1565c0' }} />,
          btnIcon: <SendIcon sx={{ fontSize: 16 }} />,
          confirmText: 'Confirm Submission',
          btnBg: '#1976d2',
          btnHoverBg: '#1565c0',
        }
      case 'APPROVE':
        return {
          title: 'Approve',
          color: '#2e7d32',
          bgColor: '#e8f5e9',
          borderColor: '#2e7d32',
          icon: (
            <CheckCircleOutlineIcon sx={{ fontSize: 20, color: '#2e7d32' }} />
          ),
          btnIcon: <CheckCircleOutlineIcon sx={{ fontSize: 16 }} />,
          confirmText: 'Confirm Approval',
          btnBg: '#2e7d32',
          btnHoverBg: '#1b5e20',
        }
      case 'REVERT':
      case 'REJECT':
        return {
          title: 'Reject',
          color: '#c62828',
          bgColor: '#ffebee',
          borderColor: '#c62828',
          icon: <UndoIcon sx={{ fontSize: 20, color: '#c62828' }} />,
          btnIcon: <UndoIcon sx={{ fontSize: 16 }} />,
          confirmText: 'Confirm Revert',
          btnBg: '#c62828',
          btnHoverBg: '#b71c1c',
        }
      default:
        return {
          title: 'Workflow Action',
          color: '#1976d2',
          bgColor: '#f5f5f5',
          borderColor: '#cccccc',
          icon: null,
          btnIcon: null,
          confirmText: 'Confirm',
          btnBg: '#1976d2',
          btnHoverBg: '#1565c0',
        }
    }
  }

  const config = getActionConfig()

  return (
    <Dialog
      open={open}
      onClose={loading ? undefined : onClose}
      fullWidth
      maxWidth='sm'
      PaperProps={{
        sx: {
          borderRadius: '12px',
          boxShadow: '0 8px 32px rgba(0,0,0,0.12)',
        },
      }}
    >
      {/* Title Bar */}
      <DialogTitle
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1.5,
          borderBottom: '1px solid #e2e8f0',
          py: 2,
          px: 3,
        }}
      >
        {config.icon}
        <Typography
          variant='h6'
          sx={{ fontWeight: 700, fontSize: '1.1rem', color: '#1e293b' }}
        >
          {config.title}
        </Typography>
      </DialogTitle>

      <DialogContent sx={{ px: 3, pt: 2.5, pb: 2 }}>
        {/* Metadata Details Card */}
        <Box
          sx={{
            mb: 2.5,
            p: 2,
            borderRadius: '8px',
            backgroundColor: config.bgColor,
            border: `1px solid ${config.borderColor}`,
          }}
        >
          <Stack spacing={1.2}>
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: 1,
              }}
            >
              <Typography
                variant='caption'
                sx={{
                  color: '#475569',
                  fontWeight: 600,
                  textTransform: 'uppercase',
                  letterSpacing: 0.5,
                }}
              >
                Details
              </Typography>
            </Box>

            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: 1.5,
                pt: 0.5,
              }}
            >
              {role && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.8 }}>
                  <AssignmentIndIcon sx={{ fontSize: 16, color: '#64748b' }} />
                  <Typography
                    variant='body2'
                    sx={{ color: '#334155', fontSize: '0.82rem' }}
                  >
                    <strong>Role:</strong> {role}
                  </Typography>
                </Box>
              )}

              {siteName && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.8 }}>
                  <LocationOnIcon sx={{ fontSize: 16, color: '#64748b' }} />
                  <Typography
                    variant='body2'
                    sx={{ color: '#334155', fontSize: '0.82rem' }}
                  >
                    <strong>Site:</strong> {siteName}
                  </Typography>
                </Box>
              )}

              {plantName && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.8 }}>
                  <LocationOnIcon sx={{ fontSize: 16, color: '#64748b' }} />
                  <Typography
                    variant='body2'
                    sx={{ color: '#334155', fontSize: '0.82rem' }}
                  >
                    <strong>Plant:</strong> {plantName}
                  </Typography>
                </Box>
              )}

              {year && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.8 }}>
                  <CalendarTodayIcon sx={{ fontSize: 16, color: '#64748b' }} />
                  <Typography
                    variant='body2'
                    sx={{ color: '#334155', fontSize: '0.82rem' }}
                  >
                    <strong>Year:</strong> {year}
                  </Typography>
                </Box>
              )}
            </Box>
          </Stack>
        </Box>

        {/* Remarks Input Area */}
        <TextField
          autoFocus
          margin='dense'
          label={
            isMandatory
              ? 'Enter Remarks (Required) *'
              : 'Enter Remarks (Optional)'
          }
          placeholder='Write reason, notes or comments for this action...'
          fullWidth
          multiline
          minRows={3}
          maxRows={6}
          value={remark}
          onChange={(e) => {
            setRemark(e.target.value)
            if (e.target.value.trim()) setError('')
          }}
          error={Boolean(error)}
          helperText={
            error || 'Remarks will be saved to the workflow audit trail.'
          }
          sx={{
            '& .MuiOutlinedInput-root': {
              borderRadius: '8px',
            },
          }}
        />
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2.5, pt: 1, gap: 1 }}>
        <Button
          onClick={onClose}
          disabled={loading}
          sx={{
            color: '#64748b',
            textTransform: 'none',
            fontWeight: 600,
          }}
        >
          Cancel
        </Button>

        <Button
          variant='contained'
          onClick={handleConfirm}
          disabled={loading}
          startIcon={
            loading ? (
              <CircularProgress size={16} color='inherit' />
            ) : (
              config.btnIcon
            )
          }
          sx={{
            backgroundColor: config.btnBg,
            '&:hover': {
              backgroundColor: config.btnHoverBg,
            },
            textTransform: 'none',
            fontWeight: 600,
            px: 2.5,
            borderRadius: '6px',
            boxShadow: '0 2px 6px rgba(0,0,0,0.15)',
          }}
        >
          {config.confirmText}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default WorkflowRemarksDialog
