import { useState, useEffect, useCallback } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { ShutdownPlanApiService } from '../../services/polyester/shutdownPlanApiService'
import { PCGShutdownTaApiService } from '../../services/pcg/pcgShutdownTaApiService'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from '../../common/utilities/excelNameUtil'
import { downloadBase64Excel } from '../../common/utilities/downloadBase64Excel'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'

const MAINTENANCE_TYPE = 'Shutdown'

function addTimeOffset(dateTime) {
  if (!dateTime) return null
  const date = new Date(dateTime)
  date.setUTCHours(date.getUTCHours() + 5)
  date.setUTCMinutes(date.getUTCMinutes() + 30)
  return date
}

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

const columns = [
  {
    field: 'discription',
    title: 'Description',
    type: 'text',
    editable: true,
    minWidth: 250,
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
    title: 'SD From',
    editable: true,
    widthT: 200,
    minWidth: 180,
    type: 'dateTime',
  },
  {
    field: 'maintEndDateTime',
    title: 'SD To',
    editable: true,
    widthT: 200,
    minWidth: 180,
    type: 'dateTime',
  },
  {
    field: 'durationInHrs',
    title: 'Duration (Hrs)',
    editable: true,
    widthT: 150,
    minWidth: 140,
    type: 'negativeNumber',
  },
  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
    widthT: 250,
    minWidth: 250,
  },
]

const initialFieldValues = {
  discription: '',
  maintenanceId: null,
  maintStartDateTime: null,
  maintEndDateTime: null,
  durationInHrs: '',
  remark: '',
  isEditable: true,
}

const dateCalculationConfig = {
  dateField1: 'maintStartDateTime',
  dateField2: 'maintEndDateTime',
  daysField: 'durationInHrs',
  requiredInHr: true,
}

const ShutdownPlanSNG = () => {
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

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
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

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    setModifiedCells({})
    try {
      const data = await PCGShutdownTaApiService.getShutdownTaTransactionsSng(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      const arr = Array.isArray(data) ? data : data?.data || []

      const formatted = arr.map((item, index) => {
        let startDate = null
        if (item?.maintStartDateTime) {
          const parsed = new Date(item.maintStartDateTime)
          if (!isNaN(parsed.getTime())) startDate = parsed
        }
        let endDate = null
        if (item?.maintEndDateTime) {
          const parsed = new Date(item.maintEndDateTime)
          if (!isNaN(parsed.getTime())) endDate = parsed
        }

        let duration = ''
        if (
          item?.durationInHrs !== undefined &&
          item?.durationInHrs !== null &&
          item?.durationInHrs !== ''
        ) {
          duration = item.durationInHrs
        } else if (
          item?.durationInMins !== undefined &&
          item?.durationInMins !== null &&
          item?.durationInMins !== ''
        ) {
          const h = Math.floor(Number(item.durationInMins) / 60)
          const m = Number(item.durationInMins) % 60
          duration = `${h}.${String(m).padStart(2, '0')}`
        }

        const rawRemark = item?.remarks ?? item?.remark ?? ''
        const cleanRemark =
          rawRemark === 'null' || rawRemark === 'NULL' ? '' : rawRemark

        return {
          ...item,
          idFromApi: item?.id,
          id: index,
          discription: item?.description || item?.discription || '',
          description: item?.description || item?.discription || '',
          maintStartDateTime: startDate,
          maintEndDateTime: endDate,
          durationInHrs: duration,
          originalRemark: cleanRemark,
          inEdit: false,
          remark: cleanRemark,
          remarks: cleanRemark,
        }
      })

      setRows(formatted)
      setOriginalRows(formatted)
    } catch (error) {
      console.error('Error fetching shutdown plan data:', error)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak])

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      setRows([])
      fetchData()
    }
  }, [PLANT_ID, AOP_YEAR, fetchData])

  const saveChanges = useCallback(async () => {
    const data = Object.values(modifiedCells)

    if (data.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
      return
    }

    for (const record of data) {
      if (!record.discription || String(record.discription).trim() === '') {
        record.isError = true
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Description is required for all records.',
          severity: 'error',
        })
        return
      }
      if (!record.maintStartDateTime) {
        record.isError = true
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'SD From date & time is required for all records.',
          severity: 'error',
        })
        return
      }
      if (!record.maintEndDateTime) {
        record.isError = true
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'SD To date & time is required for all records.',
          severity: 'error',
        })
        return
      }

      const start = new Date(record.maintStartDateTime)
      const end = new Date(record.maintEndDateTime)

      if (start > end) {
        record.isError = true
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'SD To date must be greater than or equal to SD From date.',
          severity: 'error',
        })
        return
      }

      const effectiveEnd =
        end > start &&
        end.getDate() === 1 &&
        end.getHours() === 0 &&
        end.getMinutes() === 0 &&
        end.getSeconds() === 0 &&
        end.getMilliseconds() === 0
          ? new Date(end.getTime() - 1)
          : end

      const isSameMonth =
        start.getMonth() === effectiveEnd.getMonth() &&
        start.getFullYear() === effectiveEnd.getFullYear()

      if (!isSameMonth) {
        record.isError = true
        const formatDate = (date) =>
          date.toLocaleDateString('en-GB', {
            day: '2-digit',
            month: 'short',
            year: 'numeric',
          })
        setSnackbarOpen(true)
        setSnackbarData({
          message: `SD From and SD To must be in the same month. Record '${record.discription || 'Row'}' spans multiple months (${formatDate(start)} to ${formatDate(end)}). Please split it into separate entries for each month.`,
          severity: 'error',
        })
        return
      }
    }

    const fieldsToCheck = [
      'discription',
      'maintStartDateTime',
      'maintEndDateTime',
      'durationInHrs',
    ]
    const validationError = validateRowDataWithRemarks(
      data,
      originalRows,
      fieldsToCheck,
      'discription',
      'remark',
    )
    if (validationError) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: validationError,
        severity: 'error',
      })
      return
    }

    const shutdownDetails = data.map((row) => {
      const v = findDuration(row)
      let durationInHrs = null
      if (v) {
        const delimiter = String(v).includes(':') ? ':' : '.'
        const [h = '0', m = '0'] = String(v).split(delimiter)
        durationInHrs = parseFloat(
          `${parseInt(h, 10)}.${String(m).padEnd(2, '0').slice(0, 2)}`,
        )
      }

      return {
        id: row.idFromApi || null,
        name: row.name || null,
        description: row.discription,
        maintStartDateTime: addTimeOffset(row.maintStartDateTime),
        maintEndDateTime: addTimeOffset(row.maintEndDateTime),
        durationInHrs,
        auditYear: AOP_YEAR,
        remarks: row.remarks || row.remark || null,
        normParameterFKId: row.normParameterFKId || null,
        version: row.version || 'V1',
      }
    })

    setLoading(true)
    try {
      await PCGShutdownTaApiService.saveShutdownTaTransactionsSng(
        keycloak,
        PLANT_ID,
        shutdownDetails,
        AOP_YEAR,
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
  }, [modifiedCells, AOP_YEAR, PLANT_ID, keycloak, fetchData, originalRows])

  const deleteRowData = useCallback(
    async (dataItem) => {
      const { idFromApi, id } = dataItem

      if (!idFromApi) {
        setRows((prev) => prev.filter((row) => row.id !== id))
        setModifiedCells((prev) => {
          const next = { ...prev }
          delete next[id]
          return next
        })
        return
      }

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

  const handleExcelUpload = useCallback(
    async (file) => {
      if (!file) return
      setLoading(true)
      try {
        const response = await PCGShutdownTaApiService.importShutdownPlanSng(
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

  const handleDeleteSelected = async (deleteIds) => {
    const validIds = (deleteIds || []).filter(Boolean)
    if (validIds.length === 0) return
    setLoading(true)
    try {
      await ShutdownPlanApiService.deleteMultipleShutdown(
        validIds,
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

  const handleExport = useCallback(async () => {
    setSnackbarOpen(true)
    setSnackbarData({ message: 'Excel download started!', severity: 'info' })
    try {
      await PCGShutdownTaApiService.exportShutdownPlanSng(
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

  const handleRemarkCellClick = useCallback((row) => {
    setCurrentRemark(row.remarks ?? row.remark ?? '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }, [])

  const customHandleRemarkSave = useCallback(() => {
    setRows((prevRows) => {
      let updatedRow = null
      const updatedRows = prevRows.map((row) => {
        if (row.id === currentRowId) {
          updatedRow = {
            ...row,
            remark: currentRemark,
            remarks: currentRemark,
            inEdit: true,
          }
          return updatedRow
        }
        return row
      })
      if (updatedRow) {
        setModifiedCells((prev) => ({
          ...prev,
          [updatedRow.id]: updatedRow,
        }))
      }
      return updatedRows
    })
    setRemarkDialogOpen(false)
  }, [currentRowId, currentRemark])

  const permissions = {
    allAction: true,
    showAction: true,
    addButton: true,
    deleteButton: true,
    editButton: false,
    saveBtn: true,
    showImport: true,
    showExport: true,
    showTitleNameBusiness: true,
    titleName: screenTitle?.title || 'Shutdown Plan',
    showTitle: true,
    ExcelName: EXCEL_NAME,
    remarksEditable: true,
    marginBottom: true,
    deleteMultiple: true,
  }

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
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        customHandleRemarkSave={customHandleRemarkSave}
        saveChanges={saveChanges}
        deleteRowData={deleteRowData}
        handleExcelUpload={handleExcelUpload}
        handleExport={handleExport}
        fetchData={fetchData}
        initialFieldValues={initialFieldValues}
        dateCalculationConfig={dateCalculationConfig}
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

export default ShutdownPlanSNG
