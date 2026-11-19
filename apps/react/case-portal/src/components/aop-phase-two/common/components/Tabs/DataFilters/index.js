import React, { useMemo } from 'react'
import { Box } from '@mui/material'
import usePlantContext from '../../../hooks/usePlantContext'
import { resolveByPlantContext } from '../../../utilities/plantContextResolver'
import {
  DATA_FILTERS_VARIANTS,
  DEFAULT_DATA_FILTERS_VARIANT,
} from './dataFiltersVariants'

const DataFilters = (props) => {
  const context = usePlantContext()

  const resolvedVariant = useMemo(
    () =>
      resolveByPlantContext(
        DATA_FILTERS_VARIANTS,
        context,
        DEFAULT_DATA_FILTERS_VARIANT,
      ),
    [context],
  )

  const VariantComponent =
    resolvedVariant?.Component || DEFAULT_DATA_FILTERS_VARIANT.Component
  const variantConfig =
    resolvedVariant?.config || DEFAULT_DATA_FILTERS_VARIANT.config
  const variantKey = resolvedVariant?.key || DEFAULT_DATA_FILTERS_VARIANT.key

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

export default DataFilters

