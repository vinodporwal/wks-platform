import React, { useState } from 'react'
import ImportPowerGrid from './ImportPowerGrid'
import { Box, Stack } from '@mui/material/index'
import ProcessUnitGrid from './ProcessUnitGrid'

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
    </Box>
  )
}

export default ImportPower
