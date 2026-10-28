import { useEffect, useState, useCallback, useMemo } from 'react'
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

const SRMapping = () => {
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
  const EXCEL_NAME = generateExcelName(dataGridStore, 'SR_Mapping_Output')

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
        format: valueFormatTwo,
      })),
    [headerMap, valueFormatTwo],
  )

  // Base columns copied from SenderReceiverMapping.js (read-only for output grid)
  const baseColumns = useMemo(
    () => [
      {
        field: 'cppPlantName',
        title: 'CPP Plant',
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'senderCostCenterName',
        title: 'Sender Cost Center',
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'senderCostCenterCode',
        title: 'Sender Cost Center Code',
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'senderPlantName',
        title: 'Sender Plant',
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'senderPlantCode',
        title: 'Sender Plant Code',
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'senderUtilityName',
        title: 'Utility',
        widthT: 150,
        minWidth: 150,
        type: 'text',
        editable: false,
      },
      {
        field: 'senderUtilityCode',
        title: 'Utility Code',
        widthT: 150,
        minWidth: 150,
        type: 'text',
        editable: false,
      },
      {
        field: 'senderUtilityUOM',
        title: 'Utility UOM',
        widthT: 150,
        minWidth: 150,
        type: 'text',
        editable: false,
      },
      {
        field: 'receiverCostCenterName',
        title: 'Receiver Cost Center',
        widthT: 180,
        minWidth: 180,
        type: 'text',
        editable: false,
      },
      {
        field: 'receiverCostCenterCode',
        title: 'Receiver Cost Center Code',
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'receiverPlantName',
        title: 'Receiver Plant',
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'receiverPlantCode',
        title: 'Receiver Plant Code',
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'receiverUtilityName',
        title: 'Receiver Utility',
        widthT: 150,
        minWidth: 150,
        type: 'text',
        editable: false,
      },
      {
        field: 'receiverUtilityCode',
        title: 'Receiver Utility Code',
        widthT: 180,
        minWidth: 180,
        type: 'text',
        editable: false,
      },
      {
        field: 'receiverUtilityUOM',
        title: 'Receiver Utility UOM',
        widthT: 180,
        minWidth: 180,
        type: 'text',
        editable: false,
      },
    ],
    [],
  )

  // Full column list: base columns + month columns (no group wrapper)
  const columns = useMemo(
    () => [...baseColumns, ...MONTH_COLUMNS],
    [baseColumns, MONTH_COLUMNS],
  )

  const fetchData = useCallback(async () => {
    if (!PLANT_ID_LIST?.length || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await OutputApiService.getSRMappingOutput(
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
    } catch (error) {
      console.error('Error fetching SR mapping output data:', error)
      setRows([])
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
    [PLANT_ID_LIST, AOP_YEAR],
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
      await OutputApiService.exportSRMappingOutputExcel(
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
      console.error('Error exporting SR mapping output data:', error)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  const paginationConfig = {
    threshold: 200,
    buttonCount: 5,
    pageSizes: [50, 100, 200, 500],
    defaultPageSize: 200,
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        title='SR Mapping'
        permissions={permissions}
        handleExport={handleExport}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        customHeight={65}
        pagable={false}
      />
    </Box>
  )
}

export default SRMapping
