import { useEffect, useState, useMemo, useCallback } from 'react'
import { Box } from '@mui/material'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { customValueFormatterPhaseTwo } from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { validateNestedRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import { InputApiService } from 'components/aop-phase-two/services/cpp/jmd/inputApiService'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import { downloadBase64Excel } from 'components/aop-phase-two/common/utilities/downloadBase64Excel'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { useDebounce } from 'hooks/useDebounce'

// Fiscal-year month order: Apr → Mar
const MONTH_TO_INDEX = {
  apr: 4,
  may: 5,
  jun: 6,
  jul: 7,
  aug: 8,
  sep: 9,
  oct: 10,
  nov: 11,
  dec: 12,
  jan: 1,
  feb: 2,
  mar: 3,
}

const MONTH_FIELDS = [
  'apr', 'may', 'jun', 'jul', 'aug', 'sep',
  'oct', 'nov', 'dec', 'jan', 'feb', 'mar',
]

const RpoMonthlyConfig = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { year } = dataGridStore
  const { plantObject } = dataGridStore

  const AOP_YEAR = year?.selectedYear
  // plantId is optional — send only when a plant is selected
  const PLANT_ID = plantObject?.id || null
  const EXCEL_NAME = generateExcelName(dataGridStore, 'RPOMonthlyConfig')

  const headerMap = useMemo(() => generateHeaderNames(AOP_YEAR), [AOP_YEAR])
  const customFormat = customValueFormatterPhaseTwo(2)

  // Month columns: Apr → Mar (editable)
  const MONTH_COLUMNS = useMemo(
    () =>
      MONTH_FIELDS.map((mon) => ({
        field: mon,
        title: headerMap[MONTH_TO_INDEX[mon]],
        widthT: 110,
        minWidth: 110,
        editable: true,
        type: 'number1',
        format: customFormat,
      })),
    [headerMap],
  )

  // ── Column definitions ──────────────────────────────────────────────────────
  const columns = useMemo(
    () => [
      {
        field: 'displayName',
        title: 'Parameter',
        widthT: 220,
        minWidth: 220,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'type',
        title: 'Type',
        widthT: 120,
        minWidth: 120,
        type: 'text',
        editable: false,
        locked: true,
      },
      // Editable monthly columns (Apr → Mar)
      ...MONTH_COLUMNS,
      {
        field: 'remarks',
        title: 'Remarks',
        widthT: 250,
        minWidth: 250,
        type: 'textarea',
        editable: true,
      },
    ],
    [MONTH_COLUMNS],
  )

  // ── State ───────────────────────────────────────────────────────────────────
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [customModifiedCells, setCustomModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({ message: '', severity: 'info' })

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // ── Fetch ───────────────────────────────────────────────────────────────────
  const fetchData = useCallback(async () => {
    if (!AOP_YEAR) return
    setLoading(true)
    try {
      const res = await InputApiService.getRPOMonthlyConfig(keycloak, AOP_YEAR, null)

      if (!res?.data || res.data.length === 0) {
        setRows([])
        setOriginalRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        return
      }

      const formatted = res.data.map((item, index) => ({
        ...item,
        id: item.id || `rpomc_${index}`,
        remarks: item.remarks || '',
      }))

      setRows(formatted)
      setOriginalRows(formatted)
    } catch (error) {
      console.error('[RpoMonthlyConfig] Error fetching data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, AOP_YEAR, PLANT_ID])

  useDebounce(
    () => {
      if (AOP_YEAR) {
        fetchData()
      }
    },
    1000,
    [AOP_YEAR, PLANT_ID],
  )

  // Clear modified cells when year or plant changes
  useEffect(() => {
    setModifiedCells({})
    setCustomModifiedCells({})
  }, [AOP_YEAR, PLANT_ID])

  // ── Item change handler ─────────────────────────────────────────────────────
  const handleItemChange = (e) => {
    const { dataItem, field, value } = e
    const itemId = dataItem.id
    const updates = { [field]: value, inEdit: true }

    setModifiedCells((prev) => ({
      ...prev,
      [itemId]: { ...dataItem, ...prev[itemId], ...updates },
    }))

    setRows((prev) =>
      prev.map((r) => (r.id === itemId ? { ...r, ...updates } : r)),
    )

    setCustomModifiedCells((prev) => ({
      ...prev,
      [itemId]: { ...(prev[itemId] || {}), ...updates },
    }))
  }

  // ── Save ────────────────────────────────────────────────────────────────────
  const saveChanges = async () => {
    setLoading(true)

    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
      setLoading(false)
      return
    }

    const data = modifiedData.filter((row) => row.inEdit)
    if (data.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
      setLoading(false)
      return
    }

    // Validate: remarks required for any month change
    const validationError = validateNestedRowDataWithRemarks(
      data,
      originalRows,
      MONTH_FIELDS,
      'displayName',
    )

    if (validationError) {
      setSnackbarOpen(true)
      setSnackbarData({ message: validationError, severity: 'error' })
      setLoading(false)
      return
    }

    const payload = data.map(({ inEdit, ...rest }) => ({ ...rest }))

    try {
      const response = await InputApiService.saveRPOMonthlyConfig(
        keycloak,
        AOP_YEAR,
        null,
        payload,
      )

      if (response?.code === 200 || response?.code === 207) {
        setModifiedCells({})
        setCustomModifiedCells({})
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || `Successfully saved ${data.length} change(s)!`,
          severity: response?.code === 207 ? 'warning' : 'success',
        })
        await fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Failed to save changes.',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('[RpoMonthlyConfig] Error saving data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to save changes. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  // ── Import (Excel) ──────────────────────────────────────────────────────────
  const handleExcelUpload = async (file) => {
    if (!file) return
    setLoading(true)
    try {
      const response = await InputApiService.saveRPOMonthlyConfigExcel(
        file,
        keycloak,
        AOP_YEAR,
        null,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Excel file imported successfully!',
          severity: 'success',
        })
        await fetchData()
      } else if (response?.code === 400 && response?.data) {
        // Partial save — download error Excel
        try {
          downloadBase64Excel(
            response.data,
            `RPOMonthlyConfig_Errors_${new Date().getTime()}.xlsx`,
          )
          setSnackbarOpen(true)
          setSnackbarData({
            message:
              response?.message ||
              'Import completed with errors. Please check the downloaded file.',
            severity: 'warning',
          })
          await fetchData()
        } catch (downloadError) {
          console.error('[RpoMonthlyConfig] Error downloading error file:', downloadError)
          setSnackbarOpen(true)
          setSnackbarData({
            message: 'Import failed but could not download error file.',
            severity: 'error',
          })
        }
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Failed to import Excel file.',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('[RpoMonthlyConfig] Error uploading Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Failed to import Excel file: ${error.message}`,
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  // ── Export (Excel) ──────────────────────────────────────────────────────────
  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({ message: 'Excel download started!', severity: 'info' })
    try {
      await InputApiService.exportRPOMonthlyConfigExcel(
        keycloak,
        AOP_YEAR,
        null,
        EXCEL_NAME,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('[RpoMonthlyConfig] Error exporting Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  // ── Remark dialog ───────────────────────────────────────────────────────────
  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  // ── Permissions ─────────────────────────────────────────────────────────────
  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showTitleNameBusiness: true,
    titleName: 'RPO Monthly Configuration',
    showTitle: true,
    showImport: true,
    showExport: true,
    ExcelName: EXCEL_NAME,
  }

  // ── Render ──────────────────────────────────────────────────────────────────
  return (
    <Box sx={{ mt: 2 }}>
      <LoaderBackdrop open={!!loading} />

      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        customModifiedCells={customModifiedCells}
        setCustomModifiedCells={setCustomModifiedCells}
        title={permissions.titleName}
        permissions={permissions}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={() => {}}
        saveChanges={saveChanges}
        handleExcelUpload={handleExcelUpload}
        handleExport={handleExport}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        customHeight={40}
        customItemChange={handleItemChange}
        pagable={false}
      />
    </Box>
  )
}

export default RpoMonthlyConfig

