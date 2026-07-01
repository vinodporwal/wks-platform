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
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'CDU1',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KTPD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 1,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'CDU2',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KTPD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 2,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DCOKER',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KTPD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 3,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'VGOHT-1',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KBD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 4,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'VGOHT-2',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KBD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 5,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'SCANFINNER',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 6,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'LNUU',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 7,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'CNHT',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KBD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 8,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DHT1',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KBD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 9,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DHT2',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KBD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 10,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'HNUU',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KTPD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 11,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'KNHT',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KBD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 12,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'LNHT',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KTPD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 13,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'FCCU',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KBD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 14,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'PLATFORMERUNIT',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KTPD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 15,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'PRU',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 16,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'NAPSPLIT',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KTPD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 17,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'CBA',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KTPD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 18,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'GASOLINEMEROX',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 19,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'KEROSINEMEROX',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 20,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'SATC3C4SPLITER',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 21,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'SATLPG',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 22,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'UNSATLPG',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 23,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DTA AGR SHIFTED',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 24,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DTA AGR UNSHIFTED',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 25,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DTA GASIFIER',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 26,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DTA PSA',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 27,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DTA SRU',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 28,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'CDUZ311',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KTPD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 29,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'CDUZ312',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KTPD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 30,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'COKERZ371',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KTPD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 31,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'DHDSZ355',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KBD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 32,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'DHDSZ358',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KBD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 33,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'HNUUZ221',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KTPD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 34,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SCANFINNER',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KBD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 35,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'VGOHTZ361',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KBD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 36,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'VGOHTZ362',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KBD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 37,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'FCCZ411',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KBD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 38,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'CCR Platforming',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KTPD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 39,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'ALKYLATION',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KTPD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 40,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'PRU',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 41,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'Naptha Splitter (NSPL)',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KTPD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 42,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SULPHURPRCC',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: 'KTPD',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 43,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'GASOLINEMEROX',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 44,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SAT LPG MEROX',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 45,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'UNSAT LPG MEROX',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 46,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ AGR SHIFTED',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 47,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ AGR UNSHIFTED',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 48,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ GASIFIER',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 49,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ PSA',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 50,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ SNG',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
  {
    id: 51,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ SRU',
    tentativeDurationDays: null,
    throughput: null,
    throughputUom: '',
    tentativeMonths: 'June',
    purposeOfSlowdown: '',
  },
]

const SlowdownSchedule = ({ permissions }) => {
  const [modifiedCells, setModifiedCells] = useState({})
  const [rows, setRows] = useState([])
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
  const generateMonthOptions = (
    startDate = new Date(2026, 3, 1),
    count = 12,
  ) => {
    // startDate defaults to Apr'26 (a typical AOP fiscal year start) - adjust as needed
    const months = []
    const d = new Date(startDate)
    for (let i = 0; i < count; i++) {
      const label = `${d.toLocaleString('en-US', { month: 'short' })}'${String(
        d.getFullYear(),
      ).slice(-2)}`
      months.push({ label, value: label })
      d.setMonth(d.getMonth() + 1)
    }
    return months
  }
  const monthOptions = [
    { value: 'January', label: 'January' },
    { value: 'February', label: 'February' },
    { value: 'March', label: 'March' },
    { value: 'April', label: 'April' },
    { value: 'May', label: 'May' },
    { value: 'June', label: 'June' },
    { value: 'July', label: 'July' },
    { value: 'August', label: 'August' },
    { value: 'September', label: 'September' },
    { value: 'October', label: 'October' },
    { value: 'November', label: 'November' },
    { value: 'December', label: 'December' },
  ]
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
      editable: true, // yellow - "Reference in backend By EPS Mumbai"
      minWidth: 130,
      hidden: true,
    },
    {
      field: 'sapProductId',
      title: 'SAP Product_ID',
      editable: true, // yellow - "Reference in backend By EPS Mumbai"
      minWidth: 150,
      hidden: true,
    },
    {
      field: 'site',
      title: 'Site',
      editable: false, // yellow - "Should be unique and fix"
      minWidth: 120,
    },
    {
      field: 'plant',
      title: 'Plant',
      editable: false, // yellow - "Should be unique and fix"
      minWidth: 120,
    },
    {
      field: 'tentativeDurationDays',
      title: 'Tentative Duration in days',
      editable: true, // yellow - "Note that provide days specific to days in that month."
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      minWidth: 220,
    },
    {
      field: 'throughput',
      title: 'Throughput during the Slowdown',
      editable: true, // orange - CTS Team, "Manual in Digital AOP"
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      minWidth: 280,
    },
    {
      field: 'throughputUom',
      title: 'Throughput UOM',
      editable: true, // orange - CTS Team
      minWidth: 170,
    },
    {
      field: 'tentativeMonths',
      title: 'Tentative Month',
      type: 'select',
      options: monthOptions,
      editable: true, // yellow - EPS Team
      minWidth: 160,
    },
    {
      field: 'purposeOfSlowdown',
      title: 'Purpose of Slowdown',
      editable: true, // yellow - EPS Team
      widthT: 250,
      minWidth: 220,
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
