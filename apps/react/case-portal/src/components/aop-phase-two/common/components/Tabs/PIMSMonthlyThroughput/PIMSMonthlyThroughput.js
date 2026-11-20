import React, { useEffect, useMemo, useRef, useState } from 'react'
import { Box } from '@mui/material'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { ProductionNormsApiService } from 'components/aop-phase-two/services/crude/productionNormsApiService'
import { useSession } from 'SessionStoreContext'
import { customValueFormatterPhaseTwo } from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import usePlantContext from '../../../hooks/usePlantContext'
import AdvanceKendoTable from '../../../AdvanceKendoTable/index'

const MONTH_FIELDS = [
  { field: 'apr', headerIndex: 4, fallback: 'Apr' },
  { field: 'may', headerIndex: 5, fallback: 'May' },
  { field: 'jun', headerIndex: 6, fallback: 'Jun' },
  { field: 'jul', headerIndex: 7, fallback: 'Jul' },
  { field: 'aug', headerIndex: 8, fallback: 'Aug' },
  { field: 'sep', headerIndex: 9, fallback: 'Sep' },
  { field: 'oct', headerIndex: 10, fallback: 'Oct' },
  { field: 'nov', headerIndex: 11, fallback: 'Nov' },
  { field: 'dec', headerIndex: 12, fallback: 'Dec' },
  { field: 'jan', headerIndex: 1, fallback: 'Jan' },
  { field: 'feb', headerIndex: 2, fallback: 'Feb' },
  { field: 'mar', headerIndex: 3, fallback: 'Mar' },
]

const formatDateForAPI = (date) => {
  if (!date) return ''
  const d = date instanceof Date ? date : new Date(date)
  if (Number.isNaN(d.getTime())) return ''
  const y = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${month}-${day}`
}

const PIMSMonthlyThroughputTab = ({
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
  } = context

  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const apiService = variantConfig?.apiService || ProductionNormsApiService
  const decimals = variantConfig?.decimals ?? 5
  const valueFormat = customValueFormatterPhaseTwo(decimals)
  const headerMap = useMemo(() => generateHeaderNames(AOP_YEAR), [AOP_YEAR])

  const columns = useMemo(() => {
    const baseColumns = [
      {
        field: 'displayName',
        title: 'Particulars',
        widthT: 300,
        minWidth: 250,
        type: 'text',
        editable: false,
        hidden: false,
        locked: true,
      },
      {
        field: 'uom',
        title: 'UOM',
        widthT: 120,
        minWidth: 100,
        type: 'text',
        editable: false,
        locked: true,
      },
      ...MONTH_FIELDS.map(({ field, headerIndex, fallback }) => ({
        field,
        title: headerMap[headerIndex] || fallback,
        editable: !READ_ONLY,
        widthT: 120,
        minWidth: 100,
        align: 'left',
        headerAlign: 'left',
        type: 'number1',
        format: valueFormat,
      })),
      {
        field: 'remarks',
        title: 'Remarks',
        widthT: 300,
        type: 'textarea',
        editable: !READ_ONLY,
        minWidth: 250,
      },
    ]

    return typeof variantConfig?.getColumns === 'function'
      ? variantConfig.getColumns(baseColumns, context)
      : baseColumns
  }, [headerMap, READ_ONLY, valueFormat, variantConfig, context])

  const defaultFormatRow = (item, index) => ({
    ...item,
    displayName: item.displayName || item.name || '',
    remarks: item.remarks || '',
    id: item?.id || item?.normParameterId || index + 1,
  })

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchConfigurationData()
    }
  }, [PLANT_ID, AOP_YEAR, refreshData])

  const fetchConfigurationData = async () => {
    const requestId = ++requestIdRef.current
    setLoading(true)
    try {
      const res = await apiService.getPIMSMonthlyThroughputData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (requestId !== requestIdRef.current) return

      const list = Array.isArray(res) ? res : res?.data || []

      if (!list.length) {
        setRows([])
        setOriginalRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
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
      console.error('Error fetching monthly throughput data:', error)
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
      ExcelName: `PIMS_MONTHLY_THROUGHPUT_${AOP_YEAR}`,
      showImport: !READ_ONLY,
      showTitleNameBusiness: true,
      showTitle: true,
      titleName: 'PIMS Monthly Throughput',
    }

    return typeof variantConfig?.getPermissions === 'function'
      ? variantConfig.getPermissions(basePermissions, context)
      : basePermissions
  }, [READ_ONLY, AOP_YEAR, variantConfig, context])

  const saveChanges = async () => {
    setLoading(true)

    if (!startDate || !endDate) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Start date and end date are required.',
        severity: 'error',
      })
      setLoading(false)
      return
    }

    if (!SITE_ID) {
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

    const fieldsToCheck = MONTH_FIELDS.map((m) => m.field)
    const validationError =
      typeof variantConfig?.validateRows === 'function'
        ? variantConfig.validateRows(data, originalRows, context)
        : validateRowDataWithRemarks(
            data,
            originalRows,
            fieldsToCheck,
            'displayName',
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

      await apiService.savePIMSMonthlyThroughputData(
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
      await fetchConfigurationData()
      if (
        typeof onLoadNormCalculation === 'function' &&
        variantConfig?.triggerNormCalculationOnSave
      ) {
        onLoadNormCalculation()
      }
    } catch (error) {
      console.error('Error saving monthly throughput data:', error)
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
      const response = await apiService.importPIMSMonthlyThroughputExcel(
        file,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Excel file imported successfully!',
          severity: 'success',
        })
        await fetchConfigurationData()
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
          const url = window.URL.createObjectURL(blob)
          const link = document.createElement('a')
          link.href = url
          link.download = `PIMS_Monthly_Throughput_Errors_${Date.now()}.xlsx`
          document.body.appendChild(link)
          link.click()
          document.body.removeChild(link)
          window.URL.revokeObjectURL(url)

          setSnackbarOpen(true)
          setSnackbarData({
            message:
              response?.message ||
              'Import failed with errors. Please check the downloaded file.',
            severity: 'error',
          })
          await fetchConfigurationData()
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
      await apiService.exportPIMSMonthlyThroughputExcel(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting PIMS monthly throughput data:', error)
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
        groupBy={variantConfig?.groupBy ?? ['normParameterType']}
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

export default PIMSMonthlyThroughputTab

