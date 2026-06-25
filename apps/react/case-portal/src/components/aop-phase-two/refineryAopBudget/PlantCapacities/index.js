import React, { useEffect, useState, useCallback } from 'react'
import { useGridApiRef } from '@mui/x-data-grid'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { validateFields } from 'utils/validationUtils'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable'

/**
 * Dummy/static data matching the screenshot.
 * Replace this array (and the fetchData/saveChanges TODOs below) once the
 * real GET/SAVE APIs are available.
 */
const DUMMY_ROWS = [
  {
    id: 0,
    particulars: 'CDU1 - PIMS Code 1',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA',
    plant: 'CDU1',
    uom: 'KBPSD',
    min: 300,
    max: 345,
    remarks: '',
  },
  {
    id: 1,
    particulars: 'CDU1 - PIMS Code 2',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA',
    plant: 'CDU1',
    uom: 'KBPSD',
    min: 300,
    max: 345,
    remarks: '',
  },
  {
    id: 2,
    particulars: 'FCC - PIMS Code 1',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA',
    plant: 'FCC',
    uom: 'KBPSD',
    min: 180,
    max: 220,
    remarks: '',
  },
  {
    id: 3,
    particulars: 'FCC - PIMS Code 2',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA',
    plant: 'FCC',
    uom: 'KBPSD',
    min: 180,
    max: 210,
    remarks: '',
  },
  {
    id: 4,
    particulars: 'DHT1 - PIMS Code 1',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA',
    plant: 'DHT1',
    uom: 'KBPSD',
    min: 50,
    max: 100,
    remarks: '',
  },
  {
    id: 5,
    particulars: 'DHT1 - PIMS Code 2',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA',
    plant: 'DHT1',
    uom: 'KBPSD',
    min: 50,
    max: 80,
    remarks: '',
  },
]

const PlantCapacities = ({ permissions }) => {
  const [modifiedCells, setModifiedCells] = useState({})
  const [rows, setRows] = useState()
  const [loading, setLoading] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [open1, setOpen1] = useState(false)
  const [deleteId, setDeleteId] = useState(null)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const apiRef = useGridApiRef()

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const columns = [
    {
      field: 'particulars',
      title: 'Particulars',
      editable: false,
      widthT: 300,
      minWidth: 180,
    },
    {
      field: 'pimsCode',
      title: 'PIMS Code',
      editable: true,
      minWidth: 130,
    },
    {
      field: 'sapProductId',
      title: 'SAP Product_ID',
      editable: true,
      minWidth: 150,
    },
    {
      field: 'site',
      title: 'Site',
      editable: false, // "Should be unique and fix"
      minWidth: 100,
    },
    {
      field: 'plant',
      title: 'Plant',
      editable: false, // "Should be unique and fix"
      minWidth: 100,
    },
    {
      field: 'uom',
      title: 'UoM',
      editable: false, // "Should be unique and fix"
      minWidth: 100,
    },
    {
      field: 'min',
      title: 'Min',
      editable: true, // "Manual in Digital AOP"
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      minWidth: 100,
    },
    {
      field: 'max',
      title: 'Max',
      editable: false, // "From MCU"
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      minWidth: 100,
    },
    {
      field: 'remarks',
      title: 'Remarks', // "Manual in Digital AOP"
      editable: true,
      widthT: 250,
      minWidth: 120,
    },
  ]

  // TODO: replace with the real GET API call once it's available.
  const fetchData = useCallback(async () => {
    setModifiedCells({})
    setLoading(true)
    try {
      // Simulate a small network delay so the LoaderBackdrop is visible.
      await new Promise((resolve) => setTimeout(resolve, 300))
      setRows(DUMMY_ROWS)
    } finally {
      setLoading(false)
    }
  }, [])

  // TODO: replace with the real SAVE API call once it's available.
  const saveChanges = useCallback(async () => {
    try {
      setLoading(true)
      const data = Object.values(modifiedCells)

      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        return
      }

      const requiredFields = ['remarks']
      const validationMessage = validateFields(data, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationMessage,
          severity: 'error',
        })
        return
      }

      // No save API yet — just merge edits back into local state.
      setRows((prevRows) =>
        (prevRows || []).map((row) => {
          const edited = modifiedCells[row.id]
          return edited ? { ...row, ...edited } : row
        }),
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Saved Successfully! (local only - no API wired up yet)',
        severity: 'success',
      })
      setModifiedCells({})
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const adjustedPermissions = {
    customHeight: { mainBox: '32vh', otherBox: '100%' },
    textAlignment: 'center',
    allAction: true,
    addButton: true,
    deleteButton: true,
    showAction: true,
    remarksEditable: true,
    showCalculate: false,
    showExport: true,
    ExcelName: `Plant Capacities`,
    showImport: true,
    saveBtnForRemark: true,
    saveBtn: true,
    showWorkFlowBtns: false,
    showTitle: true,
    filterable: false,
  }

  return (
    <div>
      <LoaderBackdrop open={!!loading} />

      <AdvanceKendoTable
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title='Plant Capacities'
        setRows={setRows}
        columns={columns}
        rows={rows}
        fetchData={fetchData}
        saveChanges={saveChanges}
        paginationOptions={[100, 200, 300]}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        apiRef={apiRef}
        deleteId={deleteId}
        open1={open1}
        setDeleteId={setDeleteId}
        setOpen1={setOpen1}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        permissions={adjustedPermissions}
        disableRedHighlight={true}
        screenType='pims-product-master'
      />
    </div>
  )
}

export default PlantCapacities
