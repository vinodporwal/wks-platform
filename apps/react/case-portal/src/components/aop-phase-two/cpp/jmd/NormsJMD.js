import { useEffect, useMemo, useState, useCallback } from 'react'
import { Box } from '@mui/material'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { useDispatch, useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { validateNestedRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import { UtilityPlantApiServiceV2 } from 'components/aop-phase-two/services/cpp/jmd/utilityPlantApiServiceV2'
import { setIsReleased } from 'store/reducers/dataGridStore'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import ReleaseAPIService from 'components/aop-phase-two/services/common/releaseAPIService'
import ReleaseDialog from 'components/aop-phase-two/common/components/ReleaseDialog'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { useDebounce } from 'hooks/useDebounce'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

const NormsJMD = () => {
  const keycloak = useSession()
  // State management
  const dispatch = useDispatch()
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
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Norms')

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

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const [calculateBtnEnabled, setCalculateBtnEnabled] = useState(false)
  const [openReleaseDialogBox, setOpenReleaseDialogBox] = useState(false)
  const [isReleaseDisabled, setIsReleaseDisabled] = useState(true)
  // Month field names in fiscal year order
  const MONTH_FIELDS = [
    'apr', 'may', 'jun', 'jul', 'aug', 'sep',
    'oct', 'nov', 'dec', 'jan', 'feb', 'mar',
  ]

  const MONTH_TO_INDEX = {
    apr: 4, may: 5, jun: 6, jul: 7, aug: 8, sep: 9,
    oct: 10, nov: 11, dec: 12, jan: 1, feb: 2, mar: 3,
  }

  // Generate nested month columns with children (norms, quantity, amount, price)
  const NESTED_MONTH_COLUMNS = MONTH_FIELDS.map((mon) => ({
    title: headerMap[MONTH_TO_INDEX[mon]],
    children: [
      {
        field: `${mon}.norms`,
        title: 'Norms',
        widthT: 120,
        minWidth: 120,
        editable: false,
        type: 'number1',
        format: valueFormat,
      },
      {
        field: `${mon}.quantity`,
        title: 'Quantity',
        widthT: 120,
        minWidth: 120,
        type: 'number',
        format: valueFormat,
      },
      {
        field: `${mon}.amount`,
        title: 'Amount',
        widthT: 120,
        minWidth: 120,
        type: 'number',
        format: valueFormat,
        hidden: true,
      },
      {
        field: `${mon}.price`,
        title: 'Price',
        widthT: 120,
        minWidth: 120,
        editable: true,
        type: 'number1',
        format: valueFormat,
        hidden: true,
      },
    ],
  }))

  // Column definitions
  const nestedColumns = [
    //Generating Plant
    {
      field: 'generatingPlantName',
      title: 'Generating Plant',
      widthT: 180,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 180,
    },
    //Utility
    {
      field: 'utilityName',
      title: 'Utility',
      widthT: 120,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 120,
    },
    // Utility ID
    {
      field: 'utilityId',
      title: 'Utility ID',
      widthT: 120,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 120,
    },
    //UOM
    {
      field: 'uom',
      title: 'Generation UOM',
      widthT: 180,
      type: 'text',
      editable: false,
      minWidth: 180,
    },
    // Account
    {
      field: 'accountName',
      title: 'Account',
      widthT: 120,
      type: 'text',
      editable: false,
      minWidth: 120,
    },
    // Material
    {
      field: 'materialName',
      title: 'Material',
      widthT: 120,
      type: 'text',
      editable: false,
      minWidth: 120,
    },
    // SAP Code
    {
      field: 'materialId',
      title: 'SAP Code',
      widthT: 120,
      type: 'text',
      editable: false,
      minWidth: 120,
    },
    // Issuing Plant
    {
      field: 'issuingPlantName',
      title: 'Issuing Plant',
      widthT: 150,
      type: 'text',
      editable: false,
      minWidth: 150,
    },
    {
      field: 'issuingUom',
      title: 'Issuing UOM',
      widthT: 150,
      type: 'text',
      editable: false,
      minWidth: 150,
    },
    ...NESTED_MONTH_COLUMNS,
  ]

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [calculationLoading, setCaculationLoading] = useState(false)

  const fetchNormsData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await UtilityPlantApiServiceV2.getNormBasedUtilityBudget(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )

      if (res?.data?.list?.length === 0) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        setLoading(false)
        return
      }
      let tempRes = res?.data?.list
        ?.map((item, index) => {
          // Transform month data from API response to match column structure
          const transformedItem = {
            ...item,
            id: item.id || index + 1,
            remarks: item.remarks || '',
          }
          
          // Map each month's data to the expected nested structure
          MONTH_FIELDS.forEach((month) => {
            const monthData = item[month]
            if (monthData) {
              transformedItem[month] = {
                norms: monthData.norms || monthData.Norms,
                quantity: monthData.quantity || monthData.Quantity,
                amount: monthData.amount || monthData.Amount,
                price: monthData.price || monthData.Price,
              }
            }
          })
          
          return transformedItem
        })

      setRows(tempRes)
      setCalculateBtnEnabled(true)
      setOriginalRows(tempRes)
    } catch (error) {
      console.error('Error fetching norms data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR])

  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && AOP_YEAR) {
        fetchNormsData()
        setModifiedCells({})
      }
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR, fetchNormsData],
  )

  // Permissions (adjust as needed)
  const permissions = useMemo(() => {
    return {
      showAction: true,
      addButton: false,
      deleteButton: false,
      editButton: true,
      saveBtn: false,
      allAction: true,
      showTitleNameBusiness: true,
      titleName: screenTitle?.title,
      showImport: false,
      showTitle: true,
      showCalculate: true,
      enableCalculate: calculateBtnEnabled,
      showExport: true,
      ExcelName: `Norms - ${AOP_YEAR}`,
      showReleaseBtn: true,
      isReleaseDisabled: isReleaseDisabled,
    }
  }, [calculateBtnEnabled, isReleaseDisabled])

  const getIsReleased = async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    try {
      const response = await ReleaseAPIService.getReleaseAOPStatus(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      // If response has data, disable the button (already released)
      // If no data, enable the button (not yet released)
      if (response?.data && Object.keys(response.data).length > 0) {
        setIsReleaseDisabled(true)
      } else {
        setIsReleaseDisabled(false)
      }
    } catch (error) {
      console.error('Error fetching release status:', error)
    }
  }
  useEffect(() => {
    getIsReleased()
  }, [keycloak, AOP_YEAR, PLANT_ID])

  const handleRelease = () => {
    setOpenReleaseDialogBox(true)
  }

  const closeReleaseDialogBox = () => {
    setOpenReleaseDialogBox(false)
  }

  const submitConfirmation = async () => {
    setOpenReleaseDialogBox(false)
    setLoading(true)
    try {
      const response = await ReleaseAPIService.releaseAOPReport(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Released Successfully!',
        severity: 'success',
      })
      setIsReleaseDisabled(true)
      let isReleased = 1
      dispatch(setIsReleased({ isReleased }))
    } catch (error) {
      console.error('Error releasing report:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Release Failed!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  // Calculate Norms data via API
  const handleCalculate = async () => {
    setCaculationLoading(true)
    try {
      await UtilityPlantApiServiceV2.calculateNormsData(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Calculation completed successfully!',
        severity: 'success',
      })
      // Refresh the data after calculation
      await fetchNormsData()
    } catch (error) {
      console.error('Error calculating norms data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error during calculation. Please try again.',
        severity: 'error',
      })
    } finally {
      setCaculationLoading(false)
    }
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
    const fieldsToCheck = MONTH_FIELDS.flatMap((mon) => [
      `${mon}.norms`,
      `${mon}.price`,
    ])
    const validationError = validateNestedRowDataWithRemarks(
      data,
      originalRows,
      fieldsToCheck,
      'generatingPlantName',
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
    const tempPayload = payload?.map((item) => {
      const { normHeaderId, id, inEdit, ...rest } = item
      return {
        ...rest,
        normsHeaderFkId: normHeaderId,
      }
    })

    try {
      // Transform modifiedCells into the format expected by the API

      console.log('payload', tempPayload)

      // Call the API to save changes
      // NOTE: Update this API call to expect nested format when ready
      const response = await UtilityPlantApiServiceV2.saveNormsData(
        keycloak,
        tempPayload, // Now sending nested format: { apr: { norms, quantity, ... } }
        AOP_YEAR,
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
      const response = await UtilityPlantApiServiceV2.saveNormsExcel(
        file,
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Excel file imported successfully!',
          severity: 'success',
        })
        // Refresh data after import
        await fetchNormsData()
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
          link.download = `Norms_Errors_${new Date().getTime()}.xlsx`
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
          await fetchNormsData()
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
      await UtilityPlantApiServiceV2.exportNormsExcel(
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
      console.error('Error exporting Norms data:', error)
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
      <LoaderBackdrop
        open={!!loading || calculationLoading}
        showMessage={calculationLoading}
        message='Your data is being processed. This may take a few moments—thank you for your patience.'
      />
      <AdvanceKendoTable
        columns={nestedColumns}
        rows={rows}
        setRows={setRows}
        handleCalculate={handleCalculate}
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
        groupBy={['cppPlantName','generatingPlantName', 'accountName']}
        handleRelease={handleRelease}
        isReleaseDisabled={isReleaseDisabled}
      />

      <ReleaseDialog
        openReleaseDialogBox={openReleaseDialogBox}
        closeReleaseDialogBox={closeReleaseDialogBox}
        submitConfirmation={submitConfirmation}
      />
    </Box>
  )
}

export default NormsJMD
