import React, { useState, useEffect } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import ValueFormatterPhaseTwo, {
  customValueFormatterPhaseTwo,
} from '../../common/ValueFormatterPhaseTwo'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
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
    const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
    const monthStr = monthNames[current.getMonth()]
    const yearStr = String(current.getFullYear()).slice(-2)
    const formatted = `${monthStr}-${yearStr}`
    months.push(formatted)
    current.setMonth(current.getMonth() + 1)
  }
  return months
}

const HistoricalMonths = ({ startDate, endDate }) => {
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
  }, [PLANT_ID, AOP_YEAR, startDate, endDate])

  const fetchData = async () => {
    setLoading(true)
    try {
      // Fetch manual entry data from backend API
      // const response = await ProductionNormsApiService.getHistoricalMonths(
      //   keycloak,
      //   AOP_YEAR,
      //   PLANT_ID,
      // )

      // Extract data from response - API returns { data: [...], code: 200, message: '...' }
      const manualEntryData =  []

      // Generate month column keys dynamically based on startDate and endDate
      const monthKeys = getMonthsBetweenDates(startDate, endDate, AOP_YEAR)
      const generatedMonthCols = monthKeys.map((key) => ({
        field: key,
        title: key,
        minWidth: 120,
        type: 'row-based',
        editable: true,
      }))
      setDynamicColumns(generatedMonthCols)

      const apiEorSorRow = manualEntryData.find(
        (r) => r.Particulars?.toLowerCase() === 'eor/sor' || r.productName?.toLowerCase() === 'eor/sor'
      )
      const apiCatalystRow = manualEntryData.find(
        (r) => r.Particulars?.toLowerCase() === 'type of catalyst' || r.productName?.toLowerCase() === 'type of catalyst'
      )
      const apiRemarkRow = manualEntryData.find(
        (r) => r.Particulars?.toLowerCase() === 'remark' || r.productName?.toLowerCase() === 'remark' || r.Particulars?.toLowerCase() === 'remarks' || r.productName?.toLowerCase() === 'remarks'
      )

      // Initialize the three rows with default dummy data
      const eorSorRow = {
        id: 1,
        Particulars: 'EOR/SOR',
        productName: 'EOR/SOR',
        type: 'select',
        options: ['EOR', 'SOR'],
        idFromApi: apiEorSorRow?.id || null,
        remarks: apiEorSorRow?.Remarks || apiEorSorRow?.remark || '',
        inEdit: false,
      }

      const catalystRow = {
        id: 2,
        Particulars: 'Type of catalyst',
        productName: 'Type of catalyst',
        type: 'text',
        idFromApi: apiCatalystRow?.id || null,
        remarks: apiCatalystRow?.Remarks || apiCatalystRow?.remark || '',
        inEdit: false,
      }

      const remarkRow = {
        id: 3,
        Particulars: 'Remark',
        productName: 'Remark',
        type: 'text',
        idFromApi: apiRemarkRow?.id || null,
        remarks: apiRemarkRow?.Remarks || apiRemarkRow?.remark || '',
        inEdit: false,
      }

      monthKeys.forEach((key,i) => {
        // Fallbacks from API row if key exists, otherwise dummy value
        // Row 1: EOR/SOR
        if (apiEorSorRow && apiEorSorRow[key] !== undefined && apiEorSorRow[key] !== null) {
          eorSorRow[key] = apiEorSorRow[key]
        } else {
          eorSorRow[key] = i%2 == 0 ?'EOR':'SOR'
        }

        // Row 2: Type of catalyst
        if (apiCatalystRow && apiCatalystRow[key] !== undefined && apiCatalystRow[key] !== null) {
          catalystRow[key] = apiCatalystRow[key]
        } else {
          catalystRow[key] = i%2 == 0 ?'A':'B'
        }

        // Row 3: Remark
        if (apiRemarkRow && apiRemarkRow[key] !== undefined && apiRemarkRow[key] !== null) {
          remarkRow[key] = apiRemarkRow[key]
        } else {
          remarkRow[key] = i%2 == 0 ? `Test${i}` : `Test${i+1}`
        }
      })

      const dataWithEditFlag = [eorSorRow, catalystRow, remarkRow]
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

    const monthKeys = dynamicColumns.map((col) => col.field)
    const fieldsToCheck = monthKeys
    const validationError = validateRowDataWithRemarks(
      data,
      originalRows,
      fieldsToCheck,
      'productName',
      'productName'
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

    // Build payload dynamically using month keys from API response
    const convertedData = data.map((row) => {
      const monthData = {}
      monthKeys.forEach((key) => {
        monthData[key] = row[key]
      })
      return {
        ...monthData,
        id: row.idFromApi || null,
        Particulars: row.Particulars || row.productName,
        Remarks: row?.remarks,
      }
    })

    try {
      // Call the API to save manual entry data
      await ProductionNormsApiService.saveHistoricalMonths(
        keycloak,
        AOP_YEAR,
        PLANT_ID,
        convertedData,
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
      const response = await ProductionNormsApiService.calculateHistoricalMonths(
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
    showCalculate: true,
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
