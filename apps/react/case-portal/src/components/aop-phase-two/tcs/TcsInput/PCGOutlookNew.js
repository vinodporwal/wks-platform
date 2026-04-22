import { Box, Backdrop, CircularProgress } from '@mui/material'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { TcsApiService } from 'components/aop-phase-two/services/tcs/tcsApiService'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import {
  generateCalendarYearHeaders,
  generateHeaderNames,
} from 'components/aop-phase-two/common/utilities/generateHeaders'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import { Stack } from '../../../../../node_modules/@mui/material/index'

const PCGOutlookNew = ({
  PLANT_ID,
  SITE_ID,
  VERTICAL_ID,
  AOP_YEAR,
  currentTab,
  snackbarData,
  setSnackbarData,
  snackbarOpen,
  setSnackbarOpen,
}) => {
  const keycloak = useSession()
  const valueFormat = ValueFormatterPhaseTwo()

  // State management
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [customModifiedCells, setCustomModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // Generate dummy data for PCG Outlook
  const generateDummyData = useCallback(() => {
    // Extract year from AOP_YEAR (e.g., "2026-27" -> "26")
    const year = AOP_YEAR ? AOP_YEAR.split('-')[0].slice(-2) : '25'

    const months = [
      { sNo: 1, month: `Jan'${year}` },
      { sNo: 2, month: `Feb'${year}` },
      { sNo: 3, month: `Mar'${year}` },
      { sNo: 4, month: `Apr'${year}` },
      { sNo: 5, month: `May'${year}` },
      { sNo: 6, month: `Jun'${year}` },
      { sNo: 7, month: `Jul'${year}` },
      { sNo: 8, month: `Aug'${year}` },
      { sNo: 9, month: `Sep'${year}` },
      { sNo: 10, month: `Oct'${year}` },
      { sNo: 11, month: `Nov'${year}` },
      { sNo: 12, month: `Dec'${year}` },
    ]

    return months.map((month, index) => ({
      id: `row_${index}`,
      sNo: month.sNo,
      month: month.month,
      gasifierAvailabilityTotal: 7.2,
      gasifierAvailabilityDta: 2.9,
      gasifierAvailabilitySez: 4.3,
      synGasProductionTotal: 15.1,
      synGasProductionDta: 6.0,
      synGasProductionSez: 9.1,
      cge: 71,
      remarks: '',
      inEdit: false,
    }))
  }, [AOP_YEAR])

  // Carry forward data from previous year
  const handleCarryForward = useCallback(async () => {
    try {
      console.log('No PCG Outlook data found, attempting carry-forward...')

      const carryForwardResponse = await TcsApiService.carryForwardPcgOutlook(
        keycloak,
        VERTICAL_ID,
        AOP_YEAR,
        SITE_ID,
      )

      console.log('Carry-forward response:', carryForwardResponse)

      setSnackbarData({
        message: `PCG Outlook data carried forward from previous year successfully!`,
        severity: 'success',
      })
      setSnackbarOpen(true)

      return true
    } catch (carryForwardErr) {
      console.error(
        'Error during carry-forward for PCG Outlook:',
        carryForwardErr,
      )
      return false
    }
  }, [keycloak, AOP_YEAR, SITE_ID, setSnackbarData, setSnackbarOpen])

  // Fetch PCG Outlook Data
  const fetchPcgOutlookData = useCallback(
    async (skipCarryForward = false) => {
      if (!SITE_ID || !AOP_YEAR) return
      try {
        setLoading(true)

        // Use dummy data directly for development/testing
        const transformedData = generateDummyData()

        // If data is empty and carry-forward not skipped, attempt carry-forward and refetch
        if (transformedData.length === 0 && !skipCarryForward) {
          const carryForwardSuccess = await handleCarryForward()
          if (carryForwardSuccess) {
            // Refetch data after successful carry-forward
            await fetchPcgOutlookData(true)
            return
          }
        }

        setRows(transformedData)
        setOriginalRows(transformedData)
        setModifiedCells({})
      } catch (err) {
        console.error('Error fetching PCG Outlook data:', err)
        setSnackbarData({
          message: `Failed to load PCG Outlook data. Please try again.`,
          severity: 'error',
        })
        setSnackbarOpen(true)
        setRows([])
      } finally {
        setLoading(false)
      }
    },
    [
      AOP_YEAR,
      SITE_ID,
      currentTab.id,
      generateDummyData,
      setSnackbarData,
      setSnackbarOpen,
      setModifiedCells,
    ],
  )

  // Fetch data on mount or when dependencies change
  useEffect(() => {
    if (SITE_ID && AOP_YEAR) {
      fetchPcgOutlookData()
    }
  }, [SITE_ID, AOP_YEAR, fetchPcgOutlookData])

  // Generate header names with month-year format
  const headerMap = useMemo(
    () =>
      // generateHeaderNames(AOP_YEAR)
      generateCalendarYearHeaders(AOP_YEAR),
    [AOP_YEAR],
  )

  // Column configuration for PCG Outlook with grouped headers
  const columns = useMemo(() => {
    return [
      { field: 'id', title: 'ID', hidden: true },
      {
        field: 'month',
        title: 'Month',
        width: 150,
        minWidth: 150,
        type: 'text',
        editable: false,
      },
      {
        title: 'Gasifier Availability',
        children: [
          {
            field: 'gasifierAvailabilityTotal',
            title: 'Total',
            editable: false,
            width: 100,
            minWidth: 100,
            type: 'number1',
            format: valueFormat,
          },
          {
            field: 'gasifierAvailabilityDta',
            title: 'DTA',
            editable: true,
            width: 100,
            minWidth: 100,
            type: 'number1',
            format: valueFormat,
          },
          {
            field: 'gasifierAvailabilitySez',
            title: 'SEZ',
            editable: true,
            width: 100,
            minWidth: 100,
            type: 'number1',
            format: valueFormat,
          },
        ],
      },
      {
        title: 'SynGas Production',
        children: [
          {
            field: 'synGasProductionTotal',
            title: 'Total',
            editable: false,
            width: 100,
            minWidth: 100,
            type: 'number1',
            format: valueFormat,
          },
          {
            field: 'synGasProductionDta',
            title: 'DTA',
            editable: true,
            width: 100,
            minWidth: 100,
            type: 'number1',
            format: valueFormat,
          },
          {
            field: 'synGasProductionSez',
            title: 'SEZ',
            editable: true,
            width: 100,
            minWidth: 100,
            type: 'number1',
            format: valueFormat,
          },
        ],
      },
      {
        field: 'cge',
        title: 'CGE (%)',
        editable: true,
        width: 100,
        minWidth: 100,
        type: 'number1',
        minValue: 0,
        maxValue: 100,
        format: valueFormat,
      },
      {
        field: 'remarks',
        title: 'Remark',
        editable: true,
        width: 250,
        minWidth: 250,
        type: 'textarea',
      },
    ]
  }, [headerMap])

  // Handle remark cell click
  const handleRemarkCellClick = (row) => {
    // Prevent remark dialog from opening if row is not editable
    if (!row?.isEditable && row?.isEditable !== undefined) {
      return
    }
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  // Track when modifiedCells is cleared and reset inEdit flags
  useEffect(() => {
    if (Object.keys(modifiedCells).length === 0) {
      setRows((prev) =>
        prev.map((row) => ({
          ...row,
          inEdit: false,
        })),
      )
      setCustomModifiedCells({})
    }
  }, [modifiedCells])

  // Configuration for auto-calculated totals: field -> { dtaField, sezField, totalField }
  const totalCalcConfig = {
    gasifierAvailabilityDta: {
      dtaField: 'gasifierAvailabilityDta',
      sezField: 'gasifierAvailabilitySez',
      totalField: 'gasifierAvailabilityTotal',
    },
    gasifierAvailabilitySez: {
      dtaField: 'gasifierAvailabilityDta',
      sezField: 'gasifierAvailabilitySez',
      totalField: 'gasifierAvailabilityTotal',
    },
    synGasProductionDta: {
      dtaField: 'synGasProductionDta',
      sezField: 'synGasProductionSez',
      totalField: 'synGasProductionTotal',
    },
    synGasProductionSez: {
      dtaField: 'synGasProductionDta',
      sezField: 'synGasProductionSez',
      totalField: 'synGasProductionTotal',
    },
  }

  // Helper to calculate total from DTA and SEZ
  const calculateTotal = (field, value, dataItem) => {
    const config = totalCalcConfig[field]
    if (!config) return null
    const dta = field === config.dtaField ? value : dataItem[config.dtaField]
    const sez = field === config.sezField ? value : dataItem[config.sezField]
    return parseFloat(
      ((parseFloat(dta) || 0) + (parseFloat(sez) || 0)).toFixed(2),
    )
  }

  // Custom item change handler to track inEdit flag and calculate totals
  const customItemChange = useCallback(
    (e, setRowsCallback) => {
      const { dataItem, field, value } = e
      const itemId = `${dataItem.id}`
      const total = calculateTotal(field, value, dataItem)

      // Update rows with inEdit flag and calculated total
      setRowsCallback((prev) =>
        prev.map((row) => {
          if (row.id === dataItem.id) {
            const updated = { ...row, inEdit: true, [field]: value }
            if (total !== null)
              updated[totalCalcConfig[field].totalField] = total
            return updated
          }
          return row
        }),
      )

      // Update customModifiedCells
      setCustomModifiedCells((prev) => {
        const base = { ...(prev[itemId] || {}), [field]: value }
        if (total !== null) base[totalCalcConfig[field].totalField] = total
        return { ...prev, [itemId]: base }
      })

      // Update modifiedCells with calculated total
      if (total !== null) {
        setModifiedCells((prev) => {
          const updated = {
            ...(prev[itemId] || dataItem),
            [field]: value,
            [totalCalcConfig[field].totalField]: total,
          }
          return { ...prev, [itemId]: updated }
        })
      }
    },
    [setModifiedCells, setCustomModifiedCells],
  )

  // Save changes
  const saveChanges = useCallback(async () => {
    try {
      if (Object.keys(modifiedCells).length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
        return
      }

      const rawData = Object.values(modifiedCells)
      const data = rawData.filter((row) => row.inEdit)
      console.log('PCG Outlook data to save:', data)

      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
        return
      }

      // Custom validation: If any row data is updated, remarks must be filled and different from original
      const fieldsToCheck = [
        'gasifierAvailabilityTotal',
        'gasifierAvailabilityDta',
        'gasifierAvailabilitySez',
        'synGasProductionTotal',
        'synGasProductionDta',
        'synGasProductionSez',
        'cge',
      ]
      const validationError = validateRowDataWithRemarks(
        data,
        originalRows,
        fieldsToCheck,
        'month',
        'remarks',
      )

      if (validationError) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationError,
          severity: 'error',
        })
        return
      }

      // Remove id and inEdit fields from payload, keep remarks
      const cleanedData = data.map(({ id, inEdit, ...rest }) => rest)

      const response = await TcsApiService.savePcgOutlookData(
        keycloak,
        VERTICAL_ID,
        SITE_ID,
        AOP_YEAR,
        cleanedData,
      )
      console.log('Save PCG Outlook response:', response)

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'PCG Outlook data saved successfully!',
        severity: 'success',
      })
      setModifiedCells({})
      fetchPcgOutlookData()
    } catch (error) {
      console.error('Error saving PCG Outlook data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error saving PCG Outlook data!',
        severity: 'error',
      })
    }
  }, [
    modifiedCells,
    originalRows,
    keycloak,
    SITE_ID,
    AOP_YEAR,
    setSnackbarData,
    setSnackbarOpen,
    fetchPcgOutlookData,
  ])

  // Export handler
  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'info',
    })

    try {
      await TcsApiService.exportPcgOutlookExcel(
        keycloak,
        VERTICAL_ID,
        SITE_ID,
        AOP_YEAR,
      )

      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting PCG Outlook data:', error)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  // Import handler
  const handleExcelUpload = async (file) => {
    if (!file) return

    setLoading(true)
    try {
      const response = await TcsApiService.importPcgOutlookExcel(
        keycloak,
        VERTICAL_ID,
        SITE_ID,
        AOP_YEAR,
        file,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Excel file imported successfully!',
          severity: 'success',
        })
        // Refresh data after import
        await fetchPcgOutlookData()
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
          link.download = `TCS_PCG_Outlook_Errors_${new Date().getTime()}.xlsx`
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
          await fetchPcgOutlookData()
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

  const permissions = {
    customHeight: { mainBox: '32vh', otherBox: '100%' },
    textAlignment: 'center',
    allAction: true,
    addButton: false,
    remarksEditable: true,
    showCalculate: false,
    showExport: true,
    ExcelName: `PCG_Outlook_${AOP_YEAR}`,
    showImport: true,
    saveBtnForRemark: true,
    saveBtn: true,
    showWorkFlowBtns: false,
    showTitle: true,
    filterable: false,
  }

  return (
    <Box>
      <Backdrop
        sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
        open={!!loading}
      >
        <CircularProgress color='inherit' />
      </Backdrop>

      <Stack sx={{ mt: 2 }}>
        <AdvanceKendoTable
          rows={rows}
          setRows={setRows}
          fetchData={fetchPcgOutlookData}
          configType='tcs_pcg_outlook'
          title='PCG Outlook'
          handleRemarkCellClick={handleRemarkCellClick}
          columns={columns}
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
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          externalCustomModifiedCells={customModifiedCells}
          externalSetCustomModifiedCells={setCustomModifiedCells}
          permissions={permissions}
          customItemChange={customItemChange}
        />
      </Stack>
    </Box>
  )
}

export default PCGOutlookNew
