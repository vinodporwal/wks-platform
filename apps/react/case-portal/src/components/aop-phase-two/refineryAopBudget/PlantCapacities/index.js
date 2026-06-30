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
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'CDU1',
    uom: 'KTPD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 1,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'CDU2',
    uom: 'KTPD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 2,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DCOKER',
    uom: 'KTPD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 3,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'VGOHT-1',
    uom: 'KBD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 4,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'VGOHT-2',
    uom: 'KBD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 5,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'SCANFINNER',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 6,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'LNUU',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 7,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'CNHT',
    uom: 'KBD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 8,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DHT1',
    uom: 'KBD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 9,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DHT2',
    uom: 'KBD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 10,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'HNUU',
    uom: 'KTPD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 11,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'KNHT',
    uom: 'KBD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 12,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'LNHT',
    uom: 'KTPD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 13,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'FCCU',
    uom: 'KBD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 14,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'PLATFORMERUNIT',
    uom: 'KTPD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 15,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'PRU',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 16,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'NAPSPLIT',
    uom: 'KTPD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 17,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'CBA',
    uom: 'KTPD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 18,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'GASOLINEMEROX',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 19,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'KEROSINEMEROX',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 20,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'SATC3C4SPLITER',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 21,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'SATLPG',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 22,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'UNSATLPG',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 23,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DTA AGR SHIFTED',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 24,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DTA AGR UNSHIFTED',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 25,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DTA GASIFIER',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 26,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DTA PSA',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 27,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'DTA-Refinery',
    plant: 'DTA SRU',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 28,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'CDUZ311',
    uom: 'KTPD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 29,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'CDUZ312',
    uom: 'KTPD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 30,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'COKERZ371',
    uom: 'KTPD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 31,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'DHDSZ355',
    uom: 'KBD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 32,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'DHDSZ358',
    uom: 'KBD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 33,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'HNUUZ221',
    uom: 'KTPD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 34,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SCANFINNER',
    uom: 'KBD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 35,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'VGOHTZ361',
    uom: 'KBD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 36,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'VGOHTZ362',
    uom: 'KBD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 37,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'FCCZ411',
    uom: 'KBD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 38,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'CCR Platforming',
    uom: 'KTPD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 39,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'ALKYLATION',
    uom: 'KTPD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 40,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'PRU',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 41,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'Naptha Splitter (NSPL)',
    uom: 'KTPD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 42,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SULPHURPRCC',
    uom: 'KTPD',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 43,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'GASOLINEMEROX',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 44,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SAT LPG MEROX',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 45,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'UNSAT LPG MEROX',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 46,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ AGR SHIFTED',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 47,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ AGR UNSHIFTED',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 48,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ GASIFIER',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 49,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ PSA',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 50,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ SNG',
    uom: '',
    min: null,
    max: null,
    remarks: '',
  },
  {
    id: 51,
    particulars: '',
    pimsCode: '',
    sapProductId: '',
    site: 'SEZ-Refinery',
    plant: 'SEZ SRU',
    uom: '',
    min: null,
    max: null,
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
      hidden: true,
    },
    {
      field: 'pimsCode',
      title: 'PIMS Code',
      editable: true,
      minWidth: 130,
      hidden: true,
    },
    {
      field: 'sapProductId',
      title: 'SAP Product_ID',
      editable: true,
      minWidth: 150,
      hidden: true,
    },
    {
      field: 'site',
      title: 'Site',
      editable: false, // "Should be unique and fix"
      minWidth: 120,
    },
    {
      field: 'plant',
      title: 'Plant',
      editable: false, // "Should be unique and fix"
      minWidth: 120,
    },
    {
      field: 'uom',
      title: 'UOM',
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
      editable: true, // "From MCU"
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
