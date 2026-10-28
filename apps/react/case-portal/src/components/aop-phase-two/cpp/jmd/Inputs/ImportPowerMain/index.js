import React, { useState } from 'react'
import ImportPowerGrid from './ImportPowerGrid'
import { Box, Stack } from '@mui/material/index'
import ProcessUnitGrid from './ProcessUnitGrid'
import InterSitePowerTransferGrid from './InterSitePowerTransferGrid'

const ImportPower = () => {
  const [importData, setImportData] = useState([])
  return (
    <Box>
      <Stack sx={{ mt: 2, mb: 4 }}>
        <ImportPowerGrid setImportData={setImportData} />
      </Stack>
      <Stack sx={{ mt: 2, mb: 4 }}>
        <ProcessUnitGrid importData={importData} />
      </Stack>
      <Stack sx={{ mt: 2, mb: 4 }}>
        <InterSitePowerTransferGrid />
      </Stack>
    </Box>
  )
}

export default ImportPower
