import { useState, useCallback, useMemo } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { OutputApiService } from 'components/aop-phase-two/services/cpp/jmd/outputApiService'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { useDebounce } from 'hooks/useDebounce'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

const SpinningMarginReport = () => {
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

  // Month columns: each month has 2 sub-columns: Maintained | Target
  const MONTH_COLUMNS = useMemo(
    () =>
      MONTH_FIELDS.map((mon) => ({
        title: headerMap[MONTH_TO_INDEX[mon]], // e.g. "Apr-26"
        children: [
          {
            field: mon,
            title: 'Maintained',
            widthT: 130,
            minWidth: 130,
            type: 'number1',
            editable: false,
          },
          {
            field: `${mon}Target`,
            title: 'Target',
            widthT: 130,
            minWidth: 130,
            type: 'number1',
            editable: false,
          },
        ],
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
        editable: false,
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
      if (!rowsWithId.length) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
      }
    } catch (error) {
      console.error('Error fetching Spinning Margin report data:', error)
      setRows([])
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, AOP_YEAR])

  useDebounce(
    () => {
      if (AOP_YEAR) {
        fetchData()
      }
    },
    1000,
    [AOP_YEAR, fetchData],
  )

  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: false,
    saveBtn: false,
    allAction: true,
    showTitleNameBusiness: true,
    titleName: screenTitle?.title,
    showImport: false,
    showExport: true,
    ExcelName: EXCEL_NAME,
    showTitle: true,
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
        true, // show maintained columns for report export
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
        title='Spinning Margin Maintained'
        permissions={permissions}
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

export default SpinningMarginReport
