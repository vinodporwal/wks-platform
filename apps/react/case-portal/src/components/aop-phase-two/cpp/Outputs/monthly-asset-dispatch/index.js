import { useState, useCallback, useMemo } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { OutputApiService } from 'components/aop-phase-two/services/cpp/jmd/outputApiService'
import ValueFormatterPhaseTwo, {
  customValueFormatterPhaseTwo,
} from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { useDebounce } from 'hooks/useDebounce'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

const MonthlyAssetDispatch = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    siteObject,
    plantObject,
    year,
    screenTitle,
    jmdSelectedPlants,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const lowerSiteName = siteObject?.name?.toLowerCase()
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Monthly_Asset_Dispatch')

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

  const valueFormat = ValueFormatterPhaseTwo()
  const valueFormatTwo = customValueFormatterPhaseTwo(2)

  // All columns map directly to SP output columns — vertical layout, no pivot
  const columns = useMemo(
    () => [
     

      /* ── CPP Plant ── */
      {
        field: 'cppPlant',
        title: 'CPP Plant',
        widthT: 180,
        minWidth: 180,
        type: 'text',
        editable: false,
        locked: true,
      },
      
      /* ── Utility Plant ── */
      {
        field: 'utilityPlant',
        title: 'Utility Plant',
        widthT: 160,
        minWidth: 160,
        type: 'text',
        editable: false,
      },
      {
        field: 'utilityPlantCode',
        title: 'Plant Code',
        widthT: 140,
        minWidth: 140,
        type: 'text',
        editable: false,
      },

      /* ── Utility ── */
      {
        field: 'utility',
        title: 'Utility',
        widthT: 150,
        minWidth: 150,
        type: 'text',
        editable: false,
      },
      {
        field: 'utilityCode',
        title: 'Utility Code',
        widthT: 130,
        minWidth: 130,
        type: 'text',
        editable: false,
      },

      /* ── Asset ── */
      {
        field: 'asset',
        title: 'Asset',
        widthT: 130,
        minWidth: 130,
        type: 'text',
        editable: false,
      },

      {
        field: 'assetType',
        title: 'Asset Type',
        widthT: 120,
        minWidth: 120,
        type: 'text',
        editable: false,
      },

      /* ── Material Plant ── */
      {
        field: 'materialPlant',
        title: 'Material Plant',
        widthT: 160,
        minWidth: 160,
        type: 'text',
        editable: false,
      },
      {
        field: 'materialPlantCode',
        title: 'Plant Code',
        widthT: 145,
        minWidth: 145,
        type: 'text',
        editable: false,
      },

      /* ── Material ── */
      {
        field: 'material',
        title: 'Material',
        widthT: 150,
        minWidth: 150,
        type: 'text',
        editable: false,
      },
      {
        field: 'materialCode',
        title: 'Material Code',
        widthT: 130,
        minWidth: 130,
        type: 'text',
        editable: false,
      },

       /* ── Identifiers ── */
      {
        field: 'aopYear',
        title: 'AOP Year',
        widthT: 100,
        minWidth: 100,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'month',
        title: 'Month',
        widthT: 110,
        minWidth: 110,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'monthNo',
        title: 'Month No',
        widthT: 95,
        minWidth: 95,
        type: 'wholeNumber',
        editable: false,
        hidden: true,
      },

      /* ── Dispatch Details ── */
      {
        field: 'priority',
        title: 'Priority',
        widthT: 90,
        minWidth: 90,
        type: 'wholeNumber',
        editable: false,
        format: valueFormat,
      },
      {
        field: 'minLoading',
        title: 'Min Loading',
        widthT: 110,
        minWidth: 110,
        type: 'number1',
        editable: false,
        format: valueFormatTwo,
      },
      {
        field: 'maxLoading',
        title: 'Max Loading',
        widthT: 110,
        minWidth: 110,
        type: 'number1',
        editable: false,
        format: valueFormatTwo,
      },
      {
        field: 'loadingUOM',
        title: 'Loading UOM',
        widthT: 110,
        minWidth: 110,
        type: 'text',
        editable: false,
      },
      {
        field: 'netOperatingHours',
        title: 'Net Op. Hours',
        widthT: 120,
        minWidth: 120,
        type: 'number1',
        editable: false,
        format: valueFormatTwo,
      },
      {
        field: 'heatRate',
        title: 'Heat Rate',
        widthT: 110,
        minWidth: 110,
        type: 'number1',
        editable: false,
        format: valueFormatTwo,
      },
      {
        field: 'heatForPower',
        title: 'Heat for Power/Steam',
        widthT: 125,
        minWidth: 125,
        type: 'number1',
        editable: false,
        format: valueFormatTwo,
      },
      {
        field: 'mandatoryToRun',
        title: 'Mandatory to Run',
        widthT: 140,
        minWidth: 140,
        type: 'text',
        editable: false,
      },
      {
        field: 'loading',
        title: 'Loading',
        widthT: 110,
        minWidth: 110,
        type: 'number1',
        editable: false,
        format: valueFormatTwo,
      },

      /* ── Generation ── */
      {
        field: 'generationQty',
        title: 'Generation Qty',
        widthT: 130,
        minWidth: 130,
        type: 'number1',
        editable: false,
        format: valueFormatTwo,
      },
      {
        field: 'generationUOM',
        title: 'Generation UOM',
        widthT: 130,
        minWidth: 130,
        type: 'text',
        editable: false,
      },

      /* ── Status ── */
      {
        field: 'isAvailable',
        title: 'Is Available',
        widthT: 110,
        minWidth: 110,
        type: 'text',
        editable: false,
      },
      {
        field: 'isDispatched',
        title: 'Is Dispatched',
        widthT: 115,
        minWidth: 115,
        type: 'text',
        editable: false,
      },
    ],
    [valueFormat, valueFormatTwo],
  )

  const fetchData = useCallback(async () => {
    if (!PLANT_ID_LIST?.length || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await OutputApiService.getMonthlyAssetDispatch(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )
      const data = response?.data || []
      const rowsWithId = data.map((row, index) => ({
        ...row,
        id: row.id || `row_${index}`,
      }))
      setRows(rowsWithId)
      if (!rowsWithId.length) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
      }
    } catch (error) {
      console.error('Error fetching monthly asset dispatch data:', error)
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
    setSnackbarData({ message: 'Excel download started!', severity: 'info' })
    try {
      await OutputApiService.exportMonthlyAssetDispatchExcel(
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
      console.error('Error exporting monthly asset dispatch data:', error)
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
        title='Monthly Asset Dispatch'
        permissions={permissions}
        handleExport={handleExport}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        groupBy={['cppPlant','assetMasterType']}
        customHeight={65}
        pagable={false}
      />
    </Box>
  )
}

export default MonthlyAssetDispatch
