import { useState, useRef, useMemo, useCallback } from 'react'
import { Box } from '@mui/material'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { InputApiService } from 'components/aop-phase-two/services/cpp/jmd/inputApiService'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import { useDebounce } from 'hooks/useDebounce'

const PowerAssetCapacity = () => {
  const keycloak = useSession()
  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
    jmdSelectedPlants,
  } = dataGridStore
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Power_Asset_Capacity')

  // Multi-plant list — same pattern as PowerAssetAvailability
  
  const PLANT_ID_LIST =  plantObject?.id;
  const headerMap = generateHeaderNames(AOP_YEAR)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const valueFormat = ValueFormatterPhaseTwo()

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const columns = [
    { field: 'id', title: 'ID', hidden: true },
    {
      field: 'assetName',
      title: 'Asset Name',
      widthT: 150,
      minWidth: 150,
      type: 'text',
      editable: false,
      locked: true,
    },
    {
      field: 'plantCode',
      title: 'Plant Code',
      widthT: 130,
      minWidth: 130,
      type: 'text',
      editable: false,
      locked: true,
    },
    {
      field: 'utilityDistributed',
      title: 'Utility Distributed',
      widthT: 180,
      minWidth: 180,
      type: 'text',
      editable: false,
      // locked: true,
    },
    {
      field: 'distributedSapCode',
      title: 'Utility Distributed Code',
      widthT: 200,
      minWidth: 200,
      type: 'text',
      editable: false,
      // locked: true,
    },
    {
      field: 'utilityGenerated',
      title: 'Utility Generated',
      widthT: 180,
      minWidth: 180,
      type: 'text',
      editable: false,
      // locked: true,
    },
    {
      field: 'generatedUtilityCode',
      title: 'Utility Generated Code',
      widthT: 200,
      minWidth: 200,
      type: 'text',
      editable: false,
      // locked: true,
    },
    {
      field: 'uom',
      title: 'UOM',
      widthT: 100,
      minWidth: 100,
      type: 'text',
      editable: false,
    },
    {
      field: 'fixedMin',
      title: 'Fixed Min',
      widthT: 130,
      minWidth: 130,
      editable: false,
      type: 'number1',
      format: valueFormat,
    },
    {
      field: 'fixedMax',
      title: 'Fixed Max',
      widthT: 130,
      minWidth: 130,
      editable: false,
      type: 'number1',
      format: valueFormat,
    },
    {
      title: headerMap[4],
      children: [
        {
          field: 'aprMin',
          title: 'Min Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
        {
          field: 'aprMax',
          title: 'Max Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
      ],
    },
    {
      title: headerMap[5],
      children: [
        {
          field: 'mayMin',
          title: 'Min Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
        {
          field: 'mayMax',
          title: 'Max Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
      ],
    },
    {
      title: headerMap[6],
      children: [
        {
          field: 'junMin',
          title: 'Min Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
        {
          field: 'junMax',
          title: 'Max Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
      ],
    },
    {
      title: headerMap[7],
      children: [
        {
          field: 'julMin',
          title: 'Min Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
        {
          field: 'julMax',
          title: 'Max Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
      ],
    },
    {
      title: headerMap[8],
      children: [
        {
          field: 'augMin',
          title: 'Min Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
        {
          field: 'augMax',
          title: 'Max Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
      ],
    },
    {
      title: headerMap[9],
      children: [
        {
          field: 'sepMin',
          title: 'Min Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
        {
          field: 'sepMax',
          title: 'Max Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
      ],
    },
    {
      title: headerMap[10],
      children: [
        {
          field: 'octMin',
          title: 'Min Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
        {
          field: 'octMax',
          title: 'Max Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
      ],
    },
    {
      title: headerMap[11],
      children: [
        {
          field: 'novMin',
          title: 'Min Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
        {
          field: 'novMax',
          title: 'Max Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
      ],
    },
    {
      title: headerMap[12],
      children: [
        {
          field: 'decMin',
          title: 'Min Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
        {
          field: 'decMax',
          title: 'Max Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
      ],
    },
    {
      title: headerMap[1],
      children: [
        {
          field: 'janMin',
          title: 'Min Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
        {
          field: 'janMax',
          title: 'Max Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
      ],
    },
    {
      title: headerMap[2],
      children: [
        {
          field: 'febMin',
          title: 'Min Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
        {
          field: 'febMax',
          title: 'Max Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
      ],
    },
    {
      title: headerMap[3],
      children: [
        {
          field: 'marMin',
          title: 'Min Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
        {
          field: 'marMax',
          title: 'Max Capacity',
          widthT: 140,
          minWidth: 140,
          editable: true,
          type: 'number1',
          format: valueFormat,
          minValue: 'fixedMin',
          maxValue: 'fixedMax',
        },
      ],
    },
    {
      field: 'remarks',
      title: 'Remarks',
      widthT: 250,
      type: 'textarea',
      editable: true,
      minWidth: 250,
    },
  ]

  const fetchAssetCapacityData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await InputApiService.getAssetCapacities(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )
      // Response: res.data.PowerAssetCapacities (flat array)
      const rawList = res?.data?.PowerAssetCapacities ?? res?.data ?? res
      if (!rawList || (Array.isArray(rawList) && rawList.length === 0)) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        return
      }
      const tempRes = (Array.isArray(rawList) ? rawList : []).map(
        (item, index) => ({
          ...item,
          id: item.id || index + 1,
          remarks: item.remarks || '',
        }),
      )
      setRows(tempRes)
      setOriginalRows(tempRes)
    } catch (error) {
      console.error('Error fetching power asset capacity data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR])

  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && AOP_YEAR) {
        fetchAssetCapacityData()
        setModifiedCells({})
      }
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR, fetchAssetCapacityData],
  )

  const debounceTimerRef = useRef(null)

  const customItemChange = (e, setRows) => {
    const { dataItem, field, value } = e

    // Clear previous debounce timer
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current)
    }

    // Flat field naming: e.g. 'aprMin', 'aprMax', 'mayMin', 'mayMax', ...
    // Determine if this is a monthly min or max field
    const isMin = field.endsWith('Min')
    const isMax = field.endsWith('Max')

    if (!isMin && !isMax) {
      return true
    }

    const numValue = parseFloat(value)

    // Skip validation if value is empty or NaN
    if (value === '' || isNaN(numValue)) {
      return true
    }

    // Derive the corresponding paired field (e.g. 'aprMin' <-> 'aprMax')
    const prefix = field.slice(0, -3) // e.g. 'apr'
    const correspondingField = isMin ? `${prefix}Max` : `${prefix}Min`
    const correspondingValue = dataItem[correspondingField]

    // Validate: min should not be greater than max
    if (
      isMin &&
      correspondingValue !== undefined &&
      numValue > correspondingValue
    ) {
      const label = prefix.charAt(0).toUpperCase() + prefix.slice(1)
      setSnackbarOpen(true)
      setSnackbarData({
        message: `${label} min capacity cannot be greater than ${label} max capacity`,
        severity: 'error',
      })
      return false
    }

    // Validate: max should not be less than min
    if (
      isMax &&
      correspondingValue !== undefined &&
      numValue < correspondingValue
    ) {
      const label = prefix.charAt(0).toUpperCase() + prefix.slice(1)
      setSnackbarOpen(true)
      setSnackbarData({
        message: `${label} max capacity cannot be less than ${label} min capacity`,
        severity: 'error',
      })
      return false
    }

    return true
  }

  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showImport: true,
    showExport: true,
    ExcelName: EXCEL_NAME,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: screenTitle?.title,
  }

  const saveChanges = async () => {
    setLoading(true)
    console.log('modifiedCells', modifiedCells)
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
      'aprMin',
      'aprMax',
      'mayMin',
      'mayMax',
      'junMin',
      'junMax',
      'julMin',
      'julMax',
      'augMin',
      'augMax',
      'sepMin',
      'sepMax',
      'octMin',
      'octMax',
      'novMin',
      'novMax',
      'decMin',
      'decMax',
      'janMin',
      'janMax',
      'febMin',
      'febMax',
      'marMin',
      'marMax',
      'fixedMin',
      'fixedMax',
    ]
    const validationError = validateRowDataWithRemarks(
      data,
      originalRows,
      fieldsToCheck,
      'assetName',
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

    // Backend expects AssetCapacityRequestDTO: { powerResponse: [...] }
    const payload = { powerResponse: modifiedData }

    try {
      console.log('payload', payload)

      const response = await InputApiService.saveAssetCapacities(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
        payload,
      )

      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved ${modifiedData.length} changes!`,
        severity: 'success',
      })
    } catch (error) {
      console.error('Error saving asset capacity data:', error)
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
      const response = await InputApiService.importPowerAssetCapacityExcel(
        file,
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Excel file imported successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        await fetchAssetCapacityData()
      } else if (response?.code === 400 && response?.data) {
        const byteCharacters = atob(response.data)
        const byteNumbers = Array.from(byteCharacters, (char) =>
          char.charCodeAt(0),
        )
        const byteArray = new Uint8Array(byteNumbers)

        const blob = new Blob([byteArray], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        })

        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `Error File - Asset Capacity.xlsx`)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        await fetchAssetCapacityData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
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
      await InputApiService.exportPowerAssetCapacityExcel(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
        EXCEL_NAME,
      )
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting Asset Capacity data:', error)
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

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title='Power Asset Capacity Input'
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
        customItemChange={customItemChange}
        groupBy={['plantName', 'assetType']}
      />
    </Box>
  )
}

export default PowerAssetCapacity
