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

// TODO: remove dummy rows once the SP/API is ready
const DUMMY_ROWS = [
  {
    id: 'dummy_1',
    generatingPlantName: 'CPP Unit 1',
    utilityName: 'Steam',
    utilityId: 'UTL-001',
    uom: 'MT',
    accountName: 'Fuel Consumption',
    materialName: 'Coal',
    materialId: 'SAP-1001',
    issuingPlantName: 'Plant A',
    issuingUom: 'KG',
    normTypeName: 'Fixed',
    aprNorms: 1.234567,
    mayNorms: 1.245678,
    junNorms: 1.256789,
    julNorms: 1.26789,
    augNorms: 1.278901,
    sepNorms: 1.289012,
    octNorms: 1.290123,
    novNorms: 1.301234,
    decNorms: 1.312345,
    janNorms: 1.323456,
    febNorms: 1.334567,
    marNorms: 1.345678,
  },
  {
    id: 'dummy_2',
    generatingPlantName: 'CPP Unit 1',
    utilityName: 'Power',
    utilityId: 'UTL-002',
    uom: 'KWH',
    accountName: 'Power Consumption',
    materialName: 'Diesel',
    materialId: 'SAP-1002',
    issuingPlantName: 'Plant A',
    issuingUom: 'LTR',
    normTypeName: 'Variable',
    aprNorms: 2.11,
    mayNorms: 2.12,
    junNorms: 2.13,
    julNorms: 2.14,
    augNorms: 2.15,
    sepNorms: 2.16,
    octNorms: 2.17,
    novNorms: 2.18,
    decNorms: 2.19,
    janNorms: 2.2,
    febNorms: 2.21,
    marNorms: 2.22,
  },
  {
    id: 'dummy_3',
    generatingPlantName: 'CPP Unit 2',
    utilityName: 'Steam',
    utilityId: 'UTL-003',
    uom: 'MT',
    accountName: 'Fuel Consumption',
    materialName: 'Natural Gas',
    materialId: 'SAP-1003',
    issuingPlantName: 'Plant B',
    issuingUom: 'SM3',
    normTypeName: 'Fixed',
    aprNorms: 0.98,
    mayNorms: 0.99,
    junNorms: 1.0,
    julNorms: 1.01,
    augNorms: 1.02,
    sepNorms: 1.03,
    octNorms: 1.04,
    novNorms: 1.05,
    decNorms: 1.06,
    janNorms: 1.07,
    febNorms: 1.08,
    marNorms: 1.09,
  },
  {
    id: 'dummy_4',
    generatingPlantName: 'CPP Unit 2',
    utilityName: 'Water',
    utilityId: 'UTL-004',
    uom: 'M3',
    accountName: 'Water Consumption',
    materialName: 'Raw Water',
    materialId: 'SAP-1004',
    issuingPlantName: 'Plant B',
    issuingUom: 'M3',
    normTypeName: 'Variable',
    aprNorms: 3.5,
    mayNorms: 3.55,
    junNorms: 3.6,
    julNorms: 3.65,
    augNorms: 3.7,
    sepNorms: 3.75,
    octNorms: 3.8,
    novNorms: 3.85,
    decNorms: 3.9,
    janNorms: 3.95,
    febNorms: 4.0,
    marNorms: 4.05,
  },
  {
    id: 'dummy_5',
    generatingPlantName: 'CPP Unit 3',
    utilityName: 'Power',
    utilityId: 'UTL-005',
    uom: 'KWH',
    accountName: 'Power Consumption',
    materialName: 'Coal',
    materialId: 'SAP-1001',
    issuingPlantName: 'Plant C',
    issuingUom: 'KG',
    normTypeName: 'Fixed',
    aprNorms: 1.5,
    mayNorms: 1.52,
    junNorms: 1.54,
    julNorms: 1.56,
    augNorms: 1.58,
    sepNorms: 1.6,
    octNorms: 1.62,
    novNorms: 1.64,
    decNorms: 1.66,
    janNorms: 1.68,
    febNorms: 1.7,
    marNorms: 1.72,
  },
]

const MonthlyCalculatedNorms = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { siteObject, plantObject, year, screenTitle, jmdSelectedPlants } =
    dataGridStore

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
    if (!PLANT_ID_LIST?.length || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await OutputApiService.getMonthlyCalculatedNorms(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )
      const data = response?.data || []
      const rowsWithId = data?.map((row, index) => ({
        ...row,
        id: row.id || `row_${index}`,
      }))
      // TODO: remove dummy fallback once the SP/API is ready
      setRows(rowsWithId.length ? rowsWithId : DUMMY_ROWS)
      if (!rowsWithId.length) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
      }
    } catch (error) {
      console.error('Error fetching monthly calculated norms data:', error)
      // TODO: remove dummy fallback once the SP/API is ready
      setRows(DUMMY_ROWS)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR])

  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && AOP_YEAR) {
        fetchData()
      }
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR, fetchData],
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
      await OutputApiService.exportMonthlyCalculatedNormsExcel(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
        EXCEL_NAME,
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
        groupBy={['generatingPlantName']}
        customHeight={65}
        pagable={false}
      />
    </Box>
  )
}

export default MonthlyCalculatedNorms
