import { useState, useCallback, useMemo, useEffect } from 'react'
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
import useConfigurationDates from 'components/aop-phase-two/common/hooks/useConfigurationDates'

const MonthlyCalculatedNorms = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { siteObject, plantObject, year, screenTitle, jmdSelectedPlants } =
    dataGridStore

  const { startDate, endDate, error: configError } = useConfigurationDates()

  const PLANT_ID = plantObject?.id
  const lowerSiteName = siteObject?.name?.toLowerCase()
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(
    dataGridStore,
    'Monthly_Calculated_Norms',
  )

  // Build plant ID list: JMD uses all selected plants, otherwise single plant
  const PLANT_ID_LIST = useMemo(
    () =>
      lowerSiteName === 'jmd'
        ? jmdSelectedPlants?.map((plant) => plant.id) || []
        : [PLANT_ID],
    [jmdSelectedPlants, lowerSiteName, PLANT_ID],
  )

  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const [rows, setRows] = useState([])

  const headerMap = useMemo(() => generateHeaderNames(AOP_YEAR), [AOP_YEAR])
  const valueFormat = customValueFormatterPhaseTwo(6)

  const formatDate = (date) => {
    if (!date) return ''
    const yr = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${yr}-${month}-${day}`
  }

  // Show error notification if configuration is not set up
  useEffect(() => {
    if (configError) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: configError,
        severity: 'warning',
      })
    }
  }, [configError])

  // Fiscal-year month order: Apr → Mar (field names match FixedNorms convention)
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

  // Month columns (Apr → Mar) using generateHeaderNames format (e.g., Apr-26)
  const MONTH_COLUMNS = useMemo(
    () =>
      MONTH_FIELDS.map((mon) => ({
        field: `${mon}Norms`,
        title: headerMap[MONTH_TO_INDEX[mon]],
        widthT: 100,
        minWidth: 100,
        type: 'number1',
        editable: false,
        format: valueFormat,
      })),
    [headerMap, valueFormat],
  )

  // Base columns (read-only for output grid) — same as FixedNorms
  const baseColumns = useMemo(
    () => [
      {
        field: 'cppPlantName',
        title: 'CPP Plant',
        widthT: 180,
        minWidth: 180,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'generatingPlantName',
        title: 'Generating Plant',
        widthT: 180,
        minWidth: 180,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'utilityName',
        title: 'Utility',
        widthT: 120,
        minWidth: 120,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'utilityId',
        title: 'Utility ID',
        widthT: 120,
        minWidth: 120,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'uom',
        title: 'Generation UOM',
        widthT: 180,
        minWidth: 180,
        type: 'text',
        editable: false,
      },
      {
        field: 'accountName',
        title: 'Account',
        widthT: 150,
        minWidth: 150,
        type: 'text',
        editable: false,
      },
      {
        field: 'materialName',
        title: 'Material',
        widthT: 130,
        minWidth: 130,
        type: 'text',
        editable: false,
      },
      {
        field: 'materialId',
        title: 'SAP Code',
        widthT: 130,
        minWidth: 130,
        type: 'text',
        editable: false,
      },
      {
        field: 'issuingPlantName',
        title: 'Issuing Plant',
        widthT: 150,
        minWidth: 150,
        type: 'text',
        editable: false,
      },
      {
        field: 'issuingUom',
        title: 'Issuing UOM',
        widthT: 150,
        minWidth: 150,
        type: 'text',
        editable: false,
      },
      {
        field: 'normTypeName',
        title: 'Norm Type',
        widthT: 150,
        minWidth: 150,
        type: 'text',
        editable: false,
      },
    ],
    [],
  )

  // Full column list: base columns + month columns
  const columns = useMemo(
    () => [...baseColumns, ...MONTH_COLUMNS],
    [baseColumns, MONTH_COLUMNS],
  )

  const fetchData = useCallback(async () => {
    if (!PLANT_ID_LIST?.length || !AOP_YEAR || !startDate || !endDate) return
    setLoading(true)
    try {
      const formattedStartDate = formatDate(startDate)
      const formattedEndDate = formatDate(endDate)
      const response = await OutputApiService.getMonthlyCalculatedNorms(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
        formattedStartDate,
        formattedEndDate,
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
      console.error('Error fetching monthly calculated norms data:', error)
      setRows([])
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR, startDate, endDate])

  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && AOP_YEAR && startDate && endDate) {
        fetchData()
      }
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR, startDate, endDate, fetchData],
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
    if (!startDate || !endDate) return
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'info',
    })
    try {
      const formattedStartDate = formatDate(startDate)
      const formattedEndDate = formatDate(endDate)
      await OutputApiService.exportMonthlyCalculatedNormsExcel(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
        EXCEL_NAME,
        formattedStartDate,
        formattedEndDate,
      )
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting monthly calculated norms data:', error)
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
        title='Monthly Calculated Norms'
        permissions={permissions}
        handleExport={handleExport}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        groupBy={['cppPlantName', 'generatingPlantName']}
        customHeight={65}
        pagable={false}
      />
    </Box>
  )
}

export default MonthlyCalculatedNorms
