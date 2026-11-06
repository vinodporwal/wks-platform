import React, { useState, useMemo, useCallback, useEffect } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
// import PlantFuelAvailability from './PlantFuelAvailability'
import { Stack } from '@mui/material'
import AssetFuelPriority from './AssetFuelPriority'
import { InputApiService } from 'components/aop-phase-two/services/cpp/jmd/inputApiService'
import { FuelAvailabilityAPIService } from 'components/aop-phase-two/services/cpp/jmd/fuelAvailabilityAPIService'
import { useDebounce } from 'hooks/useDebounce'

// Type used to filter fuel availability transaction rows (same as Fuel tab)
const DATA_TYPE = 'FUEL_AVAILABILITY'

const FuelPriority = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { year, jmdSelectedPlants } = dataGridStore
  const AOP_YEAR = year?.selectedYear

  const PLANT_ID_LIST = useMemo(
    () => jmdSelectedPlants?.map((plant) => plant.id) || [],
    [jmdSelectedPlants],
  )

  const [fuelOptions, setFuelOptions] = useState([])
  const [availabilityRows, setAvailabilityRows] = useState([])

  // Fuel master list (shared option shape used by the child grids)
  useEffect(() => {
    if (!keycloak?.token) return
    const fetchFuelOptions = async () => {
      try {
        const res = await InputApiService.getFuelMaster(keycloak)
        const rawOptions = res?.data || []
        const formattedOptions = rawOptions.map((fuel) => ({
          fuelId: fuel.id,
          value: fuel.id,
          label: fuel.fuelName || fuel.fuelDisplayName || fuel.id,
          fuelName: fuel.fuelName || '',
          fuelDisplayName: fuel.fuelDisplayName || '',
          fuelCode: fuel.fuelCode || '',
          uom: fuel.uom || '',
          categoryFkId: fuel.categoryFkId || '',
          categoryName: fuel.categoryName || '',
          categoryDisplayName: fuel.categoryDisplayName || '',
        }))
        setFuelOptions(formattedOptions)
      } catch (error) {
        console.error('Error fetching fuel master options:', error)
      }
    }
    fetchFuelOptions()
  }, [keycloak])

  // Fuel availability rows entered on the Fuel tab — these are the fuels
  // that can be picked for priority (replaces plant-fuel-availability-monthly).
  // Goes through the CPPPlantFuelSourceMapping table so fuels declared under
  // source plants (e.g. C2-CPP, DTA-PCG-CPP) are attributed to the CPP plant.
  const fetchAvailability = useCallback(async () => {
    try {
      const res = await FuelAvailabilityAPIService.getMappedFuelAvailability(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
        DATA_TYPE,
      )
      setAvailabilityRows(res?.data || [])
    } catch (error) {
      console.error('Error fetching mapped fuel availability data:', error)
      setAvailabilityRows([])
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR])

  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && AOP_YEAR) {
        fetchAvailability()
      }
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR, fetchAvailability],
  )

  // plantFuelMap = { plantName -> [fuelOption, ...] }
  // built from the Fuel Availability rows (cppPlantFkId + fuelId per row).
  // The fuel list is the same for every month — months in the availability
  // grid only carry quantities, not per-month fuel restrictions.
  // Keyed under both the selected-plant name and the stored cppPlantName so
  // lookups by asset rows' plantName always hit.
  const plantFuelMap = useMemo(() => {
    if (!fuelOptions.length || !availabilityRows.length) return {}
    const map = {}
    availabilityRows.forEach((row) => {
      const fuel = fuelOptions.find(
        (f) => String(f.value) === String(row.fuelId),
      )
      if (!fuel) return
      const plant = jmdSelectedPlants?.find(
        (p) => String(p.id) === String(row.cppPlantFkId),
      )
      const keys = [
        ...new Set(
          [
            plant?.name,
            plant?.displayName,
            row.cppPlantName,
            row.cppPlantFkId,
          ].filter(Boolean),
        ),
      ]
      keys.forEach((key) => {
        if (!map[key]) map[key] = []
        if (!map[key].some((o) => o.value === fuel.value)) {
          map[key].push(fuel)
        }
      })
    })
    return map
  }, [availabilityRows, fuelOptions, jmdSelectedPlants])

  return (
    <div>
      {/* <Stack>
        <PlantFuelAvailability
          fuelOptions={fuelOptions}
          setFuelOptions={setFuelOptions}
          setPlantFuelMap={setPlantFuelMap}
        />
      </Stack> */}

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
