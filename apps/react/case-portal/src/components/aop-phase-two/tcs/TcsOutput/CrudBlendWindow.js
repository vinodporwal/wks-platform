import { Box, Backdrop, CircularProgress } from '@mui/material'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { TcsOutputApiService } from 'components/aop-phase-two/services/tcs/tcsOutputApiService'
import { useSession } from 'SessionStoreContext'
import CrudBlendWindowGrid from './CrudBlendWindowComponents/CrudBlendWindowGrid'
import { extractYear } from 'components/aop-phase-two/common/utilities/generateHeaders'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const CrudBlendWindow = ({
  PLANT_ID,
  AOP_YEAR,
  currentTab,
  SITE_ID,
  VERTICAL_ID,
  snackbarData,
  setSnackbarData,
  snackbarOpen,
  setSnackbarOpen,
}) => {
  const keycloak = useSession()
  const [loading, setLoading] = useState(false)
  const [allTablesData, setAllTablesData] = useState({})

  const gridConfigs = [
    { key: 'CrudeBlendWindow', title: 'Crude Blend Window' },
    { key: 'VGOVRDrop', title: 'VGO-VR Drop' },
    { key: 'CrudeSpecificConstraints', title: 'Crude Specific Constraints' },
  ]

  const apiYear = useMemo(() => extractYear(AOP_YEAR), [AOP_YEAR])

  // Fetch all tables data once
  const fetchAllTablesData = useCallback(async () => {
    if (!AOP_YEAR || !SITE_ID) {
      console.warn('Missing required params:', { AOP_YEAR, SITE_ID })
      return
    }
    try {
      setLoading(true)
      console.log('Fetching Crude Blend Window data with:', {
        AOP_YEAR,
        SITE_ID,
      })

      const response = await TcsOutputApiService.getCrudBlendWindowData(
        keycloak,
        VERTICAL_ID,
        apiYear,
        SITE_ID,
      )

      console.log('Crude Blend Window API Response:', response)

      // Transform response into a map keyed by table name
      const tablesDataMap = {}
      if (Array.isArray(response)) {
        response.forEach((item) => {
          if (item.table && item.data) {
            tablesDataMap[item.table] = item.data
          }
        })
      }

      console.log('Transformed tables data:', tablesDataMap)
      console.log('tablesDataMap', tablesDataMap)
      setAllTablesData(tablesDataMap)
    } catch (err) {
      console.error('Error fetching Crude Blend Window data:', err)
      setSnackbarData({
        message: 'Failed to load Crude Blend Window data. Please try again.',
        severity: 'error',
      })
      setSnackbarOpen(true)
      setAllTablesData({})
    } finally {
      setLoading(false)
    }
  }, [keycloak, AOP_YEAR, SITE_ID, setSnackbarData, setSnackbarOpen])

  // Fetch data on mount and when dependencies change
  useEffect(() => {
    console.log('useEffect triggered with:', { AOP_YEAR, SITE_ID })
    if (AOP_YEAR && SITE_ID) {
      fetchAllTablesData()
    }
  }, [AOP_YEAR, SITE_ID, fetchAllTablesData])

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      {gridConfigs.map((config) => (
        <CrudBlendWindowGrid
          key={config.key}
          tableKey={config.key}
          title={config.title}
          AOP_YEAR={AOP_YEAR}
          SITE_ID={SITE_ID}
          VERTICAL_ID={VERTICAL_ID}
          tableData={allTablesData[config.key]}
          snackbarData={snackbarData}
          setSnackbarData={setSnackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          onRefresh={fetchAllTablesData}
        />
      ))}
    </Box>
  )
}

export default CrudBlendWindow
