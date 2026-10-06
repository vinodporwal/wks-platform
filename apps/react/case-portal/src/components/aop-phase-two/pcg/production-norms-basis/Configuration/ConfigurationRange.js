import { useEffect, useState } from 'react'
import { Box, Backdrop, CircularProgress } from '@mui/material'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import {
  ValueFormatterPhaseTwo,
  customValueFormatterPhaseTwo,
} from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import RowBasedKendoTable from 'components/aop-phase-two/common/RowBasedKendoTable/index'
import { ProductionNormsApiService } from 'components/aop-phase-two/services/pcg/productionNormsApiService'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const Configuration = ({ startDate, endDate, refreshData }) => {
  const keycloak = useSession()

  const [modifiedCells, setModifiedCells] = useState({})
  const [customModifiedCells, setCustomModifiedCells] = useState({})
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
  const headerMap = generateHeaderNames(AOP_YEAR)
  const valueFormat = customValueFormatterPhaseTwo(5)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [dependencyRules, setDependencyRules] = useState({})


  const columns = [
    {
      field: 'displayName',
      title: 'Data Filters',
      widthT: 250,
      minWidth: 200,
      type: 'text',
      editable: false,
    },
    {
      field: 'uom',
      title: 'UOM',
      widthT: 80,
      minWidth: 60,
      type: 'text',
      editable: false,
    },
    {
      field: 'targetValue',
      title: 'Target Value',
      editable: true,
      widthT: 120,
      minWidth: 100,
      type: 'row-based',
    },
    {
      field: 'range',
      title: 'Range',
      editable: true,
      widthT: 120,
      minWidth: 100,
      type: 'row-based',
    },
    {
      field: 'selection',
      title: 'Selection',
      editable: true,
      widthT: 80,
      minWidth: 80,
      type: 'checkbox',
    },
    {
      field: 'remarks',
      title: 'Remarks',
      widthT: 250,
      type: 'textarea',
      editable: true,
      minWidth: 250,
      showPlaceholder: false,
    },
    {
      field: 'dependantAttributeId',
      title: 'Applicable Utilities & Cat-Chems',
      widthT: 250,
      minWidth: 200,
      type: 'text',
      editable: false,
    },
  ]

const FILTER_CONFIG_DUMMY_DATA = [
  {
    id: 1,
    displayName: 'Sulphur Production',
    uom: '',
    targetValue: 'PIMS Sulphur Production (auto fetch from PIMS throughput tab)',
    range: '+/- 5%',
    selection: false,
    remarks: '',
    utilities: 'Fuel, HP Stam, LP Steam, Oxygen, Power, Return Steam Condensate, BFW',
    isEditable: true,
    nonEditableFields: ['targetValue'],
    type: 'formula-text',
  },
  {
    id: 2,
    displayName: 'AGR C003 Inline/Bypass',
    uom: '',
    targetValue: 'Bypass',
    range: '+/-5%',
    selection: false,
    remarks: '',
    utilities: 'Fuel, HP Stam, LP Steam, Oxygen, Power, Return Steam Condensate, BFW',
    isEditable: true,
    nonEditableFields: ['range'],
    type: 'dropdown',
    options: ['Inline', 'Bypass'],
  },
  {
    id: 3,
    displayName: 'AGR C003 Inline (G330PI400340 A/B/C)',
    uom: 'kg/cm2g',
    targetValue: '1.2',
    range: '',
    selection: false,
    remarks: 'Less than target value',
    utilities: 'Fuel, HP Stam, LP Steam, Oxygen, Power, Return Steam Condensate, BFW',
    isEditable: true,
    nonEditableFields: ['range'],
    type: 'text',
    hideCheckbox: true,
  },
  {
    id: 4,
    displayName: 'AGR C003 Inline ( G330FIC400305)',
    uom: '%',
    targetValue: '10',
    range: '',
    selection: false,
    remarks: 'more than target value',
    utilities: 'Fuel, HP Stam, LP Steam, Oxygen, Power, Return Steam Condensate, BFW',
    isEditable: true,
    nonEditableFields: ['range'],
    type: 'text',
    hideCheckbox: true,
  },
  {
    id: 5,
    displayName: 'AGR C003 Bypass (G330PI400340 A/B/C)',
    uom: 'kg/cm2g',
    targetValue: '1.6',
    range: '',
    selection: false,
    remarks: 'more than target value',
    utilities: 'Fuel, HP Stam, LP Steam, Oxygen, Power, Return Steam Condensate, BFW',
    isEditable: true,
    nonEditableFields: ['range'],
    type: 'text',
    hideCheckbox: true,
  },
  {
    id: 6,
    displayName: 'AGR C003 Bypass (G330FIC400305)',
    uom: '%',
    targetValue: '2',
    range: '',
    selection: false,
    remarks: 'Less than target value',
    utilities: 'Fuel, HP Stam, LP Steam, Oxygen, Power, Return Steam Condensate, BFW',
    isEditable: true,
    nonEditableFields: ['range'],
    type: 'text',
    hideCheckbox: true,
  },
  {
    id: 7,
    displayName: 'J1 Acid Gas Flow',
    uom: 'Nm3/Hr',
    targetValue: 'Manual input',
    range: '+/- 500',
    selection: false,
    remarks: '',
    utilities: 'Fuel, HP Stam, LP Steam, Oxygen, Power, Return Steam Condensate, BFW',
    isEditable: true,
    nonEditableFields: [],
    type: 'formula-text',
  },
  {
    id: 8,
    displayName: 'Average J3 Acid Gas H2S Concentration',
    uom: '%',
    targetValue: 'Manual input',
    range: '+/- 2%',
    selection: false,
    remarks: '',
    utilities: 'Fuel, HP Stam, LP Steam, Oxygen, Power, Return Steam Condensate, BFW',
    isEditable: true,
    nonEditableFields: [],
    type: 'formula-text',
  },
  {
    id: 9,
    displayName: 'Incinerator Temperature (G360TIC000157)',
    uom: 'Deg C',
    targetValue: '740',
    range: '',
    selection: false,
    remarks: 'more than target value',
    utilities: 'Fuel, HP Stam, LP Steam, Return Steam Condensate, BFW',
    isEditable: true,
    nonEditableFields: ['range'],
    type: 'text',
  },
  {
    id: 10,
    displayName: 'Incinerator Temperature (G361TIC000157)',
    uom: 'Deg C',
    targetValue: '740',
    range: '',
    selection: false,
    remarks: 'more than target value',
    utilities: 'Fuel, HP Stam, LP Steam, Return Steam Condensate, BFW',
    isEditable: true,
    nonEditableFields: ['range'],
    type: 'text',
  },
  {
    id: 11,
    displayName: 'Average O2 Enrichment',
    uom: '%',
    targetValue: 'Manual input',
    range: '+/- 2%',
    selection: false,
    remarks: '',
    utilities: 'Oxygen, Power',
    isEditable: true,
    nonEditableFields: [],
    type: 'formula-text',
  },
  {
    id: 12,
    displayName: 'SWS-1 Sour Water Processing Rate',
    uom: 'M3/Hr',
    targetValue: '200',
    range: '',
    selection: false,
    remarks: 'More than target value',
    utilities: 'LP Steam & Return Steam Condensate',
    isEditable: true,
    nonEditableFields: ['range'],
    type: 'text',
  },
  {
    id: 13,
    displayName: 'SRU-1 Sulphur production (GSR1PR001)',
    uom: 'TPD',
    targetValue: '50',
    range: '',
    selection: false,
    remarks: 'More than target value',
    utilities: 'LLP N2, LP N2, Instrument Air, DM Water, Utility Water, Plant Air, caustic, CHEM Ammonia, Chem Maxtreat 3223 SJ',
    isEditable: true,
    nonEditableFields: ['range'],
    type: 'text',
  },
  {
    id: 14,
    displayName: 'SRU-2 Sulphur production (GSR2PR001)',
    uom: 'TPD',
    targetValue: '50',
    range: '',
    selection: false,
    remarks: 'More than target value',
    utilities: 'LLP N2, LP N2, Instrument Air, DM Water, Utility Water, Plant Air, caustic, CHEM Ammonia, Chem Maxtreat 3223 SJ',
    isEditable: true,
    nonEditableFields: ['range'],
    type: 'text',
  }
]

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchConfigurationData()
    }
  }, [PLANT_ID, AOP_YEAR, refreshData])

  const fetchConfigurationData = async () => {
    setLoading(true)
    try {
      // const apiRes = FILTER_CONFIG_DUMMY_DATA
      const apiRes =  await ProductionNormsApiService.getConfigurationData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      
      let res = []
      if (apiRes && apiRes.length > 0) {
        // Merge API response with static config to preserve frontend rules like nonEditableFields and formula-text type
        res = apiRes.map((apiItem, index) => {
          const dummyItem = FILTER_CONFIG_DUMMY_DATA.find(d => d.displayName === apiItem.displayName || d.displayName === apiItem.productName) 
                         || FILTER_CONFIG_DUMMY_DATA[index] || {};
                         
          return {
            ...dummyItem,
            ...apiItem,
            // Preserve UI specific fields from dummy data over API response if they exist
            isEditable: dummyItem?.isEditable,
            nonEditableFields: dummyItem?.nonEditableFields || [],
            type: dummyItem?.type || 'text',
            options: dummyItem?.options || [],
          }
        })
      } else {
        // Fallback to dummy data if API returns empty (first load)
        res = FILTER_CONFIG_DUMMY_DATA
      }

      if (res?.length === 0) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        return
      }
      const updatedFormattedData = res?.map((item, index) => {
        // Parse config from JSON string if it exists
        let parsedAttributeValue = null
        if (item.config) {
          try {
            parsedAttributeValue =
              typeof item.config === 'string'
                ? JSON.parse(item.config)
                : item.config
          } catch (e) {
            console.error('Error parsing config:', e)
            parsedAttributeValue = null
          }
        }

        const mappingKeys = parsedAttributeValue?.valueMapping
          ? Object.keys(parsedAttributeValue.valueMapping)
          : []

        // Preserve existing type (date, dropdown, etc.) or infer from dependencies
        // Default to 'number' if no type is specified
        const type = item.type || (mappingKeys.length ? 'dropdown' : undefined)

        // Format date values to YYYY-MM-DD string format
        let formattedAttributeValue = item.attributeValue
        if ((type === 'date' || type === 'datetime') && item.attributeValue) {
          try {
            const dateObj = new Date(item.attributeValue)
            if (!isNaN(dateObj.getTime())) {
              const year = dateObj.getFullYear()
              const month = String(dateObj.getMonth() + 1).padStart(2, '0')
              const day = String(dateObj.getDate()).padStart(2, '0')
              formattedAttributeValue = `${year}-${month}-${day}`
            }
          } catch (e) {
            console.error('Error formatting date:', e)
          }
        }

        return {
          ...item,
          config: parsedAttributeValue,
          type,
          options: item.options?.length ? item.options : mappingKeys,
          remarks: item.remarks || '',
          id: item?.id || index + 1,
          attributeValue: formattedAttributeValue,
          isEditable: item.isEditable,
          allowNegative: item?.allowNegative || item.name === 'Additional TSRF',
        }
      })
      // Apply AGR C003 Inline/Bypass logic on initial load
      const row2 = updatedFormattedData.find((r) => r.id === 2)
      if (row2 && row2.targetValue) {
        updatedFormattedData.forEach((row) => {
          if (row2.targetValue === 'Bypass') {
            if (row.displayName === 'AGR C003 Inline ( G330FIC400305)' || row.displayName === 'AGR C003 Inline (G330PI400340 A/B/C)') row.isEditable = false
            if (row.displayName === 'AGR C003 Bypass (G330FIC400305)' || row.displayName === 'AGR C003 Bypass (G330PI400340 A/B/C)') row.isEditable = true
          } else if (row2.targetValue === 'Inline') {
            if (row.displayName === 'AGR C003 Inline (G330PI400340 A/B/C)' || row.displayName === 'AGR C003 Inline (G330PI400340 A/B/C)') row.isEditable = true
            if (row.displayName === 'AGR C003 Bypass (G330FIC400305)' || row.displayName === 'AGR C003 Bypass (G330PI400340 A/B/C)') row.isEditable = false
          }
        })
      }

      setRows(updatedFormattedData)
      setOriginalRows(updatedFormattedData)
      
    } catch (error) {
      console.error('Error fetching configuration data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
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
    // showExport: true,
    downloadExcelBtnFromUI: true,
    ExcelName: `Production_Norms_Configuration_${AOP_YEAR}`,
    showImport: false,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Configuration',
  }

  const formatDateForAPI = (date) => {
    if (!date) return ''
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

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

    const fieldsToCheck = ['targetValue', 'range']
    const validationError = validateRowDataWithRemarks(
      data.filter((item) => item.isEditable == true),
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

    // Transform payload to stringify config field for backend
    const payload = modifiedData.map((item) => {
      const { config, ...rest } = item
      return {
        ...rest,
        // Stringify config if it exists and is an object
        config:
          config && typeof config === 'object'
            ? JSON.stringify(config)
            : config,
      }
    })

    try {

      const response = await ProductionNormsApiService.saveConfigurationData(
        keycloak,
        AOP_YEAR,
        payload,
        PLANT_ID,
      )

      setModifiedCells({})

      if (response?.code === 422) {
        setLoading(false)
        // Show success notification first
        setSnackbarOpen(true)
        setSnackbarData({
          message: `Successfully saved ${modifiedData.length} changes!`,
          severity: 'success',
        })

        // Then show validation error after a delay
        setTimeout(() => {
          setSnackbarOpen(true)
          setSnackbarData({
            message: response.message || 'Validation error occurred.',
            severity: 'error',
            autoHide: false,
          })
        }, 1000)
      } else {
        // Code 200 - show only success notification
        setSnackbarOpen(true)
        setSnackbarData({
          message: `Successfully saved ${modifiedData.length} changes!`,
          severity: 'success',
        })
      }

      await fetchConfigurationData()
    } catch (error) {
      setLoading(false)
      console.error('Error saving configuration data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to save changes. Please try again.',
        severity: 'error',
      })
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
          link.download = `Configuration_Errors_${new Date().getTime()}.xlsx`
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
    } 
  }

  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'info',
    })

    try {
      await ProductionNormsApiService.exportConfigurationExcel(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting Configuration data:', error)
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

  const handleCustomItemChange = (e, setRowsCallback) => {
    const { dataItem, field, value } = e

    if (field !== 'targetValue' && field !== 'range') return

    // Dynamic row disable logic for AGR C003 Inline/Bypass
    if (dataItem.id === 2 && field === 'targetValue') {
      setRowsCallback((currentRows) => {
        return currentRows.map((row) => {
          if (value === 'Bypass') {
            if (row.displayName === 'AGR C003 Inline ( G330FIC400305)' || row.displayName === 'AGR C003 Inline (G330PI400340 A/B/C)') return { ...row, isEditable: false }
            if (row.displayName === 'AGR C003 Bypass (G330FIC400305)' || row.displayName === 'AGR C003 Bypass (G330PI400340 A/B/C)') return { ...row, isEditable: true }
          } else if (value === 'Inline') {
            if (row.displayName === 'AGR C003 Inline ( G330FIC400305)' || row.displayName === 'AGR C003 Inline (G330PI400340 A/B/C)') return { ...row, isEditable: true }
            if (row.displayName === 'AGR C003 Bypass (G330FIC400305)' || row.displayName === 'AGR C003 Bypass (G330PI400340 A/B/C)') return { ...row, isEditable: false }
          }
          return row
        })
      })
    }

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
        title={permissions.showTitle ? permissions.titleName : ''}
        permissions={permissions}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={() => {}}
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
