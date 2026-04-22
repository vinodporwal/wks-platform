// ⚠️ TEMP_TABS_REMOVE_AFTER_BACKEND_READY ⚠️ - This entire file is temporary
import React, { useState } from 'react'
import { Box } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import ProductionRangeGrid from './ProductionRangeGrid'
import ProductionRangeGridLimit from './ProductionRangeGridLimit'

const ProductionRange = ({ summary, summaryEdited, setSummaryEdited }) => {
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {/* Grid 1: Catalyst */}
      <ProductionRangeGrid
        summary={summary}
        summaryEdited={summaryEdited}
        setSummaryEdited={setSummaryEdited}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
      />

      {/* Grid 2: Production Range LIMIT */}
      <ProductionRangeGridLimit
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

export default ProductionRange
