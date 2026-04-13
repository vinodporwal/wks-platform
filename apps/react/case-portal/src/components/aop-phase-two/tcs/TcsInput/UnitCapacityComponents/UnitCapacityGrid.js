import { Box, Backdrop, CircularProgress, Stack } from '@mui/material'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { TcsApiService } from 'components/aop-phase-two/services/tcs/tcsApiService'
import { useSession } from 'SessionStoreContext'
import { convertFromKBPSD, convertToKBPSD } from './uomConversionUtils'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import {
  generateCalendarYearHeaders,
  generateHeaderNames,
  extractYear,
} from 'components/aop-phase-two/common/utilities/generateHeaders'

const UnitCapacityGrid = ({
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
  const [apiMetadata, setApiMetadata] = useState({ headers: [], keys: [] })

  const apiYear = useMemo(() => extractYear(AOP_YEAR), [AOP_YEAR])

  // Determine if this grid is the annual "design" capacity type
  const isDesign = capacityType === 'design'

  // Custom itemChange handler to auto-convert between KBPSD and KTPD
  const handleCustomItemChange = useCallback(
    (event, setRowsFunc) => {
      const { dataItem, field, value } = event

      // For design: only one nested field 'value.kbpsd' or 'value.ktpd'
      // For others: 12 month nested fields
      const nestedFields = isDesign
        ? ['value']
        : [
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

      const isNestedField = nestedFields.some((f) => field.startsWith(`${f}.`))

      if (!isNestedField) {
        return
      }

      setRowsFunc((prevRows) => {
        return prevRows.map((row) => {
          if (row.id !== dataItem.id) return row

          const updatedRow = { ...row }
          const [fieldName, uomType] = field.split('.')

          // Handle conversions based on which field was edited
          if (uomType === 'kbpsd') {
            updatedRow[fieldName] = {
              ...updatedRow[fieldName],
              kbpsd: value,
              ktpd: convertFromKBPSD(value, 'KTPD'),
            }
          } else if (uomType === 'ktpd') {
            updatedRow[fieldName] = {
              ...updatedRow[fieldName],
              kbpsd: convertToKBPSD(value, 'KTPD'),
              ktpd: value,
            }
          }

          return updatedRow
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
          transformedData = response.results.map((item, index) => {
            if (isDesign) {
              // Design capacity: single annual value field
              const kbpsdValue = item.jan || 0
              return {
                id: item.id || `row_${index}`,
                particulates: item.particulates,
                value: {
                  kbpsd: kbpsdValue,
                  ktpd: convertFromKBPSD(kbpsdValue, 'KTPD'),
                },
                remark: item.remark,
                insertedDateTime: item.insertedDateTime,
                inEdit: false,
              }
            }

            // Other capacity types: Backend data is in KBPSD, create nested structure for each month
            const months = [
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
            const monthData = {}

            months.forEach((month) => {
              const kbpsdValue = item[month] || 0
              monthData[month] = {
                kbpsd: kbpsdValue,
                ktpd: convertFromKBPSD(kbpsdValue, 'KTPD'),
              }
            })

            return {
              id: item.id || `row_${index}`,
              particulates: item.particulates,
              ...monthData,
              remark: item.remark,
              insertedDateTime: item.insertedDateTime,
              inEdit: false,
            }
          })
        }

        if (response?.headers && response?.keys) {
          setApiMetadata({ headers: response.headers, keys: response.keys })
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

  // Column configuration for Unit Capacity
  const columnConfig = useMemo(() => {
    const config = {
      id: {
        editable: false,
        type: 'text',
        minWidth: 50,
        widthT: 100,
        hidden: true,
      },
      particulates: {
        editable: false,
        type: 'text',
        minWidth: 150,
        widthT: 150,
      },
    }

    if (isDesign) {
      // Design capacity: single annual value column
      config['value.kbpsd'] = {
        editable: true,
        type: 'number1',
        minWidth: 80,
        widthT: 120,
        format: valueFormat,
        title: 'KBPSD',
      }
      config['value.ktpd'] = {
        editable: true,
        type: 'number1',
        minWidth: 80,
        widthT: 120,
        format: valueFormat,
        title: 'KTPD',
      }
    } else {
      // Other capacity types: monthly KBPSD and KTPD sub-columns
      const months = [
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
      months.forEach((month) => {
        config[`${month}.kbpsd`] = {
          editable: true,
          type: 'number1',
          minWidth: 80,
          widthT: 100,
          format: valueFormat,
          title: 'KBPSD',
        }
        config[`${month}.ktpd`] = {
          editable: true,
          type: 'number1',
          minWidth: 80,
          widthT: 100,
          format: valueFormat,
          title: 'KTPD',
        }
      })
    }

    config.remark = {
      title: 'Remark',
      editable: true,
      type: 'text',
      // minWidth: 200,
      // widthT: 250,
    }

    return config
  }, [isDesign, valueFormat])

  const columns = useMemo(() => {
    const { headers, keys } = apiMetadata

    if (!headers || !keys || headers.length === 0 || !headerMap) {
      return []
    }

    // Map keys to their headers from backend
    const columnMap = {}
    headers.forEach((header, index) => {
      columnMap[keys[index]] = header
    })

    // Build columns using columnConfig for type/formatting
    const cols = Object.entries(columnConfig).map(([key, config]) => ({
      field: key,
      title: config.title || columnMap[key] || key,
      ...config,
    }))

    const result = []
    // Position 0: id
    result.push(cols.find((col) => col.field === 'id'))
    // Position 1: particulates
    result.push(cols.find((col) => col.field === 'particulates'))

    if (isDesign) {
      // Design capacity: "Capacity" → "Value" → [KBPSD, KTPD] (mirrors monthly structure)
      const kbpsdCol = cols.find((col) => col.field === 'value.kbpsd')
      const ktpdCol = cols.find((col) => col.field === 'value.ktpd')
      result.push({
        title: 'Capacity',
        children: [kbpsdCol, ktpdCol].filter(Boolean),
      })
    } else {
      // Other types: monthly columns (Apr to Mar)
      const months = [
        { key: 'jan', headerKey: 1 },
        { key: 'feb', headerKey: 2 },
        { key: 'mar', headerKey: 3 },
        { key: 'apr', headerKey: 4 },
        { key: 'may', headerKey: 5 },
        { key: 'jun', headerKey: 6 },
        { key: 'jul', headerKey: 7 },
        { key: 'aug', headerKey: 8 },
        { key: 'sep', headerKey: 9 },
        { key: 'oct', headerKey: 10 },
        { key: 'nov', headerKey: 11 },
        { key: 'dec', headerKey: 12 },
      ]

      const monthlyColumns = months
        .map((month) => {
          const kbpsdCol = cols.find(
            (col) => col.field === `${month.key}.kbpsd`,
          )
          const ktpdCol = cols.find((col) => col.field === `${month.key}.ktpd`)
          return {
            title: headerMap[month.headerKey] || month.key.toUpperCase(),
            children: [kbpsdCol, ktpdCol].filter(Boolean),
          }
        })
        .filter((col) => col.children.length > 0)

      if (monthlyColumns.length > 0) {
        result.push({
          title: 'Capacity',
          children: monthlyColumns,
        })
      }
    }

    // Remark and other remaining columns (excluding id, particulates, insertedDateTime and value sub-cols)
    const skipFields = isDesign
      ? ['id', 'particulates', 'insertedDateTime', 'value.kbpsd', 'value.ktpd']
      : ['id', 'particulates', 'insertedDateTime']
    const remainingCols = cols.filter(
      (col) =>
        !skipFields.includes(col.field) &&
        ![
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
        ].some((m) => col.field.startsWith(`${m}.`)),
    )
    result.push(...remainingCols)
    return result
  }, [isDesign, apiMetadata, columnConfig, headerMap])

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
        ? ['value.kbpsd', 'value.ktpd']
        : [
            'jan.kbpsd',
            'jan.ktpd',
            'feb.kbpsd',
            'feb.ktpd',
            'mar.kbpsd',
            'mar.ktpd',
            'apr.kbpsd',
            'apr.ktpd',
            'may.kbpsd',
            'may.ktpd',
            'jun.kbpsd',
            'jun.ktpd',
            'jul.kbpsd',
            'jul.ktpd',
            'aug.kbpsd',
            'aug.ktpd',
            'sep.kbpsd',
            'sep.ktpd',
            'oct.kbpsd',
            'oct.ktpd',
            'nov.kbpsd',
            'nov.ktpd',
            'dec.kbpsd',
            'dec.ktpd',
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
      const dataInKBPSD = data.map((row) => {
        if (isDesign) {
          // Design: single annual value field storing in jan
          return {
            id: row.isNew ? null : row.id,
            particulates: row.particulates,
            jan: row.value?.kbpsd,
            remark: row.remark,
            insertedDateTime: row.insertedDateTime,
          }
        }
        // Other types: flat monthly KBPSD fields
        return {
          id: row.isNew ? null : row.id,
          particulates: row.particulates,
          jan: row.jan?.kbpsd,
          feb: row.feb?.kbpsd,
          mar: row.mar?.kbpsd,
          apr: row.apr?.kbpsd,
          may: row.may?.kbpsd,
          jun: row.jun?.kbpsd,
          jul: row.jul?.kbpsd,
          aug: row.aug?.kbpsd,
          sep: row.sep?.kbpsd,
          oct: row.oct?.kbpsd,
          nov: row.nov?.kbpsd,
          dec: row.dec?.kbpsd,
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
        />
      </Stack>
    </Box>
  )
}

export default UnitCapacityGrid
