import React, { useState } from 'react'
import PlantFuelAvailability from './PlantFuelAvailability'
import { Stack } from '@mui/material'
import AssetFuelPriority from './AssetFuelPriority'

const FuelPriority = () => {
  const [fuelOptions, setFuelOptions] = useState([])
  // plantFuelMap: { [plantName]: fuelOption[] } — derived from PlantFuelAvailability rows
  // used by AssetFuelPriority to show only plant-specific fuels in dropdowns
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
        <AssetFuelPriority
          fuelOptions={fuelOptions}
          plantFuelMap={plantFuelMap}
        />
      </Stack>
    </div>
  )
}

export default FuelPriority
