import React, { useState } from 'react'
import ImportPowerGrid from './ImportPowerGrid'
import { Box, Stack } from '@mui/material/index'
import ProcessUnitGrid from './ProcessUnitGrid'

const ImportPower = () => {
  const [importData, setImportData] = useState([])
  return (
    <Box>
      <Stack sx={{ mb: 4 }}>
        <ImportPowerGrid setImportData={setImportData} />
      </Stack>
      <Stack sx={{ mb: 2 }}>
        <ProcessUnitGrid importData={importData} />
      </Stack>
    </Box>
  )
}

export default ImportPower
