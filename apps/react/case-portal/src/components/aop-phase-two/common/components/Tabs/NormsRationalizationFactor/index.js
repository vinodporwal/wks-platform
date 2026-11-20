import React, { useMemo } from 'react'
import { Box } from '@mui/material'
import usePlantContext from '../../../hooks/usePlantContext'
import { resolveByPlantContext } from '../../../utilities/plantContextResolver'
import {
  DEFAULT_NORMS_RATIONALIZATION_FACTOR_VARIANT,
  NORMS_RATIONALIZATION_FACTOR_VARIANTS,
} from './normsRationalizationFactorVariants'

const NormsRationalizationFactor = (props) => {
  const context = usePlantContext()

  const resolvedVariant = useMemo(
    () =>
      resolveByPlantContext(
        NORMS_RATIONALIZATION_FACTOR_VARIANTS,
        context,
        DEFAULT_NORMS_RATIONALIZATION_FACTOR_VARIANT,
      ),
    [context],
  )

  const VariantComponent =
    resolvedVariant?.Component ||
    DEFAULT_NORMS_RATIONALIZATION_FACTOR_VARIANT.Component
  const variantConfig =
    resolvedVariant?.config ||
    DEFAULT_NORMS_RATIONALIZATION_FACTOR_VARIANT.config
  const variantKey =
    resolvedVariant?.key || DEFAULT_NORMS_RATIONALIZATION_FACTOR_VARIANT.key

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

export default NormsRationalizationFactor

