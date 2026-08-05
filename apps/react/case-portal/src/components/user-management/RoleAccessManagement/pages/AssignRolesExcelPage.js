import React from 'react'
import { Box } from '@mui/material'
import AssignRolesExcelPanel from '../utilities/AssignRolesExcelPanel'

const AssignRolesExcelPage = ({
  onUploadExcel,
  loading,
  result,
  onResetResult,
}) => {
  return (
    <Box sx={{ width: '100%', flex: 1, display: 'flex', flexDirection: 'column' }}>
      <AssignRolesExcelPanel
        onUploadExcel={onUploadExcel}
        loading={loading}
        result={result}
        onResetResult={onResetResult}
      />
    </Box>
  )
}

export default AssignRolesExcelPage
