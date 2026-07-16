import React from 'react'
import Backdrop from '@mui/material/Backdrop'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import Logo from 'assets/images/ril-logo2.png'

const LoaderBackdrop = ({ open, showMessage = false, message = '' }) => {
  return (
    <Backdrop
      open={open}
      sx={{
        zIndex: (theme) => theme.zIndex.drawer + 1,
        backgroundColor: 'transparent',
        // pointerEvents: 'none',
      }}
    >
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 2,
        }}
      >
        {/* Spinner + Logo */}
        <Box
          sx={{
            position: 'relative',
            width: 50,
            height: 50,
          }}
        >
          {/* Spinner Ring */}
          <Box
            sx={{
              width: '100%',
              height: '100%',
              border: '3px solid rgba(0,0,0,0.15)',
              borderTop: '3px solid #1976d2',
              borderRadius: '50%',
              animation: 'spin 0.8s linear infinite',

              '@keyframes spin': {
                from: { transform: 'rotate(0deg)' },
                to: { transform: 'rotate(360deg)' },
              },
            }}
          />

          {/* Center Logo (static) */}
          <Box
            component='img'
            src={Logo}
            alt='logo'
            sx={{
              position: 'absolute',
              top: '50%',
              left: '50%',
              width: 20,
              height: 20,
              transform: 'translate(-50%, -50%)',
              objectFit: 'contain',
            }}
          />
        </Box>

        {/* Optional message below spinner */}
        {showMessage && message && (
          <Typography
            variant='h5'
            sx={{
              color: '#1976d2',
              fontWeight: 700,
              textAlign: 'center',
              maxWidth: 600,
              px: 3,
              py: 2,
              backgroundColor: 'rgba(255, 255, 255, 0.75)',
              backdropFilter: 'blur(6px)',
              borderRadius: 2,
              boxShadow: '0 2px 12px rgba(0,0,0,0.08)',
            }}
          >
            {message}
          </Typography>
        )}
      </Box>
    </Backdrop>
  )
}

export default LoaderBackdrop
