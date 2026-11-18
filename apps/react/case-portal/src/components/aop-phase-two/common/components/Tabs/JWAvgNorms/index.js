import React, { useMemo } from 'react'
import { Box } from '@mui/material'
import usePlantContext from '../../../hooks/usePlantContext'
import { resolveByPlantContext } from '../../../utilities/plantContextResolver'
import {
  DEFAULT_JW_AVG_NORMS_VARIANT,
  JW_AVG_NORMS_VARIANTS,
} from './jwAvgNormsVariants'

const JWAvgNorms = (props) => {
  const context = usePlantContext()

  const resolvedVariant = useMemo(
    () =>
      resolveByPlantContext(
        JW_AVG_NORMS_VARIANTS,
        context,
        DEFAULT_JW_AVG_NORMS_VARIANT,
      ),
    [context],
  )

  const VariantComponent =
    resolvedVariant?.Component || DEFAULT_JW_AVG_NORMS_VARIANT.Component
  const variantConfig =
    resolvedVariant?.config || DEFAULT_JW_AVG_NORMS_VARIANT.config
  const variantKey = resolvedVariant?.key || DEFAULT_JW_AVG_NORMS_VARIANT.key

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

export default JWAvgNorms