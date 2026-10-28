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
import { CalculateIcon as CalculateImageIcon } from 'assets/images/icons'

// --- STYLED COMPONENTS ---

const CompactDialog = styled(Dialog)(() => ({
  '& .MuiPaper-root': {
    borderRadius: '12px',
    width: '600px',
    boxShadow: '0 12px 40px rgba(0,0,0,0.15)',
  },
}))

// --- COMPONENT ---

const CalculateConfirmationDialog = ({
  openCalculateDialogeBox,
  closeCalculateDialogBox,
  handleCalculateConfirmation,
}) => {
  return (
    <CompactDialog
      open={!!openCalculateDialogeBox}
      onClose={closeCalculateDialogBox}
      disableScrollLock
      slotProps={{ backdrop: { disableScrollLock: true } }}
    >
      {/* Header */}
      <DialogTitle
        sx={{
          p: 1.5,
          px: 2,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          bgcolor: '#f6f8fa',
          borderBottom: '1px solid #DDDEE1',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Box component='img' src={CalculateImageIcon} className='w16-icon' />
          <Typography
            sx={{
              fontWeight: 800,
              fontSize: '0.8rem',
              color: 'text.primary',
              letterSpacing: '0.4px',
            }}
          >
            Confirm Calculate
          </Typography>
        </Box>

        <IconButton
          size='small'
          onClick={closeCalculateDialogBox}
          sx={{ color: 'text.primary' }}
        >
          <CloseIcon fontSize='small' />
        </IconButton>
      </DialogTitle>

      {/* Content */}
      <DialogContent sx={{ p: 1.5, pt: '12px !important' }}>
        <Typography
          sx={{
            fontSize: '0.75rem',
            color: 'text.primary',
            lineHeight: 1.5,
            fontWeight: 600,
          }}
        >
          Are you sure you want to calculate? This will override the existing
          values.
        </Typography>
      </DialogContent>

      {/* Actions */}
      <DialogActions sx={{ p: 1.5, pt: 0, gap: 1 }}>
        <Button onClick={closeCalculateDialogBox} className='btn-no'>
          Cancel
        </Button>

        <Button
          onClick={handleCalculateConfirmation}
          variant='contained'
          size='small'
          className='btn-yes'
        >
          Calculate
        </Button>
      </DialogActions>
    </CompactDialog>
  )
}

export default CalculateConfirmationDialog
