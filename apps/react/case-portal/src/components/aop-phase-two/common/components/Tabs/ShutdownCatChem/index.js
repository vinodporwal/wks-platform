import React, { useMemo } from 'react'
import { Box } from '@mui/material'
import usePlantContext from '../../../hooks/usePlantContext'
import { resolveByPlantContext } from '../../../utilities/plantContextResolver'
import {
  DEFAULT_SHUTDOWN_CAT_CHEM_VARIANT,
  SHUTDOWN_CAT_CHEM_VARIANTS,
} from './shutdownCatChemVariants'

const ShutdownCatChem = (props) => {
  const context = usePlantContext()

  const resolvedVariant = useMemo(
    () =>
      resolveByPlantContext(
        SHUTDOWN_CAT_CHEM_VARIANTS,
        context,
        DEFAULT_SHUTDOWN_CAT_CHEM_VARIANT,
      ),
    [context],
  )

  const VariantComponent =
    resolvedVariant?.Component || DEFAULT_SHUTDOWN_CAT_CHEM_VARIANT.Component
  const variantConfig =
    resolvedVariant?.config || DEFAULT_SHUTDOWN_CAT_CHEM_VARIANT.config
  const variantKey =
    resolvedVariant?.key || DEFAULT_SHUTDOWN_CAT_CHEM_VARIANT.key

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

export default ShutdownCatChem

