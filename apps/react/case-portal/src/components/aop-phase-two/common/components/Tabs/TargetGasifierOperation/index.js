import React, { useMemo } from 'react'
import { Box } from '@mui/material'
import usePlantContext from '../../../hooks/usePlantContext'
import { resolveByPlantContext } from '../../../utilities/plantContextResolver'
import {
  DEFAULT_TARGET_GASIFIER_OPERATION_VARIANT,
  TARGET_GASIFIER_OPERATION_VARIANTS,
} from './targetGasifierOperationVariants'

const TargetGasifierOperation = (props) => {
  const context = usePlantContext()

  const resolvedVariant = useMemo(
    () =>
      resolveByPlantContext(
        TARGET_GASIFIER_OPERATION_VARIANTS,
        context,
        DEFAULT_TARGET_GASIFIER_OPERATION_VARIANT,
      ),
    [context],
  )

  const VariantComponent =
    resolvedVariant?.Component ||
    DEFAULT_TARGET_GASIFIER_OPERATION_VARIANT.Component
  const variantConfig =
    resolvedVariant?.config || DEFAULT_TARGET_GASIFIER_OPERATION_VARIANT.config
  const variantKey =
    resolvedVariant?.key || DEFAULT_TARGET_GASIFIER_OPERATION_VARIANT.key

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

export default TargetGasifierOperation

