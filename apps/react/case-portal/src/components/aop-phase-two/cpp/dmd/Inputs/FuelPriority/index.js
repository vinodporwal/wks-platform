import React, { useState, useMemo, useCallback, useEffect } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
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
  const { year, plantObject } = dataGridStore
  const AOP_YEAR = year?.selectedYear
  const PLANT_ID = plantObject?.id

  const PLANT_ID_LIST = useMemo(() => (PLANT_ID ? [PLANT_ID] : []), [PLANT_ID])

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
  // that can be picked for priority. Goes through the
  // CPPPlantFuelSourceMapping table; non-JMD plants self-map so their own
  // availability rows resolve directly.
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
  // The fuel list is the same for every month — months in the availability
  // grid only carry quantities, not per-month fuel restrictions.
  // Keyed under the plant name and cppPlantFkId so lookups by asset rows
  // always hit regardless of name mismatches.
  const plantFuelMap = useMemo(() => {
    if (!fuelOptions.length || !availabilityRows.length) return {}
    const map = {}
    availabilityRows.forEach((row) => {
      const fuel = fuelOptions.find(
        (f) => String(f.value) === String(row.fuelId),
      )
      if (!fuel) return
      const keys = [
        ...new Set(
          [
            plantObject?.name,
            plantObject?.displayName,
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
  }, [availabilityRows, fuelOptions, plantObject])

  return (
    <div>
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
