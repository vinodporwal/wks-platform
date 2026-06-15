import React from 'react'
import { Box, Typography } from '@mui/material'
import ProductionVolumeReferenceGrid from './ProductionVolumeReferenceGrid'
import BusinessDemandGrid from './BusinessDemandGrid'

const BusinessDemand = () => {
  return (
    <Box>
      {/* <Box sx={{ pb: 1, background: 'transparent' }}>
        <Typography
          component='span'
          className='accordian-title'
          sx={{ fontWeight: 600, fontSize: '1rem', color: '#1e293b' }}
        >
          Production Target (MT) (This is a reference for entering the Business
          Demand value)
        </Typography>
      </Box>
      <Box sx={{ mb: 4 }}>
        <ProductionVolumeReferenceGrid />
      </Box> */}
      <Box>
        <BusinessDemandGrid />
      </Box>
    </Box>
  )
}

export default BusinessDemand
