import React, { useEffect, useState, useCallback } from 'react'
import { useGridApiRef } from '@mui/x-data-grid'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { validateFields } from 'utils/validationUtils'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable'

/**
 * Source: Manual | Entry Owner: All by EPS Team
 * Dummy/static data matching the screenshot.
 * Replace this array (and the fetchData/saveChanges TODOs below) once the
 * real GET/SAVE APIs are available.
 *
 * NOTE: screenshot also calls out "Excel Upload provision required" —
 * uploadExcelBtn is left enabled below for that reason.
 */
const DUMMY_ROWS = [
  {
    id: 0,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'CDU1',
    uom: 'KTPD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 1,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'CDU2',
    uom: 'KTPD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 2,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DCOKER',
    uom: 'KTPD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 3,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'VGOHT-1',
    uom: 'KBD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 4,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'VGOHT-2',
    uom: 'KBD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 5,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'SCANFINNER',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 6,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'LNUU',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 7,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'CNHT',
    uom: 'KBD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 8,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DHT1',
    uom: 'KBD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 9,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DHT2',
    uom: 'KBD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 10,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'HNUU',
    uom: 'KTPD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 11,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'KNHT',
    uom: 'KBD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 12,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'LNHT',
    uom: 'KTPD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 13,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'FCCU',
    uom: 'KBD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 14,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'PLATFORMERUNIT',
    uom: 'KTPD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 15,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'PRU',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 16,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'NAPSPLIT',
    uom: 'KTPD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 17,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'CBA',
    uom: 'KTPD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 18,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'GASOLINEMEROX',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 19,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'KEROSINEMEROX',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 20,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'SATC3C4SPLITER',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 21,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'SATLPG',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 22,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'UNSATLPG',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 23,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DTA AGR SHIFTED',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 24,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DTA AGR UNSHIFTED',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 25,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DTA GASIFIER',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 26,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DTA PSA',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 27,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DTA SRU',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 28,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'CDUZ311',
    uom: 'KTPD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 29,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'CDUZ312',
    uom: 'KTPD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 30,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'COKERZ371',
    uom: 'KTPD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 31,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'DHDSZ355',
    uom: 'KBD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 32,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'DHDSZ358',
    uom: 'KBD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 33,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'HNUUZ221',
    uom: 'KTPD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 34,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SCANFINNER',
    uom: 'KBD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 35,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'VGOHTZ361',
    uom: 'KBD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 36,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'VGOHTZ362',
    uom: 'KBD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 37,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'FCCZ411',
    uom: 'KBD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 38,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'CCR Platforming',
    uom: 'KTPD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 39,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'ALKYLATION',
    uom: 'KTPD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 40,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'PRU',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 41,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'Naptha Splitter (NSPL)',
    uom: 'KTPD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 42,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SULPHURPRCC',
    uom: 'KTPD',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 43,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'GASOLINEMEROX',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 44,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SAT LPG MEROX',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 45,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'UNSAT LPG MEROX',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 46,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ AGR SHIFTED',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 47,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ AGR UNSHIFTED',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 48,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ GASIFIER',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 49,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ PSA',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 50,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ SNG',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
  {
    id: 51,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ SRU',
    uom: '',
    sdTotalDurationDays: '',
    dateOfCommencement: '1-Apr-26',
    purposeOfShutdown: '',
  },
]

const ShutdownSchedule = ({ permissions }) => {
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
      hidden: true,
    },
    {
      field: 'pimsCode',
      title: 'PIMS Code',
      editable: true, // "Reference in backend By EPS Mumbai"
      minWidth: 130,
      hidden: true,
    },
    {
      field: 'sapProductId',
      title: 'SAP Product_ID',
      editable: true, // "Reference in backend By EPS Mumbai"
      minWidth: 150,
      hidden: true,
    },
    {
      field: 'site',
      title: 'Site',
      editable: false, // "Should be unique and fix"
      minWidth: 150,
    },
    {
      field: 'plant',
      title: 'Plant',
      editable: false, // "Should be unique and fix"
      minWidth: 150,
    },
    {
      field: 'sdTotalDurationDays',
      title: 'SD Total duration in days',
      editable: true, // "Note that provide days specific to days in that month."
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      minWidth: 220,
    },
    {
      field: 'dateOfCommencement',
      title: 'Date of Commencement',
      editable: true, // "Manual in Digital AOP"
      minWidth: 220,
    },
    {
      field: 'purposeOfShutdown',
      title: 'Purpose of Shutdown',
      editable: true,
      widthT: 250,
      minWidth: 200,
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

      const requiredFields = ['dateOfCommencement']
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
    ExcelName: `shutdown`,
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
        title='Shutdown'
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
        screenType='shutdown-schedule'
      />
    </div>
  )
}

export default ShutdownSchedule
