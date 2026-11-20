import React, { useMemo } from 'react'
import { Box } from '@mui/material'
import usePlantContext from '../../../hooks/usePlantContext'
import { resolveByPlantContext } from '../../../utilities/plantContextResolver'
import {
  DEFAULT_EXCLUSION_DATES_VARIANT,
  EXCLUSION_DATES_VARIANTS,
} from './exclusionDatesVariants'

const ExclusionDates = (props) => {
  const context = usePlantContext()

  const resolvedVariant = useMemo(
    () =>
      resolveByPlantContext(
        EXCLUSION_DATES_VARIANTS,
        context,
        DEFAULT_EXCLUSION_DATES_VARIANT,
      ),
    [context],
  )

  const VariantComponent =
    resolvedVariant?.Component || DEFAULT_EXCLUSION_DATES_VARIANT.Component
  const variantConfig =
    resolvedVariant?.config || DEFAULT_EXCLUSION_DATES_VARIANT.config
  const variantKey =
    resolvedVariant?.key || DEFAULT_EXCLUSION_DATES_VARIANT.key

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

export default ExclusionDates

