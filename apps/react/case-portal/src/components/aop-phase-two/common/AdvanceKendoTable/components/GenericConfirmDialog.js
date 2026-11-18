import React from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  IconButton,
} from '@mui/material'
import { styled } from '@mui/material/styles'
import CloseIcon from '@mui/icons-material/Close'
import HelpOutlineIcon from '@mui/icons-material/HelpOutline'

const CompactDialog = styled(Dialog)(() => ({
  '& .MuiPaper-root': {
    borderRadius: '12px',
    width: '600px',
    boxShadow: '0 12px 40px rgba(0,0,0,0.15)',
  },
}))

const GenericConfirmDialog = ({
  open = false,
  onClose = () => {},
  onConfirm = () => {},
  title = 'CONFIRM',
  message = 'Are you sure?',
  confirmText = 'Yes',
  cancelText = 'Cancel',
  disabled = false,
}) => {
  return (
    <CompactDialog
      open={!!open}
      onClose={onClose}
      disableScrollLock
      slotProps={{ backdrop: { disableScrollLock: true } }}
    >
      <DialogTitle
        sx={{
          p: 1.5,
          px: 2,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          bgcolor: '#f8fafc',
          borderBottom: '1px solid #e2e8f0',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <HelpOutlineIcon sx={{ fontSize: '1rem', color: '#334155' }} />
          <Typography
            sx={{
              fontWeight: 800,
              fontSize: '0.8rem',
              color: '#334155',
              letterSpacing: '0.4px',
            }}
          >
            {title}
          </Typography>
        </Box>

        <IconButton size='small' onClick={onClose} sx={{ color: '#334155' }}>
          <CloseIcon fontSize='small' />
        </IconButton>
      </DialogTitle>

      <DialogContent sx={{ p: 1.5, pt: '12px !important' }}>
        <Typography
          sx={{
            fontSize: '0.75rem',
            color: '#334155',
            lineHeight: 1.5,
            fontWeight: 600,
          }}
        >
          {message}
        </Typography>
      </DialogContent>

      <DialogActions sx={{ p: 1.5, pt: 0, gap: 1 }}>
        <Button onClick={onClose} className='btn-no'>
          {cancelText}
        </Button>

        <Button
          onClick={onConfirm}
          variant='contained'
          size='small'
          disabled={disabled}
          className='btn-yes'
        >
          {confirmText}
        </Button>
      </DialogActions>
    </CompactDialog>
  )
}

export default GenericConfirmDialog
