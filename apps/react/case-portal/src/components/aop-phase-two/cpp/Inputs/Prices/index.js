import { useEffect, useState } from 'react'
import { Box } from '@mui/material'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo, {
  customValueFormatterPhaseTwo,
} from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { validateNestedRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import { InputApiService } from 'components/aop-phase-two/services/cpp/inputApiService'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import { downloadBase64Excel } from 'components/aop-phase-two/common/utilities/downloadBase64Excel'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'

const Prices = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, screenTitle } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Prices')

  const headerMap = generateHeaderNames(AOP_YEAR)
  const valueFormat = ValueFormatterPhaseTwo()
  const customFormatFive = customValueFormatterPhaseTwo(5)

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
  const MONTH_COLUMNS = [
    'apr',
    'may',
    'jun',
    'jul',
    'aug',
    'sep',
    'oct',
    'nov',
    'dec',
    'jan',
    'feb',
    'mar',
  ].map((mon) => ({
    field: `${mon}Price`,
    title: headerMap[MONTH_TO_INDEX[mon]],
    widthT: 100,
    minWidth: 100,
    editable: true,
    type: 'number1',
    format: valueFormat,
  }))

  // ── State ────────────────────────────────────────────────────────────────
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // ── Column definitions (mirrors FixedNorms / existing Prices skeleton) ──
  const columns = [
    {
      field: 'generatingPlantName',
      title: 'Generating Plant',
      widthT: 180,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 180,
    },
    {
      field: 'utilityName',
      title: 'Utility',
      widthT: 120,
      type: 'text',
      editable: false,
      locked: false,
      minWidth: 120,
    },
    {
      field: 'utilityId',
      title: 'Utility ID',
      widthT: 120,
      type: 'text',
      editable: false,
      locked: false,
      minWidth: 120,
    },
    {
      field: 'uom',
      title: 'Generation UOM',
      widthT: 180,
      type: 'text',
      editable: false,
      minWidth: 180,
    },
    {
      field: 'accountName',
      title: 'Account',
      widthT: 150,
      type: 'text',
      editable: false,
      minWidth: 150,
    },
    {
      field: 'materialName',
      title: 'Material',
      widthT: 130,
      type: 'text',
      editable: false,
      minWidth: 130,
    },
    {
      field: 'materialId',
      title: 'SAP Code',
      widthT: 130,
      type: 'text',
      editable: false,
      minWidth: 130,
    },
    {
      field: 'issuingPlantName',
      title: 'Issuing Plant',
      widthT: 150,
      type: 'text',
      editable: false,
      hidden: true,
      minWidth: 150,
    },
    {
      field: 'issuingUom',
      title: 'Issuing UOM',
      widthT: 150,
      type: 'text',
      editable: false,
      minWidth: 150,
    },

    // Monthly norms ─ Apr → Mar (generated from MONTH_COLUMNS)
    ...MONTH_COLUMNS,
    {
      field: 'modifiedBy',
      title: 'Price Source',
      widthT: 250,
      type: 'text',
      editable: true,
      minWidth: 250,
    },
    {
      field: 'remarks',
      title: 'Remarks',
      widthT: 250,
      type: 'textarea',
      editable: true,
      minWidth: 250,
      alwaysEditable: true,
    },
  ]

  // ── Fetch ─────────────────────────────────────────────────────────────────
  const fetchPricesData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      let res = await InputApiService.getPricesData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (!res?.data || res.data.length === 0) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        return
      }

      const formatted = res.data.map((item, index) => ({
        ...item,
        id: item.id || index + 1,
        remarks: item.remarks || '',
        isEditable: true,
      }))

      setRows(formatted)
      setOriginalRows(formatted)
    } catch (error) {
      console.error('Error fetching Prices data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }

  // ── Effect: reload on plant / year change ────────────────────────────────
  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchPricesData()
      setModifiedCells({})
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [PLANT_ID, AOP_YEAR])

  // ── Save ──────────────────────────────────────────────────────────────────
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

    // Validate remarks for any changed month fields
    // const fieldsToCheck = ['price', ...MONTH_COLUMNS.map((col) => col.field)]
    const fieldsToCheck = ['price', 'priceSource']
    const validationError = validateNestedRowDataWithRemarks(
      data,
      originalRows,
      fieldsToCheck,
      'generatingPlantName',
    )

    if (validationError) {
      setSnackbarOpen(true)
      setSnackbarData({ message: validationError, severity: 'error' })
      setLoading(false)
      return
    }

    // Strip internal UI-only fields before sending
    const payload = data.map(({ inEdit, ...rest }) => ({ ...rest }))

    try {
      await InputApiService.savePricesData(keycloak, AOP_YEAR, payload)

      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved ${data.length} change(s)!`,
        severity: 'success',
      })
      await fetchPricesData()
    } catch (error) {
      console.error('Error saving Prices data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to save changes. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  // ── Import (Excel) ─────────────────────────────────────────────────────────
  const handleExcelUpload = async (file) => {
    if (!file) return
    setLoading(true)
    try {
      const response = await InputApiService.savePricesExcel(
        file,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Excel file imported successfully!',
          severity: 'success',
        })
        await fetchPricesData()
      } else if (response?.code === 400 && response?.data) {
        // Partial save — download error file
        try {
          downloadBase64Excel(
            response.data,
            `Prices_Errors_${new Date().getTime()}.xlsx`,
          )

          setSnackbarOpen(true)
          setSnackbarData({
            message:
              response?.message ||
              'Import completed with errors. Please check the downloaded file.',
            severity: 'warning',
          })
          await fetchPricesData()
        } catch (downloadError) {
          console.error('Error downloading error file:', downloadError)
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
      console.error('Error uploading Prices Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Failed to import Excel file: ${error.message}`,
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  // ── Export (Excel) ─────────────────────────────────────────────────────────
  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({ message: 'Excel download started!', severity: 'info' })
    try {
      await InputApiService.exportPricesExcel(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_NAME,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting Prices data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  // ── Remark dialog ─────────────────────────────────────────────────────────
  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  // ── Permissions / table config ────────────────────────────────────────────
  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showTitleNameBusiness: true,
    titleName: 'Prices',
    showTitle: true,
    showImport: true,
    showExport: true,
    ExcelName: EXCEL_NAME,
  }

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
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
        customHeight={70}
        groupBy={['generatingPlantName']}
      />
    </Box>
  )
}

export default Prices
