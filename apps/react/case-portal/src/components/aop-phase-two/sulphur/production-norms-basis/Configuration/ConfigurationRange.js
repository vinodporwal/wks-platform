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

const FILTER_CONFIG = [
  {
    id: 1,
    displayName: 'Liquid Sulphur Production Min\nCriteria-More than target value',
    uom: 'TPD',
    apr: 1025,
    may: '',
    jun: '',
    jul: '',
    aug: '',
    sep: '',
    oct: '',
    nov: '',
    dec: '',
    jan: '',
    feb: '',
    mar: '',
    remarks: '',
    dependantAttributeId:
      'Fuel Consumption CBA, Power Consumption CBA, HP steam CBA, LP steam generation CBA, LP steam TGTU, HP steam TGTU, Specific Power consumption TGTU, Fuel Gas TGTU, H2 Consumption TGTU',
    isEditable: true,
    type: 'number1',
  },
  {
    id: 2,
    displayName: 'Liquid Sulphur Production Max\nLess than target value',
    uom: 'TPD',
    apr: 2049,
    may: '',
    jun: '',
    jul: '',
    aug: '',
    sep: '',
    oct: '',
    nov: '',
    dec: '',
    jan: '',
    feb: '',
    mar: '',
    remarks: '',
    dependantAttributeId:
      'Fuel Consumption CBA, Power Consumption CBA, HP steam CBA, LP steam generation CBA, LP steam TGTU, HP steam TGTU, Specific Power consumption TGTU, Fuel Gas TGTU, H2 Consumption TGTU',
    isEditable: true,
    type: 'number1',
  },
  {
    id: 3,
    displayName:
      'Liquid Sulphur Production Norm basis selection\n1. Last 6 months average\n2. Last 12 months average\n3. Last FY Average\n4. Predictor (Based on last 12 months)\n5. Last 4 Years Benchmark Comparison\n6. Manual Entry Norm',
    uom: 'MTPD',
    apr: '',
    may: null,
    jun: null,
    jul: null,
    aug: null,
    sep: null,
    oct: null,
    nov: null,
    dec: null,
    jan: null,
    feb: null,
    mar: null,
    remarks: '',
    dependantAttributeId:
      'Fuel Consumption CBA, Power Consumption CBA, HP steam CBA, LP steam generation CBA, LP steam TGTU, HP steam TGTU, Specific Power consumption TGTU, Fuel Gas TGTU, H2 Consumption TGTU',
    isEditable: true,
    type: 'number1',
  },
  {
    id: 4,
    displayName: 'Rich Amine processed Min (Lower Bound (SOR))',
    uom: 'm3/Hr/Train',
    apr: 350,
    may: '',
    jun: '',
    jul: '',
    aug: '',
    sep: '',
    oct: '',
    nov: '',
    dec: '',
    jan: '',
    feb: '',
    mar: '',
    remarks: '',
    dependantAttributeId: 'LP steam consumption ATU',
    isEditable: true,
    type: 'number1',
  },
  {
    id: 5,
    displayName: 'Rich Amine processed Max (Upper Bound (EOR))',
    uom: 'm3/Hr/Train',
    apr: 600,
    may: '',
    jun: '',
    jul: '',
    aug: '',
    sep: '',
    oct: '',
    nov: '',
    dec: '',
    jan: '',
    feb: '',
    mar: '',
    remarks: '',
    dependantAttributeId: 'LP steam consumption ATU',
    isEditable: true,
    type: 'number1',
  },
  {
    id: 6,
    displayName: 'Sour water Processing Min (Lower Bound (SOR))',
    uom: 'MT/M3 Sour water',
    apr: 140,
    may: '',
    jun: '',
    jul: '',
    aug: '',
    sep: '',
    oct: '',
    nov: '',
    dec: '',
    jan: '',
    feb: '',
    mar: '',
    remarks: '',
    dependantAttributeId: 'LP steam SWS',
    isEditable: true,
    type: 'number1',
  },
  {
    id: 7,
    displayName: 'Sour water Processing Max (Upper Bound (EOR))',
    uom: 'MT/M3 Sour water',
    apr: 260,
    may: '',
    jun: '',
    jul: '',
    aug: '',
    sep: '',
    oct: '',
    nov: '',
    dec: '',
    jan: '',
    feb: '',
    mar: '',
    remarks: '',
    dependantAttributeId: 'LP steam SWS',
    isEditable: true,
    type: 'number1',
  },
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
    ...monthColumns.map((month) => ({
      field: month.field,
      title: headerMap[month.headerIndex],
      minWidth: 140,
      type: 'row-based',
      editable: true,
      format: valueFormat,
    })),
    {
      field: 'remarks',
      title: 'Remark',
      widthT: 160,
      type: 'textarea',
      editable: false,
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
      const apiRes = FILTER_CONFIG
      // await ProductionNormsApiService.getConfigurationData(
      //   keycloak,
      //   PLANT_ID,
      //   AOP_YEAR,
      // )

      const res = apiRes?.length
        ? apiRes.map((apiItem) => {
            const configItem =
              FILTER_CONFIG.find((d) =>
                [apiItem.displayName, apiItem.productName].some(
                  (name) =>
                    name &&
                    (d.displayName.toLowerCase().includes(name.toLowerCase()) ||
                      name
                        .toLowerCase()
                        .includes(d.displayName.split('\n')[0].toLowerCase())),
                ),
              ) || {}

            const fallbackHideCheckbox =
              apiItem.hideCheckbox ??
              apiItem.hideCheckBox ??
              configItem.hideCheckbox ??
              configItem.hideCheckBox ??
              false
            const hideCheckbox = resolveHideCheckbox(
              apiItem.dependentAttributeConfig,
              fallbackHideCheckbox,
            )

            return {
              ...configItem,
              ...apiItem,
              hideCheckbox,
              hideCheckBox: hideCheckbox,
              isEditable: apiItem.isEditable ?? configItem.isEditable ?? true,
              nonEditableFields: configItem.nonEditableFields || [],
              type: apiItem.type || configItem.type || 'number1',
              dataType: apiItem.type || configItem.type || 'number1',
              options: configItem.options || [],
            }
          })
        : FILTER_CONFIG

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
        const type =
          item.type || item.dataType || (mappingKeys.length ? 'dropdown' : undefined)
        const fallbackHideCheckbox = item.hideCheckbox ?? item.hideCheckBox ?? false
        const hideCheckbox = resolveHideCheckbox(
          item.dependentAttributeConfig,
          fallbackHideCheckbox,
        )

        return {
          ...item,
          hideCheckbox,
          hideCheckBox: hideCheckbox,
          config,
          type,
          options: item.options?.length ? item.options : mappingKeys,
          remarks: item.remarks || '',
          id: item.id || index + 1,
          attributeValue: ['date', 'datetime'].includes(type)
            ? formatDate(item.attributeValue)
            : item.attributeValue,
          allowNegative: item.allowNegative,
          selection: item.selection?.toLowerCase() === 'true' ? true : false,
        }
      })

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

    setLoading(true)

    const payload = modifiedData.map((rest) => ({
      ...rest,
    }))

    try {
      const response = await ProductionNormsApiService.saveConfigurationData(
        keycloak,
        AOP_YEAR,
        payload,
        PLANT_ID,
      )
      setModifiedCells({})

      showSnackbar(`Successfully saved ${modifiedData.length} changes!`, 'success')
      if (response?.code === 422) {
        setTimeout(
          () =>
            showSnackbar(
              response.message || 'Validation error occurred.',
              'error',
              false,
            ),
          1000,
        )
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
