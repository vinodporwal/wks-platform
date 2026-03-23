import React from 'react'
import JCBFuel from './JCBFuel'
import FuelAvailability from './FuelAvailability'
import { Stack } from '../../../../../../node_modules/@mui/material/index'

const Fuel = () => {
  return (
    <Stack>
      <Stack sx={{ mb: 2 }}>
        <FuelAvailability />
      </Stack>
      <Stack>
        <JCBFuel />
      </Stack>
    </Stack>
  )
}

export default Fuel
