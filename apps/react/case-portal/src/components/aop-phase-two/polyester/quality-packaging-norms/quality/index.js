import React from 'react'
import { Box } from '@mui/material'
import QualityParameters from './QualityParameters'
import PriceDifferential from './PriceDifferential'

const Quality = () => {
  return (
    <Box>
      <Box>
        <QualityParameters />
      </Box>
      <Box sx={{ mt: 3 }}>
        <PriceDifferential />
      </Box>
    </Box>
  )
}

export default Quality
