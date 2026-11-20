import React from 'react'
import { Box } from '@mui/material'
import ConfigurationRange from './ConfigurationRange'

const Configuration = (props) => {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <ConfigurationRange {...props} />
    </Box>
  )
}

export default Configuration
