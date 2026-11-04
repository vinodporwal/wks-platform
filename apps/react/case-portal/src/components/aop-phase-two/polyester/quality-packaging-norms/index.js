import React, { useState } from 'react'
import { Box } from '@mui/material'
import AopTabs from 'components/AopTabs'
import Quality from './quality'
import PackagingConsumables from './packaging-consumables'
import StoresAndSpares from './storesAndSpares/index'
import PPADC from './ppaDC/index'

const QualityPackagingNorms = () => {
  const [tabIndex, setTabIndex] = useState(0)
  const defaultTabs = ['Quality', 'Packaging & Consumables', 'Stores & Spares', 'PPA DC'] //, 'Other Cost'

  return (
    <Box>
      <AopTabs
        tabIndex={tabIndex}
        setTabIndex={setTabIndex}
        tabs={defaultTabs}
      />
      <Box sx={{ mt: 2 }}>
        {tabIndex === 0 && <Quality />}
        {tabIndex === 1 && <PackagingConsumables />}
        {tabIndex === 2 && <StoresAndSpares />}
        {tabIndex === 3 && <PPADC />}
      </Box>
    </Box>
  )
}

export default QualityPackagingNorms
