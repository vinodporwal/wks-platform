import { useEffect, useState, useCallback, useMemo } from 'react'
import { Box, Backdrop, CircularProgress } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { HeatRateApiService } from 'components/aop-phase-two/services/cpp/jmd/heatRateApiService'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { customValueFormatterPhaseTwo as customValueFormat } from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { useDebounce } from 'hooks/useDebounce'
import { downloadBase64Excel } from 'components/aop-phase-two/common/utilities/downloadBase64Excel'
const STGHeatRate = ({ startDate, endDate, dateLoading }) => {
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
  const {
    plantID,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
    jmdSelectedPlants,
  } = dataGridStore
  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const valueFormat = ValueFormatterPhaseTwo()

  const PLANT_ID_LIST = useMemo(
    () => jmdSelectedPlants?.map((plant) => plant.id) || [],
    [jmdSelectedPlants],
  )

  const [selectedPlant, setSelectedPlant] = useState(null)
  const [dropdownOptions, setDropdownOptions] = useState([])
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const columns = [
    {
      field: 'equipType',
      title: 'Equipment Type',
      widthT: 180,
      minWidth: 180,
      type: 'text',
      editable: false,
      locked: true,
    },
    {
      field: 'cppUtility',
      title: 'CPP Utility',
      widthT: 120,
      minWidth: 120,
      type: 'text',
      editable: false,
    },
    {
      field: 'stgLoad',
      title: 'STG Load (MW)',
      width: 150,
      minWidth: 150,
      type: 'number1',
      editable: false,
    },
    {
      field: 'oemHeatRate',
      title: 'OEM HR',
      widthT: 150,
      minWidth: 150,
      type: 'numberWithRadio',
      format: customValueFormat(1),
      editable: true,
      numericEditable: true,
      radioGroupField: 'selectedHeatRate',
      targetField: 'finalHeatRate',
      radioValue: 'OEM',
    },
    {
      field: 'prevYearFinalHeatRate',
      title: 'PREVIOUS YEAR BUDGET HR',
      widthT: 230,
      minWidth: 230,
      type: 'numberWithRadio',
      format: customValueFormat(1),
      editable: true,
      numericEditable: false,
      radioGroupField: 'selectedHeatRate',
      targetField: 'finalHeatRate',
      radioValue: 'PREVIOUS_YEAR',
    },
    {
      field: 'proposedYearFinalHeatRate',
      title: 'PROPOSED HR',
      subtitle: '(Based On Actual Data)',
      widthT: 200,
      minWidth: 200,
      type: 'numberWithRadio',
      format: customValueFormat(1),
      editable: true,
      numericEditable: false,
      radioGroupField: 'selectedHeatRate',
      targetField: 'finalHeatRate',
      radioValue: 'PROPOSED',
    },
    {
      field: 'finalHeatRate',
      title: 'Final HR',
      widthT: 150,
      minWidth: 150,
      type: 'number1',
      format: customValueFormat(1),
      editable: true,
    },

    {
      field: 'remarks',
      title: 'Remark',
      width: 230,
      type: 'textarea',
      editable: true,
      minWidth: 230,
    },
  ]

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])

  const formatDate = (date) => {
    if (!date) return ''
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  const getPlantList = useCallback(async () => {
    setLoading(true)
    try {
      const res = await HeatRateApiService.getGTAssetDropdown(
        keycloak,
        PLANT_ID_LIST,
        'STG',
      )

      // Convert to required format with plant name
      const convertedData = res?.map((item) => {
        // Find plant name from jmdSelectedPlants by matching cppPlantFkId
        const plant = jmdSelectedPlants?.find(
          (p) => p.id?.toUpperCase() === item.cppPlantFkId?.toUpperCase(),
        )
        const plantName = plant?.name

        return {
          id: item.assetId,
          name: `${item.assetName} (${plantName})`,
          plantName: plantName,
          cppPlantFkId: item.cppPlantFkId,
        }
      })

      // Sort by plantName
      convertedData?.sort((a, b) => {
        const nameA = a.plantName || ''
        const nameB = b.plantName || ''
        return nameA.localeCompare(nameB)
      })

      if (convertedData?.length === 0) {
        setDropdownOptions([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        setLoading(false)
        return
      }
      setSelectedPlant(convertedData[0]?.id)
      setDropdownOptions(convertedData)
    } catch (error) {
      console.error('Error fetching plant list:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR, jmdSelectedPlants])

  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && AOP_YEAR) {
        getPlantList()
        setModifiedCells({})
      }
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR, getPlantList],
  )

  useEffect(() => {
    if (AOP_YEAR && startDate && endDate && selectedPlant) {
      fetchHeatRateData(
        selectedPlant,
        formatDate(startDate),
        formatDate(endDate),
      )
    }
  }, [selectedPlant, startDate, endDate])

  const fetchHeatRateData = async (
    assetId,
    formattedStartDate,
    formattedEndDate,
  ) => {
    setLoading(true)
    try {
      const res = await HeatRateApiService.getSTGHeatRateData(
        keycloak,
        assetId,
        AOP_YEAR,
        formattedStartDate,
        formattedEndDate,
        PLANT_ID_LIST,
      )

      if (res?.data?.length === 0) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        return
      }
      setRows(res?.data)
      setOriginalRows(res?.data)
    } catch (error) {
      console.error('Error fetching STG heat rate data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }

  const dropdownConfig = {
    options: dropdownOptions,
    label: 'Plant',
    placeholder: 'Select Plant',
    valueKey: 'id',
    labelKey: 'name',
  }

  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showTitleNameBusiness: true,
    titleName: screenTitle?.title,
    showImport: true,
    showExport: true,
    ExcelName: `STG Heat Rate - ${AOP_YEAR}`,
    showTitle: true,
    showDropdown: true,
  }

  const saveChanges = async () => {
    setLoading(true)
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length == 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      setLoading(false)
      return
    }

    var rawData = Object.values(modifiedCells)
    const data = rawData.filter((row) => row.inEdit)
    if (data.length == 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      setLoading(false)
      return
    }

    // Custom validation: If any row data is updated, remarks must be filled and different from original
    const fieldsToCheck = [
      'oemHeatRate',
      'prevYearFinalHeatRate',
      'proposedYearFinalHeatRate',
      'finalHeatRate',
    ]
    const validationError = validateRowDataWithRemarks(
      data,
      originalRows,
      fieldsToCheck,
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

    try {
      const payload = modifiedData.map((item) => {
        const { inEdit, ...rest } = item
        return rest
      })
      const tempPayload = JSON.stringify(payload)

      await HeatRateApiService.saveSTGHeatRateData(keycloak, AOP_YEAR, payload)

      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved ${modifiedData.length} changes!`,
        severity: 'success',
      })
      await fetchHeatRateData(
        selectedPlant,
        formatDate(startDate),
        formatDate(endDate),
      )
    } catch (error) {
      console.error('Error saving heat rate data:', error)
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
      const formattedStartDate = formatDate(startDate)
      const formattedEndDate = formatDate(endDate)

      const response = await HeatRateApiService.saveSTGHeatRateExcel(
        file,
        keycloak,
        AOP_YEAR,
        selectedPlant,
        formattedStartDate,
        formattedEndDate,
        PLANT_ID_LIST,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Excel file imported successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        await fetchHeatRateData(
          selectedPlant,
          formattedStartDate,
          formattedEndDate,
        )
      } else if (response?.code === 400 && response?.data) {
        downloadBase64Excel(response.data, 'STG_Heat_Rate_Import_Status.xlsx')
        setSnackbarOpen(true)
        setSnackbarData({
          message:
            response?.message || 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        setModifiedCells({})
        await fetchHeatRateData(
          selectedPlant,
          formattedStartDate,
          formattedEndDate,
        )
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Upload Failed!',
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
      const formattedStartDate = startDate ? formatDate(startDate) : null
      const formattedEndDate = endDate ? formatDate(endDate) : null

      const selectedOption = dropdownOptions?.find(
        (opt) => opt.id === selectedPlant,
      )
      const assetDisplayName = selectedOption?.name

      await HeatRateApiService.exportSTGHeatRateExcel(
        keycloak,
        selectedPlant,
        AOP_YEAR,
        formattedStartDate,
        formattedEndDate,
        PLANT_ID_LIST,
        assetDisplayName,
      )
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting STG Heat Rate data:', error)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  // Handle remark cell click
  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  // Custom itemChange handler for radio selection with bidirectional sync
  const handleCustomItemChange = (e, setRows) => {
    const { dataItem, field, value } = e
    const itemId = dataItem.id

    // When radio selection changes, update the Final Heat Rate
    if (field === 'selectedHeatRate') {
      // Map radioValue to field name
      const fieldMapping = {
        OEM: 'oemHeatRate',
        PREVIOUS_YEAR: 'prevYearFinalHeatRate',
        PROPOSED: 'proposedYearFinalHeatRate',
      }

      const selectedField = fieldMapping[value]
      const selectedValue = selectedField ? dataItem[selectedField] : null

      setRows((prev) =>
        prev.map((r) => {
          if (r.id === dataItem.id) {
            return {
              ...r,
              selectedHeatRate: value,
              finalHeatRate: selectedValue,
            }
          }
          return r
        }),
      )

      // Track both fields in modifiedCells
      setModifiedCells((prev) => {
        const currentRow = rows.find((r) => r.id === itemId)
        return {
          ...prev,
          [itemId]: {
            ...(prev[itemId] || currentRow),
            selectedHeatRate: value,
            finalHeatRate: selectedValue,
            inEdit: true,
          },
        }
      })

      setCustomModifiedCells((prev) => ({
        ...prev,
        [itemId]: {
          ...(prev[itemId] || {}),
          selectedHeatRate: value,
          finalHeatRate: selectedValue,
        },
      }))

      return
    }

    // When a source column is edited, update finalHeatRate ONLY if that source is currently selected
    const sourceFieldMapping = {
      oemHeatRate: 'OEM',
      prevYearFinalHeatRate: 'PREVIOUS_YEAR',
      proposedYearFinalHeatRate: 'PROPOSED',
    }

    if (sourceFieldMapping[field]) {
      const radioValueForThisField = sourceFieldMapping[field]

      setRows((prev) =>
        prev.map((r) => {
          if (r.id === dataItem.id) {
            // Only update finalHeatRate if this source is currently selected
            if (r.selectedHeatRate === radioValueForThisField) {
              return {
                ...r,
                [field]: value,
                finalHeatRate: value,
              }
            }
            // Otherwise just update the source field
            return {
              ...r,
              [field]: value,
            }
          }
          return r
        }),
      )

      // Track changes in modifiedCells and customModifiedCells
      const currentRow = rows.find((r) => r.id === itemId)
      if (currentRow?.selectedHeatRate === radioValueForThisField) {
        // Update both source field and finalHeatRate
        setModifiedCells((prev) => ({
          ...prev,
          [itemId]: {
            ...(prev[itemId] || currentRow),
            [field]: value,
            finalHeatRate: value,
            inEdit: true,
          },
        }))

        setCustomModifiedCells((prev) => ({
          ...prev,
          [itemId]: {
            ...(prev[itemId] || {}),
            [field]: value,
            finalHeatRate: value,
          },
        }))
      } else {
        // Still track the source field change even if not selected (for orange highlighting)
        setModifiedCells((prev) => ({
          ...prev,
          [itemId]: {
            ...(prev[itemId] || currentRow),
            [field]: value,
            inEdit: true,
          },
        }))

        setCustomModifiedCells((prev) => ({
          ...prev,
          [itemId]: {
            ...(prev[itemId] || {}),
            [field]: value,
          },
        }))
      }

      return
    }

    // When Final Heat Rate is manually edited, check if it matches any source column
    if (field === 'finalHeatRate') {
      const sourceFields = [
        {
          radioValue: 'OEM',
          field: 'oemHeatRate',
          value: dataItem.oemHeatRate,
        },
        {
          radioValue: 'PREVIOUS_YEAR',
          field: 'prevYearFinalHeatRate',
          value: dataItem.prevYearFinalHeatRate,
        },
        {
          radioValue: 'PROPOSED',
          field: 'proposedYearFinalHeatRate',
          value: dataItem.proposedYearFinalHeatRate,
        },
      ]

      let matchedRadioValue = null

      // Check if the entered value matches any source column value
      for (const source of sourceFields) {
        if (
          source.value !== null &&
          source.value !== undefined &&
          parseFloat(value) === parseFloat(source.value)
        ) {
          matchedRadioValue = source.radioValue
          break
        }
      }

      setRows((prev) =>
        prev.map((r) => {
          if (r.id === dataItem.id) {
            return {
              ...r,
              finalHeatRate: value,
              // Auto-select radio if value matches a source, otherwise set to OTHER
              selectedHeatRate: matchedRadioValue || 'OTHER',
            }
          }
          return r
        }),
      )

      // Track both fields in modifiedCells
      setModifiedCells((prev) => {
        const currentRow = rows.find((r) => r.id === itemId)
        return {
          ...prev,
          [itemId]: {
            ...(prev[itemId] || currentRow),
            finalHeatRate: value,
            selectedHeatRate: matchedRadioValue || 'OTHER',
            inEdit: true,
          },
        }
      })

      setCustomModifiedCells((prev) => ({
        ...prev,
        [itemId]: {
          ...(prev[itemId] || {}),
          finalHeatRate: value,
          selectedHeatRate: matchedRadioValue || 'OTHER',
        },
      }))
    }
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
        externalCustomModifiedCells={customModifiedCells}
        externalSetCustomModifiedCells={setCustomModifiedCells}
        title='STG Heat Rate'
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
        dropdownConfig={dropdownConfig}
        selectedDropdownValue={selectedPlant}
        setSelectedDropdownValue={setSelectedPlant}
        customItemChange={handleCustomItemChange}
        setSnackbarData={setSnackbarData}
        customHeight={70}
        paginationConfig={{
          threshold: 20,
          buttonCount: 5,
          pageSizes: [10, 20, 50, 100],
          defaultPageSize: 100,
        }}
      />
    </Box>
  )
}

export default STGHeatRate
