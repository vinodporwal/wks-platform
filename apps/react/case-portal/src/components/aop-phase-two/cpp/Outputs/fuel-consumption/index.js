import { useState, useCallback, useMemo } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { OutputApiService } from 'components/aop-phase-two/services/cpp/jmd/outputApiService'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { customValueFormatterPhaseTwo } from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { useDebounce } from 'hooks/useDebounce'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

const FuelConsumption = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { siteObject, year, screenTitle } = dataGridStore

  const SITE_ID = siteObject?.id
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Fuel_Consumption')

  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const [rows, setRows] = useState([])

  const headerMap = useMemo(() => generateHeaderNames(AOP_YEAR), [AOP_YEAR])
  const valueFormatTwo = customValueFormatterPhaseTwo(2)

  // Month order: Apr → Mar (fiscal year)
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
  ]

  // Each month column becomes a parent group with 3 sub-columns:
  //   Available | Allocated | Remaining
  const MONTH_COLUMNS = useMemo(
    () =>
      MONTH_FIELDS.map((mon) => ({
        title: headerMap[MONTH_TO_INDEX[mon]], // e.g. "Jan-26"
        children: [
          {
            field: `${mon}Available`,
            title: 'Available',
            widthT: 120,
            minWidth: 120,
            type: 'number1',
            editable: false,
            format: valueFormatTwo,
          },
          {
            field: `${mon}Allocated`,
            title: 'Allocated',
            widthT: 120,
            minWidth: 120,
            type: 'number1',
            editable: false,
            format: valueFormatTwo,
          },
          {
            field: `${mon}Remaining`,
            title: 'Remaining',
            widthT: 120,
            minWidth: 120,
            type: 'number1',
            editable: false,
            format: valueFormatTwo,
          },
        ],
      })),
    [headerMap, valueFormatTwo],
  )

  // Base columns (read-only for output grid)
  const baseColumns = useMemo(
    () => [
      {
        field: 'plantName',
        title: 'Plant',
        widthT: 180,
        minWidth: 180,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'plantCode',
        title: 'Plant Code',
        widthT: 140,
        minWidth: 140,
        type: 'text',
        editable: false,
        locked: true,
        hidden: true,
      },
      {
        field: 'fuelDisplayName',
        title: 'Fuel',
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
        locked: true,
      },
    ],
    [],
  )

  // Full column list: base columns + grouped month columns (each with 3 sub-columns)
  const columns = useMemo(
    () => [...baseColumns, ...MONTH_COLUMNS],
    [baseColumns, MONTH_COLUMNS],
  )

  const fetchData = useCallback(async () => {
    if (!SITE_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await OutputApiService.getFuelConsumption(
        keycloak,
        SITE_ID,
        AOP_YEAR,
      )
      const data = response?.data || []
      const rowsWithId = data?.map((row, index) => ({
        ...row,
        id: row.id || `row_${index}`,
      }))
      setRows(rowsWithId)
      if (!rowsWithId.length) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
      }
    } catch (error) {
      console.error('Error fetching fuel consumption data:', error)
      setRows([])
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, SITE_ID, AOP_YEAR])

  useDebounce(
    () => {
      if (SITE_ID && AOP_YEAR) {
        fetchData()
      }
    },
    1000,
    [SITE_ID, AOP_YEAR, fetchData],
  )

  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: false,
    saveBtn: false,
    allAction: true,
    showTitleNameBusiness: true,
    titleName: screenTitle?.title,
    showImport: false,
    showExport: true,
    ExcelName: EXCEL_NAME,
    showTitle: true,
  }

  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'info',
    })
    try {
      await OutputApiService.exportFuelConsumptionExcel(
        keycloak,
        SITE_ID,
        AOP_YEAR,
        EXCEL_NAME,
      )
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting fuel consumption data:', error)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        title='Fuel Consumption (In MMBTU)'
        permissions={permissions}
        handleExport={handleExport}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        groupBy={['plantName']}
        customHeight={65}
        pagable={false}
      />
    </Box>
  )
}

export default FuelConsumption
