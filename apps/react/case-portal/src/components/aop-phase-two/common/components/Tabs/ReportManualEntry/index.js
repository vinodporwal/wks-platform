import React, { useMemo } from 'react'
import { Box } from '@mui/material'
import usePlantContext from '../../../hooks/usePlantContext'
import { resolveByPlantContext } from '../../../utilities/plantContextResolver'
import {
  DEFAULT_REPORT_MANUAL_ENTRY_VARIANT,
  REPORT_MANUAL_ENTRY_VARIANTS,
} from './reportManualEntryVariants'

const ReportManualEntry = (props) => {
  const context = usePlantContext()

  const resolvedVariant = useMemo(
    () =>
      resolveByPlantContext(
        REPORT_MANUAL_ENTRY_VARIANTS,
        context,
        DEFAULT_REPORT_MANUAL_ENTRY_VARIANT,
      ),
    [context],
  )

  const VariantComponent =
    resolvedVariant?.Component || DEFAULT_REPORT_MANUAL_ENTRY_VARIANT.Component
  const variantConfig =
    resolvedVariant?.config || DEFAULT_REPORT_MANUAL_ENTRY_VARIANT.config
  const variantKey =
    resolvedVariant?.key || DEFAULT_REPORT_MANUAL_ENTRY_VARIANT.key

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <VariantComponent
        key={`${variantKey}-${context.contextKey}`}
        {...props}
        plantContext={context}
        variantConfig={variantConfig}
      />
    </Box>
  )
}

export default ReportManualEntry

