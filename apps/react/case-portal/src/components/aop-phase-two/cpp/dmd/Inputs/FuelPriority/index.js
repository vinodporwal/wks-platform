import React, { useState } from 'react'
import PlantFuelAvailability from './PlantFuelAvailability'
import { Stack } from '@mui/material'
import AssetFuelPriority from './AssetFuelPriority'
import AssetWiseCompatibleFuel from './AssetWiseCompatibleFuel'

const FuelPriority = () => {
  const [fuelOptions, setFuelOptions] = useState([])
  const [plantFuelMap, setPlantFuelMap] = useState({})
  return (
    <div>
      <Stack>
        <PlantFuelAvailability
          fuelOptions={fuelOptions}
          setFuelOptions={setFuelOptions}
          setPlantFuelMap={setPlantFuelMap}
        />
      </Stack>
      <Stack sx={{ mt: 2 }}>
        <AssetWiseCompatibleFuel fuelOptions={fuelOptions} />
      </Stack>
      <Stack sx={{ mt: 2 }}>
        <AssetFuelPriority
          fuelOptions={fuelOptions}
          plantFuelMap={plantFuelMap}
        />
      </Stack>
    </div>
  )
}

export default FuelPriority
