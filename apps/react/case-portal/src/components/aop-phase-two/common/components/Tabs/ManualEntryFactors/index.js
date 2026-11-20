import React, { useMemo } from 'react'
import { Box } from '@mui/material'
import usePlantContext from '../../../hooks/usePlantContext'
import { resolveByPlantContext } from '../../../utilities/plantContextResolver'
import {
  DEFAULT_MANUAL_ENTRY_FACTORS_VARIANT,
  MANUAL_ENTRY_FACTORS_VARIANTS,
} from './manualEntryFactorsVariants'

const ManualEntryFactors = (props) => {
  const context = usePlantContext()

  const resolvedVariant = useMemo(
    () =>
      resolveByPlantContext(
        MANUAL_ENTRY_FACTORS_VARIANTS,
        context,
        DEFAULT_MANUAL_ENTRY_FACTORS_VARIANT,
      ),
    [context],
  )

  const VariantComponent =
    resolvedVariant?.Component || DEFAULT_MANUAL_ENTRY_FACTORS_VARIANT.Component
  const variantConfig =
    resolvedVariant?.config || DEFAULT_MANUAL_ENTRY_FACTORS_VARIANT.config
  const variantKey =
    resolvedVariant?.key || DEFAULT_MANUAL_ENTRY_FACTORS_VARIANT.key

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

export default ManualEntryFactors

