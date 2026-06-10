import { useEffect, useState } from 'react'
import { Box } from '@mui/material'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo, {
  customValueFormatterPhaseTwo,
} from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { UtilityPlantApiServiceV2 } from 'components/aop-phase-two/services/cpp/utilityPlantApiServiceV2'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'

const UtilityRateGrid = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, screenTitle } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Prices')

  const headerMap = generateHeaderNames(AOP_YEAR)
  const valueFormat = ValueFormatterPhaseTwo()
  const customFormatTwo = customValueFormatterPhaseTwo(2)

  // Custom cell renderer for dynamic decimal formatting: 4 decimals if >= 1000, else 2 decimals
  const DynamicDecimalCell = ({
    dataItem,
    field,
    tdProps,
    customModifiedCells,
  }) => {
    let value = dataItem[field]
    let displayValue = value
    const rowId = dataItem.id

    if (value !== null && value !== undefined && value !== '') {
      const numValue = parseFloat(value)
      if (!isNaN(numValue)) {
        displayValue =
          numValue >= 1000 ? numValue.toFixed(2) : numValue.toFixed(4)
      }
    }

    const isEdited = Object.prototype.hasOwnProperty.call(
      customModifiedCells?.[rowId] || {},
      field,
    )

    return (
      <td
        {...tdProps}
        title={value}
        className={`${tdProps?.className || ''} ${isEdited ? 'edited-cell' : ''}`.trim()}
        style={{ ...tdProps?.style }}
      >
        {displayValue}
      </td>
    )
  }

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
    field: mon,
    title: headerMap[MONTH_TO_INDEX[mon]],
    widthT: 100,
    minWidth: 100,
    editable: true,
    type: 'number1',
    format: customFormatTwo,
  }))

  // ── State ────────────────────────────────────────────────────────────────
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  // ── Column definitions (mirrors FixedNorms / existing Prices skeleton) ──
  const columns = [
    {
      field: 'siteDescription',
      title: 'Site',
      widthT: 100,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 100,
    },
    {
      field: 'utilityPlant',
      title: 'Utility Plant',
      widthT: 180,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 180,
    },
    {
      field: 'utilityPlantId',
      title: 'Plant ID',
      widthT: 120,
      type: 'text',
      editable: false,
      locked: false,
      minWidth: 120,
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
      title: 'UOM',
      widthT: 110,
      type: 'text',
      editable: false,
      minWidth: 110,
    },

    {
      field: 'weightedAvgPrice',
      title: 'Weighted Avg Price',
      widthT: 180,
      type: 'number1',
      editable: false,
      minWidth: 180,
      format: customFormatTwo,
    },

    // Monthly prices ─ Apr → Mar (generated from MONTH_COLUMNS)
    ...MONTH_COLUMNS,
  ]

  // ── Fetch ─────────────────────────────────────────────────────────────────
  const fetchUtilityRateData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      let res = await UtilityPlantApiServiceV2.getUtilityRateData(
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
        isEditable: false,
      }))

      setRows(formatted)
    } catch (error) {
      console.error('Error fetching Utility Rate data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }

  // ── Effect: reload on plant / year change ────────────────────────────────
  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchUtilityRateData()
    }
  }, [PLANT_ID, AOP_YEAR])

  // ── Export (Excel) ─────────────────────────────────────────────────────────
  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({ message: 'Excel download started!', severity: 'info' })
    try {
      await UtilityPlantApiServiceV2.exportUtilityRateExcel(
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
      console.error('Error exporting Utility Rate data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  // ── Permissions / table config ────────────────────────────────────────────
  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: false,
    saveBtn: false,
    allAction: true,
    showTitleNameBusiness: true,
    titleName: 'Utility Rate',
    showTitle: true,
    showImport: false,
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
        title={permissions.titleName}
        permissions={permissions}
        handleExport={handleExport}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        customHeight={70}
        groupBy={['utilityPlant']}
      />
    </Box>
  )
}

export default UtilityRateGrid
