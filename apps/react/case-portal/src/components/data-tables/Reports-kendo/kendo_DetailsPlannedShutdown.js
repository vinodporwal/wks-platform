import React, { useCallback, useEffect, useState } from 'react'
import { Box, Backdrop } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useSession } from 'SessionStoreContext'
import { useSelector } from 'react-redux'
import { ReportDataService } from 'services/ReportDataService'
import {
  CircularProgress,
  Typography,
} from '../../../../node_modules/@mui/material/index'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import KendoDataTables from 'components/kendo-data-tables/index'
import { getRoleName } from 'services/role-service'
import { validateFields } from 'utils/validationUtils'

// ─── Helper: previous 4 year titles from AOP year ────────────────
function getPrevYearTitles(aopYear, count = 4) {
  if (!aopYear) return []
  const [start] = aopYear.split('-').map(Number)
  const years = []
  for (let i = 0; i < count; i++) {
    const y1 = start - i
    const y2 = (y1 + 1) % 100
    years.push(`${y1}-${y2.toString().padStart(2, '0')}`)
  }
  return years
}

export default function ShutdownReport() {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year, oldYear, isReleased } =
    dataGridStore

  const PLANT_ID = plantObject?.id
  const PLANT_NAME = plantObject?.name
  const SITE_NAME = siteObject?.name
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased

  const keycloak = useSession()
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const headerMap = generateHeaderNames(AOP_YEAR)
  const [loading, setLoading] = useState(false)

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const showSnackbar = useCallback((message, severity = 'info') => {
    setSnackbarData({ message, severity })
    setSnackbarOpen(true)
  }, [])
  const [rowsRoutine, setRowsRoutine] = useState([])
  const [rowsPlanned, setRowsPlanned] = useState([])
  const [modifiedCellsPlanned, setModifiedCellsPlanned] = useState({})
  const [remarkDialogOpenPlanned, setRemarkDialogOpenPlanned] = useState(false)
  const [currentRemarkPlanned, setCurrentRemarkPlanned] = useState('')
  const [currentRowIdPlanned, setCurrentRowIdPlanned] = useState(null)
  const [rowsPrevYears, setRowsPrevYears] = useState([])
  const [modifiedCellsPrevYears, setModifiedCellsPrevYears] = useState({})
  const [remarkDialogOpenPrevYears, setRemarkDialogOpenPrevYears] =
    useState(false)
  const [currentRemarkPrevYears, setCurrentRemarkPrevYears] = useState('')
  const [currentRowIdPrevYears, setCurrentRowIdPrevYears] = useState(null)

  // ── Column definition — RoutineShutdownPreviousYears ──
  const prevYearTitles = getPrevYearTitles(AOP_YEAR)

  const columnsPlanned = [
    {
      field: 'id',
      title: 'ID',
      editable: false,
      hidden: true,
    },
    {
      field: 'Activities',
      title: 'Activities',
      editable: true,
      widthT: 250,
    },
    {
      field: 'maintStartDateTime',
      title: 'SD - From',
      editable: true,
    },
    {
      field: 'maintEndDateTime',
      title: 'SD - To',
      editable: true,
    },
    {
      field: 'durationInHrs',
      title: 'Duration (Hrs)',
      editable: true,
      widthT: 120,
      type: 'number',
    },
    {
      field: 'remarks',
      title: 'Remarks',
      editable: true,
      widthT: 200,
    },
  ]
  const columnsRoutine = [
    {
      field: 'id',
      title: 'ID',
      editable: false,
      hidden: true,
    },
    {
      field: 'Activities',
      title: 'Activities',
      editable: false,
      widthT: 200,
    },
    {
      field: 'April',
      title: headerMap[4] || 'Apr',
      editable: false,
      width: 120,
      type: 'number',
    },
    {
      field: 'May',
      title: headerMap[5] || 'May',
      editable: false,
      width: 120,
      type: 'number',
    },
    {
      field: 'June',
      title: headerMap[6] || 'Jun',
      editable: false,
      width: 120,
      type: 'number',
    },
    {
      field: 'July',
      title: headerMap[7] || 'Jul',
      editable: false,
      width: 120,
      type: 'number',
    },
    {
      field: 'August',
      title: headerMap[8] || 'Aug',
      editable: false,
      width: 120,
      type: 'number',
    },
    {
      field: 'September',
      title: headerMap[9] || 'Sep',
      editable: false,
      width: 120,
      type: 'number',
    },
    {
      field: 'October',
      title: headerMap[10] || 'Oct',
      editable: false,
      width: 120,
      type: 'number',
    },
    {
      field: 'November',
      title: headerMap[11] || 'Nov',
      editable: false,
      width: 120,
      type: 'number',
    },
    {
      field: 'December',
      title: headerMap[12] || 'Dec',
      editable: false,
      width: 120,
      type: 'number',
    },
    {
      field: 'January',
      title: headerMap[1] || 'Jan',
      editable: false,
      width: 120,
      type: 'number',
    },
    {
      field: 'February',
      title: headerMap[2] || 'Feb',
      editable: false,
      width: 120,
      type: 'number',
    },
    {
      field: 'March',
      title: headerMap[3] || 'Mar',
      editable: false,
      width: 120,
      type: 'number',
    },
  ]

  const columnsPrevYears = [
    {
      field: 'id',
      title: 'ID',
      editable: false,
      hidden: true,
    },
    {
      field: 'Activities',
      title: 'Activities',
      editable: true,
      widthT: 200,
    },
    {
      field: 'PrevYear4',
      title: prevYearTitles[3] || 'Year 1',
      editable: true,
      widthT: 120,
      type: 'number',
    },
    {
      field: 'PrevYear3',
      title: prevYearTitles[2] || 'Year 2',
      editable: true,
      widthT: 120,
      type: 'number',
    },
    {
      field: 'PrevYear2',
      title: prevYearTitles[1] || 'Year 3',
      editable: true,
      widthT: 120,
      type: 'number',
    },
    {
      field: 'PrevYear1',
      title: prevYearTitles[0] || 'Year 4',
      editable: true,
      widthT: 120,
      type: 'number',
    },
  ]

  const handleRemarkCellClickPlanned = (row) => {
    if (READ_ONLY) return
    setCurrentRemarkPlanned(row.remarks || '')
    setCurrentRowIdPlanned(row.id)
    setRemarkDialogOpenPlanned(true)
  }
  const handleRemarkCellClickPrevYears = (row) => {
    if (READ_ONLY) return
    setCurrentRemarkPrevYears(row.remark || '')
    setCurrentRowIdPrevYears(row.id)
    setRemarkDialogOpenPrevYears(true)
  }
  const fetchPlannedShutdown = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) {
      setRowsPlanned([])
      return
    }
    setLoading(true)
    try {
      const res = await ReportDataService.getShutdownData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        'PlannedShutdown',
      )
      const shutdownList = res?.data?.shutdownDetailsList || []
      const mappedRows = shutdownList.map((item, idx) => ({
        id: item.id || idx + 1,
        Activities: item.activities,
        durationInHrs: item.durationHrs,
        maintStartDateTime: item.shutdownFrom,
        maintEndDateTime: item.shutdownTo,
        remarks: item.remarks,
        originalRemark: item.remarks,
        // add other fields if needed
      }))
      setRowsPlanned(mappedRows)
    } catch (error) {
      console.error('Error loading PlannedShutdown:', error)
      setRowsPlanned([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  const fetchRoutineShutdown = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) {
      setRowsRoutine([])
      return
    }
    setLoading(true)
    try {
      const res = await ReportDataService.getShutdownData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        'RoutineShutdown',
      )
      const shutdownList = res?.data?.shutdownDetailsList || []
      const mappedRows = shutdownList.map((item, idx) => ({
        id: item.id || idx + 1,
        Activities: item.activities,
        April: item.april,
        May: item.may,
        June: item.june,
        July: item.july,
        August: item.august,
        September: item.september,
        October: item.october,
        November: item.november,
        December: item.december,
        January: item.january,
        February: item.february,
        March: item.march,
        isEditable: false,
        // add other fields if needed
      }))

      setRowsRoutine(mappedRows)
    } catch (error) {
      console.error('Error loading RoutineShutdown:', error)
      setRowsRoutine([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  const fetchRoutineShutdownPreviousYears = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) {
      setRowsPrevYears([])
      return
    }
    setLoading(true)
    try {
      const res = await ReportDataService.getShutdownData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        'RoutineShutdownPreviousYears',
      )
      const shutdownList = res?.data?.shutdownDetailsList || []
      const mappedRows = shutdownList.map((item, idx) => ({
        id: item.id || idx + 1,
        Activities: item.activities,
        PrevYear1: item.prevYear1,
        PrevYear2: item.prevYear2,
        PrevYear3: item.prevYear3,
        PrevYear4: item.prevYear4,
        // add other fields if needed
      }))
      setRowsPrevYears(mappedRows)
    } catch (error) {
      console.error('Error loading RoutineShutdownPreviousYears:', error)
      setRowsPrevYears([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  const savePlannedChanges = useCallback(async () => {
    try {
      const data = Object.values(modifiedCellsPlanned)
      if (data.length === 0) {
        showSnackbar('No Records to Save!', 'info')
        return
      }
      const validationMessage = validateFields(data, ['remarks'])
      if (validationMessage) {
        showSnackbar(validationMessage, 'error')
        return
      }

      const payload = data.map((row) => ({
        id: row.id || null,
        activities: row.Activities,
        fromDateReport: row.maintStartDateTime,
        toDateReport: row.maintEndDateTime,
        durationHrs: row.durationInHrs,
        remarks: row.remarks || '',
      }))

      setLoading(true)
      const res = await ReportDataService.saveShutdownPlannedData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        'PlannedShutdown',
        payload,
      )

      if (res?.code === 200) {
        showSnackbar('Data Saved Successfully!', 'success')
        setModifiedCellsPlanned({})
        fetchPlannedShutdown()
      } else {
        showSnackbar('Data Save Failed!', 'error')
      }
    } catch (err) {
      console.error('Error saving PlannedShutdown:', err)
      showSnackbar(err.message || 'An error occurred', 'error')
    } finally {
      setLoading(false)
    }
  }, [
    modifiedCellsPlanned,
    keycloak,
    PLANT_ID,
    AOP_YEAR,
    fetchPlannedShutdown,
    showSnackbar,
  ])

  const savePrevYearsChanges = useCallback(async () => {
    try {
      const data = Object.values(modifiedCellsPrevYears)
      if (data.length === 0) {
        showSnackbar('No Records to Save!', 'info')
        return
      }

      const payload = data.map((row) => ({
        id: row.id || null,
        activities: row.Activities,
        prevYear1: row.PrevYear1,
        prevYear2: row.PrevYear2,
        prevYear3: row.PrevYear3,
        prevYear4: row.PrevYear4,
      }))

      setLoading(true)
      const res = await ReportDataService.saveShutdownPreviousYearsData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        'RoutineShutdownPreviousYears',
        payload,
      )

      if (res?.code === 200) {
        showSnackbar('Data Saved Successfully!', 'success')
        setModifiedCellsPrevYears({})
        fetchRoutineShutdownPreviousYears()
      } else {
        showSnackbar('Data Save Failed!', 'error')
      }
    } catch (err) {
      console.error('Error saving RoutineShutdownPreviousYears:', err)
      showSnackbar(err.message || 'An error occurred', 'error')
    } finally {
      setLoading(false)
    }
  }, [
    modifiedCellsPrevYears,
    keycloak,
    PLANT_ID,
    AOP_YEAR,
    fetchRoutineShutdownPreviousYears,
    showSnackbar,
  ])
  const deleteRowDataRoutineShutdown = async (paramsForDelete) => {
    setLoading(true)

    try {
      const { idFromApi, id } = paramsForDelete
      const deleteId = id

      if (!idFromApi) {
        setRowsPrevYears((prevRows) =>
          prevRows.filter((row) => row.id !== deleteId),
        )
      }

      if (idFromApi) {
        await ReportDataService.deleteRoutineShutdownData(
          idFromApi,
          keycloak,
          PLANT_ID,
        )
        setRowsPrevYears((prevRows) =>
          prevRows.filter((row) => row.id !== deleteId),
        )
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Record Deleted successfully!',
          severity: 'success',
        })
        fetchRoutineShutdownPreviousYears()
      } else {
        setLoading(false)
      }
    } catch (error) {
      console.error('Error deleting Record', error)
    }
  }

  const getAdjustedPermissionsPrevYears = (permissions, isOldYear) => {
    if (isOldYear !== 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      editButton: false,
      saveWithRemark: false,
      saveBtn: false,
      uploadExcelBtn: false,
      isOldYear,
    }
  }

  const permissionsRoutineShutdownPrevYears = getAdjustedPermissionsPrevYears(
    {
      allAction: true, // columns are editable:false — read-only display
      saveBtn: true,
      showTitle: true,
      showTitleNameBusiness: true,
      titleName:
        'Details of Routine Shutdowns for Previous Four Years(Total Shutdown Hours)',
      adjustedPermissions: true,
      downloadExcelBtn: false,
      downloadExcelBtnFromUI: true,
      uploadExcelBtn: false,
      ExcelName: `${PLANT_NAME}_Routine_Shutdown_Prev_Years`,
      addButton: true,
      deleteButton: true,
      showCalculate: false,
      showFinalSubmit: false,
    },
    IS_OLD_YEAR,
  )

  const getAdjustedPermissionsRoutine = (permissions, isOldYear) => {
    if (isOldYear !== 1) return permissions
    return {
      ...permissions,
      showAction: false,
      editButton: false,
      isOldYear,
    }
  }

  const permissionsRoutineShutdown = getAdjustedPermissionsRoutine(
    {
      allAction: true, // read-only — no inline editing
      saveBtn: false,
      showTitle: true,
      showTitleNameBusiness: true,
      titleName: 'Details of Routine Shutdowns (Monthwise)',
      adjustedPermissions: true,
      downloadExcelBtn: false,
      downloadExcelBtnFromUI: true,
      uploadExcelBtn: false,
      ExcelName: `${PLANT_NAME}_Routine_Shutdown`,
      addButton: false,
      deleteButton: false,
      showCalculate: false,
      showFinalSubmit: false,
    },
    IS_OLD_YEAR,
  )

  const getAdjustedPermissionsPlanned = (permissions, isOldYear) => {
    if (isOldYear !== 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      editButton: false,
      saveWithRemark: false,
      saveBtn: false,
      uploadExcelBtn: false,
      isOldYear,
    }
  }

  const permissionsPlannedShutdown = getAdjustedPermissionsPlanned(
    {
      allAction: true,
      saveBtn: true,
      showTitle: true,
      showTitleNameBusiness: true,
      titleName: 'Details of Planned Shutdowns other than Turnarounds',
      adjustedPermissions: true,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      ExcelName: `${PLANT_NAME}_Planned_Shutdown`,
      addButton: true,
      deleteButton: true,
      showCalculate: false,
      showFinalSubmit: false,
    },
    IS_OLD_YEAR,
  )

  useEffect(() => {
    fetchPlannedShutdown()
  }, [fetchPlannedShutdown])

  useEffect(() => {
    fetchRoutineShutdown()
  }, [fetchRoutineShutdown])

  useEffect(() => {
    fetchRoutineShutdownPreviousYears()
  }, [fetchRoutineShutdownPreviousYears])
  return (
    <Box sx={{ width: '100%' }}>
      <Backdrop
        sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
        open={!!loading}
      >
        <CircularProgress color='inherit' />
      </Backdrop>

      <Box>
        <KendoDataTables
          columns={columnsPlanned.filter((col) => !col.hidden)}
          rows={rowsPlanned}
          setRows={setRowsPlanned}
          title='Details of Planned Shutdowns other than Turnarounds'
          modifiedCells={modifiedCellsPlanned}
          setModifiedCells={setModifiedCellsPlanned}
          remarkDialogOpen={remarkDialogOpenPlanned}
          setRemarkDialogOpen={setRemarkDialogOpenPlanned}
          currentRemark={currentRemarkPlanned}
          setCurrentRemark={setCurrentRemarkPlanned}
          currentRowId={currentRowIdPlanned}
          setCurrentRowId={setCurrentRowIdPlanned}
          handleRemarkCellClick={handleRemarkCellClickPlanned}
          saveChanges={savePlannedChanges}
          permissions={permissionsPlannedShutdown}
        />

        <KendoDataTables
          columns={columnsRoutine.filter((col) => !col.hidden)}
          rows={rowsRoutine}
          setRows={setRowsRoutine}
          title='Details of Routine Shutdowns (Monthwise)'
          permissions={permissionsRoutineShutdown}
        />

        <KendoDataTables
          columns={columnsPrevYears.filter((col) => !col.hidden)}
          rows={rowsPrevYears}
          setRows={setRowsPrevYears}
          title='Details of Routine Shutdowns for Previous Four Years'
          modifiedCells={modifiedCellsPrevYears}
          setModifiedCells={setModifiedCellsPrevYears}
          remarkDialogOpen={remarkDialogOpenPrevYears}
          setRemarkDialogOpen={setRemarkDialogOpenPrevYears}
          currentRemark={currentRemarkPrevYears}
          setCurrentRemark={setCurrentRemarkPrevYears}
          currentRowId={currentRowIdPrevYears}
          deleteRowData={deleteRowDataRoutineShutdown}
          setCurrentRowId={setCurrentRowIdPrevYears}
          handleRemarkCellClick={handleRemarkCellClickPrevYears}
          saveChanges={savePrevYearsChanges}
          permissions={permissionsRoutineShutdownPrevYears}
        />
      </Box>

      <Notification
        open={snackbarOpen}
        message={snackbarData.message}
        severity={snackbarData.severity}
        onClose={() => setSnackbarOpen(false)}
      />
    </Box>
  )
}
