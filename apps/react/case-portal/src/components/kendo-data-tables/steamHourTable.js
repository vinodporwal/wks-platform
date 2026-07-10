import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'

import { MaintenanceDetailsApiService } from 'services/maintenance-details-api-service'
import { useSession } from 'SessionStoreContext'
import { validateFields } from 'utils/validationUtils'
import crackercolumns from '../../assets/CrackerMaintenanceColumn.json'
import crackercolumnsDMD from '../../assets/CrackerMaintenanceColumn_DMD.json'
import KendoDataTables from './index'
import { getRoleName } from 'services/role-service'
import MaintenanceProcessTableNMD from './processTableNMD'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { ProductionRangeApiService } from 'services/production-range-api-service copy'
// --- Month fields used throughout the component ----------------------------
const STEAM_MONTHS = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
]

const DERIVED_METRICS = ['Total S/D Hours', 'Net Operating Hrs']

const computeCalculatedRows = (currentRows) => {
  if (!currentRows || currentRows.length === 0) return currentRows

  // Group rows by section (Particulars = SectionName)
  const sectionMap = {}
  currentRows.forEach((row) => {
    const key = row.Particulars || row.SectionName || ''
    if (!sectionMap[key]) sectionMap[key] = []
    sectionMap[key].push(row)
  })

  return currentRows.map((row) => {
    const sectionKey = row.Particulars || row.SectionName || ''
    const sectionRows = sectionMap[sectionKey] || []
    const metricName = (row.Metric || '').trim()

    // Helper: find a row by metric name (exact trim match or predicate) and return its month value
    const getVal = (matcherOrString, month) => {
      const found = sectionRows.find((r) =>
        typeof matcherOrString === 'function'
          ? matcherOrString((r.Metric || '').trim())
          : (r.Metric || '').trim() === matcherOrString,
      )
      return parseFloat(found?.[month]) || 0
    }

    if (metricName === 'Total S/D Hours') {
      const updated = { ...row, Remarks: '' }
      STEAM_MONTHS.forEach((month) => {
        const routine = getVal('Routine shutdown Duration', month)
        const planned = getVal(
          (m) => m.startsWith('Planned SD other than Turnaround'),
          month,
        )
        const turnAround = getVal('Turn around duration', month)
        updated[month] = routine + planned + turnAround
      })
      return updated
    }

    if (metricName === 'Net Operating Hrs') {
      const updated = { ...row, Remarks: '' }
      STEAM_MONTHS.forEach((month) => {
        const totalAvail = getVal('Total available hours', month)
        const routine = getVal('Routine shutdown Duration', month)
        const planned = getVal(
          (m) => m.startsWith('Planned SD other than Turnaround'),
          month,
        )
        const turnAround = getVal('Turn around duration', month)
        const totalSD = routine + planned + turnAround
        updated[month] = totalAvail - totalSD
      })
      return updated
    }

    return row
  })
}

const MaintenanceProcessTable = ({ viewOnly }) => {
  const keycloak = useSession()

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    yearChanged,
    oldYear,
    plantID,
    plantObject,
    siteObject,
    verticalObject,
    year,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear
  const plantName = plantObject?.name?.toLowerCase()
  const siteName = siteObject?.name?.toLowerCase()
  const lowerVertName = verticalObject?.name?.toLowerCase()

  const PLANT_NAME_UPPERCASE = plantObject?.name
  const SITE_NAME_UPPERCASE = siteObject?.name
  const VERTICAL_NAME_UPPERCASE = verticalObject?.name

  const EXCEL_NAME = `${VERTICAL_NAME_UPPERCASE}_${SITE_NAME_UPPERCASE}_${PLANT_NAME_UPPERCASE}_Stream_Hours_Details_${AOP_YEAR}`

  const IS_OLD_YEAR = oldYear?.oldYear
  const isOldYear = false
  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const dataConfig = useMemo(
    () => ({
      serviceFn: () =>
        MaintenanceDetailsApiService.getSteamHoursData(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        ),
    }),
    [keycloak, PLANT_ID, AOP_YEAR],
  )

  const headerMap = generateHeaderNames(AOP_YEAR)
  const [columns, setColumns] = useState([])
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)

  // Wrapper around setRows that automatically recalculates derived rows
  // (Total S/D Hours, Net Operating Hrs) after every mutation.
  const setRowsWithCalculation = useCallback((updater) => {
    setRows((prev) => {
      const next = typeof updater === 'function' ? updater(prev) : updater
      return computeCalculatedRows(next)
    })
  }, [])

  const handleCustomItemChange = useCallback(
    (
      e,
      {
        setModifiedCells: setMod,
        setCustomModifiedCells: setCustomMod,
        rows: currentRows,
      },
    ) => {
      const { dataItem, field, value } = e
      const section = dataItem.Particulars || dataItem.SectionName || ''

      // Apply the edit, then recompute ? filter to derived rows in the same section only
      const afterEdit = currentRows.map((r) =>
        r.id === dataItem.id ? { ...r, [field]: value } : r,
      )
      const derivedRows = computeCalculatedRows(afterEdit).filter(
        (r) =>
          (r.Particulars || r.SectionName) === section &&
          DERIVED_METRICS.includes((r.Metric || '').trim()),
      )

      if (derivedRows.length === 0) return

      // Build both state updates in one pass
      const modUpdate = {}
      const customUpdate = {}
      derivedRows.forEach((row) => {
        modUpdate[row.id] = { ...row, inEdit: true }
        customUpdate[row.id] = { [field]: row[field] }
      })

      setMod((prev) => ({ ...prev, ...modUpdate }))
      setCustomMod((prev) => {
        const next = { ...prev }
        derivedRows.forEach((row) => {
          next[row.id] = { ...(prev[row.id] || {}), ...customUpdate[row.id] }
        })
        return next
      })
    },
    [],
  )

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
  const [calculationObject, setCalculationObject] = useState([])
  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    if (
      row?.IsEditable === 0 ||
      row?.isEditable === 0 ||
      row?.IsEditable === '0' ||
      row?.isEditable === '0' ||
      row?.IsEditable === false ||
      row?.isEditable === false
    ) {
      return
    }

    setCurrentRemark(row.Remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }
  function isLeapYear(yearStr) {
    // yearStr is like "2025-26"
    if (!yearStr) return false
    const year = parseInt(yearStr.split('-')[0], 10)
    if (!year) return false
    return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0
  }
  const getNextRemark = (originalRemark) => {
    const remark = (originalRemark || '').trim()
    if (!remark) {
      return 'V1'
    }
    const match = remark.match(/(.*?)\s*[vV](\d+)$/)
    if (match) {
      const base = match[1].trim()
      const version = parseInt(match[2], 10)
      const nextVersion = version + 1
      return base ? `${base} V${nextVersion}` : `V${nextVersion}`
    } else {
      return `${remark} V1`
    }
  }

  const saveChanges = useCallback(async () => {
    try {
      setLoading(true)
      if (Object.keys(modifiedCells).length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
        setLoading(false)
        return
      }

      const rawData = Object.values(modifiedCells)
      const processedData = rawData
        .filter((row) => row.inEdit)
        .map((row) => {
          const metricName = (row.Metric || '').trim()
          const isTargetMetric = ['Total S/D Hours', 'Net Operating Hrs'].includes(metricName)
          if (isTargetMetric) {
            const nextRemark = getNextRemark(row.originalRemark)
            return {
              ...row,
              isEditable: true,
              Remarks: nextRemark,
            }
          }
          return row
        })
        .filter((row) => row.isEditable == true || row.isEditable === 1)

      if (processedData.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
        setLoading(false)
        return
      }

      // --- MONTHLY SUM VALIDATION (move here) ---
      // 1. Filter the data to only include non-target editable rows for validation
      const editableRowsForValidation = processedData.filter((row) => {
        const metricName = (row.Metric || '').trim()
        const isTargetMetric = ['Total S/D Hours', 'Net Operating Hrs'].includes(metricName)
        return !isTargetMetric
      })

      // 2. Run the validation only on those filtered rows
      const validationMessage = validateFields(editableRowsForValidation, ['Remarks'])

      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({ message: validationMessage, severity: 'error' })
        setLoading(false)
        return
      }
      await saveStreamHoursData(processedData)
    } catch (err) {
      console.error('Save Stream Hours Data Error:', err)
    } finally {
      setLoading(false)
    }
  }, [modifiedCells])

  const saveStreamHoursData = async (newRows) => {
    setLoading(true)
    try {
      const months = [
        'Jan',
        'Feb',
        'Mar',
        'Apr',
        'May',
        'Jun',
        'Jul',
        'Aug',
        'Sep',
        'Oct',
        'Nov',
        'Dec',
      ]

      let payload = newRows.map((row) => {
        const obj = {
          auditYear: AOP_YEAR,
          normParameterFKId: row.NormParmId,
          remarks: row.Remarks || '00',
          id: null,
          UOM: '',
          isEditable: row.isEditable === true || row.isEditable === 1,
        }

        months.forEach((m) => {
          const lowerMonth = m.toLowerCase()
          obj[lowerMonth] = row[m] !== undefined ? parseFloat(row[m]) || 0 : 0
        })

        return obj
      })

      const response = await ProductionRangeApiService.postData(
        keycloak,
        payload,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Saved successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Error saving data!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error saving data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const FORMATE_DECIMAL = ValueFormatterProduction()

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) {
      setRows([])
      return
    }
    setLoading(true)
    try {
      const resp = await dataConfig.serviceFn(keycloak)
      const raw = resp.data?.data
      setCalculationObject(resp?.data?.aopCalculation)
      const hiddenFields = [
        'DisplayOrder',
        'IsEditable',
        'NormParamId',
        'SectionName',
        'NormParmId',
        'SectionOrder',
      ]
      const months = [
        'Jan',
        'Feb',
        'Mar',
        'Apr',
        'May',
        'Jun',
        'Jul',
        'Aug',
        'Sep',
        'Oct',
        'Nov',
        'Dec',
      ]
      const dynamicColumns = (resp.data?.columns || columns).map((col) => ({
        ...col,
        align: months.includes(col.field) ? 'right' : 'left',
        editable: months.includes(col.field) ? true : col.editable,
        hidden: hiddenFields.includes(col.field) ? true : col.hidden,
        type: months.includes(col.field) ? 'number' : undefined,
        format: months.includes(col.field) ? FORMATE_DECIMAL : undefined,
        isVisible: hiddenFields.includes(col.field) ? false : true,
        minWidth: col.field == 'Metric' ? 300 : 100,
      }))

      setColumns(dynamicColumns)

      const formatted = (raw || []).map((item, idx) => {
        const newItem = { ...item }
        months.forEach((month) => {
          if (newItem[month]) {
            newItem[month] = parseFloat(newItem[month]) || 0
          }
        })
        if (newItem.Total) {
          newItem.Total = parseFloat(newItem.Total) || 0
        }
        return {
          ...newItem,
          idFromApi: item.Id,
          id: idx,
          isEditable: item?.IsEditable,
          originalRemark: item?.Remarks?.trim(),
          Particulars: item.SectionName,
        }
      })

      const finalData = [...formatted]

      setRows(computeCalculatedRows(finalData))
    } catch (err) {
      console.error('Error fetching data:', err)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, keycloak, AOP_YEAR])

  useEffect(() => {
    fetchData()
  }, [fetchData, oldYear, yearChanged, PLANT_ID, AOP_YEAR])

  const downloadExcelForConfiguration = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })

    try {
      let response
      response = await MaintenanceDetailsApiService.StreamHoursExport(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_NAME,
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

  const uploadMaintenance = async (rawFile) => {
    setLoading(true)

    try {
      let response

      response = await MaintenanceDetailsApiService.saveStreamHoursImport(
        rawFile,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Uploaded Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData()
      } else if (response?.code === 400 && response?.data) {
        const byteCharacters = atob(response.data)
        const byteNumbers = Array.from(byteCharacters, (char) =>
          char.charCodeAt(0),
        )
        const byteArray = new Uint8Array(byteNumbers)

        const blob = new Blob([byteArray], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        })

        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', 'Error File - Stream Hours Details.xlsx')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
          severity: 'error',
        })
      }

      return response
    } catch (error) {
      console.error('Error uploading Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleExcelUpload = (rawFile) => {
    uploadMaintenance(rawFile)
  }
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
      minWidth: 100,
    }))
  }

  // Shared editable field
  const isEditableField = {
    field: 'isEditable',
    title: 'isEditable',
    hidden: true,
    minWidth: 100,
    isVisible: false,
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
      uploadExcelBtn: false,
      downloadExcelBtn: false,
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
          saveBtn: true,
          allAction: true,
          downloadExcelBtn: false,
          uploadExcelBtn: false,
          showRefresh: false,
          showCalculate: false,
          showCalculateVisibility: true,

          //BUTTON SHOULD BE DISABLED FOR NOW , LATER WE NEED TO CHANGE THE LOGIC
          // showCalculateVisibility: false,

          showNote: true,
        },
        isOldYear,
      ),
    [isOldYear],
  )

  return (
    <div>
      <Backdrop
        open={loading}
        sx={{ color: '#fff', zIndex: (t) => t.zIndex.drawer + 1 }}
      >
        <CircularProgress color='inherit' />
      </Backdrop>
      <KendoDataTables
        columns={columns}
        rows={rows}
        setRows={setRowsWithCalculation}
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
        saveChanges={saveChanges}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        // supressGridHeight={true}
        handleExcelUpload={handleExcelUpload}
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        groupBy='Particulars'
        customItemChange={handleCustomItemChange}
      />
    </div>
  )
}
export default MaintenanceProcessTable
