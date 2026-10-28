import React from 'react'
import { Box } from '@mui/material'
import ProductionRangeGrid from './ProductionRangeGrid'
import ProductionRangeGridLimit from './ProductionRangeGridLimit'

const ProductionRange = () => {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {/* Grid 1: Catalyst */}
      <ProductionRangeGrid />

      {/* Grid 2: Production Range LIMIT */}
      {/* <ProductionRangeGridLimit /> */}
    </Box>
  )
}

export default ProductionRange
