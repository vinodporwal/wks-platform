import React, { useMemo } from 'react'
import { Box } from '@mui/material'
import usePlantContext from '../../../hooks/usePlantContext'
import { resolveByPlantContext } from '../../../utilities/plantContextResolver'
import {
  DEFAULT_PRODUCTION_RANGE_VARIANT,
  PRODUCTION_RANGE_VARIANTS,
} from './productionRangeVariants'

const ProductionRange = (props) => {
  const context = usePlantContext()

  const resolvedVariant = useMemo(
    () =>
      resolveByPlantContext(
        PRODUCTION_RANGE_VARIANTS,
        context,
        DEFAULT_PRODUCTION_RANGE_VARIANT,
      ),
    [context],
  )

  const VariantComponent =
    resolvedVariant?.Component || DEFAULT_PRODUCTION_RANGE_VARIANT.Component
  const variantConfig =
    resolvedVariant?.config || DEFAULT_PRODUCTION_RANGE_VARIANT.config
  const variantKey =
    resolvedVariant?.key || DEFAULT_PRODUCTION_RANGE_VARIANT.key

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

export default ProductionRange

