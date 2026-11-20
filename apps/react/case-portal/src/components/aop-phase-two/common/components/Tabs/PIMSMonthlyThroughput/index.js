import React, { useMemo } from 'react'
import { Box } from '@mui/material'
import usePlantContext from '../../../hooks/usePlantContext'
import { resolveByPlantContext } from '../../../utilities/plantContextResolver'
import {
  DEFAULT_PIMS_MONTHLY_THROUGHPUT_VARIANT,
  PIMS_MONTHLY_THROUGHPUT_VARIANTS,
} from './pimsMonthlyThroughputVariants'

const PIMSMonthlyThroughput = (props) => {
  const context = usePlantContext()

  const resolvedVariant = useMemo(
    () =>
      resolveByPlantContext(
        PIMS_MONTHLY_THROUGHPUT_VARIANTS,
        context,
        DEFAULT_PIMS_MONTHLY_THROUGHPUT_VARIANT,
      ),
    [context],
  )

  const VariantComponent =
    resolvedVariant?.Component ||
    DEFAULT_PIMS_MONTHLY_THROUGHPUT_VARIANT.Component
  const variantConfig =
    resolvedVariant?.config || DEFAULT_PIMS_MONTHLY_THROUGHPUT_VARIANT.config
  const variantKey =
    resolvedVariant?.key || DEFAULT_PIMS_MONTHLY_THROUGHPUT_VARIANT.key

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

export default PIMSMonthlyThroughput

