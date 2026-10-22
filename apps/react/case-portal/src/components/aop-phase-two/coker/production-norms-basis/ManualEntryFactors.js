import { useCallback, useEffect, useMemo, useState } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { ProductionNormsApiService } from 'components/aop-phase-two/services/coker/productionNormsApiService'
import { useSession } from 'SessionStoreContext'
import {
  validateRowDataWithoutRemarks,
  validateRowDataWithRemarks,
} from 'components/aop-phase-two/common/commonUtilityFunctions'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { customValueFormatterPhaseTwo } from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { generateExcelNameWithoutExt } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import RowBasedKendoTable from 'components/aop-phase-two/common/RowBasedKendoTable/index'

const ManualEntryFactors = ({ startDate, endDate, refreshData }) => {
  const keycloak = useSession()

  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, siteObject } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const AOP_YEAR = year?.selectedYear

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const valueFormat = customValueFormatterPhaseTwo(4)
  const EXCEL_NAME = generateExcelNameWithoutExt(dataGridStore, 'Manual_Entry_Factors')

  const formatDateForAPI = (date) => {
    if (!date) return ''
    const d = new Date(date)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  }

  const columns = useMemo(() => {
    return [
      {
        field: 'displayName',
        title: 'Particulars',
        widthT: 320,
        minWidth: 260,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'uom',
        title: 'UOM',
        widthT: 120,
        minWidth: 100,
        type: 'text',
        editable: false,
      },
      {
        field: 'apr',
        title: 'Achieved Temp in Norms Basis month',
        widthT: 180,
        minWidth: 160,
        type: 'row-based',
        editable: false,
        align: 'center',
        headerAlign: 'center',
        format: valueFormat,
      },
      {
        field: 'may',
        title: 'Proposed Temp for next year Norms',
        widthT: 180,
        minWidth: 160,
        type: 'row-based',
        editable: true,
        align: 'center',
        headerAlign: 'center',
        format: valueFormat,
      },
      {
        field: 'jun',
        title: 'Factor derived from Calculation',
        widthT: 180,
        minWidth: 160,
        type: 'row-based',
        editable: false,
        align: 'center',
        headerAlign: 'center',
        format: valueFormat,
      },
      {
        field: 'remarks',
        title: 'Remarks',
        widthT: 350,
        minWidth: 300,
        type: 'textarea',
        editable: true,
      },
    ]
  }, [valueFormat])

  const fetchFactorsData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      setRows([])
      setOriginalRows([])
      setModifiedCells({})

      const res = await ProductionNormsApiService.getManualEntryFactors(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (res?.data?.length === 0) {
        setRows([])
        setOriginalRows([])
        return
      }

      const dataSource = res?.data || []

      const formattedData = dataSource.map((item, index) => {

       
        return {
          ...item,
          id: item?.id || index + 1,
          normParameterFKId: item?.normParameterFKId,
          displayName: item?.displayName,
          apr: item?.apr,
          may: item?.may,
          jun: item?.jun,
          remarks: item?.remarks || '',
          uom: item?.uom || item?.UOM || '',
          normType: item?.type,
          type: 'number1',
          format: valueFormat,
          isEditable: item?.isEditable,
          allowNegative: true,
        }
      })

      setRows(formattedData)
      setOriginalRows(formattedData)
    } catch (error) {
      console.error('Error fetching manual entry factors data:', error)
      setRows([])
      setOriginalRows([])
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak, valueFormat])

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchFactorsData()
    }, 50)
    return () => clearTimeout(timer)
  }, [fetchFactorsData, refreshData])

  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    downloadExcelBtnFromUI: true,
    showExport: false,
    ExcelName: EXCEL_NAME,
    showImport: false,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Manual Entry - Factors',
  }

  const customItemChange = useCallback((e, setRowsState, setModifiedCellsState) => {
    const { dataItem, field, value } = e
    const numVal = value === '' || isNaN(value) ? value : Number(value)

    
  }, [])

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

    const fieldsToCheck = ['may']
    const validator = validateRowDataWithRemarks
    const validationError = validator(data, originalRows, fieldsToCheck, 'displayName')

    if (validationError) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: validationError,
        severity: 'error',
      })
      setLoading(false)
      return
    }

    
    const periodFrom = formatDateForAPI(startDate)
    const periodTo = formatDateForAPI(endDate)

    const payload = rows.map((row) => ({
      normParameterFKId: row.normParameterFKId,
      displayName: row.displayName,
      apr: String(row.apr ?? ''),
      may: String(row.may ?? ''),
      jun: String(row.jun ?? ''),
      remarks: row.remarks,
      uom: row.uom,
      auditYear: AOP_YEAR,
      plantId: PLANT_ID,
    }))

    try {
      const response = await ProductionNormsApiService.saveManualEntryFactors(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        payload,
        periodFrom,
        periodTo,
      )

      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message:
          response?.message || `Successfully saved ${modifiedData.length} changes!`,
        severity: response?.code === 400 ? 'warning' : 'success',
      })
      fetchFactorsData()
    } catch (error) {
      console.error('Error saving manual entry factors data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to save changes. Please try again.',
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
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting Factors data:', error)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const handleDynamicColumnMerger = (dataItem, field) => {
    
    if (dataItem?.id === 3 || dataItem?.id === 4) {
      if (field === 'apr') {
        return { hidden: true }
      }
      if (field === 'may') {
        return {
          colSpan: 2,
          style: {
            textAlign: 'center',
            verticalAlign: 'middle',
            backgroundColor: '#ffffff',
          },
        }
      }
    }

    // Grey cells: row 1 & 2 achievedTemp, and factor column across all rows
    if (
      ((dataItem?.id === 1 || dataItem?.id === 2) && field === 'apr') ||
      field === 'jun'
    ) {
      return {
        style: {
          backgroundColor: '#d9d9d9',
          color: '#2b2b2b',
          textAlign: 'center',
          verticalAlign: 'middle',
          fontWeight: 500,
        },
      }
    }

    if (field === 'may') {
      return {
        style: {
          textAlign: 'center',
          verticalAlign: 'middle',
        },
      }
    }

    return {}
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <RowBasedKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        dynamicColumnMerger={handleDynamicColumnMerger}
        customItemChange={customItemChange}
        title={permissions.showTitle ? permissions.titleName : ''}
        permissions={permissions}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        saveChanges={saveChanges}
        handleExport={handleExport}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        groupBy={['normType']}
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

export default ManualEntryFactors
