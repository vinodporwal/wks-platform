import React, { useMemo } from 'react'
import { Box } from '@mui/material'
import usePlantContext from '../../../hooks/usePlantContext'
import { resolveByPlantContext } from '../../../utilities/plantContextResolver'
import {
  CONFIGURATION_VARIANTS,
  DEFAULT_CONFIGURATION_VARIANT,
} from './configurationVariants'

const Configuration = (props) => {
  const context = usePlantContext()

  const resolvedVariant = useMemo(
    () =>
      resolveByPlantContext(
        CONFIGURATION_VARIANTS,
        context,
        DEFAULT_CONFIGURATION_VARIANT,
      ),
    [context],
  )

  const VariantComponent =
    resolvedVariant?.Component || DEFAULT_CONFIGURATION_VARIANT.Component
  const variantConfig =
    resolvedVariant?.config || DEFAULT_CONFIGURATION_VARIANT.config
  const variantKey = resolvedVariant?.key || DEFAULT_CONFIGURATION_VARIANT.key

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

export default Configuration

