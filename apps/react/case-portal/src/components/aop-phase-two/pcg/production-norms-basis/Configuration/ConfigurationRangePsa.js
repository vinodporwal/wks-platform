import { useEffect, useRef, useState } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import RowBasedKendoTable from 'components/aop-phase-two/common/RowBasedKendoTable/index'
import { ProductionNormsApiService } from 'components/aop-phase-two/services/pcg/productionNormsApiService'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const TARGET_VALUE_TITLE = 'Target Value'

const COLUMNS = [
  { field: 'displayName', title: 'Data Filters', widthT: 250, minWidth: 200, type: 'text', editable: false },
  { field: 'uom', title: 'UOM', widthT: 80, minWidth: 60, type: 'text', editable: false },
  { field: 'targetValue', title: TARGET_VALUE_TITLE, editable: true, widthT: 120, minWidth: 100, type: 'row-based' },
  { field: 'range', title: 'Range', editable: true, widthT: 120, minWidth: 100, type: 'row-based' },
  { field: 'selection', title: 'Selection', editable: true, widthT: 80, minWidth: 80, type: 'checkbox' },
  { field: 'remarks', title: 'Criteria', widthT: 250, type: 'textarea', editable: false, minWidth: 250, showPlaceholder: false },
  { field: 'dependantAttributeId', title: 'Applicable Utilities & Cat-Chems', widthT: 250, minWidth: 200, type: 'text', editable: false },
]

// Name matching ignores case and spaces, so "TGC1Online/Offline" and "TGC1 Online/Offline" both match
const norm = (s) => String(s ?? '').toLowerCase().replace(/\s+/g, '')

// Rows that must be fully disabled (targetValue, range, selection, remarks).
// DTA and SEZ have different row names (SEZ names carry a "Z" prefix), so each site has its own list.
const DTA_PSA_READONLY_ROWS = [
  'Crude-1 311PA651-246 sulphur',
  'Crude-2 312PA651-246 sulphur',
  '395FX019',
  '395JWALLRW001',
]

const SEZ_PSA_READONLY_ROWS = [
  'Crude-1 Z311PA651-246 sulphur',
  'Crude-2 Z312PA651-246 sulphur',
  'ZC395IN008',
  'ZC395IN001',
]

const DTA_PSA_READONLY_NORM = DTA_PSA_READONLY_ROWS.map(norm)
const SEZ_PSA_READONLY_NORM = SEZ_PSA_READONLY_ROWS.map(norm)

const ALL_EDITABLE_FIELDS = ['targetValue', 'range', 'selection', 'remarks']

// One entry per dropdown (toggle) row.
// prefix = normalised start of the child row names, e.g. "TGC1 Offline (...)" -> "tgc1"
const TOGGLES = [
  { toggleName: 'TGC2 Online/Offline', prefix: 'tgc2', options: ['Online', 'Offline'], defaultValue: 'Offline' },
  { toggleName: 'TGC1 Online/Offline', prefix: 'tgc1', options: ['Online', 'Offline'], defaultValue: 'Online' },
  { toggleName: 'CO2 Ingestion Mode On/Off', prefix: 'co2ingestionmode', options: ['On', 'Off'], defaultValue: 'On' },
]

// Kept for backward compatibility with older references
const TGC2_TOGGLE_ROW = TOGGLES[0].toggleName

const getToggleByRow = (row) => TOGGLES.find((t) => norm(t.toggleName) === norm(row?.displayName))

// Fallback / per-row config (matched by displayName)
const FILTER_CONFIG = TOGGLES.map((t) => ({
  displayName: t.toggleName,
  targetValue: t.defaultValue,
  type: 'dropdown',
  options: t.options,
  nonEditableFields: ['range'],
  hideCheckbox: true,
}))

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

// ---- Target Value must be numeric (Range stays free text) ----
// Fallback cleanup: keeps digits, one decimal point and a leading minus.
const sanitizeNumber = (value) => {
  const str = String(value ?? '')
  const negative = str.trim().startsWith('-')
  const cleaned = str.replace(/[^0-9.]/g, '')
  const [intPart, ...decimalParts] = cleaned.split('.')
  const result = decimalParts.length ? `${intPart}.${decimalParts.join('')}` : intPart
  return (negative ? '-' : '') + result
}

// Final value check (used on save)
const isNumeric = (value) => {
  const str = String(value ?? '').trim()
  return str === '' || /^-?\d+(\.\d+)?$/.test(str)
}

// Partial value check while typing ("-", "5.", ".5" are allowed while the user is still typing)
const isTypingNumber = (value) => /^-?\d*\.?\d*$/.test(value)

// Only plain text rows are numeric-restricted; the dropdown (toggle) rows are left alone
const isNumericTargetRow = (row) => row?.type === 'text'

// Finds out whether an input sits in the "Target Value" column by matching its cell
// position to the grid header title.
const isTargetValueInput = (input) => {
  if (!input || input.tagName !== 'INPUT') return false
  if (input.type === 'checkbox' || input.type === 'radio') return false
  // Dropdown / combobox editors are not restricted
  if (input.closest('.k-dropdownlist, .k-combobox, .k-picker, [role="combobox"]')) return false

  const td = input.closest('td')
  const grid = input.closest('.k-grid')
  if (!td || !grid) return false

  const ariaIndex = td.getAttribute('aria-colindex')
  let headerCell = null
  if (ariaIndex) {
    headerCell = grid.querySelector(`.k-grid-header th[aria-colindex="${ariaIndex}"]`)
  }
  if (!headerCell) {
    const index = Array.prototype.indexOf.call(td.parentElement.children, td)
    const headerCells = grid.querySelectorAll('.k-grid-header thead tr:first-child th')
    headerCell = headerCells[index]
  }
  return (headerCell?.textContent || '').trim() === TARGET_VALUE_TITLE
}

// ---- Toggle helpers (TGC2 / TGC1 Online-Offline, CO2 Ingestion Mode On-Off) ----

// Returns the option ('Online' / 'Offline' / 'On' / 'Off') a child row belongs to, or null
const getChildOption = (row, toggle) => {
  const name = norm(row.displayName)
  if (name === norm(toggle.toggleName)) return null
  const idx = name.indexOf(toggle.prefix)
  if (idx === -1) return null
  const rest = name.slice(idx + toggle.prefix.length) // e.g. "offline(g385ii...)" or "on(g330fi...)"
  // longest option first, so "Online" is not mistaken for "On"
  const sorted = [...toggle.options].sort((a, b) => b.length - a.length)
  return sorted.find((opt) => rest.startsWith(opt.toLowerCase())) || null
}

// Enables only the child rows matching the dropdown value; the rest are disabled
const applyToggle = (rows, toggle, value) =>
  rows.map((row) => {
    const option = getChildOption(row, toggle)
    if (!option) return row
    return { ...row, isEditable: row.isApiEditable === false ? false : value === option }
  })

// Applies every toggle from the rows' current dropdown values (used after fetch)
const applyAllToggles = (rows) =>
  TOGGLES.reduce((acc, toggle) => {
    const toggleRow = acc.find((r) => norm(r.displayName) === norm(toggle.toggleName))
    return toggleRow ? applyToggle(acc, toggle, toggleRow.targetValue) : acc
  }, rows)

const ConfigurationRangePsa = ({ refreshData }) => {
  const keycloak = useSession()
  const { plantObject, year, verticalObject, siteObject } = useSelector((state) => state.dataGridStore)

  const vertical = verticalObject?.name?.toLowerCase()?.trim()
  const site = siteObject?.name?.toLowerCase()?.trim()
  const plant = plantObject?.name?.toLowerCase()?.trim()
  const IS_PCG_DTA_PSA = vertical === 'pcg' && site === 'dta' && plant === 'psa'
  const IS_PCG_SEZ_PSA = vertical === 'pcg' && site === 'sez' && plant === 'psa'

  // Read-only row names for the current site (nothing is locked for any other plant)
  const readonlyRowNames = IS_PCG_SEZ_PSA
    ? SEZ_PSA_READONLY_NORM
    : IS_PCG_DTA_PSA
      ? DTA_PSA_READONLY_NORM
      : []

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const containerRef = useRef(null)
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

  // Block non-numeric typing / pasting in the Target Value column before it reaches the input
  useEffect(() => {
    const container = containerRef.current
    if (!container) return undefined

    const getNextValue = (input, insertText) => {
      const value = input.value ?? ''
      const start = input.selectionStart ?? value.length
      const end = input.selectionEnd ?? value.length
      return value.slice(0, start) + insertText + value.slice(end)
    }

    const handleBeforeInput = (event) => {
      const input = event.target
      if (!isTargetValueInput(input)) return
      if (!event.inputType || !event.inputType.startsWith('insert')) return
      if (event.inputType === 'insertFromPaste') return // handled by the paste listener
      const text = event.data
      if (text === null || text === undefined) return
      if (!isTypingNumber(getNextValue(input, text))) event.preventDefault()
    }

    const handlePaste = (event) => {
      const input = event.target
      if (!isTargetValueInput(input)) return
      const text = event.clipboardData?.getData('text') ?? ''
      if (!isTypingNumber(getNextValue(input, text))) event.preventDefault()
    }

    container.addEventListener('beforeinput', handleBeforeInput, true)
    container.addEventListener('paste', handlePaste, true)
    return () => {
      container.removeEventListener('beforeinput', handleBeforeInput, true)
      container.removeEventListener('paste', handlePaste, true)
    }
  }, [])

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) fetchConfigurationData()
  }, [PLANT_ID, AOP_YEAR, refreshData, IS_PCG_DTA_PSA, IS_PCG_SEZ_PSA])

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
        const configItem = FILTER_CONFIG.find((d) => norm(d.displayName) === norm(apiItem.displayName)) || {}
        const isReadonlyRow = readonlyRowNames.includes(norm(apiItem.displayName))
        const isToggleRow = Boolean(getToggleByRow(apiItem))

        // Rows with empty selection from API have no checkbox
        const fallbackHideCheckbox = configItem.hideCheckbox ?? !apiItem.selection
        const hideCheckbox = resolveHideCheckbox(apiItem.dependentAttributeConfig, fallbackHideCheckbox)

        // NOTE: API sends isEditable=false for every row, so it is ignored here.
        // Only the PSA read-only rows are locked; toggle child rows are driven by the dropdowns.
        const isApiEditable = !isReadonlyRow

        const nonEditableFields = isReadonlyRow
          ? ALL_EDITABLE_FIELDS
          : (configItem.nonEditableFields || [])

        // Row type is 'text' so Range accepts +-1, +-5 etc. (Target Value is restricted to numbers)
        const type = configItem.type || 'text'
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

      // Initial toggle state for TGC2, TGC1 and CO2 Ingestion Mode
      formattedData = applyAllToggles(formattedData)

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

    // Target Value must be a number (Range can be free text)
    const invalidRow = modifiedData.find(
      (row) => isNumericTargetRow(row) && !isNumeric(row.targetValue),
    )
    if (invalidRow) {
      return showSnackbar(`Target Value must be a number for "${invalidRow.displayName}"`, 'error')
    }

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

  // Dropdown change on a toggle row (TGC2 / TGC1 Online-Offline, CO2 Ingestion Mode On-Off)
  // enables/disables its matching child rows.
  // Fallback cleanup for Target Value in case a non-numeric value still gets through.
  const handleCustomItemChange = ({ dataItem, field, value }, setRowsCallback) => {
    if (!['targetValue', 'range'].includes(field)) return

    const toggle = getToggleByRow(dataItem)
    if (toggle && field === 'targetValue') {
      setRowsCallback((currentRows) => applyToggle(currentRows, toggle, value))
      return
    }

    if (field === 'targetValue' && isNumericTargetRow(dataItem)) {
      const cleaned = sanitizeNumber(value)
      if (cleaned !== String(value ?? '')) {
        setRowsCallback((currentRows) =>
          currentRows.map((row) => (row.id === dataItem.id ? { ...row, targetValue: cleaned } : row)),
        )
      }
    }
  }

  return (
    <Box ref={containerRef}>
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