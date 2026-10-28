import { Stack } from '@mui/material'
import Efficiency from './Efficiency'
import FuelRatio from './FuelRatio'

const EfficiencyAndFuelRatio = () => {
  return (
    <Stack>
      <Stack sx={{ mb: 2 }}>
        <Efficiency />
      </Stack>
      <Stack>
        <FuelRatio />
      </Stack>
    </Stack>
  )
}

export default EfficiencyAndFuelRatio
