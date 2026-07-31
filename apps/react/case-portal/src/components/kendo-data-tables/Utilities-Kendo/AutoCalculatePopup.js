import React from 'react'
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material'
import { styled } from '@mui/material/styles'
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded'

const AlertDialog = styled(Dialog)(({ theme }) => ({
  '& .MuiPaper-root': {
    borderRadius: '14px',
    width: '440px',
    boxShadow: '0 20px 60px rgba(0,0,0,0.2)',
    overflow: 'hidden',
  },
}))

const AutoCalculatePopup = ({ open, onYes, onNo }) => {
  return (
    <AlertDialog
      open={open}
      disableEscapeKeyDown
      disableScrollLock
      slotProps={{
        backdrop: { disableScrollLock: true },
      }}
    >
      <DialogTitle
        sx={{
          p: 1.5,
          px: 2,
          display: 'flex',
          alignItems: 'center',
          gap: 1.5,
          bgcolor: '#f8fafc',
          borderBottom: '1px solid #e2e8f0',
        }}
      >
        <WarningAmberRoundedIcon
          sx={{ color: '#f59e0b', fontSize: '1.4rem' }}
        />
        <Typography
          sx={{
            fontWeight: 800,
            fontSize: '0.85rem',
            color: '#334155',
            letterSpacing: '0.3px',
          }}
        >
          Calculate ?
        </Typography>
      </DialogTitle>

      <DialogContent sx={{ p: 2, pt: '16px !important' }}>
        <Typography
          sx={{
            fontSize: '0.8rem',
            color: '#475569',
            lineHeight: 1.6,
            fontWeight: 600,
          }}
        >
          Basis values have been updated. Click Calculate to refresh the
          results.
        </Typography>
      </DialogContent>

      <DialogActions sx={{ p: 1.5, gap: 1, borderTop: '1px solid #e2e8f0' }}>
        <Button onClick={onNo} className='btn-no'>
          No
        </Button>

        <Button
          onClick={onYes}
          variant='contained'
          size='small'
          className='btn-yes'
        >
          YES, Calculate
        </Button>
      </DialogActions>
    </AlertDialog>
  )
}

export default AutoCalculatePopup
