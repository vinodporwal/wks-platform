import React from 'react'
import { Box } from '@mui/material'
import AverageAssetLoading from './AverageAssetLoading'
import AverageAssetLoadingDetailed from './AverageAssetLoadingDetailed'
import PowerAndSteamLoading from './PowerAndSteamLoading'

const AssetLoading = () => {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <PowerAndSteamLoading />
      {/* <AverageAssetLoading /> */}
      <AverageAssetLoadingDetailed />
    </Box>
  )
}

export default AssetLoading
