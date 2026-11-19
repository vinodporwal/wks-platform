import React, { useMemo } from 'react'
import { Box } from '@mui/material'
import usePlantContext from '../../../hooks/usePlantContext'
import { resolveByPlantContext } from '../../../utilities/plantContextResolver'
import {
  CONSTANTS_VARIANTS,
  DEFAULT_CONSTANTS_VARIANT,
} from './constantsVariants'

const Constants = (props) => {
  const context = usePlantContext()

  const resolvedVariant = useMemo(
    () =>
      resolveByPlantContext(
        CONSTANTS_VARIANTS,
        context,
        DEFAULT_CONSTANTS_VARIANT,
      ),
    [context],
  )

  const VariantComponent =
    resolvedVariant?.Component || DEFAULT_CONSTANTS_VARIANT.Component
  const variantConfig =
    resolvedVariant?.config || DEFAULT_CONSTANTS_VARIANT.config
  const variantKey = resolvedVariant?.key || DEFAULT_CONSTANTS_VARIANT.key

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

export default Constants

