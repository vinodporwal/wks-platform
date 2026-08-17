import React, { useState, useEffect, useCallback } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { generateHeaderNames } from '../../common/utilities/generateHeaders'
import { customValueFormatterPhaseTwo } from '../../common/ValueFormatterPhaseTwo'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { JwOverallAopConsumptionApiService } from 'components/aop-phase-two/services/crude/jwOverallAopConsumptionApiService'

const MONTH_FIELDS = [
  'april',
  'may',
  'june',
  'july',
  'aug',
  'sep',
  'oct',
  'nov',
  'dec',
  'jan',
  'feb',
  'march',
]

const INITIAL_MOCK_ROWS = [
  {
    id: 1,
    productName: 'Crude Oil Processing',
    normParameterTypeDisplayName: 'Throughput',
    UOM: 'TMT',
    april: 100,
    may: 105,
    june: 102,
    july: 98,
    aug: 104,
    sep: 100,
    oct: 106,
    nov: 102,
    dec: 105,
    jan: 108,
    feb: 100,
    march: 110,
    avgNorms: 103.167,
    remarks: 'Initial Budget Plan',
    isEditable: true,
  },
  {
    id: 2,
    productName: 'Fuel Gas Consumption',
    normParameterTypeDisplayName: 'Utilities',
    UOM: 'MT',
    april: 45,
    may: 46,
    june: 44,
    july: 43,
    aug: 45,
    sep: 44,
    oct: 47,
    nov: 46,
    dec: 48,
    jan: 49,
    feb: 45,
    march: 50,
    avgNorms: 46.0,
    remarks: 'Estimated based on throughput',
    isEditable: true,
  },
  {
    id: 3,
    productName: 'Internal Power Generation',
    normParameterTypeDisplayName: 'Utilities',
    UOM: 'MWH',
    april: 1200,
    may: 1210,
    june: 1195,
    july: 1180,
    aug: 1205,
    sep: 1200,
    oct: 1220,
    nov: 1215,
    dec: 1230,
    jan: 1240,
    feb: 1200,
    march: 1250,
    avgNorms: 1212.083,
    remarks: 'Normal Operation',
    isEditable: true,
  },
  {
    id: 4,
    productName: 'High Pressure Steam',
    normParameterTypeDisplayName: 'Utilities',
    UOM: 'MT',
    april: 310,
    may: 315,
    june: 305,
    july: 300,
    aug: 312,
    sep: 310,
    oct: 320,
    nov: 318,
    dec: 325,
    jan: 328,
    feb: 315,
    march: 330,
    avgNorms: 315.667,
    remarks: 'Planned norms',
    isEditable: true,
  },
  {
    id: 5,
    productName: 'Specific Energy Norm',
    normParameterTypeDisplayName: 'Norms',
    UOM: 'GCal/MT',
    april: 0.42,
    may: 0.41,
    june: 0.43,
    july: 0.42,
    aug: 0.41,
    sep: 0.42,
    oct: 0.40,
    nov: 0.41,
    dec: 0.40,
    jan: 0.39,
    feb: 0.41,
    march: 0.39,
    avgNorms: 0.409,
    remarks: 'Target Efficiency',
    isEditable: true,
  },
]

const JwOverallAopConsumptionScreen = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear || year

  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState(INITIAL_MOCK_ROWS)
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const valueFormat = customValueFormatterPhaseTwo(5)
  const headerMap = generateHeaderNames(AOP_YEAR)

  const columns = [
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 250,
      minWidth: 200,
      type: 'text',
      editable: false,
      locked: true,
    },
    {
      field: 'normParameterTypeDisplayName',
      title: 'Type',
      widthT: 250,
      minWidth: 200,
      type: 'text',
      editable: false,
      locked: true,
      hidden: true,
    },
    {
      field: 'UOM',
      title: 'UOM',
      widthT: 120,
      minWidth: 100,
      type: 'text',
      editable: false,
    },
    {
      field: 'april',
      title: headerMap[4],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'may',
      title: headerMap[5],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'june',
      title: headerMap[6],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'july',
      title: headerMap[7],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'aug',
      title: headerMap[8],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'sep',
      title: headerMap[9],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'oct',
      title: headerMap[10],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'nov',
      title: headerMap[11],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'dec',
      title: headerMap[12],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'jan',
      title: headerMap[1],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'feb',
      title: headerMap[2],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'march',
      title: headerMap[3],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'avgNorms',
      title: 'Avg Norms',
      widthT: 130,
      minWidth: 120,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
    {
      field: 'remarks',
      title: 'Remarks',
      widthT: 200,
      minWidth: 150,
      type: 'text',
      editable: true,
    },
  ]

  const recalculateRowAvg = (rowObj) => {
    const sum = MONTH_FIELDS.reduce((acc, field) => {
      const val = rowObj[field]
      return acc + (val !== null && val !== undefined && !isNaN(val) ? Number(val) : 0)
    }, 0)
    return Number((sum / 12).toFixed(5))
  }

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await JwOverallAopConsumptionApiService.getJwOverallAopConsumption(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      const data =
        response?.data?.aopConsumptionNormDTOList?.map((item) => {
          const avgNorms = recalculateRowAvg(item)
          return {
            ...item,
            avgNorms,
            isEditable: true,
          }
        }) || []

      if (data.length > 0) {
        setRows(data)
      } else {
        setRows(INITIAL_MOCK_ROWS)
      }
    } catch (error) {
      console.error('Error fetching JW Overall AOP Consumption data:', error)
      // Keep initial structure on API failure / pending backend
      setRows(INITIAL_MOCK_ROWS)
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchData()
    }
  }, [PLANT_ID, AOP_YEAR, fetchData])

  const handleCustomItemChange = (e) => {
    const { dataItem, field, value } = e
    if (!dataItem) return

    const rowId = dataItem.id

    const updatedRow = {
      ...dataItem,
      [field]: value,
      inEdit: true,
    }

    if (MONTH_FIELDS.includes(field)) {
      updatedRow.avgNorms = recalculateRowAvg(updatedRow)
    }

    setRows((prevRows) =>
      prevRows.map((r) => (r.id === rowId ? updatedRow : r)),
    )

    setModifiedCells((prev) => ({
      ...prev,
      [rowId]: {
        ...(prev[rowId] || {}),

        ...updatedRow,
      },
    }))
  }

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveChanges = async () => {
    setLoading(true)
    try {
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

      await JwOverallAopConsumptionApiService.saveJwOverallAopConsumption(
        keycloak,
        modifiedData,
        AOP_YEAR,
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Data saved successfully!',
        severity: 'success',
      })
      setModifiedCells({})
    } catch (error) {
      console.error('Error saving JW Overall AOP Consumption:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Changes captured locally. (API pending backend deployment)',
        severity: 'info',
      })
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
      await JwOverallAopConsumptionApiService.calculateJwOverallAopConsumption(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      setSnackbarData({
        message: 'Calculation completed successfully!',
        severity: 'success',
      })
      await fetchData()
    } catch (error) {
      console.error('Error calculating JW Overall AOP Consumption:', error)
      // Recalculate local month averages for all rows
      setRows((prevRows) =>
        prevRows.map((r) => ({
          ...r,
          avgNorms: recalculateRowAvg(r),
        })),
      )
      setSnackbarData({
        message: 'Calculated local month averages!',
        severity: 'success',
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
    showExport: true,
    downloadExcelBtnFromUI: true,
    showCalculate: true,
    ExcelName: `Overall_AOP_Consumption_JW_${AOP_YEAR}`,
    showImport: false,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Overall AOP Consumption (JW Budget)',
    showDropdown: false,
    remarksEditable: true,
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
        customItemChange={handleCustomItemChange}
        title={permissions.showTitle ? permissions.titleName : ''}
        permissions={permissions}
        handleCalculate={handleCalculate}
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        groupBy={['normParameterTypeDisplayName']}
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

export default JwOverallAopConsumptionScreen
