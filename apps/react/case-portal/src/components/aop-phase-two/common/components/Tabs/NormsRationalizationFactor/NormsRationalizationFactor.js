import React, { useEffect, useMemo, useRef, useState } from 'react'
import { Box } from '@mui/material'
import { ProductionTabApiService } from 'components/aop-phase-two/services/common/productionTabApiService'
import { useSession } from 'SessionStoreContext'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import { customValueFormatterPhaseTwo } from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import usePlantContext from '../../../hooks/usePlantContext'
import AdvanceKendoTable from '../../../AdvanceKendoTable/index'

const formatDateForAPI = (date) => {
  if (!date) return ''
  const d = date instanceof Date ? date : new Date(date)
  if (Number.isNaN(d.getTime())) return ''
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const NormsRationalizationFactorTab = ({
  startDate,
  endDate,
  refreshData,
  onLoadNormCalculation,
  plantContext,
  variantConfig = {},
}) => {
  const keycloak = useSession()
  const fallbackContext = usePlantContext()
  const context = plantContext || fallbackContext

  const requestIdRef = useRef(0)

  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const {
    plantId: PLANT_ID,
    aopYear: AOP_YEAR,
    isOldYear: IS_OLD_YEAR,
    isReleased: IS_RELEASED,
  } = context

  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const apiService = variantConfig?.apiService || ProductionTabApiService
  const valueFormat = customValueFormatterPhaseTwo(variantConfig?.decimals ?? 2)

  const columns = useMemo(() => {
    const baseColumns = [
      {
        field: 'productName',
        title: 'Particulars',
        widthT: 300,
        minWidth: 250,
        type: 'text',
        editable: false,
        hidden: false,
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
        field: 'value',
        title: 'Value',
        editable: !READ_ONLY,
        widthT: 150,
        minWidth: 120,
        align: 'left',
        headerAlign: 'left',
        type: 'number1',
        format: valueFormat,
      },
      {
        field: 'remarks',
        title: 'Remark',
        widthT: 350,
        type: 'textarea',
        editable: !READ_ONLY,
        minWidth: 300,
      },
    ]

    return typeof variantConfig?.getColumns === 'function'
      ? variantConfig.getColumns(baseColumns, context)
      : baseColumns
  }, [READ_ONLY, valueFormat, variantConfig, context])

  const defaultFormatRow = (item, index) => ({
    ...item,
    id: item?.normParameterFKId || index + 1,
    productName: item.displayName || '',
    uom: item.uom || '',
    value:
      item?.value !== undefined && item?.value !== null ? item.value : '',
    remarks: item.remarks || '',
    type:
      item.type ||
      item.normParameterTypeDisplayName ||
      'Norms Rationalization Factor',
    normParameterTypeDisplayName:
      item.normParameterTypeDisplayName ||
      item.type ||
      'Norms Rationalization Factor',
  })

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchRationalizationFactorData()
    }
  }, [PLANT_ID, AOP_YEAR, refreshData])

  const fetchRationalizationFactorData = async () => {
    const requestId = ++requestIdRef.current
    setLoading(true)
    try {
      const res = await apiService.getRationalizationFactorData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (requestId !== requestIdRef.current) return

      const list = Array.isArray(res) ? res : res?.data || []

      if (!list.length) {
        setRows([])
        setOriginalRows([])
        return
      }

      const formatFn =
        typeof variantConfig?.formatRow === 'function'
          ? (item, index) => variantConfig.formatRow(item, index, context)
          : defaultFormatRow

      const formattedData = list.map(formatFn)
      setRows(formattedData)
      setOriginalRows(formattedData)
    } catch (error) {
      if (requestId !== requestIdRef.current) return
      console.error('Error fetching Norms Rationalization Factor data:', error)
      setRows([])
      setOriginalRows([])
    } finally {
      if (requestId === requestIdRef.current) {
        setLoading(false)
      }
    }
  }

  const permissions = useMemo(() => {
    const basePermissions = {
      showAction: true,
      addButton: false,
      deleteButton: false,
      editButton: !READ_ONLY,
      saveBtn: !READ_ONLY,
      allAction: true,
      showExport: false,
      ExcelName: `Production_Norms_Rationalization_Factor_${AOP_YEAR}`,
      showImport: false,
      showTitleNameBusiness: true,
      showTitle: true,
      titleName: 'Norms Rationalization Factor',
    }

    return typeof variantConfig?.getPermissions === 'function'
      ? variantConfig.getPermissions(basePermissions, context)
      : basePermissions
  }, [READ_ONLY, AOP_YEAR, variantConfig, context])

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
    const recordsToValidate = data.length > 0 ? data : modifiedData

    const validationError =
      typeof variantConfig?.validateRows === 'function'
        ? variantConfig.validateRows(recordsToValidate, originalRows, context)
        : validateRowDataWithRemarks(
            recordsToValidate,
            originalRows,
            ['value'],
            'productName',
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

    const payload =
      typeof variantConfig?.buildSavePayload === 'function'
        ? variantConfig.buildSavePayload(recordsToValidate, context)
        : recordsToValidate.map((item) => ({
            normParameterFKId: item.normParameterFKId,
            displayName: item.displayName || item.productName || '',
            value:
              item.value !== null && item.value !== undefined
                ? String(item.value)
                : '0',
            remarks: item.remarks || '',
            uom: item.uom || '',
            normParameterTypeDisplayName:
              item.normParameterTypeDisplayName ||
              item.type ||
              'Norms Rationalization Factor',
            type:
              item.type ||
              item.normParameterTypeDisplayName ||
              'Norms Rationalization Factor',
          }))

    try {
      const response = await apiService.saveRationalizationFactorData(
        keycloak,
        AOP_YEAR,
        PLANT_ID,
        payload,
      )

      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message:
          response?.message || `Successfully saved ${payload.length} changes!`,
        severity: 'success',
      })
      await fetchRationalizationFactorData()
      if (
        typeof onLoadNormCalculation === 'function' &&
        variantConfig?.triggerNormCalculationOnSave
      ) {
        onLoadNormCalculation()
      }
    } catch (error) {
      console.error('Error saving Norms Rationalization Factor data:', error)
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

    if (!startDate || !endDate) {
      setSnackbarOpen(true)
      setSnackbarData({
        message:
          'Period dates are required. Please ensure dates are loaded from AOP Period Basis.',
        severity: 'error',
      })
      return
    }

    setLoading(true)
    try {
      const periodFrom = formatDateForAPI(startDate)
      const periodTo = formatDateForAPI(endDate)

      const response = await apiService.importConstantsExcel(
        file,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        periodFrom,
        periodTo,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Excel file imported successfully!',
          severity: 'success',
        })
        await fetchRationalizationFactorData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Failed to import Excel file.',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error uploading Excel file:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Failed to import Excel file: ${error.message}`,
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
      await apiService.exportConstantsExcel(keycloak, PLANT_ID, AOP_YEAR)
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting Norms Rationalization Factor data:', error)
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
        groupBy={variantConfig?.groupBy ?? ['type']}
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

export default NormsRationalizationFactorTab

