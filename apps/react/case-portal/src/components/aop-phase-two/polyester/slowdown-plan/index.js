import { useState, useEffect, useCallback } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { SlowdownPlanApiService } from '../../services/polyester/slowdownPlanApiService'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from '../../common/utilities/excelNameUtil'
import { downloadBase64Excel } from '../../common/utilities/downloadBase64Excel'

// ─── Constants ────────────────────────────────────────────────────────────────
const MAINTENANCE_TYPE = 'Slowdown'

// ─── Month options (April → March fiscal order) ───────────────────────────────
const MONTH_OPTIONS = [
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
  'January',
  'February',
  'March',
]

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Format a duration string to "HH.MM" with zero-padding */
function formatDuration(row) {
  const v = row.durationInHrs
  if (!v && v !== 0) return null
  const [h = '00', m = '00'] = String(v).split('.')
  return `${h.padStart(2, '0')}.${m.padStart(2, '0')}`
}

/** Parse fiscal year string "2026-27" → { startLimit, endLimit } */
function parseFiscalYear(yearStr) {
  if (!yearStr || !yearStr.includes('-')) return null
  const parts = yearStr.split('-')
  const startYear = parseInt(parts[0], 10)
  const endYearShort = parseInt(parts[1], 10)
  const endYear = endYearShort < 100 ? 2000 + endYearShort : endYearShort
  if (isNaN(startYear) || isNaN(endYear)) return null
  return {
    startLimit: new Date(`${startYear}-04-01T00:00:00`),
    endLimit: new Date(`${endYear}-03-31T23:59:59`),
  }
}

// ─── Column definitions (PE vertical, NMD site — mirrors SlowDownPeColumns) ──
const columns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    type: 'text',
    editable: true,
    minWidth: 250,
    widthT: 250,
  },
  {
    field: 'maintenanceId',
    title: 'Maintenance ID',
    hidden: true,
    editable: false,
  },
  {
    // Particulars / Grade — select input dropdown
    field: 'productName1',
    title: 'Particulars',
    type: 'select',
    editable: true,
    widthT: 160,
    minWidth: 160,
  },
  {
    field: 'monthly',
    title: 'Month',
    type: 'select',
    editable: true,
    widthT: 130,
    minWidth: 130,
  },
  {
    // Auto-handled by AdvanceKendoTable: field name includes 'durationInHrs'
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
    widthT: 140,
    minWidth: 140,
  },
  {
    field: 'rate',
    title: 'Reduced Rate (TPH)',
    type: 'number',
    editable: true,
    widthT: 160,
    minWidth: 160,
  },
  {
    // Auto-handled by AdvanceKendoTable: field name is 'remark' → RemarkCell dialog
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
  productName1: '',
  monthly: '',
  durationInHrs: '',
  rate: '',
  remark: '',
  isEditable: true,
}

// ─── Component ────────────────────────────────────────────────────────────────

const SlowdownPlan = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year, screenTitle } =
    dataGridStore

  const PLANT_ID = plantObject?.id
  const PLANT_NAME = plantObject?.name?.toUpperCase()
  const SITE_NAME = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME = verticalObject?.name?.toUpperCase()
  const AOP_YEAR = year?.selectedYear

  const EXCEL_NAME = generateExcelName(dataGridStore, 'Slowdown_Plan')

  // ─── State ──────────────────────────────────────────────────────────────────
  const [rows, setRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  // Remark dialog
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // ─── Dynamic column options (month + product) ─────────────────────────────

  const columnsWithOptions = columns.map((col) => {
    if (col.field === 'monthly') {
      return {
        ...col,
        options: MONTH_OPTIONS.map((m) => ({ value: m, label: m })),
      }
    }
    if (col.field === 'productName1') {
      return {
        ...col,
        options: products.map((p) => ({
          value: p.displayName,
          label: p.displayName,
        })),
      }
    }
    return col
  })

  // ─── Fetch ────────────────────────────────────────────────────────────────

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    setModifiedCells({})
    try {
      const data = await SlowdownPlanApiService.getSlowdownActivities(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      // API returns a plain array (or {code, data})
      const arr = Array.isArray(data) ? data : data?.data || []

      const formatted = arr.map((item, index) => ({
        ...item,
        idFromApi: item?.maintenanceId || item?.id,
        id: `${index}`,
        originalRemark: item.remark,
        productName1: item.productName || item.productName1 || '',
        monthly: item.monthly || item.month || '',
        inEdit: false,
        isEditable: true,
      }))

      setRows(formatted)
    } catch (error) {
      console.error('Error fetching slowdown plan data:', error)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak])

  // ─── Fetch Products ──────────────────────────────────────────────────────

  const fetchProducts = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      let res = await SlowdownPlanApiService.getGrades(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      const list = Array.isArray(res) ? res : res?.data || []
      const productList = list.map((product) => ({
        id: product.displayName,
        displayName: product.displayName,
        realId: product.id,
      }))
      setProducts(productList)
    } catch (error) {
      console.error('Error fetching grades:', error)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak])

  // ─── Initial load ────────────────────────────────────────────────────────

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      setRows([])
      fetchData()
      fetchProducts()
    }
  }, [PLANT_ID, AOP_YEAR, fetchData, fetchProducts])

  // ─── Save ─────────────────────────────────────────────────────────────────

  const saveChanges = useCallback(async () => {
    const data = Object.values(modifiedCells)

    // 1. No records
    if (data.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
      return
    }

    // 2. Parse fiscal year boundary (for optional future use — PE slowdown
    //    uses month field instead of date range, so we just validate month here)

    // 3. Required fields: discription, productName1, monthly, durationInHrs, rate, remark
    const requiredFields = [
      { field: 'discription', label: 'Slowdown Desc' },
      { field: 'productName1', label: 'Particulars' },
      { field: 'monthly', label: 'Month' },
      { field: 'durationInHrs', label: 'Duration (hrs)' },
      { field: 'rate', label: 'Reduced Rate (TPH)' },
      { field: 'remark', label: 'Remarks' },
    ]

    for (const record of data) {
      for (const { field, label } of requiredFields) {
        const value = record[field]
        if (
          value === null ||
          value === undefined ||
          (typeof value === 'string' && value.trim() === '')
        ) {
          record.isError = true
          setSnackbarOpen(true)
          setSnackbarData({
            message: `"${label}" is required for all records.`,
            severity: 'error',
          })
          return
        }
      }
    }

    // 4. Duplicate description check across all rows
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

    // 5. Build payload  (mirrors slowDownDetailsPEPP from Slowdown.js)
    const slowdownDetails = data.map((row) => {
      const matched = products.find((p) => p.displayName === row.productName1)
      return {
        id: row.idFromApi || null,
        productId: matched?.realId || null,
        productName: row.productName1 || null,
        discription: row.discription || '',
        durationInHrs: formatDuration(row),
        month: row.monthly || null,
        remark: row.remark || '',
        rate: row.rate ?? null,
        audityear: AOP_YEAR,
        rateEO: null,
        rateEOE: null,
      }
    })

    setLoading(true)
    try {
      await SlowdownPlanApiService.saveSlowdownActivities(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        slowdownDetails,
      )
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Saved Successfully!', severity: 'success' })
      await fetchData()
    } catch (error) {
      console.error('Error saving slowdown plan:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to save. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, rows, AOP_YEAR, PLANT_ID, keycloak, fetchData, products])

  // ─── Delete ───────────────────────────────────────────────────────────────

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
        await SlowdownPlanApiService.deleteSlowdownActivity(
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
        console.error('Error deleting slowdown activity:', error)
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Error deleting record!',
          severity: 'error',
        })
      } finally {
        setLoading(false)
      }
    },
    [keycloak, fetchData],
  )

  // ─── Import ───────────────────────────────────────────────────────────────

  const handleExcelUpload = useCallback(
    async (file) => {
      if (!file) return
      setLoading(true)
      try {
        const response = await SlowdownPlanApiService.importSlowdownActivities(
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
        console.error('Error importing slowdown plan:', error)
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

  // ─── Export ───────────────────────────────────────────────────────────────

  const handleExport = useCallback(async () => {
    setSnackbarOpen(true)
    setSnackbarData({ message: 'Excel download started!', severity: 'info' })
    try {
      await SlowdownPlanApiService.exportSlowdownActivities(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_NAME,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel downloaded successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting slowdown plan:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  // ─── Remark dialog ────────────────────────────────────────────────────────

  const handleRemarkCellClick = useCallback((row) => {
    setCurrentRemark(row.remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }, [])

  // ─── Permissions ──────────────────────────────────────────────────────────

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
    titleName: screenTitle?.title,
    showTitle: true,
    ExcelName: EXCEL_NAME,
    remarksEditable: true,
    marginBottom: true,
  }

  // ─── Render ───────────────────────────────────────────────────────────────

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <AdvanceKendoTable
        columns={columnsWithOptions}
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
        // Snackbar
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        customHeight={70}
      />
    </Box>
  )
}

export default SlowdownPlan
