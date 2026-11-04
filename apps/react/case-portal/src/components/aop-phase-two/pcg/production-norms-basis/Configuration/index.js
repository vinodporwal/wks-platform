import React from 'react'
import { Box } from '@mui/material'
import ConfigurationRange from './ConfigurationRange'
import ConfigurationLimit from './ConfigurationLimit'
import { useSelector } from 'react-redux'
import ConfigurationRangePsa from './ConfigurationRangePsa'

const Configuration = (props) => {
  // const { plantObject, verticalObject, siteObject } = useSelector((state) => state.dataGridStore)
  const { plantObject, year, verticalObject, siteObject } = useSelector((state) => state.dataGridStore)
  const vertical = verticalObject?.name?.toLowerCase()?.trim()
  const site = siteObject?.name?.toLowerCase()?.trim()
  const plant = plantObject?.name?.toLowerCase()?.trim()
  const IS_PCG_DTA_PSA = vertical === 'pcg' && site === 'dta' && plant === 'psa'

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {IS_PCG_DTA_PSA ? (
        <ConfigurationRangePsa/>
      ) : (
        <ConfigurationRange/>
      )}
      {/* <ConfigurationLimit /> */}
    </Box>
  )
}

export default Configuration