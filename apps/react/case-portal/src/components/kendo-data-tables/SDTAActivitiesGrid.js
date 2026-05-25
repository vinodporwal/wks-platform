import React from 'react'
import { Box } from '@mui/material'
import KendoDataTablesCracker from './index-cracker.js'
const CustomRow = ({ dataItem, className, ...rest }) => {
  const rowClassName = [
    className,
    dataItem.isError ? 'error-row' : '',
  ]
    .filter(Boolean)
    .join(' ')
  return (
    <tr {...rest?.trProps} className={rowClassName.trim()}>
      {rest.children}
    </tr>
  )
}
const SDTAActivitiesGrid = ({
  columns,
  rows,
  setRows,
  fetchData,
  handleRemarkCellClick,
  remarkDialogOpen,
  currentRemark,
  setCurrentRemark,
  currentRowId,
  snackbarData,
  snackbarOpen,
  setSnackbarOpen,
  setSnackbarData,
  modifiedCells,
  allMonths,
  setModifiedCells,
  permissions,
  saveChanges,
  setRemarkDialogOpen,
  handleCalculate,
  summaryEdited,
  setSummaryEdited,
  gridKey,
}) => {
  return (
    <Box sx={{ mt: 1 }}>
      <KendoDataTablesCracker
        columns={columns}
        key={gridKey}
        rows={rows}
        setRows={setRows}
        editable={true}
        editField='inEdit'
        fetchData={fetchData}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        modifiedCells={modifiedCells}
        allMonths={allMonths}
        setModifiedCells={setModifiedCells}
        permissions={permissions}
        saveChanges={saveChanges}
        setRemarkDialogOpen={setRemarkDialogOpen}
        titleName='IBR/SD/HSS Activities'
        rowRender={CustomRow}
        handleCalculate={handleCalculate}
        summaryEdited={summaryEdited}
        setSummaryEdited={setSummaryEdited}
      />
    </Box>
  )
}

export default SDTAActivitiesGrid
