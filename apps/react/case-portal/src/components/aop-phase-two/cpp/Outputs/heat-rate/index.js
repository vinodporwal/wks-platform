import { useEffect, useState, useCallback, useMemo } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { OutputApiService } from 'components/aop-phase-two/services/cpp/jmd/outputApiService'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { useDebounce } from 'hooks/useDebounce'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

const HeatRate = () => {
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
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Final_Heat_Rate')

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

  const columns = [
    {
      field: 'siteName',
      title: 'Site',
      type: 'text',
      editable: false,
      locked: false,
      minWidth: 80,
    },
    {
      field: 'cppPlantName',
      title: 'CPP Plant',
      type: 'text',
      editable: false,
      locked: false,
      minWidth: 100,
    },
    {
      field: 'assetType',
      title: 'Asset Type',
      type: 'text',
      editable: false,
      minWidth: 100,
    },
    {
      field: 'assetName',
      title: 'Asset Name',
      type: 'text',
      editable: false,
      minWidth: 120,
    },
    {
      field: 'utilityId',
      title: 'Utility Id',
      type: 'text',
      editable: false,
      minWidth: 80,
    },
    {
      field: 'load',
      title: 'Load',
      type: 'number1',
      editable: false,
      minWidth: 80,
    },
    {
      field: 'finalHeatRate',
      title: 'Final Heat Rate',
      type: 'number1',
      editable: false,
      minWidth: 80,
    },
  ]

  const fetchData = useCallback(async () => {
    if (!PLANT_ID_LIST?.length || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await OutputApiService.getFinalHeatRate(
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
      console.error('Error fetching final heat rate data:', error)
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
      await OutputApiService.exportFinalHeatRateExcel(
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
      console.error('Error exporting final heat rate data:', error)
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
        title='Heat Rate'
        permissions={permissions}
        handleExport={handleExport}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        groupBy={['cppPlantName', 'assetType', 'assetName']}
        // paginationConfig={paginationConfig}
        customHeight={65}
        pagable={false}
      />
    </Box>
  )
}

export default HeatRate
