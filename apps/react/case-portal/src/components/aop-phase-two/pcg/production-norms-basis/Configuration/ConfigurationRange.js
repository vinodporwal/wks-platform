import { useEffect, useState } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
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

// Rows that must be completely read-only (user cannot edit anything)
const PSA_READONLY_ROWS = [
  'Crude-1 311PA651-246 sulphur',
  'Crude-2 312PA651-246 sulphur',
  '395FX019',
  '395JWALLRW001',
]

// Per-row UI config, matched with API rows by displayName.
// Any API row NOT listed here falls back to a numeric editor (type 'number1').
const FILTER_CONFIG = [
  // Dropdown (Online / Offline) only for the targetValue column of this row
  {
    displayName: 'TGC2 Online/Offline',
    type: 'dropdown',
    options: ['Online', 'Offline'],
    fieldTypes: { targetValue: 'dropdown', range: 'number1' }, // per-column editor type
    nonEditableFields: [], // range is editable
    hideCheckbox: false, // show the checkbox
    isEditable: true,
  },

  // PSA Product rows: fully disabled
  ...PSA_READONLY_ROWS.map((name) => ({
    displayName: name,
    type: 'number1',
    options: [],
    nonEditableFields: [],
    hideCheckbox: true,
    isEditable: false,
  })),
]

const PERMISSIONS = {
  showAction: true, addButton: false, deleteButton: false, editButton: true, saveBtn: true,
  allAction: true, downloadExcelBtnFromUI: true, ExcelName: 'Production_Norms_Configuration_Range',
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

const ConfigurationRange = ({ startDate, endDate, refreshData }) => {
  const keycloak = useSession()
  const { plantObject, year, verticalObject, siteObject } = useSelector((state) => state.dataGridStore)

  const vertical = verticalObject?.name?.toLowerCase()?.trim()
  const site = siteObject?.name?.toLowerCase()?.trim()
  const plant = plantObject?.name?.toLowerCase()?.trim()
  const IS_PCG_DTA_PSA = vertical === 'pcg' && site === 'dta' && plant === 'psa'

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

  const showSnackbar = (message, severity = 'info', autoHide = true) => {
    setSnackbarOpen(true)
    setSnackbarData({ message, severity, autoHide })
  }

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) fetchConfigurationData()
  }, [PLANT_ID, AOP_YEAR, refreshData, IS_PCG_DTA_PSA])

  const fetchConfigurationData = async () => {
    setLoading(true)
    try {
      const apiRes = await ProductionNormsApiService.getConfigurationData(keycloak, PLANT_ID, AOP_YEAR)

      // Merge API rows with local per-row config (matched by displayName).
      // Rows without config => numeric editor, editable (unless API says otherwise)
      const res = (apiRes || []).map((apiItem) => {
        const configItem =
          (IS_PCG_DTA_PSA ? FILTER_CONFIG : []).find(
            (d) =>
              d.displayName?.trim() === (apiItem.displayName || apiItem.productName)?.trim()
          ) || {}

        const fallbackHideCheckbox =
          apiItem.hideCheckbox ??
          apiItem.hideCheckBox ??
          configItem.hideCheckbox ??
          configItem.hideCheckBox ??
          false
        const hideCheckbox = resolveHideCheckbox(apiItem.dependentAttributeConfig, fallbackHideCheckbox)

        return {
          ...apiItem,
          ...configItem, // config wins over empty API values
          isEditable: configItem.isEditable ?? apiItem.isEditable ?? true,
          nonEditableFields: configItem.nonEditableFields || [],
          apiDataType: apiItem.dataType ?? '', // original backend dataType (UI overrides dataType below)
          fieldTypes: configItem.fieldTypes || null, // per-column editor type
          type: configItem.type || 'number1', // default = numeric
          dataType: configItem.type || 'number1',
          options: configItem.options || [],
          hideCheckbox,
          hideCheckBox: hideCheckbox,
        }
      })

      if (!res.length) {
        showSnackbar('No data found')
        setLoading(false)
        setRows([])
        setOriginalRows([])
        return
      }

      let formattedData = res.map((item, index) => {
        const config = parseConfig(item.config)
        const mappingKeys = config?.valueMapping ? Object.keys(config.valueMapping) : []
        const type = item.dataType || (mappingKeys.length ? 'dropdown' : undefined)
        const fallbackHideCheckbox = item.hideCheckbox ?? item.hideCheckBox ?? false
        const hideCheckbox = resolveHideCheckbox(item.dependentAttributeConfig, fallbackHideCheckbox)

        return {
          ...item,
          hideCheckbox,
          hideCheckBox: hideCheckbox,
          config, type,
          options: item.options?.length ? item.options : mappingKeys,
          remarks: item.remarks || '',
          id: item.id || item.normParameterFkId || index + 1,
          attributeValue: ['date', 'datetime'].includes(type) ? formatDate(item.attributeValue) : item.attributeValue,
          allowNegative: item.allowNegative,
          selection: item.selection?.toLowerCase() === 'true' ? true : false
        }
      })

      // (kept from Configuration.js) Inline/Bypass dependency - only applies if such a row exists
      const inlineBypassRow = formattedData.find(row => row.displayName === 'AGR C003 Inline/Bypass')
      if (inlineBypassRow) {
        const value = inlineBypassRow.targetValue
        formattedData = formattedData.map(row => {
          if (row.displayName !== 'AGR C003 Inline/Bypass') {
            const isInline = row.displayName?.includes('AGR C003 Inline')
            const isBypass = row.displayName?.includes('AGR C003 Bypass')
            if (isInline || isBypass) {
              return { ...row, isEditable: value === (isInline ? 'Inline' : 'Bypass') }
            }
          }
          return row
        })
      }

      setRows(formattedData)
      setOriginalRows(formattedData)
      setLoading(false)
    } catch (error) {
      console.error('Error fetching data:', error)
      showSnackbar('Error fetching data', 'error')
      setLoading(false)
    }
  }

  const saveChanges = async () => {
    const modifiedData = Object.values(modifiedCells)
    const dataToSave = modifiedData.filter((row) => row.inEdit)
    console.log('modifiedData', modifiedData)
    console.log('dataToSave', dataToSave)
    if (!dataToSave.length) return showSnackbar('No Records to Save!')

    // const validationError = validateRowDataWithRemarks(
    //   dataToSave.filter((item) => item.isEditable),
    //   originalRows,
    //   ['targetValue', 'range', 'selection'],
    //   'displayName'
    // )

    // if (validationError) return showSnackbar(validationError, 'error')

    setLoading(true)

    // Remove UI-only fields and restore the backend's original dataType
    // (the UI sets dataType = 'dropdown' for rendering, which the backend may reject for targetValue)
    const payload = modifiedData.map((row) => {
      const {
        type,
        options,
        fieldTypes,
        nonEditableFields,
        hideCheckbox,
        hideCheckBox,
        apiDataType,
        config,
        inEdit,
        ...rest
      } = row
      return { ...rest, dataType: apiDataType ?? '' }
    })

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

  const handleCustomItemChange = ({ dataItem, field, value }, setRowsCallback) => {
    if (!['targetValue', 'range'].includes(field)) return

    if (dataItem.displayName === 'AGR C003 Inline/Bypass' && field === 'targetValue') {
      setRowsCallback((currentRows) => currentRows.map((row) => {
        if (row.displayName !== 'AGR C003 Inline/Bypass') {
          const isInline = row.displayName?.includes('AGR C003 Inline')
          const isBypass = row.displayName?.includes('AGR C003 Bypass')
          if (isInline || isBypass) {
            return { ...row, isEditable: value === (isInline ? 'Inline' : 'Bypass') }
          }
        }
        return row
      }))
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
        handleRemarkCellClick={(row) => {setCurrentRemark(row.remarks || ''); setCurrentRowId(row.id); setRemarkDialogOpen(true)}}
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

export default ConfigurationRange