import React, { useState } from 'react'
import { Stack } from '@mui/material/index'
import FixedNorms from './FixedNorms'
import Quantity from './Quantity'

const InputNorms = () => {
  const [refreshKey, setRefreshKey] = useState(0)
  const triggerRefresh = () => setRefreshKey((k) => k + 1)

  return (
    <Stack>
      <FixedNorms refreshKey={refreshKey} onRefreshAll={triggerRefresh} />
      <Quantity refreshKey={refreshKey} onRefreshAll={triggerRefresh} />
    </Stack>
  )
}

export default InputNorms
