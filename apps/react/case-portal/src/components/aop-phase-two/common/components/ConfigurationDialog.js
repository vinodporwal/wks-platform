import React from 'react'
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  Typography,
  Zoom,
} from '@mui/material'
import { styled } from '@mui/material/styles'
import CloseIcon from '@mui/icons-material/Close'
import CloudDownloadIcon from '@mui/icons-material/CloudDownload'
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth'

// --- STYLED COMPONENTS ---

const StyledConfirmDialog = styled(Dialog)(() => ({
  '& .MuiPaper-root': {
    borderRadius: '24px',
    padding: '12px',
    backgroundColor: 'rgba(255, 255, 255, 0.98)',
    backdropFilter: 'blur(16px)',
    boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.2)',
    border: '1px solid #ffffff',
  },
}))

const DateHighlight = styled(Box)(() => ({
  display: 'inline-flex',
  alignItems: 'center',
  backgroundColor: 'rgba(1, 0, 203, 0.05)',
  color: '#0100cb',
  padding: '4px 12px',
  borderRadius: '8px',
  fontWeight: 700,
  fontSize: '0.85rem',
  margin: '0 4px',
}))

// --- COMPONENT ---

const ConfigurationDialog = ({
  open,
  onClose,
  onConfirm,
  startDate,
  endDate,
}) => {
  const formatDateForText = (date, time = false) => {
    if (!date) return ''
    const parsedDate = new Date(date)
    if (isNaN(parsedDate)) return 'Invalid Date'
    const day = String(parsedDate.getDate()).padStart(2, '0')
    const month = String(parsedDate.getMonth() + 1).padStart(2, '0')
    const year = parsedDate.getFullYear()
    let formatted = `${day}-${month}-${year}`
    if (time) {
      let hours = parsedDate.getHours()
      const minutes = String(parsedDate.getMinutes()).padStart(2, '0')
      const ampm = hours >= 12 ? 'PM' : 'AM'
      hours = hours % 12
      hours = hours ? hours : 12
      const formattedTime = `${String(hours).padStart(2, '0')}:${minutes} ${ampm}`
      formatted += ` ${formattedTime}`
    }
    return formatted
  }

  return (
    <StyledConfirmDialog
      open={open}
      onClose={onClose}
      TransitionComponent={Zoom}
      transitionDuration={300}
      disableScrollLock
    >
      {/* Close button */}
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: -1 }}>
        <IconButton onClick={onClose} size='small'>
          <CloseIcon fontSize='small' />
        </IconButton>
      </Box>

      {/* Icon + Title */}
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          pt: 0,
        }}
      >
        <Box
          sx={{
            p: 0.5,
            borderRadius: '50%',
            bgcolor: 'rgba(1, 0, 203, 0.1)',
            color: '#2563eb',
            mb: 0.5,
            animation: 'pulse 2s infinite',
          }}
        >
          <CloudDownloadIcon sx={{ fontSize: 32 }} />
        </Box>

        <DialogTitle
          sx={{
            textAlign: 'center',
            fontWeight: 800,
            fontSize: '1.15rem',
            color: '#1e293b',
            pb: 0,
          }}
        >
          Confirm Data Refresh
        </DialogTitle>
      </Box>

      {/* Body */}
      <DialogContent sx={{ textAlign: 'center', pt: 1 }}>
        <DialogContentText
          sx={{
            color: '#64748b',
            fontSize: '0.85rem',
            lineHeight: 1.45,
          }}
        >
          You are about to synchronize data for the selected period:
        </DialogContentText>

        {/* Date range highlight */}
        <Box
          sx={{
            mt: 2,
            mb: 1,
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            gap: 0.75,
          }}
        >
          <DateHighlight>
            <CalendarMonthIcon sx={{ fontSize: '0.9rem', mr: 0.5 }} />
            {formatDateForText(startDate)}
          </DateHighlight>

          <Typography variant='caption' fontWeight={900} color='text.disabled'>
            TO
          </Typography>

          <DateHighlight>
            <CalendarMonthIcon sx={{ fontSize: '0.9rem', mr: 0.5 }} />
            {formatDateForText(endDate)}
          </DateHighlight>
        </Box>
      </DialogContent>

      {/* Actions */}
      <DialogActions
        sx={{
          justifyContent: 'center',
          gap: 1.5,
          pb: 0,
          px: 0,
        }}
      >
        <Button
          onClick={onClose}
          sx={{
            color: '#64748b',
            textTransform: 'none',
            fontWeight: 700,
            fontSize: '0.85rem',
            px: 0,
          }}
        >
          No
        </Button>

        <Button
          onClick={onConfirm}
          variant='contained'
          autoFocus
          sx={{
            bgcolor: '#0100cb',
            textTransform: 'none',
            fontWeight: 700,
            fontSize: '0.85rem',
            px: 3,
            borderRadius: '10px',
            boxShadow: '0 8px 12px -3px rgba(1, 0, 203, 0.3)',
            transition: 'all 0.3s ease',
            '&:hover': {
              bgcolor: '#01008b',
              transform: 'scale(1.03)',
              boxShadow: '0 12px 16px -3px rgba(1, 0, 203, 0.4)',
            },
          }}
        >
          Yes, Refresh Data
        </Button>
      </DialogActions>
    </StyledConfirmDialog>
  )
}

export default ConfigurationDialog
