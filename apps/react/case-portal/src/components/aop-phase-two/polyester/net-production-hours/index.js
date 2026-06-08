import { useState, useEffect, useCallback } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { generateHeaderNames } from '../../common/utilities/generateHeaders'
import ValueFormatterPhaseTwo from '../../common/ValueFormatterPhaseTwo'
import { NetProductionHoursApiService } from '../../services/polyester/netProductionHoursApiService'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

const monthFields = [
  'April',
  'May',
  'June',
  'July',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
  'Jan',
  'Feb',
  'Mar',
]

const NetProductionHours = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year } = dataGridStore
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Net_Production_Hours')
  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear

  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const valueFormat = ValueFormatterPhaseTwo()
  const headerMap = generateHeaderNames(AOP_YEAR)

  const columns = [
    {
      field: 'Name',
      title: 'Description',
      widthT: 250,
      minWidth: 200,
      type: 'text',
      editable: false,
      locked: true,
    },
    ...monthFields.map((field, i) => {
      const monthIndexMap = {
        April: 4,
        May: 5,
        June: 6,
        July: 7,
        Aug: 8,
        Sep: 9,
        Oct: 10,
        Nov: 11,
        Dec: 12,
        Jan: 1,
        Feb: 2,
        Mar: 3,
      }
      return {
        field,
        title: headerMap[monthIndexMap[field]],
        widthT: 110,
        minWidth: 110,
        type: 'number1',
        editable: false,
        format: valueFormat,
      }
    }),
    {
      field: 'allMonthsTotal',
      title: 'Total Hrs',
      widthT: 110,
      minWidth: 110,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
  ]

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setRows([])
    setLoading(true)
    try {
      let resp = await NetProductionHoursApiService.getNetProductionHours(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      const formatted = (resp || []).map((item, idx) => {
        const allMonthsTotal = monthFields.reduce((sum, month) => {
          return sum + (parseFloat(item[month]) || 0)
        }, 0)

        return {
          ...item,
          idFromApi: item.id,
          id: idx,
          isEditable: false,
          originalRemark: item.remarks,
          allMonthsTotal,
        }
      })

      setRows(formatted)
    } catch (error) {
      console.error('Error fetching net production hours data:', error)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel export started!',
      severity: 'info',
    })

    try {
      await NetProductionHoursApiService.exportNetProductionHours(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_NAME,
      )
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting net production hours data:', error)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    } finally {
      setSnackbarOpen(true)
    }
  }

  const permissions = {
    showAction: false,
    addButton: false,
    deleteButton: false,
    editButton: false,
    saveBtn: false,
    allAction: true,
    showExport: false,
    downloadExcelBtnFromUI: true,
    ExcelName: EXCEL_NAME,
    showImport: false,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Net Production Hours',
    showDropdown: false,
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
        title={permissions.showTitle ? permissions.titleName : ''}
        permissions={permissions}
        handleExport={handleExport}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        paginationConfig={{
          threshold: 100,
          buttonCount: 5,
          pageSizes: [10, 20, 50, 100],
          defaultPageSize: 100,
        }}
      />
    </Box>
  )
}

export default NetProductionHours
