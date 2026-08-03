import { useState, useCallback, useEffect } from 'react'
import { Box, Stack } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useMemo } from 'react'
import HoursGrid from './HoursGrid'
import PowerGrid from './PowerGrid'
import STGGrid from './STGGrid'
import { InputApiService } from 'components/aop-phase-two/services/cpp/jmd/inputApiService'

const ShutdownAndOperational = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { year, jmdSelectedPlants, plantObject } = dataGridStore
  const AOP_YEAR = year?.selectedYear

  const PLANT_ID_LIST = plantObject?.id

  // useMemo(
  //   () => jmdSelectedPlants?.map((plant) => plant.id) || [],
  //   [jmdSelectedPlants],
  // )

  const [hoursRows, setHoursRows] = useState([])
  const [powerData, setPowerData] = useState([])
  const [steamData, setSteamData] = useState([])
  const [dataLoading, setDataLoading] = useState(false)

  // Single shared fetch — called once on load and after any save/import in either grid
  const fetchData = useCallback(async () => {
    if (!PLANT_ID_LIST?.length || !AOP_YEAR) return
    setDataLoading(true)
    try {
      const res = await InputApiService.getOperationHoursData(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )
      setPowerData([
        ...(res?.data?.PowerOperationalHours || []),
        ...(res?.data?.ImportOperationalHours || []),
      ])
      setSteamData(res?.data?.SteamOperationalHours || [])
    } catch (error) {
      console.error('Error fetching operational hours data:', error)
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
        <HoursGrid onHoursRowsChange={setHoursRows} />
      </Stack>
      <Stack sx={{ mb: 2 }}>
        <PowerGrid
          hoursRows={hoursRows}
          apiData={powerData}
          dataLoading={dataLoading}
          onRefresh={fetchData}
        />
      </Stack>
      <Stack>
        <STGGrid
          hoursRows={hoursRows}
          apiData={steamData}
          dataLoading={dataLoading}
          onRefresh={fetchData}
        />
      </Stack>
    </Box>
  )
}

export default ShutdownAndOperational
