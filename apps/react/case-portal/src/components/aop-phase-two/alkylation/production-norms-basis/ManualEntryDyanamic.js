import React, { useState, useEffect } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { ProductionNormsApiService } from 'components/aop-phase-two/services/merox/productionNormsApiService'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { customValueFormatterPhaseTwo } from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'

const ManualEntryDyanamic = ({ startDate, endDate }) => {
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

  const formatDateForAPI = (date) => {
    if (!date) return ''
    const yr = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${yr}-${month}-${day}`
  }

  const PERIOD_FROM = formatDateForAPI(startDate)
  const PERIOD_TO = formatDateForAPI(endDate)

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
      field: 'Particulars',
      title: 'Particulars',
      minWidth: 250,
      type: 'text',
      editable: false,
      locked: true,
    },
    {
      field: 'UOM',
      title: 'UOM',
      minWidth: 120,
      type: 'text',
      editable: false,
      locked: true,
    },
    ...dynamicColumns,
    {
      field: 'Remarks',
      title: 'Remarks',
      minWidth: 200,
      type: 'textarea',
      editable: true,
    },
  ]

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR && PERIOD_FROM && PERIOD_TO) {
      fetchData()
    }
  }, [PLANT_ID, AOP_YEAR])

  const fetchData = async () => {
    setLoading(true)
    try {
      const response = await ProductionNormsApiService.getManualProduction(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        PERIOD_FROM,
        PERIOD_TO,
      )

      const responseData = response?.data || {}
      const apiRows = responseData?.data || []
      const apiColumns = responseData?.columns || []

      const EXCLUDED_KEYS = [
        'Particulars',
        'UOM',
        'Remarks',
        'id',
        'inEdit',
        'productName',
      ]

      let monthKeys = []

      if (apiColumns.length > 0) {
        monthKeys = apiColumns.filter((key) => !EXCLUDED_KEYS.includes(key))
      } else if (apiRows.length > 0) {
        monthKeys = Object.keys(apiRows[0]).filter(
          (key) => !EXCLUDED_KEYS.includes(key),
        )
      }

      const generatedMonthCols = monthKeys.map((key) => ({
        field: key,
        title: key,
        minWidth: 120,
        type: 'number1',
        editable: true,
        format: valueFormat,
      }))
      setDynamicColumns(generatedMonthCols)

      const dataWithEditFlag = apiRows.map((row, index) => ({
        ...row,
        Remarks: row.Remarks || row.remarks || '',
        id: index + 1,
        idFromApi: row.id || null,
        inEdit: false,
      }))

      setRows(dataWithEditFlag)
      setOriginalRows(JSON.parse(JSON.stringify(dataWithEditFlag)))
    } catch (error) {
      console.error('Error fetching manual production data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error fetching data',
        severity: 'error',
      })
      setRows([])
      setOriginalRows([])
    } finally {
      setLoading(false)
    }
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
        await ProductionNormsApiService.calculateManualProduction(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          PERIOD_FROM,
          PERIOD_TO,
        )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Calculation completed successfully!',
          severity: 'success',
        })
        // await fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Calculation failed. Please try again.',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error calculating manual production:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Calculation failed. Please try again.',
        severity: 'error',
      })
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
      'Particulars',
      'Remarks',
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

    const convertedData = data.map((row) => {
      const monthData = {}
      monthKeys.forEach((key) => {
        monthData[key] = row[key]
      })
      return {
        ...monthData,
        Particulars: row.Particulars,
        UOM: row.UOM,
        Remarks: row.Remarks || '',
      }
    })

    try {
      const response = await ProductionNormsApiService.saveManualProduction(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        PERIOD_FROM,
        PERIOD_TO,
        convertedData,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data saved successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        await fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Failed to save data!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error saving manual production data:', error)
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
    setCurrentRemark(row.Remarks || row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
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
    titleName: 'Manual Production',
    showDropdown: false,
    remarksEditable: true,
    showCalculate: false,
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
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

export default ManualEntryDyanamic
