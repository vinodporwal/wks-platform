import { useState } from 'react'
import { Box } from '@mui/material'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'

// TODO: replace dummy data with API integration once endpoints are ready
const DUMMY_ROWS = [
  { id: 'eff_1', assetName: 'CCPP STG-1', uom: '%', value: 90, remarks: '' },
  { id: 'eff_2', assetName: 'CCPP STG-2', uom: '%', value: 90, remarks: '' },
  { id: 'eff_3', assetName: 'CCPP STG-3', uom: '%', value: 90, remarks: '' },
  { id: 'eff_4', assetName: 'CPP STG-1', uom: '%', value: 90, remarks: '' },
  { id: 'eff_5', assetName: 'CPP STG-2', uom: '%', value: 90, remarks: '' },
]

const Efficiency = () => {
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
      field: 'assetName',
      title: 'Asset Name',
      type: 'text',
      editable: false,
      minWidth: 200,
    },
    {
      field: 'uom',
      title: 'UOM',
      type: 'text',
      editable: false,
      minWidth: 120,
    },
    {
      field: 'value',
      title: 'Value',
      type: 'number1',
      editable: true,
      minWidth: 120,
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
    titleName: 'Efficiency',
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
        title='Efficiency'
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

export default Efficiency
