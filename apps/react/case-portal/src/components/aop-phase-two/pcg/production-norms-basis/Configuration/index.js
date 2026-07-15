import React from 'react'
import { Box } from '@mui/material'
import ConfigurationRange from './ConfigurationRange'
import ConfigurationLimit from './ConfigurationLimit'

const Configuration = () => {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <ConfigurationRange />
      <ConfigurationLimit />
    </Box>
  )
}

export default Configuration
