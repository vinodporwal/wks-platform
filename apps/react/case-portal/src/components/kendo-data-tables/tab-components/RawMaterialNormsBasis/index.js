// ⚠️ TEMP_TABS_REMOVE_AFTER_BACKEND_READY ⚠️ - This entire file is temporary
import React from 'react'
import { Box } from '@mui/material'
import RawMaterialGrid from './RawMaterialGrid'
import IBINlossesGrid from './IBINlossesGrid'
import AsPerStoichiometryLossesGrid from './AsPerStoichiometryLossesGrid'

const RawMaterialNormsBasis = ({
  summary,
  summaryEdited,
  setSummaryEdited,
}) => {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {/* Grid 1: Raw Material */}
      <RawMaterialGrid
        summary={summary}
        summaryEdited={summaryEdited}
        setSummaryEdited={setSummaryEdited}
      />

      {/* Grid 2: IBIN Losses */}
      <IBINlossesGrid
        summary={summary}
        summaryEdited={summaryEdited}
        setSummaryEdited={setSummaryEdited}
      />

      {/* Grid 3: As Per Stoichiometry Losses */}
      <AsPerStoichiometryLossesGrid
        summary={summary}
        summaryEdited={summaryEdited}
        setSummaryEdited={setSummaryEdited}
      />
    </Box>
  )
}

export default RawMaterialNormsBasis
