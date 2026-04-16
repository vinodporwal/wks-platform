import { Box } from '@mui/material'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
import KendoDataTables from './index'
import { DataService } from 'services/DataService'
import { validateFields } from 'utils/validationUtils'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import Notification from 'components/Utilities/Notification'

const MaterialBalance = ({ permissions }) => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    plantObject,
    siteObject,
    verticalObject,
    year,
    oldYear,
    isReleased,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear
   const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

// Static Data for Material Balance
  const mockMatbalData = useMemo(() => [
    { id: 1, Type: 'Feed', Particulars: 'Crude Oil', UOM: 'MT', april: 100, may: 110, june: 105, july: 115, august: 120, september: 118, october: 122, november: 125, december: 128, january: 130, february: 125, march: 135, remarks: 'Planned feed' },
    { id: 2, Type: 'Feed', Particulars: 'Imported Condensate', UOM: 'MT', april: 50, may: 55, june: 52, july: 58, august: 60, september: 59, october: 61, november: 62, december: 64, january: 65, february: 62, march: 68, remarks: 'Spot purchase' },
    { id: 3, Type: 'Production', Particulars: 'LPG', UOM: 'MT', april: 20, may: 22, june: 21, july: 23, august: 24, september: 24, october: 25, november: 26, december: 27, january: 28, february: 27, march: 29, remarks: 'High demand' },
    { id: 4, Type: 'Production', Particulars: 'Naphtha', UOM: 'MT', april: 40, may: 44, june: 42, july: 46, august: 48, september: 47, october: 49, november: 50, december: 51, january: 52, february: 50, march: 54, remarks: 'Export quality' },
    { id: 5, Type: 'Fuel/Loss', Particulars: 'Fuel Oil', UOM: 'MT', april: 5, may: 5, june: 5, july: 5, august: 5, september: 5, october: 5, november: 5, december: 5, january: 5, february: 5, march: 5, remarks: 'Internal consumption' },
    { id: 6, Type: 'Fuel/Loss', Particulars: 'Losses', UOM: 'MT', april: 2, may: 2, june: 2, july: 2, august: 2, september: 2, october: 2, november: 2, december: 2, january: 2, february: 2, march: 2, remarks: 'Standard loss' },
  ], [])

  const fetchMatbalData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setRows(mockMatbalData)
    return;
    setLoading(true)
    try {
      // Using 'matbal' as the type for Material Balance constraints
      const response = await DataService.getMaterialBalanceData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.code === 200) {
        const formattedData = (response?.data || []).map((item, index) => ({
          ...item,
          idFromApi: item.id,
          id: index,
          Particulars: item.NormTypeName || item.DisplayName,
          UOM: item.UOM || 'MT',
          remarks: item.Remarks || '',
          originalRemark: item.Remarks || '',
        }))
        setRows(formattedData)
      } else {
        setRows([])
      }
    } catch (error) {
      console.error('Error fetching Matbal data:', error)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  useEffect(() => {
    fetchMatbalData()
  }, [fetchMatbalData])

  const colDefs = useMemo(() => [
    { field: 'Particulars', title: 'Particulars', editable: false, widthT: 100 },
    { field: 'UOM', title: 'UOM', editable: false, widthT: 80 },
    { field: 'april', title: 'Apr', editable: true, type: 'number', format: '{0:#.###}', width: 120 },
    { field: 'may', title: 'May', editable: true, type: 'number', format: '{0:#.###}', width: 120 },
    { field: 'june', title: 'Jun', editable: true, type: 'number', format: '{0:#.###}', width: 120 },
    { field: 'july', title: 'Jul', editable: true, type: 'number', format: '{0:#.###}', width: 120 },
    { field: 'august', title: 'Aug', editable: true, type: 'number', format: '{0:#.###}', width: 120 },
    { field: 'september', title: 'Sep', editable: true, type: 'number', format: '{0:#.###}', width: 120 },
    { field: 'october', title: 'Oct', editable: true, type: 'number', format: '{0:#.###}', width: 120 },
    { field: 'november', title: 'Nov', editable: true, type: 'number', format: '{0:#.###}', width: 120 },
    { field: 'december', title: 'Dec', editable: true, type: 'number', format: '{0:#.###}', width: 120 },
    { field: 'january', title: 'Jan', editable: true, type: 'number', format: '{0:#.###}', width: 120 },
    { field: 'february', title: 'Feb', editable: true, type: 'number', format: '{0:#.###}', width: 120 },
    { field: 'march', title: 'Mar', editable: true, type: 'number', format: '{0:#.###}', width: 120 },
    { field: 'remarks', title: 'Remark', editable: false, widthT: 100 },
  ], [])

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveChanges = useCallback(async () => {
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) return

    const requiredFields = ['remarks']
    const validationMessage = validateFields(modifiedData, requiredFields)
    if (validationMessage) {
      setSnackbarData({ message: validationMessage, severity: 'error' })
      setSnackbarOpen(true)
      return
    }

    setLoading(true)
    try {
      const payload = modifiedData.map(row => ({
        id: row.idFromApi || null,
        plantId: PLANT_ID,
        auditYear: AOP_YEAR,
        normParameterFKId: row.normParameterFKId,
        april: row.april || 0,
        may: row.may || 0,
        june: row.june || 0,
        july: row.july || 0,
        august: row.august || 0,
        september: row.september || 0,
        october: row.october || 0,
        november: row.november || 0,
        december: row.december || 0,
        january: row.january || 0,
        february: row.february || 0,
        march: row.march || 0,
        remarks: row.remarks,
      }))

      const response = await DataService.saveMaterialBalanceData(keycloak, PLANT_ID, AOP_YEAR, payload)
      if (response?.code === 200) {
        setSnackbarData({ message: 'Saved Successfully!', severity: 'success' })
        setSnackbarOpen(true)
        setModifiedCells({})
        fetchMatbalData()
      } else {
        setSnackbarData({ message: 'Save Failed!', severity: 'error' })
        setSnackbarOpen(true)
      }
    } catch (error) {
      console.error('Error saving Matbal data:', error)
      setSnackbarData({ message: 'Error saving data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, PLANT_ID, AOP_YEAR, keycloak, fetchMatbalData])

  const handleExcelUpload = async (file) => {
    setLoading(true)
    try {
      const response = await DataService.saveMaterialBalanceExcel(file, keycloak, PLANT_ID, AOP_YEAR)
      if (response?.code === 200) {
        setSnackbarData({ message: 'Imported Successfully!', severity: 'success' })
        setSnackbarOpen(true)
        fetchMatbalData()
      } else {
        setSnackbarData({ message: 'Import Failed!', severity: 'error' })
        setSnackbarOpen(true)
      }
    } catch (error) {
      console.error('Error importing Matbal data:', error)
      setSnackbarData({ message: 'Error importing data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }

  const downloadExcelForConfiguration = async () => {
    try {
      const excelName = `${verticalObject?.name}_${siteObject?.name}_${plantObject?.name}_Material_Balance`
      await DataService.materialBalanceExport(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        excelName,
      )
      setSnackbarData({ message: 'Export Started!', severity: 'success' })
      setSnackbarOpen(true)
    } catch (error) {
      console.error('Error exporting Matbal data:', error)
      setSnackbarData({ message: 'Error exporting data', severity: 'error' })
      setSnackbarOpen(true)
    }
  }

  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
      allAction: false,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: permissions?.showAction ?? true,
      saveWithRemark: permissions?.saveWithRemark ?? true,
      saveBtn: permissions?.saveBtn ?? true,
      allAction: true,
      downloadExcelBtn: true,
      showTitleNameBusiness: true,
      titleName: 'Material Balance',
      uploadExcelBtn: true,
    },
    isOldYear,
  )

  return (
    <Box>
      <Backdrop sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }} open={loading}>
        <CircularProgress color='inherit' />
      </Backdrop>

      <KendoDataTables
        rows={rows}
        setRows={setRows}
        columns={colDefs}
        permissions={adjustedPermissions}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        groupBy='Type'
        title='Material Balance'
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        handleExcelUpload={handleExcelUpload}
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        plantID={PLANT_ID}
      />

      <Notification
        open={snackbarOpen}
        message={snackbarData.message}
        severity={snackbarData.severity}
        onClose={() => setSnackbarOpen(false)}
      />
    </Box>
  )
}

export default MaterialBalance
