import React from 'react'
import { Stack } from '@mui/material'
import PowerAssetCapacity from './PowerAssetCapacity'
import SteamAssetCapacity from './SteamAssetCapacity'

const AssetCapacity = () => {
  return (
    <Stack>
      <Stack sx={{ mb: 2 }}>
        <PowerAssetCapacity />
      </Stack>
      <Stack>
        <SteamAssetCapacity />
      </Stack>
    </Stack>
  )
}

export default AssetCapacity
