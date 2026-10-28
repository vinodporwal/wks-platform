import { useState, useEffect, useCallback, useMemo } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { OutputApiService } from 'components/aop-phase-two/services/cpp/jmd/outputApiService'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { useDebounce } from 'hooks/useDebounce'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import { customValueFormatterPhaseTwo } from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'

const PowerDemandGrid = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { year, screenTitle, jmdSelectedPlants, plantObject, siteObject } =
    dataGridStore

  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Power_Demand')
  const valueFormat = customValueFormatterPhaseTwo(2)
  // Build plant ID list: JMD uses selected plants, otherwise single plantObject
  const PLANT_ID_LIST = useMemo(() => {
    if (jmdSelectedPlants?.length) {
      return jmdSelectedPlants.map((plant) => plant.id)
    }
    if (plantObject?.id) {
      return [plantObject.id]
    }
    return []
  }, [jmdSelectedPlants, plantObject])

  const [loading, setLoading] = useState(false)
  const [exportLoading, setExportLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [rows, setRows] = useState([])

  // ===================== || API INTEGRATION: GET & EXPORT || ===================== //

  const fetchData = useCallback(async () => {
    if (!PLANT_ID_LIST?.length || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await OutputApiService.getPowerDemand(
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
      console.error('Error fetching Power Demand data:', error)
      setRows([])
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR])

  // Fetch immediately on mount or when parameters change
  useEffect(() => {
    if (PLANT_ID_LIST?.length && AOP_YEAR) {
      fetchData()
    }
  }, [PLANT_ID_LIST, AOP_YEAR, fetchData])

  // Debounced fetch for rapid dropdown/store changes
  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && AOP_YEAR) {
        fetchData()
      }
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR],
  )

  const handleExport = async () => {
    if (!PLANT_ID_LIST?.length || !AOP_YEAR) return
    setExportLoading(true)
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'info',
    })
    try {
      await OutputApiService.exportPowerDemandExcel(
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
      console.error('Error exporting Power Demand data:', error)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    } finally {
      setExportLoading(false)
    }
  }

  // ===================== || GRID COLUMNS & PERMISSIONS || ===================== //

  const headerMap = useMemo(() => generateHeaderNames(AOP_YEAR), [AOP_YEAR])

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

  // Month columns (Apr → Mar) using generateHeaderNames format (e.g., Apr-26)
  const MONTH_COLUMNS = useMemo(
    () =>
      MONTH_FIELDS.map((mon) => ({
        field: mon,
        title: headerMap[MONTH_TO_INDEX[mon]],
        widthT: 120,
        minWidth: 120,
        type: 'number1',
        editable: false,
        format: valueFormat,
      })),
    [headerMap],
  )

  // Base columns (read-only for output grid)
  const baseColumns = useMemo(
    () => [
      {
        field: 'cppPlantName',
        title: 'Plant Name',
        widthT: 180,
        minWidth: 180,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'utilityName',
        title: 'Utility Name',
        widthT: 180,
        minWidth: 180,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'utilityUOM',
        title: 'UOM',
        widthT: 100,
        minWidth: 100,
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
    ],
    [],
  )

  // Full column list: base columns + month columns + remarks
  const columns = useMemo(
    () => [...baseColumns, ...MONTH_COLUMNS],
    [baseColumns, MONTH_COLUMNS],
  )

  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: false,
    saveBtn: false,
    allAction: true,
    showTitleNameBusiness: true,
    titleName: 'Power Demand',
    showImport: false,
    showExport: true,
    ExcelName: EXCEL_NAME,
    showTitle: true,
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading || !!exportLoading} />
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        title='Power Demand'
        permissions={permissions}
        handleExport={handleExport}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        groupBy={['type']}
        customHeight={50}
        pagable={false}
      />
    </Box>
  )
}

export default PowerDemandGrid
