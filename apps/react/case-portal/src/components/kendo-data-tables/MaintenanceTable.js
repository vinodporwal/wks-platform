import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
import { MaintenanceDetailsApiService } from 'services/maintenance-details-api-service'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from './index'
import AopTabs from 'components/AopTabs'
import { Box } from '@mui/material'
import { DataService } from 'services/DataService'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
//import ElastomerMaintenanceTable from './ElastomerMaintenanceTable'
const MaintenanceTable = () => {
  // State for tabs
  const [tabIndex, setTabIndex] = useState(0)
  const [tabs, setTabs] = useState([])
  const [lineDetails, setLineDetails] = useState([])
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const keycloak = useSession()

  const {
    verticalChange,
    yearChanged,
    oldYear,
    plantID,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
  } = dataGridStore
  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()
  const IS_PTA = verticalObject?.name?.toLowerCase() === 'pta'
  const IS_CHEMICAL = verticalObject?.name?.toLowerCase() === 'chemical'

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const vertName = verticalChange?.selectedVertical
  const SCREEN_NAME = screenTitle?.title
  const lowerVertName = vertName?.toLowerCase()
  const IS_PP_DTA =
    verticalObject?.name?.toLowerCase() === 'pp' &&
    siteObject?.name?.toLowerCase() === 'dta'
  const IS_PP_SEZ =
    verticalObject?.name?.toLowerCase() === 'pp' &&
    siteObject?.name?.toLowerCase() === 'sez'
  const IS_PP_HMD =
    verticalObject?.name?.toLowerCase() === 'pp' &&
    siteObject?.name?.toLowerCase() === 'hmd'
  const IS_PVC_DMD =
    verticalObject?.name?.toLowerCase() === 'pvc' &&
    siteObject?.name?.toLowerCase() === 'dmd'
  const IS_PVC_HMD =
    verticalObject?.name?.toLowerCase() === 'pvc' &&
    siteObject?.name?.toLowerCase() === 'hmd'
  const IS_PVC_VMD =
    verticalObject?.name?.toLowerCase() === 'pvc' &&
    siteObject?.name?.toLowerCase() === 'vmd'
  const IS_ELASTOMER_JMD =
    verticalObject?.name?.toLowerCase() === 'elastomer' &&
    siteObject?.name?.toLowerCase() === 'jmd'
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}`
  const dataConfig = useMemo(
    () => ({
      serviceFn: (keycloak, PLANT_ID, AOP_YEAR, lineId) => {
        if (
          (IS_PP_DTA ||
            IS_PP_SEZ ||
            IS_PVC_DMD ||
            IS_PP_HMD ||
            IS_PVC_HMD ||
            IS_PVC_VMD) &&
          lineId
        ) {
          return MaintenanceDetailsApiService.getMaintenanceDataLineWise(
            keycloak,
            PLANT_ID,
            AOP_YEAR,
            lineId,
          )
        }
        return MaintenanceDetailsApiService.getMaintenanceData(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      },
    }),
    [
      PLANT_ID,
      AOP_YEAR,
      lowerVertName,
      IS_PP_DTA,
      IS_PP_SEZ,
      IS_PVC_DMD,
      IS_PVC_HMD,
      IS_PP_HMD,
      IS_PVC_VMD,
      tabIndex,
      lineDetails,
    ],
  )

  const headerMap = generateHeaderNames(AOP_YEAR)

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [deleteId, setDeleteId] = useState(null)
  const [open1, setOpen1] = useState(false)

  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setRows([])
    setLoading(true)
    try {
      const selectedLine = lineDetails[tabIndex]
      const lineId = selectedLine?.id
      const resp = await dataConfig.serviceFn(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        lineId,
      )
      const raw = resp
      const monthFields = [
        'April',
        'May',
        'June',
        'July',
        'Aug',
        'Sep',
        'Oct',
        'Nov',
        'Dec',
        'Jan',
        'Feb',
        'Mar',
      ]

      const formatted = (raw || []).map((item, idx) => {
        const updatedItem = { ...item }

        const allMonthsTotal = monthFields.reduce((sum, month) => {
          let value = parseFloat(item[month]) || 0

          if (IS_ELASTOMER_JMD) {
            updatedItem[month] = value === 0 ? '-' : value
          }

          return sum + value
        }, 0)

        return {
          ...updatedItem,
          idFromApi: item.id,
          id: idx,
          isEditable: false,
          originalRemark: item.remarks,
          allMonthsTotal,
        }
      })

      setRows(formatted)
    } catch (err) {
      console.error('Error fetching data:', err)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak, dataConfig])

  useEffect(() => {
    fetchData()
  }, [fetchData, oldYear, yearChanged, PLANT_ID, AOP_YEAR, lineDetails])

  // Fetch line details when component mounts or plantID/year changes
  const fetchLineDetails = async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    try {
      const response = await DataService.getLineDetails(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code != 200) {
        setTabs([])
        return
      }
      if (response && Array.isArray(response?.data)) {
        setLineDetails(response.data)
        // Update tabs based on the response
        const lineTabs = response?.data.map((line) => line.displayName)
        setTabs(lineTabs)
      }
    } catch (err) {
      console.error('Error fetching line details:', err)
      // Fallback to default tabs if API fails
      setTabs([])
    }
  }

  useEffect(() => {
    if (
      IS_PP_DTA ||
      IS_PP_SEZ ||
      IS_PVC_DMD ||
      IS_PP_HMD ||
      IS_PVC_HMD ||
      IS_PVC_VMD
    ) {
      fetchLineDetails()
    }
  }, [PLANT_ID, keycloak, yearChanged])

  // Helper to generate monthly fields
  const getMonthlyColumns = () => {
    const months = [
      { field: 'April', index: 4 },
      { field: 'May', index: 5 },
      { field: 'June', index: 6 },
      { field: 'July', index: 7 },
      { field: 'Aug', index: 8 },
      { field: 'Sep', index: 9 },
      { field: 'Oct', index: 10 },
      { field: 'Nov', index: 11 },
      { field: 'Dec', index: 12 },
      { field: 'Jan', index: 1 },
      { field: 'Feb', index: 2 },
      { field: 'Mar', index: 3 },
    ]

    return months.map(({ field, index }) => ({
      field,
      title: headerMap[index],
      type: 'number',
      format: '{0:n2}',
      editable: false,
      align: 'right',
      headerAlign: 'left',
      minWidth: 85,
    }))
  }

  // Shared editable field
  const isEditableField = {
    field: 'isEditable',
    title: 'isEditable',
    hidden: true,
    isVisible: false,
  }

  // Base function to generate column set
  const generateColumns = (nameWidthT) => [
    {
      field: 'Name',
      title: 'Description',
      align: 'left',
      headerAlign: 'left',
      widthT: nameWidthT,
      editable: false,
      isEditable: false,
      minWidth: 200,
    },
    ...getMonthlyColumns(),
    isEditableField,
  ]
  // Base function to generate column set
  const generateColumnsPEPP = (nameWidthT) => [
    {
      field: 'Name',
      title: 'Description',
      align: 'left',
      headerAlign: 'left',
      widthT: nameWidthT,
      editable: false,
      isEditable: false,
      minWidth: 200,
    },
    ...getMonthlyColumns(),
    isEditableField,

    {
      field: 'allMonthsTotal',
      title: 'Total Hrs',
      type: 'number',
      format: '{0:n2}',
      editable: false,
      minWidth: 85,
    },
  ]

  const generateColumnsELASTOMER = (nameWidthT) => [
    {
      field: 'Name',
      title: 'Description',
      align: 'left',
      headerAlign: 'left',
      widthT: nameWidthT,
      editable: false,
      minWidth: 200,
    },
    ...getMonthlyColumns(),
    isEditableField,
    {
      field: 'allMonthsTotal',
      title: 'Total',
      type: 'number',
      format: '{0:00}',
      editable: false,
      minWidth: 85,
    },
  ]
  const generateColumnsELASTOMERJMD = (nameWidthT) => [
    {
      field: 'Name',
      title: 'Description',
      align: 'left',
      headerAlign: 'left',
      widthT: nameWidthT,
      editable: false,
      minWidth: 200,
    },
    ...getMonthlyColumns(),
    isEditableField,
    {
      field: 'total',
      title: 'Total',
      type: 'number',
      format: '{0:n2}',
      editable: false,
      minWidth: 85,
    },
  ]

  // Column sets
  const productionColumnsMEG = generateColumns(390)
  const productionColumnsPE = generateColumnsPEPP(200)
  const productionColumnsPP = generateColumnsPEPP(220)
  const productionColumnsNonMEG = generateColumns(200)
  const productionColumnsELASTOMER = generateColumnsELASTOMER(200)
  const productionColumnsELASTOMERJMD = generateColumnsELASTOMERJMD(200)

  // Column selection
  let basecols

  switch (lowerVertName) {
    case 'meg':
      basecols = productionColumnsMEG
      break
    case 'pe':
      basecols = productionColumnsPE
      break
    case 'pp':
      basecols = productionColumnsPP
      break
    case 'elastomer':
      basecols = IS_ELASTOMER_JMD
        ? productionColumnsELASTOMERJMD
        : productionColumnsELASTOMER
      break
    case 'pet':
      basecols = productionColumnsPP
      break
    case 'pvc':
      basecols = productionColumnsPP
      break
    default:
      basecols = productionColumnsNonMEG
      break
  }

  if (IS_PTA || IS_CHEMICAL) {
    basecols = [
      ...basecols,
      {
        field: 'allMonthsTotal',
        title: 'Total',
        type: 'number',
        format: '{0:n2}',
        editable: false,
        minWidth: 85,
      },
    ]
  }

  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
      allAction: false,
    }
  }

  const adjustedPermissions = useMemo(
    () =>
      getAdjustedPermissions(
        {
          showAction: false,
          addButton: false,
          deleteButton: false,
          editButton: false,
          showUnit: false,
          saveWithRemark: false,
          saveBtn: dataConfig.isCracker,
          allAction: true,
          downloadExcelBtnFromUI:
            IS_PP_DTA ||
            IS_PP_SEZ ||
            IS_PVC_DMD ||
            IS_PP_HMD ||
            IS_PVC_HMD ||
            IS_PVC_VMD
              ? false
              : true,
          ExcelName: `${EXCEL_EXPORT_TITLE}_${SCREEN_NAME}`,
          showRefresh: false,
          showTitleNameBusiness: true,
          titleName: SCREEN_NAME,

          downloadExcelBtn:
            IS_PP_DTA ||
            IS_PP_SEZ ||
            IS_PVC_DMD ||
            IS_PP_HMD ||
            IS_PVC_HMD ||
            IS_PVC_VMD
              ? true
              : false,
        },
        isOldYear,
      ),
    [isOldYear, AOP_YEAR, PLANT_ID, SCREEN_NAME],
  )
  // if (lowerVertName == 'elastomer') {
  //   return <ElastomerMaintenanceTable />
  // }

  const downloadExcelForConfiguration = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })

    try {
      let response
      response = await MaintenanceDetailsApiService.MaintenanceExportLineWise(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        `${EXCEL_EXPORT_TITLE}_${SCREEN_NAME}`,
      )
    } catch (error) {
      console.error('Error downloading Excel:', error)
      setSnackbarData({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
    } finally {
      setSnackbarOpen(true)
    }
  }

  return (
    <>
      {/* LINE1-LINE6 Tabs - Only for PP VERTICAL | DTA SITE */}
      {(IS_PP_DTA ||
        IS_PP_SEZ ||
        IS_PVC_DMD ||
        IS_PP_HMD ||
        IS_PVC_HMD ||
        IS_PVC_VMD) && (
        <Box display='flex' alignItems='center' sx={{ mb: 1, mt: 1 }}>
          <AopTabs tabIndex={tabIndex} setTabIndex={setTabIndex} tabs={tabs} />
        </Box>
      )}
      <div>
        <LoaderBackdrop open={!!loading} />

        <KendoDataTables
          columns={basecols}
          rows={rows}
          setRows={setRows}
          fetchData={fetchData}
          deleteId={deleteId}
          setDeleteId={setDeleteId}
          open1={open1}
          setOpen1={setOpen1}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          snackbarData={snackbarData}
          setSnackbarData={setSnackbarData}
          permissions={adjustedPermissions}
          currentRowId={currentRowId}
          downloadExcelForConfiguration={downloadExcelForConfiguration}
        />
      </div>
    </>
  )
}
export default MaintenanceTable
