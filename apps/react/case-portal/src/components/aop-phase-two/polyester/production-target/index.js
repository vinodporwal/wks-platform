import React, { useState } from 'react'
import { Box, Stack } from '@mui/material'
import DesignCapacityGrid from './DesignCapacityGrid'
import MaxAchievedCapacityGrid from './MaxAchievedCapacityGrid'
import ProposedOperatingCapacityGrid from './ProposedOperatingCapacityGrid'
import PercentageSummaryGrid from './PercentageSummaryGrid'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const ProductionTarget = () => {
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <Stack spacing={4} mt={2}>
        {/* Grid 1: Design Capacity - Read-only with unit change */}
        <DesignCapacityGrid
          snackbarData={snackbarData}
          setSnackbarData={setSnackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          loading={loading}
          setLoading={setLoading}
        />

        {/* Grid 2: Max Achieved Capacity - Read-only */}
        <MaxAchievedCapacityGrid
          snackbarData={snackbarData}
          setSnackbarData={setSnackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          loading={loading}
          setLoading={setLoading}
        />

        {/* Grid 3: Proposed Operating Capacity - Editable with TPH/TPD, Remarks, and Excel import/export */}
        <ProposedOperatingCapacityGrid
          snackbarData={snackbarData}
          setSnackbarData={setSnackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          loading={loading}
          setLoading={setLoading}
        />

        {/* Grid 4: Percentage Summary - Calculated from Operating Capacity, read-only */}
        <PercentageSummaryGrid
          snackbarData={snackbarData}
          setSnackbarData={setSnackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          loading={loading}
          setLoading={setLoading}
        />
      </Stack>
    </Box>
  )
}

export default ProductionTarget
