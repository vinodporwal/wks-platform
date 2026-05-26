import React, { useState, useEffect } from 'react'
import useConfigurationDates from 'components/aop-phase-two/common/hooks/useConfigurationDates'
import Notification from 'components/aop-phase-two/common/utilities/Notification'
import GTHeatRate from './GTHeatRate'
import STGHeatRate from './STGHeatRate'
import HRSGHeatRate from './HRSGHeatRate'
import { Box, Stack } from '@mui/material'

const index = () => {
  const { startDate, endDate, loading, error } = useConfigurationDates()
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

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
      <Stack sx={{ mb: 2 }}>
        <GTHeatRate
          startDate={startDate}
          endDate={endDate}
          dateLoading={loading}
        />
      </Stack>
      <Stack sx={{ mb: 2 }}>
        <STGHeatRate
          startDate={startDate}
          endDate={endDate}
          dateLoading={loading}
        />
      </Stack>
      <Stack>
        <HRSGHeatRate
          startDate={startDate}
          endDate={endDate}
          dateLoading={loading}
        />
      </Stack>

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
