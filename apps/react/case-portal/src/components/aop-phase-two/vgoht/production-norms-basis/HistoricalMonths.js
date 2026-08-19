import React, { useState, useEffect } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo, {
  customValueFormatterPhaseTwo,
} from '../../common/ValueFormatterPhaseTwo'
import { ProductionNormsApiService } from '../../services/vgoht/productionNormsApiService'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import RowBasedKendoTable from 'components/aop-phase-two/common/RowBasedKendoTable/index'

const parseDate = (d) => {
  if (!d) return null
  const date = new Date(d)
  return isNaN(date.getTime()) ? null : date
}

const getMonthsBetweenDates = (startVal, endVal, aopYear) => {
  let start = parseDate(startVal)
  let end = parseDate(endVal)

  if (!start || !end) {
    if (aopYear) {
      const yr = parseInt(aopYear, 10)
      start = new Date(yr, 0, 1) // Jan 1st of AOP_YEAR
      end = new Date(yr + 1, 3, 30) // Apr 30th of NEXT year
    } else {
      return []
    }
  }

  const months = []
  let current = new Date(start.getFullYear(), start.getMonth(), 1)
  const stop = new Date(end.getFullYear(), end.getMonth(), 1)
  while (current <= stop) {
    const monthNames = [
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
    const monthStr = monthNames[current.getMonth()]
    const yearStr = String(current.getFullYear()).slice(-2)
    const formatted = `${monthStr}-${yearStr}`
    months.push(formatted)
    current.setMonth(current.getMonth() + 1)
  }
  return months
}

const formatDateForAPI = (date) => {
  if (!date) return null
  const d = new Date(date)
  if (isNaN(d.getTime())) return null
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const HistoricalMonths = ({ startDate, endDate, refreshData }) => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear

  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [dynamicColumns, setDynamicColumns] = useState([])

  const valueFormat = customValueFormatterPhaseTwo(5)

  const selectOptions = [
    { value: 'EOR', label: 'EOR' },
    { value: 'SOR', label: 'SOR' },
  ]

  const baseColumns = [
    {
      field: 'id',
      title: 'Id',
      minWidth: 200,
      type: 'text',
      editable: false,
      locked: true,
      hidden: true,
    },
    {
      field: 'productName',
      title: 'Particulars',
      minWidth: 200,
      type: 'text',
      editable: false,
      locked: true,
    },
    {
      field: 'TypeDisplayName',
      title: 'Type',
      minWidth: 200,
      type: 'text',
      editable: false,
      locked: true,
      hidden: true,
    },
    ...dynamicColumns,
    // {
    //   field: 'remarks',
    //   title: 'Remarks',
    //   minWidth: 200,
    //   type: 'text',
    //   editable: true,
    // },
  ]

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchData()
    }
  }, [PLANT_ID, AOP_YEAR, refreshData])

  const fetchData = async () => {
    setLoading(true)
    try {
      const formattedFrom = formatDateForAPI(startDate)
      const formattedTo = formatDateForAPI(endDate)

      const response = await ProductionNormsApiService.getHistoricalMonths(
        keycloak,
        AOP_YEAR,
        PLANT_ID,
        formattedFrom,
        formattedTo,
        'Historical Months',
      )

      const apiResponse = response?.data || {}
      const manualEntryData = apiResponse.data || []
      const apiColumns = apiResponse.columns || []

      // Generate month column keys dynamically based on startDate and endDate, or API columns
      const monthKeys =
        apiColumns.length > 0
          ? apiColumns.filter((col) => col !== 'Particulars')
          : getMonthsBetweenDates(startDate, endDate, AOP_YEAR)

      const generatedMonthCols = monthKeys.map((key) => ({
        field: key,
        title: key,
        minWidth: 120,
        type: 'row-based',
        editable: true,
      }))
      setDynamicColumns(generatedMonthCols)

      // Generate row objects dynamically based on whatever rows are returned by API
      const dataWithEditFlag = manualEntryData.map((apiRow, index) => {
        const particulars = apiRow.Particulars || apiRow.productName || ''
        const isEorSor = particulars.toLowerCase() === 'eor/sor'

        const row = {
          id: index + 1,
          Particulars: particulars,
          productName: particulars,
          type: isEorSor ? 'select' : 'text',
          idFromApi: apiRow.id || null,
          remarks: apiRow.Remarks || apiRow.remark || '',
          inEdit: false,
        }

        if (isEorSor) {
          row.options = ['EOR', 'SOR']
        }

        monthKeys.forEach((key) => {
          row[key] =
            apiRow[key] !== undefined && apiRow[key] !== null ? apiRow[key] : ''
        })

        return row
      })

      setRows(dataWithEditFlag)
      setOriginalRows(JSON.parse(JSON.stringify(dataWithEditFlag)))
    } catch (error) {
      console.error('Error fetching manual entry data:', error)
    } finally {
      setLoading(false)
    }
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

    const monthKeys = dynamicColumns.map((col) => col.field)

    // Identify which month columns have been edited
    const editedMonths = new Set()
    for (const row of rows) {
      const originalRow = originalRows.find((orig) => orig.id === row.id)
      if (originalRow) {
        for (const key of monthKeys) {
          const val1 = row[key] === undefined || row[key] === null ? '' : String(row[key]).trim()
          const val2 = originalRow[key] === undefined || originalRow[key] === null ? '' : String(originalRow[key]).trim()
          if (val1 !== val2) {
            editedMonths.add(key)
          }
        }
      }
    }

    // Validate only edited month columns
    let validationError = ''
    if (editedMonths.size > 0) {
      for (const row of rows) {
        for (const key of editedMonths) {
          if (row[key] === undefined || row[key] === null || String(row[key]).trim() === '') {
            validationError = `${row.Particulars || row.productName} value is required for ${key}`
            break
          }
        }
        if (validationError) break
      }
    }

    if (validationError) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: validationError,
        severity: 'error',
      })
      setLoading(false)
      return
    }

    const convertedData = rows.map((row) => {
      const rowData = {
        Particulars: row.Particulars || row.productName,
      }
      monthKeys.forEach((key) => {
        rowData[key] = row[key] !== undefined ? row[key] : null
      })
      return rowData
    })

    try {
      const formattedFrom = formatDateForAPI(startDate)
      const formattedTo = formatDateForAPI(endDate)

      // Call the API to save manual entry data
      await ProductionNormsApiService.saveHistoricalMonths(
        keycloak,
        AOP_YEAR,
        PLANT_ID,
        formattedFrom,
        formattedTo,
        convertedData,
        'Historical Months'
      )

      // If we reach here, save was successful
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Data saved successfully!',
        severity: 'success',
      })
      setModifiedCells({})
      setOriginalRows([])
      await fetchData()
    } catch (error) {
      console.error('Error saving manual entry data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error saving data!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const handleCalculate = async () => {
    setLoading(true)
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Calculating...',
      severity: 'info',
    })

    try {
      const response =
        await ProductionNormsApiService.calculateHistoricalMonths(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )

      if (response?.code === 422) {
        setTimeout(() => {
          setSnackbarOpen(true)
          setSnackbarData({
            message: response.message || 'Validation error occurred.',
            severity: 'error',
            autoHide: false,
          })
        }, 500)
      } else if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Calculation completed successfully!',
          severity: 'success',
        })
        await fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Calculation failed. Please try again.',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error calculating steady state consumption:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Calculation failed. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showExport: false,
    showImport: false,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Historical Months',
    showDropdown: false,
    remarksEditable: true,
    showCalculate: false,
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <RowBasedKendoTable
        columns={baseColumns}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title={permissions.showTitle ? permissions.titleName : ''}
        permissions={permissions}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={() => {}}
        saveChanges={saveChanges}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        handleCalculate={handleCalculate}
        // groupBy={['normParameterTypeDisplayName']}
        customHeight={70}
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

export default HistoricalMonths
