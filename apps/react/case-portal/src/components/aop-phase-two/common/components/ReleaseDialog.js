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

// --- STYLED COMPONENTS ---

const CompactDialog = styled(Dialog)(() => ({
  '& .MuiPaper-root': {
    borderRadius: '12px',
    width: '600px',
    boxShadow: '0 12px 40px rgba(0,0,0,0.15)',
  },
}))

// --- COMPONENT ---

const ReleaseDialog = ({
  openReleaseDialogBox,
  closeReleaseDialogBox,
  submitConfirmation,
}) => {
  return (
    <CompactDialog
      open={!!openReleaseDialogBox}
      onClose={closeReleaseDialogBox}
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
          bgcolor: '#f8fafc',
          borderBottom: '1px solid #e2e8f0',
        }}
      >
        <Typography
          sx={{
            fontWeight: 800,
            fontSize: '0.8rem',
            color: '#334155',
            letterSpacing: '0.5px',
          }}
        >
          CONFIRM RELEASE
        </Typography>

        <IconButton
          size='small'
          onClick={closeReleaseDialogBox}
          sx={{ color: '#64748b' }}
        >
          <CloseIcon fontSize='small' />
        </IconButton>
      </DialogTitle>

      {/* Content */}
      <DialogContent sx={{ p: 1.5, pt: '12px !important' }}>
        <Typography
          sx={{
            fontSize: '0.75rem',
            color: '#475569',
            lineHeight: 1.5,
            fontWeight: 500,
          }}
        >
          Please confirm that{' '}
          <Box component='b' sx={{ color: '#16a34a' }}>
            Norms
          </Box>{' '}
          are verified before releasing for review.
        </Typography>
      </DialogContent>

      {/* Actions */}
      <DialogActions sx={{ p: 1.5, pt: 0, gap: 1 }}>
        <Button onClick={closeReleaseDialogBox} className='btn-no'>
          Cancel
        </Button>

        <Button
          onClick={submitConfirmation}
          variant='contained'
          size='small'
          autoFocus
          className='btn-yes'
        >
          Confirm
        </Button>
      </DialogActions>
    </CompactDialog>
  )
}

export default ReleaseDialog
