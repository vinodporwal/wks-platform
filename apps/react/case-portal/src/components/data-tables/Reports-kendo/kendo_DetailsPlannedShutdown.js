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
import dayjs from 'dayjs'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

// ─── Helper: previous 4 year titles from AOP year ────────────────
function getPrevYearTitles(aopYear, count = 4) {
  if (!aopYear) return []
  const [start] = aopYear.split('-').map(Number)
  const years = []
  for (let i = 1; i <= count; i++) {
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
  //const IS_OLD_YEAR = false
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

  const [modifiedCellsRoutine, setModifiedCellsRoutine] = useState({})
  const [remarkDialogOpenRoutine, setRemarkDialogOpenRoutine] = useState(false)
  const [currentRemarkRoutine, setCurrentRemarkRoutine] = useState('')
  const [currentRowIdRoutine, setCurrentRowIdRoutine] = useState(null)

  // ── Column definition — RoutineShutdownPreviousYears ──
  const prevYearTitles = getPrevYearTitles(AOP_YEAR)

  const columnsPlanned = [
    {
      field: 'Activities',
      title: 'Activities',
      editable: true,
      widthT: 250,
    },
    {
      field: 'taSD',
      title: 'SD - From',
      editable: true,
    },
    {
      field: 'taED',
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
      field: 'Activities',
      title: 'Activities',
      editable: true,
      widthT: 200,
    },
    {
      field: 'April',
      title: headerMap[4] || 'Apr',
      editable: true,
      width: 120,
      type: 'number',
    },
    {
      field: 'May',
      title: headerMap[5] || 'May',
      editable: true,
      width: 120,
      type: 'number',
    },
    {
      field: 'June',
      title: headerMap[6] || 'Jun',
      editable: true,
      width: 120,
      type: 'number',
    },
    {
      field: 'July',
      title: headerMap[7] || 'Jul',
      editable: true,
      width: 120,
      type: 'number',
    },
    {
      field: 'August',
      title: headerMap[8] || 'Aug',
      editable: true,
      width: 120,
      type: 'number',
    },
    {
      field: 'September',
      title: headerMap[9] || 'Sep',
      editable: true,
      width: 120,
      type: 'number',
    },
    {
      field: 'October',
      title: headerMap[10] || 'Oct',
      editable: true,
      width: 120,
      type: 'number',
    },
    {
      field: 'November',
      title: headerMap[11] || 'Nov',
      editable: true,
      width: 120,
      type: 'number',
    },
    {
      field: 'December',
      title: headerMap[12] || 'Dec',
      editable: true,
      width: 120,
      type: 'number',
    },
    {
      field: 'January',
      title: headerMap[1] || 'Jan',
      editable: true,
      width: 120,
      type: 'number',
    },
    {
      field: 'February',
      title: headerMap[2] || 'Feb',
      editable: true,
      width: 120,
      type: 'number',
    },
    {
      field: 'March',
      title: headerMap[3] || 'Mar',
      editable: true,
      width: 120,
      type: 'number',
    },
    // {
    //   field: 'remarks',
    //   title: 'Remarks',
    //   editable: true,
    //   widthT: 200,
    // },
  ]
  const columnsPrevYears = [
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
  const handleRemarkCellClickRoutine = (row) => {
    if (READ_ONLY) return
    setCurrentRemarkRoutine(row.remarks || '')
    setCurrentRowIdRoutine(row.id)
    setRemarkDialogOpenRoutine(true)
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
        ...item,
        idFromApi: item?.id,
        id: idx,
        idRow: `PS-${idx}`,
        Activities: item.activities,
        durationInHrs: item.durationHrs,
        taSD: item.shutdownFrom,
        taED: item.shutdownTo,
        remarks: item.remarks,
        originalRemark: item.remarks ?? '',
        inEdit: false,
        isEditable: true,
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
        ...item,
        idFromApi: item?.id,
        id: idx,
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
        remarks: item.remarks || '',
        originalRemark: item.remarks || '',
        inEdit: false,
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
        ...item,
        id: idx,
        idFromApi: item?.id,
        idRow: `RSPY-${idx}`,
        Activities: item.activities,
        PrevYear1: item.prevYear1,
        PrevYear2: item.prevYear2,
        PrevYear3: item.prevYear3,
        PrevYear4: item.prevYear4,
        inEdit: false,
        isEditable: true,
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
      const requiredFields = [
        'remarks',
        'Activities',
        'taSD',
        'taED',
        'durationInHrs',
      ]

      const validationMessage = validateFields(data, requiredFields)

      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationMessage,
          severity: 'error',
        })
        setLoading(false)
        return
      }

      const payload = data.map((row) => ({
        id: row?.idFromApi || null,
        activities: row.Activities,

        shutdownFrom: row.taSD ? dayjs(row.taSD).format('YYYY-MM-DD') : null,
        shutdownTo: row.taED ? dayjs(row.taED).format('YYYY-MM-DD') : null,

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
      const requiredFields = ['Activities']

      const validationMessage = validateFields(data, requiredFields)

      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationMessage,
          severity: 'error',
        })
        setLoading(false)
        return
      }

      const payload = data.map((row) => ({
        id: row?.idFromApi || null,

        activities: row.Activities || null,
        prevYear1: row.PrevYear1 || null,
        prevYear2: row.PrevYear2 || null,
        prevYear3: row.PrevYear3 || null,
        prevYear4: row.PrevYear4 || null,
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
  const saveRoutineChanges = useCallback(async () => {
    try {
      const data = Object.values(modifiedCellsRoutine)
      if (data.length === 0) {
        showSnackbar('No Records to Save!', 'info')
        return
      }
      const validationMessage = validateFields(data, ['remarks', 'Activities'])
      if (validationMessage) {
        showSnackbar(validationMessage, 'error')
        return
      }

      const payload = data.map((row) => ({
        id: row?.idFromApi || null,
        activities: row.Activities || null,
        april: row.April || null,
        may: row.May || null,
        june: row.June || null,
        july: row.July || null,
        august: row.August || null,
        september: row.September || null,
        october: row.October || null,
        november: row.November || null,
        december: row.December || null,
        january: row.January || null,
        february: row.February || null,
        march: row.March || null,
        remarks: row.remarks || '',
        originalRemark: row.remarks || '',
      }))

      setLoading(true)
      const res = await ReportDataService.saveShutdownRoutineData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        'RoutineShutdown',
        payload,
      )

      if (res?.code === 200) {
        showSnackbar('Data Saved Successfully!', 'success')
        setModifiedCellsRoutine({})
        fetchRoutineShutdown()
      } else {
        showSnackbar('Data Save Failed!', 'error')
      }
    } catch (err) {
      console.error('Error saving RoutineShutdown:', err)
      showSnackbar(err.message || 'An error occurred', 'error')
    } finally {
      setLoading(false)
    }
  }, [
    modifiedCellsRoutine,
    keycloak,
    PLANT_ID,
    AOP_YEAR,
    fetchRoutineShutdown,
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

  //deleteRowDataPlannedShutdown
  const deleteRowDataPlannedShutdown = async (paramsForDelete) => {
    setLoading(true)

    try {
      const { idFromApi, id } = paramsForDelete
      const deleteId = id

      if (!idFromApi) {
        setRowsPlanned((prevRows) =>
          prevRows.filter((row) => row.id !== deleteId),
        )
      }

      if (idFromApi) {
        await ReportDataService.deletePlannedShutdownData(
          idFromApi,
          keycloak,
          PLANT_ID,
        )
        setRowsPlanned((prevRows) =>
          prevRows.filter((row) => row.id !== deleteId),
        )
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Record Deleted successfully!',
          severity: 'success',
        })
        fetchPlannedShutdown()
      } else {
        setLoading(false)
      }
    } catch (error) {
      console.error('Error deleting Record', error)
    }
  }
  const deleteRowDataRoutineShutdownMonthwise = async (paramsForDelete) => {
    setLoading(true)

    try {
      const { idFromApi, id } = paramsForDelete
      const deleteId = id

      if (!idFromApi) {
        setRowsRoutine((prevRows) =>
          prevRows.filter((row) => row.id !== deleteId),
        )
      }

      if (idFromApi) {
        await ReportDataService.deleteRoutineShutdownsMonthwiseData(
          idFromApi,
          keycloak,
          PLANT_ID,
        )
        setRowsRoutine((prevRows) =>
          prevRows.filter((row) => row.id !== deleteId),
        )
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Record Deleted successfully!',
          severity: 'success',
        })
        fetchRoutineShutdown()
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
      allAction: true,
      saveBtn: true,
      showTitle: true,
      showTitleNameBusiness: true,
      titleName:
        'Details of Routine Shutdowns for Previous Four Years(Total Shutdown Hours)',
      adjustedPermissions: true,
      downloadExcelBtn: false,
      downloadExcelBtnFromUI: false,
      uploadExcelBtn: false,
      ExcelName: `${PLANT_NAME}_Routine_Shutdown_Prev_Years`,
      addButton: true,
      deleteButton: true,
      showCalculate: false,
      showFinalSubmit: false,
      editButton: true,
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
      allAction: true,
      saveBtn: false,
      showTitle: true,
      showTitleNameBusiness: true,
      titleName: 'Details of Routine Shutdowns (Monthwise)',
      adjustedPermissions: true,
      downloadExcelBtn: false,
      downloadExcelBtnFromUI: false,
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
      editButton: true,
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
      <LoaderBackdrop open={!!loading} />

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
          deleteRowData={deleteRowDataPlannedShutdown}
        />

        <KendoDataTables
          columns={columnsRoutine.filter((col) => !col.hidden)}
          rows={rowsRoutine}
          setRows={setRowsRoutine}
          title='Details of Routine Shutdowns (Monthwise)'
          //----------
          modifiedCells={modifiedCellsRoutine}
          setModifiedCells={setModifiedCellsRoutine}
          remarkDialogOpen={remarkDialogOpenRoutine}
          setRemarkDialogOpen={setRemarkDialogOpenRoutine}
          currentRemark={currentRemarkRoutine}
          setCurrentRemark={setCurrentRemarkRoutine}
          currentRowId={currentRowIdRoutine}
          deleteRowData={deleteRowDataRoutineShutdownMonthwise}
          setCurrentRowId={setCurrentRowIdRoutine}
          handleRemarkCellClick={handleRemarkCellClickRoutine}
          saveChanges={saveRoutineChanges}
          //----------
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
