import React, { useMemo } from 'react'
import { Box } from '@mui/material'
import usePlantContext from '../../../hooks/usePlantContext'
import { resolveByPlantContext } from '../../../utilities/plantContextResolver'
import {
  DEFAULT_ENERGY_FACTOR_VARIANT,
  ENERGY_FACTOR_VARIANTS,
} from './energyFactorVariants'

const EnergyFactor = (props) => {
  const context = usePlantContext()

  const resolvedVariant = useMemo(
    () =>
      resolveByPlantContext(
        ENERGY_FACTOR_VARIANTS,
        context,
        DEFAULT_ENERGY_FACTOR_VARIANT,
      ),
    [context],
  )

  const VariantComponent =
    resolvedVariant?.Component || DEFAULT_ENERGY_FACTOR_VARIANT.Component
  const variantConfig =
    resolvedVariant?.config || DEFAULT_ENERGY_FACTOR_VARIANT.config
  const variantKey = resolvedVariant?.key || DEFAULT_ENERGY_FACTOR_VARIANT.key

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

export default EnergyFactor

