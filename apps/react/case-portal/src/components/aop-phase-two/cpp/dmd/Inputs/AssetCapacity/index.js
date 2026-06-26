import React, { useCallback, useRef, useState } from 'react'
import { Stack } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useDebounce } from 'hooks/useDebounce'
import { InputApiService } from 'components/aop-phase-two/services/cpp/jmd/inputApiService'
import PowerAssetCapacity from './PowerAssetCapacity'
import SteamAssetCapacity from './SteamAssetCapacity'

const AssetCapacity = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year } = dataGridStore
  const AOP_YEAR = year?.selectedYear
  const PLANT_ID_LIST = plantObject?.id

  const [powerRows, setPowerRows] = useState([])
  const [steamRows, setSteamRows] = useState([])
  const [loading, setLoading] = useState(false)
  const fetchInProgressRef = useRef(false)

  const fetchAssetCapacityData = useCallback(async () => {
    if (!PLANT_ID_LIST || !AOP_YEAR) return
    if (fetchInProgressRef.current) return
    fetchInProgressRef.current = true
    setLoading(true)
    try {
      const res = await InputApiService.getAssetCapacities(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )
      setPowerRows(res?.data?.PowerAssetCapacities ?? [])
      setSteamRows(res?.data?.SteamAssetCapacities ?? [])
    } catch (error) {
      console.error('Error fetching asset capacity data:', error)
      setPowerRows([])
      setSteamRows([])
    } finally {
      setLoading(false)
      fetchInProgressRef.current = false
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR])

  useDebounce(
    () => {
      fetchAssetCapacityData()
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR, fetchAssetCapacityData],
  )
  return (
    <Stack>
      <Stack sx={{ mb: 2 }}>
        <PowerAssetCapacity
          initialRows={powerRows}
          onRefresh={fetchAssetCapacityData}
          externalLoading={loading}
        />
      </Stack>
      <Stack>
        <SteamAssetCapacity
          initialRows={steamRows}
          onRefresh={fetchAssetCapacityData}
          externalLoading={loading}
        />
      </Stack>
    </Stack>
  )
}

export default AssetCapacity
