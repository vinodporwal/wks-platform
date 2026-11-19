import React, { useEffect, useMemo, useRef, useState } from 'react'
import { Box } from '@mui/material'
import { ProductionNormsApiService } from 'components/aop-phase-two/services/crude/productionNormsApiService'
import { useSession } from 'SessionStoreContext'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import { customValueFormatterPhaseTwo } from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { downloadBlobExcel } from 'components/aop-phase-two/common/utilities/downloadBase64Excel'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
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

const ConstantsTab = ({
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
    siteId: SITE_ID,
    aopYear: AOP_YEAR,
    isOldYear: IS_OLD_YEAR,
    isReleased: IS_RELEASED,
    dataGridStore,
  } = context

  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const apiService = variantConfig?.apiService || ProductionNormsApiService
  const valueFormat = customValueFormatterPhaseTwo(variantConfig?.decimals ?? 5)
  const titleName = variantConfig?.titleName || 'Constants'
  const excelPageName =
    variantConfig?.excelPageName || 'Production_Norms_Constants'
  const EXCEL_NAME = generateExcelName(dataGridStore, excelPageName)

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
        field: 'UOM',
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
    remarks: item.remarks || '',
    id: item?.id || index + 1,
    type: item?.type || item?.Type,
  })

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchConstantsData()
    }
  }, [PLANT_ID, AOP_YEAR, refreshData])

  const fetchConstantsData = async () => {
    const requestId = ++requestIdRef.current
    setLoading(true)
    try {
      const res = await apiService.getConstantsData(keycloak, PLANT_ID, AOP_YEAR)

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
      console.error('Error fetching constants data:', error)
      setRows([])
      setOriginalRows([])
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
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
      showExport: true,
      ExcelName: EXCEL_NAME,
      showImport: !READ_ONLY,
      showTitleNameBusiness: true,
      showTitle: true,
      titleName,
    }

    return typeof variantConfig?.getPermissions === 'function'
      ? variantConfig.getPermissions(basePermissions, context)
      : basePermissions
  }, [READ_ONLY, EXCEL_NAME, titleName, variantConfig, context])

  const saveChanges = async () => {
    setLoading(true)

    const requireDatesOnSave = variantConfig?.requireDatesOnSave !== false
    if (requireDatesOnSave && (!startDate || !endDate)) {
      setSnackbarOpen(true)
      setSnackbarData({
        message:
          'Period dates are required. Please ensure dates are loaded from AOP Period Basis.',
        severity: 'error',
      })
      setLoading(false)
      return
    }

    if (requireDatesOnSave && !SITE_ID) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Site ID is required.',
        severity: 'error',
      })
      setLoading(false)
      return
    }

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

    const validationError =
      typeof variantConfig?.validateRows === 'function'
        ? variantConfig.validateRows(data, originalRows, context)
        : validateRowDataWithRemarks(
            data,
            originalRows,
            ['value'],
            variantConfig?.validationLabelField || 'productName',
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
        ? variantConfig.buildSavePayload(modifiedData, context)
        : modifiedData

    try {
      const periodFrom = formatDateForAPI(startDate)
      const periodTo = formatDateForAPI(endDate)

      await apiService.saveConstantsData(
        keycloak,
        AOP_YEAR,
        payload,
        PLANT_ID,
        SITE_ID,
        periodFrom,
        periodTo,
      )

      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved ${modifiedData.length} changes!`,
        severity: 'success',
      })
      await fetchConstantsData()
      if (
        typeof onLoadNormCalculation === 'function' &&
        variantConfig?.triggerNormCalculationOnSave
      ) {
        onLoadNormCalculation()
      }
    } catch (error) {
      console.error('Error saving constants data:', error)
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

    const requireDatesOnImport = variantConfig?.requireDatesOnImport !== false
    if (requireDatesOnImport && (!startDate || !endDate)) {
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
        await fetchConstantsData()
      } else if (response?.code === 400 && response?.data) {
        try {
          const base64Data = response.data
          const binaryString = window.atob(base64Data)
          const bytes = new Uint8Array(binaryString.length)
          for (let i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i)
          }
          const blob = new Blob([bytes], {
            type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          })
          downloadBlobExcel(blob, `Constants_Errors_${Date.now()}.xlsx`)

          setSnackbarOpen(true)
          setSnackbarData({
            message:
              response?.message ||
              'Import failed with errors. Please check the downloaded file.',
            severity: 'error',
          })
          await fetchConstantsData()
        } catch (downloadError) {
          console.error('Error downloading error file:', downloadError)
          setSnackbarOpen(true)
          setSnackbarData({
            message: 'Import failed but could not download error file.',
            severity: 'error',
          })
        }
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
      await apiService.exportConstantsExcel(
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
      console.error('Error exporting constants data:', error)
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

export default ConstantsTab

