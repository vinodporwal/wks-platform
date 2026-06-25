import React, { useEffect, useState, useCallback } from 'react'
import { useGridApiRef } from '@mui/x-data-grid'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { validateFields } from 'utils/validationUtils'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable'

/**
 * Source/Entry Owner:
 *   1. Yellow cells -> entered by EPS Team   (pimsCode, sapProductId, site,
 *      plant, tentativeDurationDays, tentativeMonth, purposeOfSlowdown)
 *   2. Orange cells -> entered by CTS Team   (throughput, throughputUom)
 *
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
    tentativeDurationDays: 11,
    throughput: 170,
    throughputUom: 'kbpsd',
    tentativeMonth: "Apr'26",
    purposeOfSlowdown: '',
  },
  {
    id: 1,
    particulars: 'CDU1 - PIMS Code 1',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA',
    plant: 'CDU1',
    tentativeDurationDays: 8,
    throughput: 32,
    throughputUom: 'kbpsd',
    tentativeMonth: "Jun'26",
    purposeOfSlowdown: '',
  },
]

const SlowdownSchedule = ({ permissions }) => {
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
      editable: true, // yellow - "Reference in backend By EPS Mumbai"
      minWidth: 130,
    },
    {
      field: 'sapProductId',
      title: 'SAP Product_ID',
      editable: true, // yellow - "Reference in backend By EPS Mumbai"
      minWidth: 150,
    },
    {
      field: 'site',
      title: 'Site',
      editable: false, // yellow - "Should be unique and fix"
      minWidth: 100,
    },
    {
      field: 'plant',
      title: 'Plant',
      editable: false, // yellow - "Should be unique and fix"
      minWidth: 100,
    },
    {
      field: 'tentativeDurationDays',
      title: 'Tentative Duration in days',
      editable: true, // yellow - "Note that provide days specific to days in that month."
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      minWidth: 170,
    },
    {
      field: 'throughput',
      title: 'Throughput during the Slowdown',
      editable: true, // orange - CTS Team, "Manual in Digital AOP"
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      minWidth: 180,
    },
    {
      field: 'throughputUom',
      title: 'Throughput UOM',
      editable: true, // orange - CTS Team
      minWidth: 130,
    },
    {
      field: 'tentativeMonth',
      title: 'Tentative Month',
      editable: true, // yellow - EPS Team
      minWidth: 140,
    },
    {
      field: 'purposeOfSlowdown',
      title: 'Purpose of Slowdown',
      editable: true, // yellow - EPS Team
      widthT: 250,
      minWidth: 170,
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

      const requiredFields = ['tentativeMonth']
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
    ExcelName: `slowdown`,
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
        title='Slowdown'
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
        screenType='slowdown-schedule'
      />
    </div>
  )
}

export default SlowdownSchedule
