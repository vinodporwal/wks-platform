import React from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import UtilityRateGrid from './UtilityRateGrid'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const UtilityRate = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year } = dataGridStore

  const lowerVertName = verticalObject?.name?.toLowerCase()
  const lowerSiteName = siteObject?.name?.toLowerCase()
  const IS_CPP = lowerVertName === 'cpp'

  const renderBySite = () => {
    switch (lowerSiteName) {
      case 'nmd':
      default:
        return <UtilityRateGrid />
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

export default UtilityRate
