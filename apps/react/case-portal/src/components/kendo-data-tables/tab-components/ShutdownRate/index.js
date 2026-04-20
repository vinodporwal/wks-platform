import React, { useState } from 'react'
import { Box } from '@mui/material'
import Notification from 'components/Utilities/Notification'

import ShutdownRateGrid from './ShutdownRateGrid'

const ShutdownRate = ({ summary, summaryEdited, setSummaryEdited }) => {
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {/* Grid 1: Catalyst */}
      <ShutdownRateGrid
        summary={summary}
        summaryEdited={summaryEdited}
        setSummaryEdited={setSummaryEdited}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
      />

      <Notification
        open={snackbarOpen}
        message={snackbarData?.message || ''}
        severity={snackbarData?.severity || 'info'}
        onClose={() => setSnackbarOpen(false)}
      />
    </Box>
  )
}

export default ShutdownRate
