import React from 'react'
import Snackbar from '@mui/material/Snackbar'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import { styled, keyframes } from '@mui/material/styles'

// --- ANIMATIONS ---

// The "Spring" Entrance: Scales up and snaps into place
const springIn = keyframes`
  0% { transform: translate3d(100%, 0, 0) scale(0.5); opacity: 0; }
  70% { transform: translate3d(-10%, 0, 0) scale(1.05); opacity: 1; }
  100% { transform: translate3d(0, 0, 0) scale(1); opacity: 1; }
`

// The Progress Bar shrink animation
const shrink = keyframes`
  from { width: 100%; }
  to { width: 0%; }
`

const accentColors = {
  success: '#10b981',
  warning: '#f59e0b',
  error: '#ef4444',
  info: '#0100cb',
}

// --- STYLED COMPONENTS ---

const StyledAlert = styled(Alert)(({ theme, severity }) => ({
  backgroundColor: 'rgba(255, 255, 255, 0.96)',
  backdropFilter: 'blur(12px)',
  color: '#1e293b',
  fontWeight: 600,
  fontSize: '0.85rem',
  borderRadius: '16px', // Rounded for modern look
  padding: '6px 10px',
  boxShadow:
    '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05)',
  border: '1px solid rgba(0, 0, 0, 0.05)',
  position: 'relative',
  overflow: 'hidden',

  // Apply the Spring Animation
  animation: `${springIn} 0.6s cubic-bezier(0.23, 1, 0.32, 1)`,

  // Smooth interaction transitions
  transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',

  '&:hover': {
    transform: 'translateY(-4px) scale(1.02)',
    boxShadow: '0 25px 30px -5px rgba(0, 0, 0, 0.15)',
  },

  '& .MuiAlert-icon': {
    color: `${accentColors[severity]} !important`,
    fontSize: '24px',
    transition: 'transform 0.3s ease',
  },

  '&:hover .MuiAlert-icon': {
    transform: 'rotate(-10deg) scale(1.2)',
  },

  '& .MuiAlert-message': {
    padding: '8px 0',
  },
}))

// The visual timer at the bottom
const ProgressBar = styled(Box)(({ severity, duration }) => ({
  position: 'absolute',
  bottom: 0,
  left: 0,
  height: '4px',
  backgroundColor: accentColors[severity],
  opacity: 0.6,
  animation: `${shrink} ${duration}ms linear forwards`,
}))

const Notification = ({
  open,
  message = 'Action completed',
  severity = 'info',
  duration = 4000,
  onClose,
  autoHide = true,
}) => {
  return (
    <Snackbar
      open={open}
      autoHideDuration={autoHide ? duration : null}
      onClose={onClose}
      anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
      sx={{
        top: '0px !important',
        right: '10px !important',
        zIndex: 9999,
      }}
    >
      <StyledAlert
        onClose={onClose}
        severity={severity}
        // Important: this prevents the default MUI transition so our custom one works
        TransitionProps={{ enter: false, exit: false }}
      >
        {message}
        {/* Adds the animated timer bar at the bottom */}
        <ProgressBar severity={severity} duration={duration} />
      </StyledAlert>
    </Snackbar>
  )
}

export default Notification
