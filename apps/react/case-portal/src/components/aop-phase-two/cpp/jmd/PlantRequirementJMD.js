import { useEffect, useState, useMemo, useCallback } from 'react'
import { Box, Backdrop, CircularProgress } from '@mui/material'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { useSelector } from 'react-redux'
import { UtilityPlantApiServiceV2 } from 'components/aop-phase-two/services/cpp/jmd/utilityPlantApiServiceV2'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo, {
  customValueFormatterPhaseTwo,
} from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { useDebounce } from 'hooks/useDebounce'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

const PlantRequirementJMD = () => {
  const keycloak = useSession()
  // State management

  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    yearChanged,
    oldYear,
    plantID,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
    jmdSelectedPlants,
  } = dataGridStore
  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Plant_Requirement')

  const PLANT_ID_LIST = useMemo(
    () => jmdSelectedPlants?.map((plant) => plant.id) || [],
    [jmdSelectedPlants],
  )

  const lowerVertName = verticalObject?.name?.toLowerCase()
  const lowerSiteName = siteObject?.name?.toLowerCase()
  const IS_CPP_JMD = lowerVertName === 'cpp' && lowerSiteName === 'jmd'
  const IS_CPP_NMD = lowerVertName === 'cpp' && lowerSiteName === 'nmd'

  const headerMap = generateHeaderNames(AOP_YEAR)
  const valueFormat = ValueFormatterPhaseTwo()
  const customFormatTwo = customValueFormatterPhaseTwo(2)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // Fiscal-year month order: Apr → Mar
  const MONTH_TO_INDEX = {
    apr: 4,
    may: 5,
    jun: 6,
    jul: 7,
    aug: 8,
    sep: 9,
    oct: 10,
    nov: 11,
    dec: 12,
    jan: 1,
    feb: 2,
    mar: 3,
  }

  // Base column configuration for month columns
  const monthBaseColumnConfig = {
    editable: true,
    widthT: 100,
    minWidth: 100,
    align: 'left',
    headerAlign: 'left',
    type: 'number1',
    allowNegative: true,
    format: customFormatTwo,
  }

  // Month field names in fiscal year order
  const MONTH_FIELDS = [
    'apr',
    'may',
    'jun',
    'jul',
    'aug',
    'sep',
    'oct',
    'nov',
    'dec',
    'jan',
    'feb',
    'mar',
  ]

  // Generate month columns by duplicating base config
  const MONTH_COLUMNS = MONTH_FIELDS.map((mon) => ({
    ...monthBaseColumnConfig,
    field: mon,
    title: headerMap[MONTH_TO_INDEX[mon]],
  }))

  // Column definitions
  const columns = [
    {
      field: 'processPlant',
      title: 'Process Plant',
      widthT: 150,
      minWidth: 150,
      type: 'text',
      editable: false,
      hidden: false,
    },
    {
      field: 'processPlantId',
      title: 'Plant Code',
      widthT: 120,
      minWidth: 120,
      type: 'text',
      editable: false,
      hidden: true,
    },
    {
      field: 'cppUtility',
      title: 'CPP Utilities',
      widthT: 150,
      minWidth: 150,
      type: 'text',
      editable: false,
    },
    {
      field: 'cppUtilityId',
      title: 'CPP Utility ID',
      widthT: 170,
      minWidth: 170,
      type: 'text',
      editable: false,
    },
    {
      field: 'cppPlant',
      title: 'CPP Plant',
      widthT: 150,
      minWidth: 150,
      type: 'text',
      editable: false,
    },
    {
      field: 'cppPlantId',
      title: 'CPP Plant ID',
      widthT: 170,
      minWidth: 170,
      type: 'text',
      editable: false,
      hidden: true,
    },
    {
      field: 'uom',
      title: 'UOM',
      widthT: 100,
      minWidth: 100,
      type: 'text',
      editable: false,
    },

    // Monthly columns - Apr → Mar (editable)
    ...MONTH_COLUMNS,

    {
      field: 'total',
      title: 'Total',
      widthT: 130,
      minWidth: 130,
      type: 'number1',
      editable: false,
      format: customFormatTwo,
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

  const fetchPlantRequirementData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await UtilityPlantApiServiceV2.getPlantRequirementData(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )
      if (res?.length === 0) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        setLoading(false)
        return
      }

      console.log('res', res)
      const formattedData = res?.map((item, index) => ({
        ...item,
        remarks: item.remarks || '',
        id: item?.id || index + 1,
        total: MONTH_FIELDS.reduce((sum, key) => sum + (item[key] || 0), 0),
      }))
      setRows(formattedData)
      setOriginalRows(formattedData)
    } catch (error) {
      console.error('Error fetching plant requirement data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR])

  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && AOP_YEAR) {
        fetchPlantRequirementData()
        setModifiedCells({})
      }
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR, fetchPlantRequirementData],
  )

  // Permissions (adjust as needed)
  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showExport: true,
    ExcelName: `Plant Requirement - ${AOP_YEAR}`,
    showImport: true,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: screenTitle?.title,
  }

  // Save handler with API call
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
      'apr',
      'may',
      'jun',
      'jul',
      'aug',
      'sep',
      'oct',
      'nov',
      'dec',
      'jan',
      'feb',
      'mar',
    ]
    const validationError = validateRowDataWithRemarks(
      data,
      originalRows,
      fieldsToCheck,
      'processPlant',
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

    const payload = modifiedData
    try {
      // Transform modifiedCells into the format expected by the API
      console.log('payload', payload)

      // Call the API to save changes
      const response = await UtilityPlantApiServiceV2.savePlantRequirementData(
        keycloak,
        AOP_YEAR,
        payload,
      )

      // Update the local state with the saved data
      // setRows(updatedRows)
      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved ${modifiedData.length} changes!`,
        severity: 'success',
      })
    } catch (error) {
      console.error('Error saving plant requirement data:', error)
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
      const response = await UtilityPlantApiServiceV2.savePlantRequirementExcel(
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
        // Refresh data after import
        await fetchPlantRequirementData()
      } else if (response?.code === 400 && response?.data) {
        // Handle error response with Excel file download
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
          link.download = `Plant_Requirement_Errors_${new Date().getTime()}.xlsx`
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
          // Refresh data after import
          await fetchPlantRequirementData()
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
      await UtilityPlantApiServiceV2.exportPlantRequirementExcel(
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
      console.error('Error exporting Plant Requirement data:', error)
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
        customHeight={80}
        paginationConfig={{
          threshold: 100, // Show pagination if > 50 rows
          buttonCount: 5,
          pageSizes: [10, 20, 50, 100],
          defaultPageSize: 100,
        }}
        groupBy={'processPlant'}
      />
    </Box>
  )
}

export default PlantRequirementJMD
