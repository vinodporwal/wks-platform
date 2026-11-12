import { useEffect, useState } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import RowBasedKendoTable from 'components/aop-phase-two/common/RowBasedKendoTable/index'
import { ProductionNormsApiService } from 'components/aop-phase-two/services/pcg/productionNormsApiService'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const COLUMNS = [
  { field: 'displayName', title: 'Data Filters', widthT: 250, minWidth: 200, type: 'text', editable: false },
  { field: 'uom', title: 'UOM', widthT: 80, minWidth: 60, type: 'text', editable: false },
  { field: 'targetValue', title: 'Target Value', editable: true, widthT: 120, minWidth: 100, type: 'row-based' },
  { field: 'range', title: 'Range', editable: true, widthT: 120, minWidth: 100, type: 'row-based' },
  { field: 'selection', title: 'Selection', editable: true, widthT: 80, minWidth: 80, type: 'checkbox' },
  { field: 'remarks', title: 'Criteria', widthT: 250, type: 'textarea', editable: false, minWidth: 250, showPlaceholder: false },
  { field: 'dependantAttributeId', title: 'Applicable Utilities & Cat-Chems', widthT: 250, minWidth: 200, type: 'text', editable: false },
]

// Rows that must be fully disabled (targetValue, range, selection, remarks)
const PSA_READONLY_ROWS = [
  'Crude-1 311PA651-246 sulphur',
  'Crude-2 312PA651-246 sulphur',
  '395FX019',
  '395JWALLRW001',
]

const TGC2_TOGGLE_ROW = 'TGC2 Online/Offline'
const ALL_EDITABLE_FIELDS = ['targetValue', 'range', 'selection', 'remarks']

// Fallback / per-row config (matched by displayName)
const FILTER_CONFIG = [
  {
    displayName: TGC2_TOGGLE_ROW,
    targetValue: 'Offline',
    type: 'dropdown',
    options: ['Online', 'Offline'],
    nonEditableFields: ['range'],
    hideCheckbox: true,
  },
]

const PERMISSIONS = {
  showAction: true, addButton: false, deleteButton: false, editButton: true, saveBtn: true,
  allAction: true, downloadExcelBtnFromUI: true, ExcelName: 'Production_Norms_Configuration_PSA',
  showImport: false, showTitleNameBusiness: true, showTitle: true, titleName: 'Configuration',
}

const parseConfig = (config) => {
  if (!config) return null
  try { return typeof config === 'string' ? JSON.parse(config) : config }
  catch { return null }
}

const resolveHideCheckbox = (dependentAttributeConfig, fallbackHideCheckbox) => {
  if (dependentAttributeConfig) {
    if (typeof dependentAttributeConfig === 'string') {
      const match = dependentAttributeConfig.match(/["']?isCheckBoxVisible["']?\s*:\s*["']?(true|false)["']?/i)
      if (match) return match[1].toLowerCase() === 'false'
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

// ---- TGC2 Online/Offline helpers ----
const getTgcType = (row) => {
  const name = row.displayName?.toLowerCase() || ''
  if (!name.includes('tgc2') || row.displayName === TGC2_TOGGLE_ROW) return null
  if (name.includes('online')) return 'Online'
  if (name.includes('offline')) return 'Offline'
  return null
}

// Enables only the TGC rows matching the dropdown value; the rest are disabled
const applyTgcToggle = (rows, value) =>
  rows.map((row) => {
    const tgcType = getTgcType(row)
    if (!tgcType) return row
    return { ...row, isEditable: row.isApiEditable === false ? false : value === tgcType }
  })

const ConfigurationRangePsa = ({ refreshData }) => {
  const keycloak = useSession()
  const { plantObject, year } = useSelector((state) => state.dataGridStore)

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({ message: '', severity: 'info' })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [rows, setRows] = useState([])
  const [customModifiedCells, setCustomModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

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
      const apiRes = await ProductionNormsApiService.getConfigurationData(keycloak, PLANT_ID, AOP_YEAR)

      if (!apiRes?.length) {
        showSnackbar('No data found')
        setRows([])
        return
      }

      let formattedData = apiRes.map((apiItem, index) => {
        const configItem = FILTER_CONFIG.find((d) => d.displayName === apiItem.displayName) || {}
        const isReadonlyRow = PSA_READONLY_ROWS.includes(apiItem.displayName)
        const isToggleRow = apiItem.displayName === TGC2_TOGGLE_ROW

        // Rows with empty selection from API have no checkbox
        const fallbackHideCheckbox = configItem.hideCheckbox ?? !apiItem.selection
        const hideCheckbox = resolveHideCheckbox(apiItem.dependentAttributeConfig, fallbackHideCheckbox)

        // NOTE: API sends isEditable=false for every row, so it is ignored here.
        // Only the PSA read-only rows are locked; TGC rows are driven by the dropdown.
        const isApiEditable = !isReadonlyRow

        const nonEditableFields = isReadonlyRow
          ? ALL_EDITABLE_FIELDS
          : (configItem.nonEditableFields || [])

        const type = configItem.type || 'number1'
        const config = parseConfig(apiItem.config)

        return {
          ...configItem,
          ...apiItem,
          config,
          type,
          dataType: type,
          options: configItem.options || [],
          hideCheckbox,
          hideCheckBox: hideCheckbox,
          isEditable: isApiEditable,
          isApiEditable,
          nonEditableFields,
          editableFields: isReadonlyRow ? [] : undefined,
          remarks: apiItem.remarks || '',
          id: apiItem.id || apiItem.normParameterFkId || index + 1,
          selection: String(apiItem.selection ?? '').toLowerCase() === 'true',
          // Default the dropdown if the backend sent it empty
          targetValue: isToggleRow && !apiItem.targetValue ? configItem.targetValue : apiItem.targetValue,
        }
      })

      // Initial TGC2 Online/Offline toggle state
      const toggleRow = formattedData.find((row) => row.displayName === TGC2_TOGGLE_ROW)
      if (toggleRow) formattedData = applyTgcToggle(formattedData, toggleRow.targetValue)

      setRows(formattedData)
    } catch (error) {
      console.error('Error fetching data:', error)
      showSnackbar('Error fetching data', 'error')
    } finally {
      setLoading(false)
    }
  }

  const saveChanges = async () => {
    const modifiedData = Object.values(modifiedCells)
    const dataToSave = modifiedData.filter((row) => row.inEdit)
    if (!dataToSave.length) return showSnackbar('No Records to Save!')

    setLoading(true)

    const payload = modifiedData.map((rest) => ({
      ...rest,
      isEditable: rest.isApiEditable !== undefined ? rest.isApiEditable : rest.isEditable,
    }))

    try {
      const response = await ProductionNormsApiService.saveConfigurationData(keycloak, AOP_YEAR, payload, PLANT_ID)
      setModifiedCells({})

      showSnackbar(`Successfully saved ${modifiedData.length} changes!`, 'success')
      if (response?.code === 422) {
        setTimeout(() => showSnackbar(response.message || 'Validation error occurred.', 'error', false), 1000)
      }
      await fetchConfigurationData()
    } catch (error) {
      console.error('Error saving data:', error)
      showSnackbar('Failed to save changes. Please try again.', 'error')
      setLoading(false)
    }
  }

  const handleExcelUpload = async (file) => {
    if (!file) return
    setLoading(true)
    try {
      const response = await ProductionNormsApiService.importConfigurationExcel(file, keycloak, PLANT_ID, AOP_YEAR)

      if (response?.code === 200) {
        showSnackbar(response.message || 'Excel file imported successfully!', 'success')
        await fetchConfigurationData()
      } else if (response?.code === 400 && response?.data) {
        downloadErrorExcel(response.data)
        showSnackbar(response.message || 'Import failed with errors. Please check the downloaded file.', 'error')
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

      const blob = new Blob([bytes], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
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
      await ProductionNormsApiService.exportConfigurationExcel(keycloak, PLANT_ID, AOP_YEAR)
      showSnackbar('Excel download completed successfully!', 'success')
    } catch (error) {
      showSnackbar('Excel download failed. Please try again.', 'error')
    }
  }

  // Dropdown change on 'TGC2 Online/Offline' enables/disables the TGC Online/Offline rows
  const handleCustomItemChange = ({ dataItem, field, value }, setRowsCallback) => {
    if (!['targetValue', 'range'].includes(field)) return

    if (dataItem.displayName === TGC2_TOGGLE_ROW && field === 'targetValue') {
      setRowsCallback((currentRows) => applyTgcToggle(currentRows, value))
    }
  }

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
        permissions={{ ...PERMISSIONS, ExcelName: `${PERMISSIONS.ExcelName}_${AOP_YEAR}` }}
        handleRemarkCellClick={(row) => { setCurrentRemark(row.remarks || ''); setCurrentRowId(row.id); setRemarkDialogOpen(true) }}
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
        customItemChange={handleCustomItemChange}
        externalCustomModifiedCells={customModifiedCells}
        externalSetCustomModifiedCells={setCustomModifiedCells}
        paginationConfig={{ threshold: 100, buttonCount: 5, pageSizes: [10, 20, 50, 100], defaultPageSize: 100 }}
      />
    </Box>
  )
}

export default ConfigurationRangePsa