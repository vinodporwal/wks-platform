import { useState, useCallback, useMemo } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { OutputApiService } from 'components/aop-phase-two/services/cpp/jmd/outputApiService'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import ValueFormatterPhaseTwo, {
  customValueFormatterPhaseTwo,
} from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { useDebounce } from 'hooks/useDebounce'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

const FuelAllocation = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { siteObject, plantObject, year, screenTitle, jmdSelectedPlants } =
    dataGridStore

  const PLANT_ID = plantObject?.id
  const lowerSiteName = siteObject?.name?.toLowerCase()
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Fuel_Allocation')

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
  const valueFormatTwo = customValueFormatterPhaseTwo(2)

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

  // Each month column becomes a parent group with 2 sub-columns:
  //   Gen. Qty | Allocation Qty
  const MONTH_COLUMNS = useMemo(
    () =>
      MONTH_FIELDS.map((mon) => ({
        title: headerMap[MONTH_TO_INDEX[mon]], // e.g. "Apr-26"
        children: [
          {
            field: `${mon}GenerationQuantity`,
            title: 'Gen. Qty',
            widthT: 130,
            minWidth: 130,
            type: 'number1',
            editable: false,
            format: valueFormatTwo,
          },
          {
            field: `${mon}AllocationQuantity`,
            title: 'Allocation Qty',
            widthT: 140,
            minWidth: 140,
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
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'assetName',
        title: 'Asset Name',
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'issuingPlantName',
        title: 'Issuing Plant',
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'materialName',
        title: 'Material',
        widthT: 180,
        minWidth: 180,
        type: 'text',
        editable: false,
      },
      {
        field: 'isFreeSteamFuel',
        title: 'Fuel Allocation',
        widthT: 140,
        minWidth: 140,
        type: 'text',
        editable: false,
      },

      // {
      //   field: 'issuingUOM',
      //   title: 'UOM',
      //   widthT: 100,
      //   minWidth: 100,
      //   type: 'text',
      //   editable: false,
      // },
      // {
      //   field: 'accountName',
      //   title: 'Account',
      //   widthT: 140,
      //   minWidth: 140,
      //   type: 'text',
      //   editable: false,
      // },
    ],
    [],
  )

  // Full column list: base columns + grouped month columns (each with 2 sub-columns)
  const columns = useMemo(
    () => [...baseColumns, ...MONTH_COLUMNS],
    [baseColumns, MONTH_COLUMNS],
  )

  const fetchData = useCallback(async () => {
    if (!PLANT_ID_LIST?.length || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await OutputApiService.getFuelAllocationQuantity(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )
      const data = response?.data || []
      const rowsWithId = data?.map((row, index) => ({
        ...row,
        id: row.id || `row_${index}`,
        isFreeSteamFuel: row.isFreeSteamFuel,
      }))
      setRows(rowsWithId)
      if (!rowsWithId.length) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
      }
    } catch (error) {
      console.error('Error fetching fuel allocation data:', error)
      setRows([])
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
      await OutputApiService.exportFuelAllocationQuantityExcel(
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
      console.error('Error exporting fuel allocation data:', error)
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
        title='Fuel Allocation'
        permissions={permissions}
        handleExport={handleExport}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        groupBy={['cppPlantName', 'assetName']}
        customHeight={65}
        pagable={false}
      />
    </Box>
  )
}

export default FuelAllocation
