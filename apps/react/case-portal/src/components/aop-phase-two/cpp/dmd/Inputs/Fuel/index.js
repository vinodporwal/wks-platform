import React, { useState, useEffect, useMemo } from 'react'
import NetCalorificValue from './NetCalorificValue'
import FuelAvailability from './FuelAvailability'
import { Stack } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { FuelAvailabilityAPIService } from 'components/aop-phase-two/services/cpp/jmd/fuelAvailabilityAPIService'

const Fuel = () => {
  const keycloak = useSession()
  const [allFuels, setAllFuels] = useState([])
  const [allCategories, setAllCategories] = useState([])

  // Fetch fuel master data (categories + fuels) once on mount.
  // Both child grids (FuelAvailability + NetCalorificValue) share the
  // same fuel/category list, so we fetch it here and pass as props to
  // avoid duplicate API calls.
  useEffect(() => {
    if (!keycloak?.token) return
    const fetchFuels = async () => {
      try {
        const [catRes, fuelRes] = await Promise.all([
          FuelAvailabilityAPIService.getFuels(keycloak, 'CATEGORY'),
          FuelAvailabilityAPIService.getFuels(keycloak, 'FUEL'),
        ])
        setAllCategories(catRes?.data || [])
        setAllFuels(fuelRes?.data || [])
      } catch (error) {
        console.error('Error fetching fuel master data:', error)
        setAllCategories([])
        setAllFuels([])
      }
    }
    fetchFuels()
  }, [keycloak])

  // Category dropdown options (shared by both grids)
  const categoryOptions = useMemo(
    () =>
      allCategories.map((cat) => ({
        value: cat.id,
        label: cat.fuelDisplayName || cat.fuelName || cat.id,
      })),
    [allCategories],
  )

  return (
    <Stack>
      <Stack sx={{ mb: 2 }}>
        <FuelAvailability
          allFuels={allFuels}
          allCategories={allCategories}
          categoryOptions={categoryOptions}
        />
      </Stack>
      <Stack>
        <NetCalorificValue
          allFuels={allFuels}
          allCategories={allCategories}
          categoryOptions={categoryOptions}
        />
      </Stack>
    </Stack>
  )
}

export default Fuel
