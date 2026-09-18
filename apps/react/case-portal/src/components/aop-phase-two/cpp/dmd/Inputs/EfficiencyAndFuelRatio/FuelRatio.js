import { useState } from 'react'
import { Box } from '@mui/material'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'

// TODO: replace dummy data with API integration once endpoints are ready
const DUMMY_ROWS = [
  { id: 'fuel_1', fuel: 'Coal', gcv: 4000, percentageByWt: 82, remarks: '' },
  {
    id: 'fuel_2',
    fuel: 'Bio Mass',
    gcv: 3400,
    percentageByWt: 18,
    remarks: '',
  },
]

const FuelRatio = () => {
  const [modifiedCells, setModifiedCells] = useState({})
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [rows, setRows] = useState(DUMMY_ROWS)

  const columns = [
    {
      field: 'fuel',
      title: 'Fuel',
      type: 'text',
      editable: false,
      minWidth: 200,
    },
    {
      field: 'gcv',
      title: 'GCV (KCal/KG)',
      type: 'number1',
      editable: true,
      minWidth: 150,
    },
    {
      field: 'percentageByWt',
      title: 'Percentage By Wt',
      type: 'number1',
      editable: true,
      minWidth: 160,
    },
    {
      field: 'remarks',
      title: 'Remark',
      widthT: 250,
      type: 'textarea',
      editable: true,
      minWidth: 250,
    },
  ]

  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: false,
    saveBtn: true,
    allAction: true,
    showTitle: true,
    titleName: 'Fuel Ratio',
  }

  const saveChanges = () => {
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      return
    }

    setModifiedCells({})
    setSnackbarOpen(true)
    setSnackbarData({
      message: `Successfully saved ${modifiedData.length} changes!`,
      severity: 'success',
    })
  }

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  return (
    <Box>
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title='Fuel Ratio'
        permissions={permissions}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={() => {}}
        saveChanges={saveChanges}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
      />
    </Box>
  )
}

export default FuelRatio
