import React from 'react'
import { Box, Typography } from '@mui/material'

const DisabledUOM = ({ disabledUOM }) => {
  return (
    <Box
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: '4px 12px',
        borderRadius: '6px',
        backgroundColor: '#f8fafc',
        border: '1px solid #e2e8f0',
        height: '32px', // matching height of standard input controls
        boxSizing: 'border-box',
      }}
    >
      <Typography
        variant='caption'
        sx={{
          color: '#64748b',
          fontWeight: 600,
          fontSize: '11px',
          textTransform: 'uppercase',
          letterSpacing: '0.5px',
          mr: 1,
          fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
        }}
      >
        UNIT:
      </Typography>
      <Typography
        variant='body2'
        sx={{
          color: '#0f172a',
          fontWeight: 700,
          fontSize: '13px',
          fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
        }}
      >
        {disabledUOM}
      </Typography>
    </Box>
  )
}

export default DisabledUOM
