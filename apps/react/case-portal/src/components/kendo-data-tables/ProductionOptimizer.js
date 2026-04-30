import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from './index'
import AopTabs from 'components/AopTabs'
import { Box } from '@mui/material'
import { DataService } from 'services/DataService'
import { ProductionNormsApiService } from 'services/production-norms-api-service'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const ProductionOptimizer = () => {
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

  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR)

  const vertName = verticalChange?.selectedVertical
  const SCREEN_NAME = screenTitle?.title
  const lowerVertName = vertName?.toLowerCase()
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}`
  const headerMap = generateHeaderNames(AOP_YEAR)

  const [rows, setRows] = useState([])
  const [columns, setColumns] = useState([])
  const [dropdownOptions, setDropdownOptions] = useState([])
  const [selectedMode, setSelectedMode] = useState('')

  const [rows1, setRows1] = useState([])
  const [columns1, setColumns1] = useState([])
  const [combinedDropdownOptions, setCombinedDropdownOptions] = useState([])
  const [selectedCombinedMode, setSelectedCombinedMode] = useState('')

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

  const valueFormat = ValueFormatterProduction()

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

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
        setLineDetails([])
        return
      }
      if (response && Array.isArray(response?.data)) {
        setLineDetails(response.data)
        const lineTabs = response?.data.map((line) => line.displayName)
        setTabs(lineTabs)
      }
    } catch (err) {
      console.error('Error fetching line details:', err)
      setTabs([])
      setLineDetails([])
    }
  }

  useEffect(() => {
    fetchLineDetails()
  }, [PLANT_ID, keycloak, yearChanged])

  const fetchDropdownOptions = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      const selectedLine = lineDetails[tabIndex]
      const lineId = selectedLine?.id ?? null
      const res = await ProductionNormsApiService.GetOptimizerdropdown(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        lineId,
        'dropdown',
      )
      if (res?.code === 200) {
        const options = res?.data?.map((item) => ({
          label: item.displayName,
          value: item.name,
        }))
        setDropdownOptions(options || [])
        if (options?.length > 0) {
          setSelectedMode(options[0].value)
        }
      }
    } catch (err) {
      console.error('Error fetching dropdown:', err)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak, tabIndex, lineDetails])

  const fetchCombinedDropdownOptions = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      const res = await ProductionNormsApiService.GetCombinedOptimizerdropdown(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (res?.code === 200) {
        const options = res?.data?.map((item) => ({
          label: item.displayName,
          value: item.name,
        }))
        setCombinedDropdownOptions(options || [])
        if (options?.length > 0) {
          setSelectedCombinedMode(options[0].value)
        }
      }
    } catch (err) {
      console.error('Error fetching combined dropdown:', err)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak])

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchDropdownOptions()
      fetchCombinedDropdownOptions()
    }
  }, [PLANT_ID, AOP_YEAR, yearChanged, tabIndex])

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR || !selectedMode) return
    setRows([])
    setLoading(true)
    try {
      const selectedLine = lineDetails[tabIndex]
      const lineId = selectedLine?.id ?? null
      const res = await ProductionNormsApiService.GetOptimizerData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        lineId,
        selectedMode,
      )
      if (res?.code === 200) {
        const monthKeyMap = {
          Apr: 4,
          May: 5,
          Jun: 6,
          Jul: 7,
          Aug: 8,
          Sep: 9,
          Oct: 10,
          Nov: 11,
          Dec: 12,
          Jan: 1,
          Feb: 2,
          Mar: 3,
        }
        const dynamicColumns = res?.data?.columns?.map((col) => {
          const monthKey = monthKeyMap[col.field]
          const isMonth = !!monthKey
          return {
            field: col.field,
            title: isMonth ? headerMap[monthKey] || col.title : col.title,
            editable: col.editable ?? false,
            align: col.type === 'number' ? 'right' : 'left',
            format: col.type === 'number' ? valueFormat : '{0:#.###}',
            type: col.type,
            isEditable: false,
          }
        })
        setColumns(dynamicColumns || [])
        const mapped = res?.data?.data?.map((item, index) => ({
          id: index + 1,
          sno: index + 1,
          ...item,
          isEditable: false,
        }))
        setRows(mapped || [])
      } else {
        setRows([])
        setColumns([])
      }
    } catch (err) {
      console.error('Error fetching data:', err)
      setRows([])
      setColumns([])
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak, tabIndex, lineDetails, selectedMode])

  const fetchDataCombined = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR || !selectedCombinedMode) return
    setRows1([])
    setLoading(true)
    try {
      const res = await ProductionNormsApiService.GetOptimizerCombinedData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        selectedCombinedMode,
      )
      if (res?.code === 200) {
        const monthKeyMap = {
          Apr: 4,
          May: 5,
          Jun: 6,
          Jul: 7,
          Aug: 8,
          Sep: 9,
          Oct: 10,
          Nov: 11,
          Dec: 12,
          Jan: 1,
          Feb: 2,
          Mar: 3,
        }
        const dynamicColumns = res?.data?.columns?.map((col) => {
          const monthKey = monthKeyMap[col.field]
          const isMonth = !!monthKey
          return {
            field: col.field,
            title: isMonth ? headerMap[monthKey] || col.title : col.title,
            editable: col.editable ?? false,
            align: col.type === 'number' ? 'right' : 'left',
            format: col.type === 'number' ? valueFormat : '{0:#.###}',
            type: col.type,
          }
        })
        setColumns1(dynamicColumns || [])
        const mapped = res?.data?.data?.map((item, index) => ({
          id: index + 1,
          sno: index + 1,
          ...item,
          isEditable: false,
        }))
        setRows1(mapped || [])
      } else {
        setRows1([])
        setColumns1([])
      }
    } catch (err) {
      console.error('Error fetching combined data:', err)
      setRows1([])
      setColumns1([])
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak, selectedCombinedMode])

  useEffect(() => {
    if (selectedMode) {
      fetchData()
    }
  }, [
    fetchData,
    oldYear,
    yearChanged,
    PLANT_ID,
    AOP_YEAR,
    lineDetails,
    tabIndex,
    selectedMode,
  ])

  useEffect(() => {
    if (selectedCombinedMode) {
      fetchDataCombined()
    }
  }, [
    fetchDataCombined,
    oldYear,
    yearChanged,
    PLANT_ID,
    AOP_YEAR,
    tabIndex,
    selectedCombinedMode,
  ])

  const handleCalculate = async () => {
    setRows([])
    setLoading(true)
    try {
      const data = await ProductionNormsApiService.handleCalculateOptimizer(
        PLANT_ID,
        AOP_YEAR,
        keycloak,
      )
      if (data == 0 || data) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        fetchDropdownOptions()
        fetchCombinedDropdownOptions()
        fetchData()
        fetchDataCombined()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Refresh Failed!',
          severity: 'error',
        })
      }
      return data
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: error.message || 'An error occurred',
        severity: 'error',
      })
      console.error('Error!', error)
    }
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
          saveBtn: false,
          allAction: true,
          downloadExcelBtnFromUI: true,
          titleName: 'Production Optimizer',
          ExcelName: `${EXCEL_EXPORT_TITLE}_Production Optimizer`,
          showRefresh: false,
          showTitleNameBusiness: true,
          marginBottom: true,
          showCalculate: true,
          showCalculateVisibility: true,
          dropdownLabel: 'Type',
          showModes: dropdownOptions.length > 0,
          modes: dropdownOptions.map((opt) => ({
            name: opt.value,
            displayName: opt.label,
          })),
        },
        isOldYear,
      ),
    [isOldYear, AOP_YEAR, PLANT_ID, SCREEN_NAME, dropdownOptions],
  )

  const adjustedPermissionsCombined = useMemo(
    () =>
      getAdjustedPermissions(
        {
          showAction: false,
          addButton: false,
          deleteButton: false,
          editButton: false,
          showUnit: false,
          saveWithRemark: false,
          saveBtn: false,
          allAction: true,
          downloadExcelBtnFromUI: true,
          titleName: 'Combined Production Optimizer',
          ExcelName: `${EXCEL_EXPORT_TITLE}_Combined Production Optimizer`,
          showRefresh: false,
          showTitleNameBusiness: true,
          marginBottom: true,
          showModes: combinedDropdownOptions.length > 0,
          modes: combinedDropdownOptions.map((opt) => ({
            name: opt.value,
            displayName: opt.label,
          })),
        },
        isOldYear,
      ),
    [isOldYear, AOP_YEAR, PLANT_ID, SCREEN_NAME, combinedDropdownOptions],
  )

  return (
    <>
      <Box display='flex' alignItems='center' sx={{ mb: 1, mt: 1 }}>
        <AopTabs tabIndex={tabIndex} setTabIndex={setTabIndex} tabs={tabs} />
      </Box>

      <div>
        <LoaderBackdrop open={!!loading} />

        <KendoDataTables
          columns={columns}
          rows={rows}
          setRows={setRows}
          fetchData={fetchData}
          deleteId={deleteId}
          title='Production Optimizer'
          setDeleteId={setDeleteId}
          open1={open1}
          setOpen1={setOpen1}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          snackbarData={snackbarData}
          setSnackbarData={setSnackbarData}
          permissions={adjustedPermissions}
          currentRowId={currentRowId}
          selectMode={selectedMode}
          setSelectMode={(val) => setSelectedMode(val)}
          handleCalculate={handleCalculate}
        />
        <KendoDataTables
          title='Combined Production Optimizer'
          columns={columns1}
          rows={rows1}
          setRows={setRows1}
          fetchData={fetchDataCombined}
          deleteId={deleteId}
          setDeleteId={setDeleteId}
          open1={open1}
          setOpen1={setOpen1}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          snackbarData={snackbarData}
          setSnackbarData={setSnackbarData}
          permissions={adjustedPermissionsCombined}
          currentRowId={currentRowId}
          selectMode={selectedCombinedMode}
          setSelectMode={(val) => setSelectedCombinedMode(val)}
        />
      </div>
    </>
  )
}

export default ProductionOptimizer
