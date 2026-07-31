import { useState, useCallback, useEffect, useMemo } from 'react'
import { Box, Stack } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import PowerAssetAvailability from './PowerAssetAvailability'
import SteamAssetAvailability from './SteamAssetAvailability'
import { AssetPriorityApiService } from 'components/aop-phase-two/services/cpp/jmd/assetPriorityApiService'

const AssetAvailability = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { year, jmdSelectedPlants, plantObject } = dataGridStore
  const AOP_YEAR = year?.selectedYear

  const PLANT_ID_LIST = plantObject?.id

  // useMemo(
  //   () => jmdSelectedPlants?.map((plant) => plant.id) || [],
  //   [jmdSelectedPlants],
  // )

  const [powerData, setPowerData] = useState([])
  const [steamData, setSteamData] = useState([])
  const [dataLoading, setDataLoading] = useState(false)

  const fetchData = useCallback(async () => {
    if (!PLANT_ID_LIST?.length || !AOP_YEAR) return
    setDataLoading(true)
    try {
      const res = await AssetPriorityApiService.getAssetPriority(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )
      setPowerData(res?.data?.PowerAssetPriorities || [])
      setSteamData(res?.data?.SteamAssetPriorities || [])
    } catch (error) {
      console.error('Error fetching asset priority data:', error)
    } finally {
      setDataLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  return (
    <Box>
      <Stack sx={{ mb: 2 }}>
        <PowerAssetAvailability
          apiData={powerData}
          dataLoading={dataLoading}
          onRefresh={fetchData}
        />
      </Stack>
      <Stack>
        <SteamAssetAvailability
          apiData={steamData}
          dataLoading={dataLoading}
          onRefresh={fetchData}
        />
      </Stack>
    </Box>
  )
}

export default AssetAvailability
