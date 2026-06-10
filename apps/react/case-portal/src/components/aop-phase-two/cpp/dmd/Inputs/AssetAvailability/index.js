import React from 'react'
import { Stack } from '@mui/material'
import PowerAssetAvailability from './PowerAssetAvailability'
import SteamAssetAvailability from './SteamAssetAvailability'
const AssetAvailability = () => {
  return (
    <Stack>
      <Stack sx={{ mb: 2 }}>
        <PowerAssetAvailability />
      </Stack>
      <Stack>
        <SteamAssetAvailability />
      </Stack>
    </Stack>
  )
}

export default AssetAvailability
