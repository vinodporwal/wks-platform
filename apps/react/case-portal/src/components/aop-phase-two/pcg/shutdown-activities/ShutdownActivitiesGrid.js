import { useState, useEffect, useCallback } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { ShutdownPlanApiService } from '../../services/polyester/shutdownPlanApiService'
import { generateExcelName } from '../../common/utilities/excelNameUtil'
import { downloadBase64Excel } from '../../common/utilities/downloadBase64Excel'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

// ─── Constants ────────────────────────────────────────────────────────────────
const MAINTENANCE_TYPE = 'Shutdown'

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Add IST (+5:30) offset to a Date before sending to API */
function addTimeOffset(dateTime) {
  if (!dateTime) return null
  const date = new Date(dateTime)
  date.setUTCHours(date.getUTCHours() + 5)
  date.setUTCMinutes(date.getUTCMinutes() + 30)
  return date
}

/**
 * Compute duration from start/end dates if not manually set.
 * Returns "HH.MM" string e.g. "10.30".
 */
function findDuration(row) {
  if (row.durationInHrs) return row.durationInHrs
  if (row.maintStartDateTime && row.maintEndDateTime) {
    const start = new Date(row.maintStartDateTime)
    const end = new Date(row.maintEndDateTime)
    if (!isNaN(start.getTime()) && !isNaN(end.getTime())) {
      const mins = (end - start) / (1000 * 60)
      const hours = Math.floor(mins / 60)
      const minutes = mins % 60
      return `${hours}.${Math.round(minutes).toString().padStart(2, '0')}`
    }
  }
  return ''
}

/** Format a duration string to "HH.MM" with zero-padding */
function formatDuration(row) {
  const v = findDuration(row)
  if (!v) return null
  const [h = '00', m = '00'] = String(v).split('.')
  return `${h.padStart(2, '0')}.${m.padStart(2, '0')}`
}

// ─── Column definitions ─────────────────────────────────────────────────────
const columns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    type: 'text',
    editable: true,
    minWidth: 300,
    widthT: 300,
  },
  {
    field: 'maintenanceId',
    title: 'Maintenance ID',
    hidden: true,
    editable: false,
  },
  {
    field: 'maintStartDateTime',
    title: 'SD - From',
    editable: true,
    widthT: 200,
    minWidth: 180,
    type: 'dateTime',
  },
  {
    field: 'maintEndDateTime',
    title: 'SD - To',
    editable: true,
    widthT: 200,
    minWidth: 180,
    type: 'dateTime',
  },
  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
    widthT: 140,
    minWidth: 140,
  },
  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
    widthT: 250,
    minWidth: 250,
  },
]

// Initial field values for new rows added via the "Add Item" button
const initialFieldValues = {
  discription: '',
  maintenanceId: null,
  maintStartDateTime: null,
  maintEndDateTime: null,
  durationInHrs: '',
  remark: '',
  isEditable: true,
}

/**
 * Date ↔ Duration auto-calculation config.
 * AdvanceKendoTable's built-in applyDateCalculations handles all 5 scenarios:
 *   1. start + end change  → compute durationInHrs
 *   2. duration change + start exists → compute maintEndDateTime
 *   3. duration change + end exists (no start) → compute maintStartDateTime
 *   4. start changes + duration exists (no end) → compute maintEndDateTime
 *   5. end changes + duration exists (no start) → compute maintStartDateTime
 *
 * requiredInHr: true → duration string is in "HH.MM" format (hours + minutes)
 */
const dateCalculationConfig = {
  dateField1: 'maintStartDateTime',
  dateField2: 'maintEndDateTime',
  daysField: 'durationInHrs',
  requiredInHr: true,
}

// ─── Component ────────────────────────────────────────────────────────────────

const ShutdownActivitiesGrid = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year, screenTitle } =
    dataGridStore

  const PLANT_ID = plantObject?.id
  const PLANT_NAME = plantObject?.name?.toUpperCase()
  const SITE_NAME = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME = verticalObject?.name?.toUpperCase()
  const AOP_YEAR = year?.selectedYear

  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME}_${SITE_NAME}_${PLANT_NAME}`
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Shutdown_Plan')

  // ─── State ──────────────────────────────────────────────────────────────────
  const [rows, setRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // ─── Fetch ───────────────────────────────────────────────────────────────────

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    setModifiedCells({})
    try {
      const data = await ShutdownPlanApiService.getShutdownPlan(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      // API returns a plain array (not {code, data})
      const arr = Array.isArray(data) ? data : data?.data || []

      // Full month names in calendar order (Jan=0 … Dec=11)
      const CALENDAR_MONTH_NAMES = [
        'January',
        'February',
        'March',
        'April',
        'May',
        'June',
        'July',
        'August',
        'September',
        'October',
        'November',
        'December',
      ]

      const formatted = arr.map((item, index) => {
        const startDate = item?.maintStartDateTime
          ? new Date(item.maintStartDateTime)
          : null
        const endDate = item?.maintEndDateTime
          ? new Date(item.maintEndDateTime)
          : null

        return {
          ...item,
          idFromApi: item?.id,
          id: index,
          originalRemark: item.remark,
          inEdit: false,
          maintStartDateTime: startDate,
          maintEndDateTime: endDate,
          discription: item.discription,
        }
      })

      setRows(formatted)
    } catch (error) {
      console.error('Error fetching shutdown plan data:', error)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak])

  // ─── Initial load ────────────────────────────────────────────────────────────

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      setRows([])
      fetchData()
    }
  }, [PLANT_ID, AOP_YEAR])

  // ─── Save ─────────────────────────────────────────────────────────────────────

  const saveChanges = useCallback(async () => {
    const data = Object.values(modifiedCells)

    // 1. No records
    if (data.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
      return
    }

    // 2. Parse fiscal year boundary for date validation
    const yearStr = AOP_YEAR
    let startLimit, endLimit
    if (yearStr) {
      const [startYear, endYear] = yearStr
        .split('-')
        .map((y) => parseInt(y.trim(), 10))
      if (!isNaN(startYear) && !isNaN(endYear)) {
        startLimit = new Date(`${startYear}-04-01T00:00:00`)
        endLimit = new Date(`20${endYear}-03-31T23:59:59`)
      }
    }

    const formatDateDDMMYYYY = (date) => {
      if (!(date instanceof Date) || isNaN(date)) return ''
      const d = date.getDate().toString().padStart(2, '0')
      const m = (date.getMonth() + 1).toString().padStart(2, '0')
      return `${d}/${m}/${date.getFullYear()}`
    }

    // 3. Date validation: fiscal year bounds, start < end, required
    for (const record of data) {
      const startDate =
        record.maintStartDateTime instanceof Date
          ? record.maintStartDateTime
          : new Date(record.maintStartDateTime)
      const endDate =
        record.maintEndDateTime instanceof Date
          ? record.maintEndDateTime
          : new Date(record.maintEndDateTime)

      if (!record.maintStartDateTime || !record.maintEndDateTime) {
        record.isError = true
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Start Date and End Date are required for all records.',
          severity: 'error',
        })
        return
      }

      if (
        startLimit &&
        endLimit &&
        (isNaN(startDate) ||
          isNaN(endDate) ||
          startDate < startLimit ||
          startDate > endLimit ||
          endDate < startLimit ||
          endDate > endLimit)
      ) {
        record.isError = true
        setSnackbarOpen(true)
        setSnackbarData({
          message: `Dates must be between ${formatDateDDMMYYYY(startLimit)} and ${formatDateDDMMYYYY(endLimit)} for selected year.`,
          severity: 'error',
        })
        return
      }

      if (
        record.maintStartDateTime &&
        record.maintEndDateTime &&
        new Date(record.maintStartDateTime).getTime() >=
          new Date(record.maintEndDateTime).getTime()
      ) {
        record.isError = true
        setSnackbarOpen(true)
        setSnackbarData({
          message: `Start time must be before end time for "${record.discription || 'this record'}".`,
          severity: 'error',
        })
        return
      }
    }

    // 4. Check shutdown doesn't span multiple months
    const allRecords = [...rows]
    for (const row of allRecords) {
      const start = new Date(row.maintStartDateTime)
      const end = new Date(row.maintEndDateTime)
      if (isNaN(start.getTime()) || isNaN(end.getTime())) continue

      const isSameMonth =
        start.getMonth() === end.getMonth() &&
        start.getFullYear() === end.getFullYear()

      if (!isSameMonth) {
        row.isError = true
        const formatDate = (date) =>
          date.toLocaleDateString('en-GB', {
            day: '2-digit',
            month: 'short',
            year: 'numeric',
          })
        setSnackbarOpen(true)
        setSnackbarData({
          message: `The shutdown timeframe for '${row.discription}' spans multiple months (from ${formatDate(start)} to ${formatDate(end)}). Please split it into separate entries for each month.`,
          severity: 'error',
        })
        return
      }
    }

    // 5. Check for overlapping timeframes
    for (let i = 0; i < allRecords.length; i++) {
      const a = allRecords[i]
      const aStart = new Date(a.maintStartDateTime).getTime()
      const aEnd = new Date(a.maintEndDateTime).getTime()
      if (isNaN(aStart) || isNaN(aEnd)) continue

      for (let j = 0; j < allRecords.length; j++) {
        if (i === j) continue
        const b = allRecords[j]
        const bStart = new Date(b.maintStartDateTime).getTime()
        const bEnd = new Date(b.maintEndDateTime).getTime()
        if (isNaN(bStart) || isNaN(bEnd)) continue

        if (aStart < bEnd && bStart < aEnd) {
          a.isError = true
          b.isError = true
          setSnackbarOpen(true)
          setSnackbarData({
            message: `The shutdown timeframe for "${a.discription || b.discription || 'this record'}" overlaps with "${b.discription}". Please ensure no overlapping timeframes.`,
            severity: 'error',
          })
          return
        }
      }
    }

    // 6. Required fields: discription and remark
    for (const record of data) {
      if (!record.discription || String(record.discription).trim() === '') {
        record.isError = true
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Shutdown Desc is required for all records.',
          severity: 'error',
        })
        return
      }
      if (!record.remark || String(record.remark).trim() === '') {
        record.isError = true
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Please update the field: Shutdown Basis',
          severity: 'error',
        })
        return
      }
    }

    // 5. Duplicate description check across all rows
    const allDescriptions = rows.map((r) =>
      (r.discription || '').trim().toLowerCase(),
    )
    const duplicate = allDescriptions.find(
      (d, i) => d && allDescriptions.indexOf(d) !== i,
    )
    if (duplicate) {
      rows.forEach((row) => {
        row.isError = (row.discription || '').trim().toLowerCase() === duplicate
      })
      setSnackbarOpen(true)
      setSnackbarData({
        message: `The description "${duplicate}" already exists. Please enter a unique description.`,
        severity: 'error',
      })
      return
    }

    // 7. Build payload — send dates instead of month (PET vertical)
    const shutdownDetails = data.map((row) => ({
      discription: row.discription || row.discriptionDrpdwn,
      maintStartDateTime: addTimeOffset(row.maintStartDateTime),
      maintEndDateTime: addTimeOffset(row.maintEndDateTime),
      durationInHrs: (() => {
        const v = findDuration(row)
        if (!v) return null
        const [h = '00', m = '00'] = String(v).split('.')
        return `${h.padStart(2, '0')}.${m.padStart(2, '0')}`
      })(),
      audityear: AOP_YEAR,
      id: row.idFromApi || null,
      remark: row.remark || 'null',
    }))

    setLoading(true)
    try {
      await ShutdownPlanApiService.saveShutdownPlan(
        keycloak,
        PLANT_ID,
        shutdownDetails,
      )
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Saved Successfully!', severity: 'success' })
      await fetchData()
    } catch (error) {
      console.error('Error saving shutdown plan:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to save. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, rows, AOP_YEAR, PLANT_ID, keycloak, fetchData])

  // ─── Delete ───────────────────────────────────────────────────────────────────

  const deleteRowData = useCallback(
    async (dataItem) => {
      const { idFromApi, id } = dataItem

      // New row (not yet persisted) — just remove from local state
      if (!idFromApi) {
        setRows((prev) => prev.filter((row) => row.id !== id))
        setModifiedCells((prev) => {
          const next = { ...prev }
          delete next[id]
          return next
        })
        return
      }

      // Persisted row — call DELETE endpoint
      setLoading(true)
      try {
        await ShutdownPlanApiService.deleteShutdownActivity(
          keycloak,
          idFromApi,
          PLANT_ID,
        )
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Record Deleted Successfully!',
          severity: 'success',
        })
        await fetchData()
      } catch (error) {
        console.error('Error deleting shutdown activity:', error)
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Error deleting record!',
          severity: 'error',
        })
      } finally {
        setLoading(false)
      }
    },
    [keycloak, PLANT_ID, fetchData],
  )

  // ─── Import ───────────────────────────────────────────────────────────────────

  const handleExcelUpload = useCallback(
    async (file) => {
      if (!file) return
      setLoading(true)
      try {
        const response = await ShutdownPlanApiService.importShutdownNonProduct(
          file,
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )

        if (response?.code === 200) {
          setSnackbarOpen(true)
          setSnackbarData({
            message: response?.message || 'Uploaded Successfully!',
            severity: 'success',
          })
          setModifiedCells({})
          await fetchData()
        } else if (response?.code === 400 && response?.data) {
          // Partial save — download error Excel
          downloadBase64Excel(
            response.data,
            `Error File - ${MAINTENANCE_TYPE}.xlsx`,
          )
          setSnackbarOpen(true)
          setSnackbarData({
            message:
              response?.message || 'Partial data saved. Error file downloaded.',
            severity: 'warning',
          })
          await fetchData()
        } else {
          setSnackbarOpen(true)
          setSnackbarData({
            message: response?.message || 'Upload Failed!',
            severity: 'error',
          })
        }
      } catch (error) {
        console.error('Error importing shutdown plan:', error)
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Unexpected error during import.',
          severity: 'error',
        })
      } finally {
        setLoading(false)
      }
    },
    [keycloak, PLANT_ID, AOP_YEAR, fetchData],
  )

  // ─── Delete Selected ───────────────────────────────────────────────────────────────────

  const handleDeleteSelected = async (deleteIds) => {
    if (!deleteIds || deleteIds?.length === 0) return
    setLoading(true)
    try {
      await ShutdownPlanApiService.deleteMultipleShutdown(
        deleteIds,
        keycloak,
        PLANT_ID,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Record Deleted Successfully!',
        severity: 'success',
      })
      await fetchData()
    } catch (error) {
      console.error('Error deleting shutdown activity:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error deleting record!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  // ─── Export ───────────────────────────────────────────────────────────────────

  const handleExport = useCallback(async () => {
    setSnackbarOpen(true)
    setSnackbarData({ message: 'Excel download started!', severity: 'info' })
    try {
      await ShutdownPlanApiService.exportShutdownNonProduct(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_EXPORT_TITLE,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel downloaded successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting shutdown plan:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, EXCEL_EXPORT_TITLE])

  // ─── Remark dialog ────────────────────────────────────────────────────────────

  const handleRemarkCellClick = useCallback((row) => {
    setCurrentRemark(row.remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }, [])

  // ─── Permissions ──────────────────────────────────────────────────────────────

  const permissions = {
    allAction: true,
    showAction: true,
    addButton: true, // "Add Item" button — adds a blank row inline
    deleteButton: true, // trash icon on each editable row
    editButton: false,
    saveBtn: true,
    showImport: true, // Excel import button
    showExport: true, // Excel export (download) button
    showTitleNameBusiness: true,
    titleName: screenTitle?.title,
    showTitle: true,
    ExcelName: EXCEL_NAME,
    remarksEditable: true,
    marginBottom: true,
    deleteMultiple: true,
  }

  // ─── Render ───────────────────────────────────────────────────────────────────

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title={permissions.titleName}
        permissions={permissions}
        // Remark dialog wiring
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={() => {}}
        // Actions
        saveChanges={saveChanges}
        deleteRowData={deleteRowData}
        handleExcelUpload={handleExcelUpload}
        handleExport={handleExport}
        fetchData={fetchData}
        // New-row defaults
        initialFieldValues={initialFieldValues}
        // 3-field auto-calculation: start ↔ end ↔ duration
        dateCalculationConfig={dateCalculationConfig}
        // Snackbar
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        handleDeleteSelected={handleDeleteSelected}
        customHeight={70}
        screenType='shutdown'
      />
    </Box>
  )
}

export default ShutdownActivitiesGrid
