import { useEffect, useState } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import RowBasedKendoTable from 'components/aop-phase-two/common/RowBasedKendoTable/index'
import { ProductionNormsApiService } from 'components/aop-phase-two/services/sulphur/productionNormsApiService'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { customValueFormatterPhaseTwo } from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

const monthColumns = [
  { field: 'apr', headerIndex: 4 },
  { field: 'may', headerIndex: 5 },
  { field: 'jun', headerIndex: 6 },
  { field: 'jul', headerIndex: 7 },
  { field: 'aug', headerIndex: 8 },
  { field: 'sep', headerIndex: 9 },
  { field: 'oct', headerIndex: 10 },
  { field: 'nov', headerIndex: 11 },
  { field: 'dec', headerIndex: 12 },
  { field: 'jan', headerIndex: 1 },
  { field: 'feb', headerIndex: 2 },
  { field: 'mar', headerIndex: 3 },
]


const PERMISSIONS = {
  showAction: true,
  addButton: false,
  deleteButton: false,
  editButton: true,
  saveBtn: true,
  allAction: true,
  downloadExcelBtnFromUI: true,
  ExcelName: 'Production_Norms_Configuration',
  showImport: false,
  showTitleNameBusiness: true,
  showTitle: true,
  titleName: 'Configuration',
  groupByNonNullOnly: true,
  singleCheckboxPerGroup: true,
}

const parseConfig = (config) => {
  if (!config) return null
  try {
    return typeof config === 'string' ? JSON.parse(config) : config
  } catch {
    return null
  }
}

const resolveHideCheckbox = (dependentAttributeConfig, fallbackHideCheckbox) => {
  if (dependentAttributeConfig) {
    if (typeof dependentAttributeConfig === 'string') {
      const match = dependentAttributeConfig.match(
        /["']?isCheckBoxVisible["']?\s*:\s*["']?(true|false)["']?/i,
      )
      if (match) {
        return match[1].toLowerCase() === 'false'
      }
    } else if (typeof dependentAttributeConfig === 'object') {
      if (typeof dependentAttributeConfig.isCheckBoxVisible === 'boolean') {
        return !dependentAttributeConfig.isCheckBoxVisible
      }
      if (typeof dependentAttributeConfig.isCheckBoxVisible === 'string') {
        return dependentAttributeConfig.isCheckBoxVisible.toLowerCase() === 'false'
      }
    }
  }
  return Boolean(fallbackHideCheckbox)
}

const formatDate = (val) => {
  if (!val) return val
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const Configuration = ({ startDate, endDate, refreshData }) => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [customModifiedCells, setCustomModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const valueFormat = customValueFormatterPhaseTwo(5)
  const headerMap = generateHeaderNames(AOP_YEAR)

  const COLUMNS = [
    {
      field: 'displayName',
      title: 'Data filters',
      widthT: 320,
      minWidth: 280,
      type: 'textarea',
      editable: false,
    },
    {
      field: 'uom',
      title: 'UOM',
      widthT: 110,
      minWidth: 90,
      editable: false,
    },
    {
      field: 'selection',
      title: 'Selection',
      widthT: 110,
      minWidth: 110,
      type: 'checkbox',
      editable: true,
    },
    ...monthColumns.map((month) => ({
      field: month.field,
      title: headerMap[month.headerIndex],
      minWidth: 140,
      type: 'number1',
      editable: true,
      format: valueFormat,
    })),
    {
      field: 'remarks',
      title: 'Remarks',
      widthT: 160,
      type: 'textarea',
      editable: true,
      minWidth: 140,
      showPlaceholder: false,
    },
    {
      field: 'dependantAttributeId',
      title: 'Applicable Utilities and Cat-chems',
      widthT: 350,
      minWidth: 300,
      editable: false,
    },
  ]

  const showSnackbar = (message, severity = 'info', autoHide = true) => {
    setSnackbarOpen(true)
    setSnackbarData({ message, severity, autoHide })
  }

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) fetchConfigurationData()
  }, [PLANT_ID, AOP_YEAR, refreshData])

  const fetchConfigurationData = async () => {
    setLoading(true)
    try {
      const response = await ProductionNormsApiService.getConfigurationData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      const apiList = Array.isArray(response?.data)
        ? response.data
        : Array.isArray(response)
          ? response
          : []

      let formattedData = []

      if (apiList.length > 0) {
        formattedData = apiList.map((item, index) => {
          const isTickRow = Boolean(item.isTickRow)
          const isSelected = Boolean(item.selected)
          const hideCheckbox = !isTickRow

          return {
            id: item.normParameterFKId || index + 1,
            normParameterFKId: item.normParameterFKId,
            displayName: item.displayName || '',
            uom: item.uom || '',
            normsType: item.groupName || item.normsType || '',
            groupName: item.groupName || '',
            selection: isSelected,
            selected: isSelected,
            isTickRow,
            hideCheckbox,
            hideCheckBox: hideCheckbox,
            type: isTickRow ? 'checkbox' : item.dataType || 'number1',
            dataType: item.dataType || (isTickRow ? 'checkbox' : 'number1'),
            isEditable: item.isEditable !== undefined ? item.isEditable : true,
            remarks: item.remarks || '',
            dependantAttributeId: item.utilities || '',
            utilities: item.utilities || '',
            displayOrder:
              item.displayOrder != null ? item.displayOrder : index + 1,
            apr: item.apr ?? '',
            may: item.may ?? '',
            jun: item.jun ?? '',
            jul: item.jul ?? '',
            aug: item.aug ?? '',
            sep: item.sep ?? '',
            oct: item.oct ?? '',
            nov: item.nov ?? '',
            dec: item.dec ?? '',
            jan: item.jan ?? '',
            feb: item.feb ?? '',
            mar: item.mar ?? '',
          }
        })
      }

      setRows(formattedData)
      setOriginalRows(formattedData)
    } catch (error) {
      console.error('Error fetching sulphur configuration data:', error)
      setRows([])
      setOriginalRows([])
    } finally {
      setLoading(false)
    }
  }

  const saveChanges = async () => {
    const modifiedData = Object.values(modifiedCells)
    const dataToSave = modifiedData.filter((row) => row.inEdit)
    if (!dataToSave.length) return showSnackbar('No Records to Save!')

    const payload = dataToSave.map((row) => ({
      normParameterFKId: row.normParameterFKId,
      displayName: row.displayName || '',
      groupName: row.groupName || row.normsType || '',
      dataType: row.dataType || (row.isTickRow ? 'checkbox' : 'number1'),
      isTickRow: Boolean(row.isTickRow),
      selected: Boolean(row.selection ?? row.selected),
      apr: row.apr != null ? String(row.apr) : '',
      may: row.may != null ? String(row.may) : '',
      jun: row.jun != null ? String(row.jun) : '',
      jul: row.jul != null ? String(row.jul) : '',
      aug: row.aug != null ? String(row.aug) : '',
      sep: row.sep != null ? String(row.sep) : '',
      oct: row.oct != null ? String(row.oct) : '',
      nov: row.nov != null ? String(row.nov) : '',
      dec: row.dec != null ? String(row.dec) : '',
      jan: row.jan != null ? String(row.jan) : '',
      feb: row.feb != null ? String(row.feb) : '',
      mar: row.mar != null ? String(row.mar) : '',
      uom: row.uom || '',
      remarks: row.remarks || '',
      utilities: row.utilities || row.dependantAttributeId || '',
      isEditable: row.isEditable !== undefined ? Boolean(row.isEditable) : true,
      displayOrder: row.displayOrder != null ? Number(row.displayOrder) : 0,
    }))

    const hasMissingId = payload.some((row) => !row.normParameterFKId)
    if (hasMissingId) {
      showSnackbar(
        'Cannot save: Configuration records are missing Norm Parameter ID.',
        'error',
      )
      return
    }

    setLoading(true)

    try {
      const response = await ProductionNormsApiService.saveConfigurationData(
        keycloak,
        AOP_YEAR,
        payload,
        PLANT_ID,
      )

      if (response?.code === 200 || response?.success) {
        setModifiedCells({})
        showSnackbar(
          response?.message ||
            `Successfully saved ${dataToSave.length} changes!`,
          'success',
        )
        await fetchConfigurationData()
      } else if (response?.code === 400) {
        showSnackbar(
          response?.message || 'Partial data updated with errors.',
          'warning',
        )
        await fetchConfigurationData()
      } else {
        showSnackbar(response?.message || 'Failed to save changes.', 'error')
      }
    } catch (error) {
      console.error('Error saving data:', error)
      showSnackbar('Failed to save changes. Please try again.', 'error')
    } finally {
      setLoading(false)
    }
  }

  const handleExcelUpload = async (file) => {
    if (!file) return
    setLoading(true)
    try {
      const response = await ProductionNormsApiService.importConfigurationExcel(
        file,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        showSnackbar(response.message || 'Excel file imported successfully!', 'success')
        await fetchConfigurationData()
      } else if (response?.code === 400 && response?.data) {
        downloadErrorExcel(response.data)
        showSnackbar(
          response.message ||
            'Import failed with errors. Please check the downloaded file.',
          'error',
        )
        await fetchConfigurationData()
      } else {
        showSnackbar(response?.message || 'Failed to import Excel file.', 'error')
      }
    } catch (error) {
      showSnackbar(`Failed to import Excel file: ${error.message}`, 'error')
    } finally {
      setLoading(false)
    }
  }

  const downloadErrorExcel = (base64Data) => {
    try {
      const binaryString = window.atob(base64Data)
      const bytes = new Uint8Array(binaryString.length)
      for (let i = 0; i < binaryString.length; i++) bytes[i] = binaryString.charCodeAt(i)

      const blob = new Blob([bytes], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `Configuration_Errors_${Date.now()}.xlsx`
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    } catch (e) {
      console.error('Error downloading error file:', e)
    }
  }

  const handleExport = async () => {
    showSnackbar('Excel download started!')
    try {
      await ProductionNormsApiService.exportConfigurationExcel(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      showSnackbar('Excel download completed successfully!', 'success')
    } catch (error) {
      showSnackbar('Excel download failed. Please try again.', 'error')
    }
  }

  const excelExportName = generateExcelName(dataGridStore, 'Production_Norms_Configuration')

  return (
    <Box>
      <LoaderBackdrop open={loading} />
      <RowBasedKendoTable
        columns={COLUMNS}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title={PERMISSIONS.showTitle ? PERMISSIONS.titleName : ''}
        permissions={{
          ...PERMISSIONS,
          ExcelName: excelExportName,
        }}
        handleRemarkCellClick={(row) => {
          setCurrentRemark(row.remarks || '')
          setCurrentRowId(row.id)
          setRemarkDialogOpen(true)
        }}
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
        externalCustomModifiedCells={customModifiedCells}
        externalSetCustomModifiedCells={setCustomModifiedCells}
        groupBy={PERMISSIONS.groupByNonNullOnly ? ['normsType'] : undefined}
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

export default Configuration
