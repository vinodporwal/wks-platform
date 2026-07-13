import { useEffect, useState, useMemo, useCallback } from 'react'
import { Box, Backdrop, CircularProgress, Stack } from '@mui/material'
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
const GTHeatRate = ({ startDate, endDate, dateLoading }) => {
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

  // const PLANT_ID_LIST = useMemo(
  //   () => jmdSelectedPlants?.map((plant) => plant.id) || [],
  //   [jmdSelectedPlants],
  // )

  const PLANT_ID_LIST = plantObject?.id

  const columns = [
    {
      field: 'equipType',
      title: 'Equipment Type',
      widthT: 180,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 180,
    },
    {
      field: 'cppUtility',
      title: 'CPP Utility',
      widthT: 120,
      type: 'text',
      editable: false,
      minWidth: 120,
    },
    {
      field: 'gtLoad',
      title: 'GT Load',
      widthT: 120,
      type: 'number1',
      format: customValueFormat(1),
      editable: true,
      minWidth: 120,
    },
    {
      field: 'oemHeatRate',
      title: 'OEM HR',
      widthT: 150,
      type: 'numberWithRadio',
      format: customValueFormat(1),
      editable: true,
      numericEditable: true,
      minWidth: 150,
      radioGroupField: 'selectedHeatRate',
      targetField: 'finalHeatRate',
      radioValue: 'OEM',
    },
    {
      field: 'prevYearFinalHeatRate',
      title: 'PREVIOUS YEAR BUDGET HR',
      widthT: 230,
      type: 'numberWithRadio',
      format: customValueFormat(1),
      editable: true,
      numericEditable: false,
      minWidth: 230,
      radioGroupField: 'selectedHeatRate',
      targetField: 'finalHeatRate',
      radioValue: 'PREVIOUS_YEAR',
    },
    {
      field: 'proposedYearFinalHeatRate',
      title: 'PROPOSED HR',
      subtitle: '(Based On Actual Data)',
      widthT: 200,
      type: 'numberWithRadio',
      format: customValueFormat(1),
      editable: true,
      numericEditable: false,
      minWidth: 200,
      radioGroupField: 'selectedHeatRate',
      targetField: 'finalHeatRate',
      radioValue: 'PROPOSED',
    },
    {
      field: 'finalHeatRate',
      title: 'Final HR',
      widthT: 150,
      type: 'number1',
      format: customValueFormat(1),
      editable: true,
      minWidth: 150,
    },
    {
      field: 'freeSteamFactor',
      title: 'Free Steam Factor',
      widthT: 170,
      type: 'number1',
      format: customValueFormat(2),
      editable: true,
      minWidth: 170,
    },
    {
      field: 'remarks',
      title: 'Remark',
      widthT: 250,
      type: 'textarea',
      editable: true,
      minWidth: 250,
    },
  ]

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [selectedPlant, setSelectedPlant] = useState('')
  const [dropdownOptions, setDropdownOptions] = useState([])

  const formatDate = (date) => {
    if (!date) return ''
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  useEffect(() => {
    if (selectedPlant && startDate && endDate) {
      const formattedStartDate = formatDate(startDate)
      const formattedEndDate = formatDate(endDate)
      fetchHeatRateData(selectedPlant, formattedStartDate, formattedEndDate)
    }
  }, [selectedPlant, startDate, endDate])

  const getPlantList = useCallback(async () => {
    setLoading(true)
    try {
      const res = await HeatRateApiService.getGTAssetDropdown(
        keycloak,
        PLANT_ID_LIST,
        'GT',
      )

      // Convert to required format with plant name
      const convertedData = res?.map((item) => {
        // Find plant name from jmdSelectedPlants by matching cppPlantFkId
        // const plant = jmdSelectedPlants?.find(
        //   (p) => p.id?.toUpperCase() === item.cppPlantFkId?.toUpperCase(),
        // )
        // const plantName = plant?.name

        return {
          id: item.assetId,
          name: `${item.assetName} (${plantObject?.name})`,
          plantName: plantObject?.name,
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

  const fetchHeatRateData = useCallback(
    async (assetId, startDate, endDate) => {
      setLoading(true)
      try {
        const res = await HeatRateApiService.getGTHeatRateData(
          keycloak,
          assetId,
          AOP_YEAR,
          startDate,
          endDate,
          PLANT_ID_LIST,
        )

        if (res?.data?.length === 0) {
          setRows([])
          setSnackbarOpen(true)
          setSnackbarData({ message: 'No data found', severity: 'info' })
          return
        }
        let tempRes = res?.data?.map((item, index) => {
          const selectedHeatRate = item.selectedHeatRate || 'PROPOSED'

          // Validate if selectedHeatRate matches the actual finalHeatRate value
          const fieldMapping = {
            OEM: 'oemHeatRate',
            PREVIOUS_YEAR: 'prevYearFinalHeatRate',
            PROPOSED: 'proposedYearFinalHeatRate',
          }

          const selectedField = fieldMapping[selectedHeatRate]
          const selectedValue = selectedField ? item[selectedField] : null
          const finalValue = item.finalHeatRate

          // Check if selected column value matches final heat rate
          const isMatch =
            selectedValue !== null &&
            selectedValue !== undefined &&
            finalValue !== null &&
            finalValue !== undefined &&
            parseFloat(selectedValue) === parseFloat(finalValue)

          return {
            ...item,
            id: item.id || index + 1,
            remarks: item.remarks || '',
            selectedHeatRate: isMatch ? selectedHeatRate : 'OTHER',
          }
        })
        // Sort by gtLoad in ascending order
        tempRes.sort((a, b) => {
          const aLoad = parseFloat(a.gtLoad) || 0
          const bLoad = parseFloat(b.gtLoad) || 0
          return aLoad - bLoad
        })
        setRows(tempRes)
        setOriginalRows(tempRes)
      } catch (error) {
        console.error('Error fetching heat rate data:', error)
        setSnackbarOpen(true)
        setSnackbarData({ message: 'Error fetching data', severity: 'error' })
      } finally {
        setLoading(false)
      }
    },
    [keycloak, AOP_YEAR],
  )

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
    ExcelName: `GT Heat Rate - ${AOP_YEAR}`,
    showTitle: true,
    showDropdown: true,
  }

  const dropdownConfig = {
    options: dropdownOptions,
    label: 'Plant',
    placeholder: 'Select Plant',
    valueKey: 'id',
    labelKey: 'name',
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

    const data = modifiedData.filter((row) => row.inEdit)
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
      'gtLoad',
      'oemHeatRate',
      'freeSteamFactor',
      'finalHeatRate',
    ]
    const validationError = validateRowDataWithRemarks(
      data,
      originalRows,
      fieldsToCheck,
      'equipType',
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
        const { inEdit, updatedDate, createdDate, ...rest } = item
        return rest
      })

      const res = await HeatRateApiService.saveGTHeatRateData(
        keycloak,
        AOP_YEAR,
        payload,
      )
      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved ${modifiedData.length} changes!`,
        severity: 'success',
      })
      const formattedStartDate = formatDate(startDate)
      const formattedEndDate = formatDate(endDate)
      fetchHeatRateData(selectedPlant, formattedStartDate, formattedEndDate)
    } catch (error) {
      console.error('Error saving heat rate data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Failed to save changes. Error: ${error?.message || 'Unknown error'}`,
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

      const response = await HeatRateApiService.saveGTHeatRateExcel(
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
        downloadBase64Excel(response.data, 'GT_Heat_Rate_Import_Status.xlsx')
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
      const formattedStartDate = formatDate(startDate)
      const formattedEndDate = formatDate(endDate)

      // Get the selected asset name with plant name
      const selectedAsset = dropdownOptions.find(
        (opt) => opt.id === selectedPlant,
      )
      const assetDisplayName = selectedAsset?.name || 'GT_Heat_Rate'

      await HeatRateApiService.exportGTHeatRateExcel(
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
      console.error('Error exporting Heat Rate data:', error)
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
        title='GT Heat Rate'
        permissions={permissions}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={() => { }}
        saveChanges={saveChanges}
        handleExcelUpload={handleExcelUpload}
        handleExport={handleExport}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        dropdownConfig={dropdownConfig}
        selectedDropdownValue={selectedPlant}
        setSelectedDropdownValue={setSelectedPlant}
        customItemChange={handleCustomItemChange}
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

export default GTHeatRate
