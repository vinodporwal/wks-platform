import React from 'react'
import { Box, Stack } from '@mui/material'
import { useSelector } from 'react-redux'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import NormsQtyCostReportMonthly from './NormsQtyCostReportMonthly'
import NormsQtyCostReportAnnual from './NormsQtyCostReportAnnual'

const QtyCostReport = () => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { siteObject, verticalObject } = dataGridStore

  const lowerVertName = verticalObject?.name?.toLowerCase()
  const lowerSiteName = siteObject?.name?.toLowerCase()
  const IS_CPP = lowerVertName === 'cpp'

  const renderBySite = () => {
    switch (lowerSiteName) {
      case 'nmd':
      default:
        return (
          <Box>
            <Stack>
              <NormsQtyCostReportMonthly />
            </Stack>
            <Stack>
              <NormsQtyCostReportAnnual />
            </Stack>
          </Box>
        )
    }
  }

  if (!IS_CPP) return null

  return (
    <Box>
      <LoaderBackdrop open={false} />
      {renderBySite()}
    </Box>
  )
}

export default QtyCostReport
