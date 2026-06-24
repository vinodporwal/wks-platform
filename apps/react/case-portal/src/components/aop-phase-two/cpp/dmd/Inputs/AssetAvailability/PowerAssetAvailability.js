import { useEffect, useState, useMemo } from 'react'
import { Box } from '@mui/material'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { AssetPriorityApiService } from 'components/aop-phase-two/services/cpp/jmd/assetPriorityApiService'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

const PowerAssetAvailability = ({
  initialRows = [],
  onRefresh,
  externalLoading = false,
}) => {
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
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Asset_Priority')

  const PLANT_ID_LIST = plantObject?.id
  const headerMap = generateHeaderNames(AOP_YEAR)
  const valueFormat = ValueFormatterPhaseTwo()

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // Column definitions
  const columns = [
    //Generating Plant
    {
      field: 'assetName',
      title: 'Asset Name',
      widthT: 150,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 150,
    },

    // Apr
    {
      title: headerMap[4],
      field: 'apr',
      widthT: 100,
      type: 'wholeNumber',
      editable: true,
      wholeNumberOnly: true,
    },
    // May
    {
      title: headerMap[5],
      field: 'may',
      widthT: 100,
      type: 'wholeNumber',
      editable: true,
      wholeNumberOnly: true,
    },
    // Jun
    {
      title: headerMap[6],
      field: 'jun',
      widthT: 100,
      type: 'wholeNumber',
      editable: true,
      wholeNumberOnly: true,
    },
    // Jul
    {
      title: headerMap[7],
      field: 'jul',
      widthT: 100,
      type: 'wholeNumber',
      editable: true,
      wholeNumberOnly: true,
    },
    // Aug
    {
      title: headerMap[8],
      field: 'aug',
      widthT: 100,
      type: 'wholeNumber',
      editable: true,
      wholeNumberOnly: true,
    },
    // Sep
    {
      title: headerMap[9],
      field: 'sep',
      widthT: 100,
      type: 'wholeNumber',
      editable: true,
      wholeNumberOnly: true,
    },
    // Oct
    {
      title: headerMap[10],
      field: 'oct',
      widthT: 100,
      type: 'wholeNumber',
      editable: true,
      wholeNumberOnly: true,
    },
    // Nov
    {
      title: headerMap[11],
      field: 'nov',
      widthT: 100,
      type: 'wholeNumber',
      editable: true,
      wholeNumberOnly: true,
    },
    //Dec
    {
      title: headerMap[12],
      field: 'dec',
      widthT: 100,
      type: 'wholeNumber',
      editable: true,
      wholeNumberOnly: true,
    },
    //Jan
    {
      title: headerMap[1],
      field: 'jan',
      widthT: 100,
      type: 'wholeNumber',
      editable: true,
      wholeNumberOnly: true,
    },
    //Feb
    {
      title: headerMap[2],
      field: 'feb',
      widthT: 100,
      type: 'wholeNumber',
      editable: true,
      wholeNumberOnly: true,
    },
    //mar
    {
      title: headerMap[3],
      field: 'mar',
      widthT: 100,
      type: 'wholeNumber',
      editable: true,
      wholeNumberOnly: true,
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

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])

  useEffect(() => {
    setRows(initialRows)
    setOriginalRows(initialRows)
    setModifiedCells({})
  }, [initialRows])

  const combinedLoading = loading || externalLoading

  // Permissions (adjust as needed)
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
    ExcelName: EXCEL_NAME,
    showTitle: true,
  }

  // Save handler with API call
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

    try {
      const payload = modifiedData.map((item) => {
        const { inEdit, ...rest } = item
        return rest
      })

      // Call the API to save changes
      const response = await AssetPriorityApiService.saveAssetPriority(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
        { powerResponse: payload },
      )

      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved ${modifiedData.length} changes!`,
        severity: 'success',
      })
      await onRefresh?.()
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
      const response = await AssetPriorityApiService.importPowerAssetPriority(
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
        await onRefresh?.()
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
        link.setAttribute('download', `Error File - Asset Priority.xlsx`)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        await onRefresh?.()
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
      await AssetPriorityApiService.exportPowerAssetPriority(
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
      console.error('Error exporting Power Asset Priority data:', error)
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
      <LoaderBackdrop open={!!combinedLoading} />
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title='Power Asset Priority'
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
        groupBy={['assetType']}
      />
    </Box>
  )
}

export default PowerAssetAvailability
