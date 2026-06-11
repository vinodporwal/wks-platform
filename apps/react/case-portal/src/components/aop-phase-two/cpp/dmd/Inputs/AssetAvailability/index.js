import React, { useCallback, useState } from 'react'
import { Stack } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useDebounce } from 'hooks/useDebounce'
import { AssetPriorityApiService } from 'components/aop-phase-two/services/cpp/jmd/assetPriorityApiService'
import PowerAssetAvailability from './PowerAssetAvailability'
import SteamAssetAvailability from './SteamAssetAvailability'

const AssetAvailability = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year } = dataGridStore
  const AOP_YEAR = year?.selectedYear
  const PLANT_ID_LIST = plantObject?.id

  const [powerRows, setPowerRows] = useState([])
  const [steamRows, setSteamRows] = useState([])
  const [loading, setLoading] = useState(false)
  

  const fetchAssetPriorityData = useCallback(async () => {
    if (!PLANT_ID_LIST || !AOP_YEAR) return
    setLoading(true)
    try {
      const res = await AssetPriorityApiService.getAssetPriority(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )
      setPowerRows(res?.data?.PowerAssetPriorities ?? [])
      setSteamRows(res?.data?.SteamAssetPriorities ?? [])
    } catch (error) {
      console.error('Error fetching asset priority data:', error)
      setPowerRows([])
      setSteamRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR])

  useDebounce(
    () => {
      fetchAssetPriorityData()
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR, fetchAssetPriorityData],
  )

  return (
    <Stack>
      <Stack sx={{ mb: 2 }}>
        <PowerAssetAvailability
          initialRows={powerRows}
          onRefresh={fetchAssetPriorityData}
          externalLoading={loading}
        />
      </Stack>
      <Stack>
        <SteamAssetAvailability
          initialRows={steamRows}
          onRefresh={fetchAssetPriorityData}
          externalLoading={loading}
        />
      </Stack>
    </Stack>
  )
}
export default AssetAvailability