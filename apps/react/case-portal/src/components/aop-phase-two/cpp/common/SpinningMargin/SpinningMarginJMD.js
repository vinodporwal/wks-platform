import { useState, useCallback, useMemo, useEffect } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { OutputApiService } from 'components/aop-phase-two/services/cpp/jmd/outputApiService'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { useDebounce } from 'hooks/useDebounce'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'

const SpinningMarginJMD = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    plantObject,
    siteObject,
    year,
    screenTitle,
    jmdSelectedPlants,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const lowerSiteName = siteObject?.name?.toLowerCase()
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Spinning_Margin_Report')

  // Build plant ID list: JMD uses all selected plants, otherwise single plant (or null for all)
  const PLANT_ID_LIST = useMemo(
    () =>
      lowerSiteName === 'jmd'
        ? jmdSelectedPlants?.map((plant) => plant.id) || []
        : PLANT_ID
        ? [PLANT_ID]
        : [],
    [jmdSelectedPlants, lowerSiteName, PLANT_ID],
  )

  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const headerMap = useMemo(() => generateHeaderNames(AOP_YEAR), [AOP_YEAR])

  // Fiscal-year month order: Apr → Mar
  const MONTH_TO_INDEX = {
    apr: 4,
    may: 5,
    jun: 6,
    jul: 7,
    aug: 8,
    sep: 9,
    oct: 10,
    nov: 11,
    dec: 12,
    jan: 1,
    feb: 2,
    mar: 3,
  }

  const MONTH_FIELDS = [
    'apr',
    'may',
    'jun',
    'jul',
    'aug',
    'sep',
    'oct',
    'nov',
    'dec',
    'jan',
    'feb',
    'mar',
  ]

  const TARGET_FIELDS = [
    'aprTarget',
    'mayTarget',
    'junTarget',
    'julTarget',
    'augTarget',
    'sepTarget',
    'octTarget',
    'novTarget',
    'decTarget',
    'janTarget',
    'febTarget',
    'marTarget',
  ]

  // Month columns (Apr → Mar) using generateHeaderNames format (e.g., Apr-26)
  const MONTH_COLUMNS = useMemo(
    () =>
      MONTH_FIELDS.map((mon) => ({
        field: `${mon}Target`,
        title: headerMap[MONTH_TO_INDEX[mon]], // e.g. "Apr-26"
        widthT: 130,
        minWidth: 130,
        type: 'number1',
        editable: true,
      })),
    [headerMap],
  )

  // Base columns (read-only for output grid)
  const baseColumns = useMemo(
    () => [
      {
        field: 'plantName',
        title: 'Plant Name',
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'marginType',
        title: 'Type',
        widthT: 150,
        minWidth: 150,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'utilityName',
        title: 'Utility Name',
        widthT: 180,
        minWidth: 180,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'utilityCode',
        title: 'Utility Code',
        widthT: 150,
        minWidth: 150,
        type: 'text',
        editable: false,
        locked: true,
      },
    ],
    [],
  )

  const trailingColumns = useMemo(
    () => [
      {
        field: 'remarks',
        title: 'Remarks',
        widthT: 250,
        minWidth: 250,
        type: 'textarea',
        editable: true,
      },
      {
        field: 'modifiedBy',
        title: 'Modified By',
        widthT: 180,
        minWidth: 180,
        type: 'text',
        editable: false,
        hidden:true
      },
    ],
    [],
  )

  // Full column list: base columns + 12 monthly columns (each with Maintained | Target) + remarks & modifiedBy
  const columns = useMemo(
    () => [...baseColumns, ...MONTH_COLUMNS, ...trailingColumns],
    [baseColumns, MONTH_COLUMNS, trailingColumns],
  )

  const fetchData = useCallback(async () => {
    if (!AOP_YEAR) return
    setLoading(true)
    try {
      const response = await OutputApiService.getSpinningMarginReport(
        keycloak,
        null,
        AOP_YEAR,
      )
      const data = response?.data || []
      const rowsWithId = data?.map((row, index) => ({
        ...row,
        id: row.id || `row_${index}`,
        // Rows with no plant id are common to all plants
        plantName:
          row.plantFkId == null ? 'Combined/All Plant' : row.plantName || '',
        utilityName: row.utilityName || '',
        utilityCode: row.utilityCode || '',
        remarks: row.remarks || '',
      }))
      setRows(rowsWithId)
      setOriginalRows(rowsWithId)
      if (!rowsWithId.length) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
      }
    } catch (error) {
      console.error('Error fetching Spinning Margin report data:', error)
      setRows([])
      setOriginalRows([])
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR])

  useDebounce(
    () => {
      if (AOP_YEAR) {
        fetchData()
      }
    },
    1000,
    [AOP_YEAR, fetchData],
  )

  useEffect(() => {
    setModifiedCells({})
  }, [AOP_YEAR, PLANT_ID_LIST])

  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: false,
    saveBtn: true,
    allAction: true,
    showTitleNameBusiness: true,
    titleName: screenTitle?.title,
    showImport: true,
    showExport: true,
    ExcelName: EXCEL_NAME,
    showTitle: true,
  }

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveChanges = async () => {
    setLoading(true)
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      setLoading(false)
      return
    }

    const data = modifiedData.filter((row) => row.inEdit)
    if (data.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      setLoading(false)
      return
    }

    const validationError = validateRowDataWithRemarks(
      data,
      originalRows,
      TARGET_FIELDS,
      'plantName',
    )

    if (validationError) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: validationError,
        severity: 'error',
      })
      setLoading(false)
      return
    }

    try {
      const payload = data.map((item) => {
        const { inEdit, ...rest } = item
        return rest
      })

      const response = await OutputApiService.saveSpinningMarginReport(
        keycloak,
        PLANT_ID_LIST?.length ? PLANT_ID_LIST : null,
        AOP_YEAR,
        payload,
      )

      if (response?.code === 200 || response?.code === 207) {
        setModifiedCells({})
        setSnackbarOpen(true)
        setSnackbarData({
          message:
            response?.message || `Successfully saved ${data.length} change(s)!`,
          severity: response?.code === 207 ? 'warning' : 'success',
        })
        await fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Failed to save changes.',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error saving Spinning Margin report data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to save changes. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleExcelUpload = async (file) => {
    if (!file) return
    setLoading(true)
    try {
      const response = await OutputApiService.importSpinningMarginReportExcel(
        file,
        keycloak,
        null,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message:
            response?.message || 'Excel file imported successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        await fetchData()
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
        const a = document.createElement('a')
        a.href = url
        a.download = `ERROR_${EXCEL_NAME}`
        document.body.appendChild(a)
        a.click()
        a.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message:
            response?.message ||
            'Some records failed to import. Error file downloaded.',
          severity: 'warning',
        })
        setModifiedCells({})
        await fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Failed to import Excel file.',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error importing Spinning Margin report file:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to import Excel file. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'info',
    })
    try {
      await OutputApiService.exportSpinningMarginReportExcel(
        keycloak,
        null,
        AOP_YEAR,
        EXCEL_NAME,
        false, // hide maintained columns for input export
      )
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting Spinning Margin report data:', error)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
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
        title='Spinning Margin'
        permissions={permissions}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        saveChanges={saveChanges}
        handleExcelUpload={handleExcelUpload}
        handleExport={handleExport}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        pagable={false}
      />
    </Box>
  )
}

export default SpinningMarginJMD
