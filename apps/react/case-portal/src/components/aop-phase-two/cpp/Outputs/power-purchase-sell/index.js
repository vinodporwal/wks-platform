import React from 'react'
import { Box } from '@mui/material'
import PowerDemandGrid from './PowerDemandGrid'
import InterSitePowerTransferGrid from './InterSitePowerTransferGrid'

const PurchasePowerAndSell = () => {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <PowerDemandGrid />
      <InterSitePowerTransferGrid />
    </Box>
  )
}

export default PurchasePowerAndSell

