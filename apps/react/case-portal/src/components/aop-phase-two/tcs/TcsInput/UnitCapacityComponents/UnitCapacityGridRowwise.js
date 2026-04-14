import { Box, Backdrop, CircularProgress, Stack } from '@mui/material'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { TcsApiService } from 'components/aop-phase-two/services/tcs/tcsApiService'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { convertFromKBPSD } from './uomConversionUtils'
import {
  generateCalendarYearHeaders,
  extractYear,
} from 'components/aop-phase-two/common/utilities/generateHeaders'

const UnitCapacityGridRowwise = ({
  capacityType,
  title,
  PLANT_ID,
  SITE_ID,
  VERTICAL_ID,
  AOP_YEAR,
  snackbarData,
  setSnackbarData,
  snackbarOpen,
  setSnackbarOpen,
}) => {
  const keycloak = useSession()
  const valueFormat = ValueFormatterPhaseTwo()
  // const headerMap = generateHeaderNames(AOP_YEAR)
  const headerMap = generateCalendarYearHeaders(AOP_YEAR)

  // State management for this capacity type only
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const apiYear = useMemo(() => extractYear(AOP_YEAR), [AOP_YEAR])

  // Determine if this grid is the annual "design" capacity type
  const isDesign = capacityType === 'design'

  // Custom itemChange handler - updates KBPSD and syncs KTPD conversion
  const handleCustomItemChange = useCallback(
    (event, setRowsFunc) => {
      const { dataItem, field, value } = event

      // For design: only 'value' field
      // For others: 12 month fields (jan, feb, mar, apr, may, jun, jul, aug, sep, oct, nov, dec)
      const monthFields = [
        'jan',
        'feb',
        'mar',
        'apr',
        'may',
        'jun',
        'jul',
        'aug',
        'sep',
        'oct',
        'nov',
        'dec',
      ]
      const designFields = ['value']
      const validFields = isDesign ? designFields : monthFields

      if (!validFields.includes(field)) {
        return
      }

      setRowsFunc((prevRows) => {
        return prevRows.map((row) => {
          // Update the edited KBPSD row
          if (row.id === dataItem.id) {
            return { ...row, [field]: value }
          }

          // If editing a KBPSD row, also update the corresponding KTPD row
          if (
            dataItem.isKBPSD &&
            row.id === dataItem.id.replace('_kbpsd', '_ktpd')
          ) {
            return { ...row, [field]: convertFromKBPSD(value, 'KTPD') }
          }

          return row
        })
      })
    },
    [isDesign],
  )

  // Carry forward data from previous year
  const handleCarryForward = useCallback(async () => {
    try {
      console.log(
        `No data found for ${capacityType}, attempting carry-forward...`,
      )

      const carryForwardResponse =
        await TcsApiService.carryForwardTcsUnitCapacity(
          keycloak,
          PLANT_ID,
          apiYear,
          capacityType,
        )

      console.log('Carry-forward response:', carryForwardResponse)

      setSnackbarData({
        message: `Data carried forward from previous year successfully!`,
        severity: 'success',
      })
      setSnackbarOpen(true)

      return true
    } catch (carryForwardErr) {
      console.error(
        `Error during carry-forward for ${capacityType}:`,
        carryForwardErr,
      )
      return false
    }
  }, [
    keycloak,
    PLANT_ID,
    apiYear,
    capacityType,
    setSnackbarData,
    setSnackbarOpen,
  ])

  // Fetch Unit Capacity data for this capacity type
  const fetchUnitCapacityData = useCallback(
    async (skipCarryForward = false) => {
      if (!PLANT_ID || !AOP_YEAR) return
      try {
        setLoading(true)
        const response = await TcsApiService.getTcsUnitCapacityData(
          keycloak,
          PLANT_ID,
          apiYear,
          capacityType,
          'KBPSD',
        )

        let transformedData = []
        if (response?.results && Array.isArray(response.results)) {
          if (isDesign) {
            transformedData = response.results.flatMap((item, index) => {
              const kbpsdRow = {
                id: `${item.id || `row_${index}`}_kbpsd`,
                particulates: item.particulates,
                uom: 'KBPSD',
                value: item.jan || 0,
                remark: item.remark || '',
                insertedDateTime: item.insertedDateTime,
                inEdit: false,
                isEditable: true,
                isKBPSD: true,
              }

              const ktpdRow = {
                id: `${item.id || `row_${index}`}_ktpd`,
                particulates: item.particulates,
                uom: 'KTPD',
                value: convertFromKBPSD(item.jan || 0, 'KTPD'),
                remark: '',
                insertedDateTime: item.insertedDateTime,
                inEdit: false,
                isKBPSD: false,
                isEditable: false,
              }

              return [kbpsdRow, ktpdRow]
            })
          } else {
            transformedData = response.results.flatMap((item, index) => {
              const kbpsdRow = {
                id: `${item.id || `row_${index}`}_kbpsd`,
                particulates: item.particulates,
                uom: 'KBPSD',
                jan: item.jan || 0,
                feb: item.feb || 0,
                mar: item.mar || 0,
                apr: item.apr || 0,
                may: item.may || 0,
                jun: item.jun || 0,
                jul: item.jul || 0,
                aug: item.aug || 0,
                sep: item.sep || 0,
                oct: item.oct || 0,
                nov: item.nov || 0,
                dec: item.dec || 0,
                remark: item.remark || '',
                insertedDateTime: item.insertedDateTime,
                inEdit: false,
                isEditable: true,
                isKBPSD: true,
              }

              const ktpdRow = {
                id: `${item.id || `row_${index}`}_ktpd`,
                particulates: item.particulates,
                uom: 'KTPD',
                jan: convertFromKBPSD(item.jan || 0, 'KTPD'),
                feb: convertFromKBPSD(item.feb || 0, 'KTPD'),
                mar: convertFromKBPSD(item.mar || 0, 'KTPD'),
                apr: convertFromKBPSD(item.apr || 0, 'KTPD'),
                may: convertFromKBPSD(item.may || 0, 'KTPD'),
                jun: convertFromKBPSD(item.jun || 0, 'KTPD'),
                jul: convertFromKBPSD(item.jul || 0, 'KTPD'),
                aug: convertFromKBPSD(item.aug || 0, 'KTPD'),
                sep: convertFromKBPSD(item.sep || 0, 'KTPD'),
                oct: convertFromKBPSD(item.oct || 0, 'KTPD'),
                nov: convertFromKBPSD(item.nov || 0, 'KTPD'),
                dec: convertFromKBPSD(item.dec || 0, 'KTPD'),
                remark: '',
                insertedDateTime: item.insertedDateTime,
                inEdit: false,
                isKBPSD: false,
                isEditable: true,
              }

              return [kbpsdRow, ktpdRow]
            })
          }
        }

        // If data is empty and carry-forward not skipped, attempt carry-forward and refetch
        if (transformedData.length === 0 && !skipCarryForward) {
          const carryForwardSuccess = await handleCarryForward()
          if (carryForwardSuccess) {
            // Refetch data after successful carry-forward
            await fetchUnitCapacityData(true)
            return
          }
        }

        setRows(transformedData)
        setOriginalRows(transformedData)
      } catch (err) {
        console.error(
          `Error fetching Unit Capacity data (${capacityType}):`,
          err,
        )
        setSnackbarData({
          message: `Failed to load Unit Capacity data. Please try again.`,
          severity: 'error',
        })
        setSnackbarOpen(true)
        setRows([])
      } finally {
        setLoading(false)
      }
    },
    [
      keycloak,
      PLANT_ID,
      apiYear,
      capacityType,
      isDesign,
      handleCarryForward,
      setSnackbarData,
      setSnackbarOpen,
    ],
  )

  // Fetch capacity data when dropdown selection changes
  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      // Clear modified cells when UOM changes to reset edit state
      setModifiedCells({})
      fetchUnitCapacityData()
    }
  }, [PLANT_ID, apiYear, fetchUnitCapacityData])

  // Column definitions - static configuration with KBPSD editable and KTPD read-only
  const columns = useMemo(() => {
    if (isDesign) {
      return [
        { field: 'id', title: 'ID', hidden: true },
        {
          field: 'particulates',
          title: 'Particulars',
          widthT: 150,
          minWidth: 150,
          type: 'text',
          editable: false,
          hidden: false,
        },
        {
          field: 'uom',
          title: 'UOM',
          editable: false,
          widthT: 100,
          minWidth: 100,
          type: 'text',
        },
        {
          title: 'Capacity',
          children: [
            {
              field: 'value',
              title: 'Value',
              editable: true,
              widthT: 100,
              minWidth: 100,
              type: 'number1',
              format: valueFormat,
            },
          ],
        },
        {
          field: 'remark',
          title: 'Remarks',
          widthT: 250,
          minWidth: 250,
          type: 'text',
          editable: true,
        },
      ]
    } else {
      const monthColumns = [
        {
          field: 'jan',
          title: headerMap[1],
          editable: true,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'feb',
          title: headerMap[2],
          editable: true,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'mar',
          title: headerMap[3],
          editable: true,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'apr',
          title: headerMap[4],
          editable: true,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'may',
          title: headerMap[5],
          editable: true,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'jun',
          title: headerMap[6],
          editable: true,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'jul',
          title: headerMap[7],
          editable: true,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'aug',
          title: headerMap[8],
          editable: true,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'sep',
          title: headerMap[9],
          editable: true,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'oct',
          title: headerMap[10],
          editable: true,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'nov',
          title: headerMap[11],
          editable: true,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'dec',
          title: headerMap[12],
          editable: true,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
      ]

      return [
        { field: 'id', title: 'ID', hidden: true },
        {
          field: 'particulates',
          title: 'Particulars',
          widthT: 150,
          minWidth: 150,
          type: 'text',
          editable: false,
          hidden: false,
        },
        {
          field: 'uom',
          title: 'UOM',
          editable: false,
          widthT: 100,
          minWidth: 100,
          type: 'text',
        },
        {
          title: 'Capacity',
          children: monthColumns,
        },
        {
          field: 'remark',
          title: 'Remarks',
          widthT: 250,
          minWidth: 250,
          type: 'text',
          editable: true,
        },
      ]
    }
  }, [isDesign, valueFormat, headerMap])

  // Handle remark cell click
  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  // Save changes for this capacity type
  const saveChanges = useCallback(async () => {
    try {
      if (Object.keys(modifiedCells).length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
        return
      }

      const rawData = Object.values(modifiedCells)
      const data = rawData.filter((row) => row.inEdit)

      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
        return
      }

      // Custom validation: If any row data is updated, remarks must be filled and different from original
      const fieldsToCheck = isDesign
        ? ['value']
        : [
            'jan',
            'feb',
            'mar',
            'apr',
            'may',
            'jun',
            'jul',
            'aug',
            'sep',
            'oct',
            'nov',
            'dec',
          ]

      const validationError = validateRowDataWithRemarks(
        data,
        originalRows,
        fieldsToCheck,
        'particulates',
        'remark',
      )

      if (validationError) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationError,
          severity: 'error',
        })
        return
      }

      // Build payload — backend expects flat fields
      const dataInKBPSD = data
        .filter((row) => row.isKBPSD) // Only save KBPSD rows, skip KTPD rows
        .map((row) => {
          // Strip _kbpsd or _ktpd suffix from ID
          const cleanId = row.id.replace(/_kbpsd$/, '').replace(/_ktpd$/, '')

          if (isDesign) {
            // Design: single annual value field storing in jan
            return {
              id: row.isNew ? null : cleanId,
              particulates: row.particulates,
              jan: row.value,
              remark: row.remark,
              insertedDateTime: row.insertedDateTime,
            }
          }
          // Other types: flat monthly fields
          return {
            id: row.isNew ? null : cleanId,
            particulates: row.particulates,
            jan: row.jan,
            feb: row.feb,
            mar: row.mar,
            apr: row.apr,
            may: row.may,
            jun: row.jun,
            jul: row.jul,
            aug: row.aug,
            sep: row.sep,
            oct: row.oct,
            nov: row.nov,
            dec: row.dec,
            remark: row.remark,
            insertedDateTime: row.insertedDateTime,
          }
        })

      const response = await TcsApiService.saveUnitCapacityData(
        keycloak,
        PLANT_ID,
        apiYear,
        capacityType,
        'KBPSD',
        dataInKBPSD,
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unit Capacity data saved successfully!',
        severity: 'success',
      })
      setModifiedCells({})
    } catch (error) {
      console.error('Error saving Unit Capacity data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error saving Unit Capacity data!',
        severity: 'error',
      })
    }
  }, [
    isDesign,
    modifiedCells,
    originalRows,
    keycloak,
    PLANT_ID,
    apiYear,
    capacityType,
    setSnackbarData,
    setSnackbarOpen,
  ])

  // Export handler
  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'info',
    })

    try {
      await TcsApiService.exportUnitCapacityExcel(
        keycloak,
        PLANT_ID,
        SITE_ID,
        VERTICAL_ID,
        apiYear,
        capacityType,
      )

      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting Unit Capacity data:', error)
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
      const response = await TcsApiService.importUnitCapacityExcel(
        keycloak,
        PLANT_ID,
        apiYear,
        capacityType,
        file,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Excel file imported successfully!',
          severity: 'success',
        })
        // Refresh data after import
        await fetchUnitCapacityData()
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
          link.download = `TCS_Unit_Capacity_${capacityType}_Errors_${new Date().getTime()}.xlsx`
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
          await fetchUnitCapacityData()
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
    ExcelName: `Unit_Capacity_${capacityType}_${AOP_YEAR}`,
    showImport: true,
    saveBtnForRemark: true,
    saveBtn: true,
    showWorkFlowBtns: false,
    showTitle: true,
    showDropdown: false,
  }

  return (
    <Box>
      <Backdrop
        sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
        open={loading}
      >
        <CircularProgress color='inherit' />
      </Backdrop>

      <Stack sx={{ mt: 2 }}>
        <AdvanceKendoTable
          rows={rows}
          setRows={setRows}
          fetchData={() => fetchUnitCapacityData()}
          title={title}
          handleRemarkCellClick={handleRemarkCellClick}
          columns={columns}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
          setCurrentRowId={() => {}}
          saveChanges={saveChanges}
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          permissions={permissions}
          customItemChange={handleCustomItemChange}
          handleExcelUpload={handleExcelUpload}
          handleExport={handleExport}
          groupBy={['particulates']}
        />
      </Stack>
    </Box>
  )
}

export default UnitCapacityGridRowwise
