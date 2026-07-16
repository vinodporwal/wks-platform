import { useState, useEffect, useCallback, useMemo } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { Box, Stack } from '@mui/material'
import { InputApiService } from 'components/aop-phase-two/services/cpp/jmd/inputApiService'
import HoursGrid from './HoursGrid'
import PowerGrid from './PowerGrid'
import STGGrid from './STGGrid'

const ShutdownAndOperational = () => {
  const [hoursRows, setHoursRows] = useState([])
  const [powerData, setPowerData] = useState([])
  const [steamData, setSteamData] = useState([])
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: '',
  })
  const [loading, setLoading] = useState(false)

  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, jmdSelectedPlants } = dataGridStore
  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) {
      setPowerData([])
      setSteamData([])
      return
    }

    try {
      const res = await InputApiService.getOperationHoursData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      setPowerData(res?.data?.PowerOperationalHours || [])
      setSteamData(res?.data?.SteamOperationalHours || [])
      if (!res || res?.data?.PowerOperationalHours?.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        setLoading(false)
        return
      }
      if (!res || res?.data?.SteamOperationalHours?.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        setLoading(false)
        return
      }
    } catch (error) {
      console.error('Error fetching shutdown and operational data:', error)
      setPowerData([])
      setSteamData([])
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  useEffect(() => {
    if (AOP_YEAR && PLANT_ID && hoursRows.length) {
      fetchData()
    }
  }, [AOP_YEAR, PLANT_ID, hoursRows, fetchData])

  return (
    <Box>
      <Stack sx={{ mb: 2 }}>
        <HoursGrid onHoursRowsChange={setHoursRows} />
      </Stack>
      <Stack sx={{ mb: 2 }}>
        <PowerGrid
          hoursRows={hoursRows}
          powerData={powerData}
          refreshData={fetchData}
          snackbarOpen={snackbarOpen}
          snackbarData={snackbarData}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          loading={loading}
          setLoading={setLoading}
        />
      </Stack>
      <Stack>
        <STGGrid
          hoursRows={hoursRows}
          steamData={steamData}
          refreshData={fetchData}
          snackbarOpen={snackbarOpen}
          snackbarData={snackbarData}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          loading={loading}
          setLoading={setLoading}
        />
      </Stack>
    </Box>
  )
}

export default ShutdownAndOperational
