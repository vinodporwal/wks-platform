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

const PowerAndSteamLoading = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    siteObject,
    plantObject,
    verticalObject,
    year,
    screenTitle,
    jmdSelectedPlants,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const lowerSiteName = siteObject?.name?.toLowerCase()
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Power_And_Steam_Loading')

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
  const valueFormat = ValueFormatterPhaseTwo()
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

  // One column per month showing Avg. Loading
  const MONTH_COLUMNS = useMemo(
    () =>
      MONTH_FIELDS.map((mon) => ({
        field: `${mon}AvgLoading`,
        title: headerMap[MONTH_TO_INDEX[mon]], // e.g. "Apr-26"
        widthT: 130,
        minWidth: 130,
        type: 'number1',
        editable: false,
        format: valueFormatTwo,
      })),
    [headerMap, valueFormatTwo],
  )

  // Base columns (read-only for output grid)
  const baseColumns = useMemo(
    () => [
      {
        field: 'cppPlantName',
        title: 'Site',
        widthT: 150,
        minWidth: 150,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'issuingPlantName',
        title: 'Plant',
        widthT: 180,
        minWidth: 180,
        type: 'text',
        editable: false,
      },
      {
        field: 'issuingPlantCode',
        title: 'Plant Code',
        widthT: 120,
        minWidth: 120,
        type: 'text',
        editable: false,
      },
      {
        field: 'loadingUOM',
        title: 'UOM',
        widthT: 90,
        minWidth: 90,
        type: 'text',
        editable: false,
      },
      {
        field: 'materialCategory',
        title: 'Material Category',
        widthT: 140,
        minWidth: 140,
        type: 'text',
        editable: false,
      },
      {
        field: 'utilityName',
        title: 'CPP Utility',
        widthT: 160,
        minWidth: 160,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'utilityCode',
        title: 'Utility Code',
        widthT: 110,
        minWidth: 110,
        type: 'text',
        editable: false,
      },
      {
        field: 'materialName',
        title: 'Material',
        widthT: 160,
        minWidth: 160,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'materialCode',
        title: 'Material Code',
        widthT: 110,
        minWidth: 110,
        type: 'text',
        editable: false,
      },
      {
        field: 'generatingPlantName',
        title: 'CPP Plant',
        widthT: 180,
        minWidth: 180,
        type: 'text',
        editable: false,
      },
      {
        field: 'generatingPlantCode',
        title: 'CPP Plant Code',
        widthT: 140,
        minWidth: 140,
        type: 'text',
        editable: false,
      },
    ],
    [],
  )

  // Full column list: base columns + one Avg. Loading column per month
  const columns = useMemo(
    () => [...baseColumns, ...MONTH_COLUMNS],
    [baseColumns, MONTH_COLUMNS],
  )

  const fetchData = useCallback(async () => {
    if (!PLANT_ID_LIST?.length || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await OutputApiService.getPowerAndSteamLoading(
        keycloak,
        PLANT_ID_LIST,
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
      console.error('Error fetching power and asset loading data:', error)
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
      await OutputApiService.exportPowerAndSteamLoadingExcel(
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
      console.error('Error exporting power and asset loading data:', error)
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
        title='Power and Steam Loading'
        permissions={permissions}
        handleExport={handleExport}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        groupBy={['cppPlantName', 'materialCategory']}
        customHeight={65}
        pagable={false}
      />
    </Box>
  )
}

export default PowerAndSteamLoading
