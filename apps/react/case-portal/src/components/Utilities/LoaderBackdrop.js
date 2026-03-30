import React from 'react'
import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'

const LoaderBackdrop = ({ open }) => {
  return (
    <Backdrop
      open={open}
      sx={{
        zIndex: (theme) => theme.zIndex.drawer + 1,
        color: '#fff',
        backdropFilter: 'blur(10px) saturate(120%)',
        WebkitBackdropFilter: 'blur(10px) saturate(120%)',
        background:
          'radial-gradient(circle at center, rgba(255,255,255,0.06), rgba(0,0,0,0.55))',
        animation: 'fadeIn 180ms ease-out',
        '@keyframes fadeIn': {
          from: { opacity: 0 },
          to: { opacity: 1 },
        },
      }}
    >
      <CircularProgress
        size={48}
        thickness={4}
        sx={{
          filter: 'drop-shadow(0 0 10px rgba(255,255,255,0.35))',
        }}
      />
    </Backdrop>
  )
}

export default LoaderBackdrop
