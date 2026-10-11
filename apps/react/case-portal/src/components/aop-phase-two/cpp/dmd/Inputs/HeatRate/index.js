import React, { useState, useEffect } from 'react'
import { useSelector } from 'react-redux'
import useConfigurationDates from 'components/aop-phase-two/common/hooks/useConfigurationDates'
import Notification from 'components/aop-phase-two/common/utilities/Notification'
import GTHeatRate from './GTHeatRate'
import STGHeatRate from './STGHeatRate'
import HRSGHeatRate from './HRSGHeatRate'
import { Box, Stack } from '@mui/material'
import AUXBOILERHeatRate from './AUXBOILERHeatRate'
import CCPPHeatRate from './CCPPHeatRate'

const GRID_COMPONENTS = {
  GT: GTHeatRate,
  STG: STGHeatRate,
  HRSG: HRSGHeatRate,
  AUXBOILER: AUXBOILERHeatRate,
  CCPP: CCPPHeatRate,
}

const ALL_GRIDS = ['GT', 'STG', 'HRSG', 'AUXBOILER', 'CCPP']

// Site-wise grids to be shown; unlisted sites show all grids
const SITE_WISE_GRIDS = {
  vmd: ['GT', 'HRSG', 'AUXBOILER'],
  dmd: ALL_GRIDS,
  hmd: ALL_GRIDS,
  pmd: ['GT', 'STG', 'HRSG'],
}

const index = () => {
  const { startDate, endDate, loading, error } = useConfigurationDates()
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { siteObject } = dataGridStore
  const lowerSiteName = siteObject?.name?.toLowerCase()

  const gridsToShow = SITE_WISE_GRIDS[lowerSiteName] || ALL_GRIDS

  // Show error notification if configuration is not set up
  useEffect(() => {
    if (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: error,
        severity: 'warning',
      })
    }
  }, [error])

  return (
    <Box>
      {gridsToShow.map((gridKey) => {
        const GridComponent = GRID_COMPONENTS[gridKey]
        return (
          <Stack key={gridKey} sx={{ mb: 2 }}>
            <GridComponent
              startDate={startDate}
              endDate={endDate}
              dateLoading={loading}
            />
          </Stack>
        )
      })}

      {/* Notification */}
      <Notification
        open={snackbarOpen}
        onClose={() => setSnackbarOpen(false)}
        message={snackbarData.message}
        severity={snackbarData.severity}
      />
    </Box>
  )
}

export default index
