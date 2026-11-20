import React, { useMemo } from 'react'
import { Box } from '@mui/material'
import usePlantContext from '../../../hooks/usePlantContext'
import { resolveByPlantContext } from '../../../utilities/plantContextResolver'
import {
  DEFAULT_HISTORICAL_MONTHS_VARIANT,
  HISTORICAL_MONTHS_VARIANTS,
} from './historicalMonthsVariants'

const HistoricalMonths = (props) => {
  const context = usePlantContext()

  const resolvedVariant = useMemo(
    () =>
      resolveByPlantContext(
        HISTORICAL_MONTHS_VARIANTS,
        context,
        DEFAULT_HISTORICAL_MONTHS_VARIANT,
      ),
    [context],
  )

  const VariantComponent =
    resolvedVariant?.Component || DEFAULT_HISTORICAL_MONTHS_VARIANT.Component
  const variantConfig =
    resolvedVariant?.config || DEFAULT_HISTORICAL_MONTHS_VARIANT.config
  const variantKey =
    resolvedVariant?.key || DEFAULT_HISTORICAL_MONTHS_VARIANT.key

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

export default HistoricalMonths

