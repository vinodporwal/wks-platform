import React from 'react'
import { Box } from '@mui/material'
import ConfigurationRange from './ConfigurationRange'

const Configuration = () => {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <ConfigurationRange />
    </Box>
  )
}

export default Configuration
