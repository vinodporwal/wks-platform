import { Box, Backdrop, CircularProgress, Stack } from '@mui/material'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { TcsApiService } from 'components/aop-phase-two/services/tcs/tcsApiService'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import {
  generateCalendarYearHeaders,
  extractYear,
} from 'components/aop-phase-two/common/utilities/generateHeaders'

const UnitCapacitySimple = ({
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
  const headerMap = generateCalendarYearHeaders(AOP_YEAR)

  const [loading, setLoading] = useState(true)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const apiYear = useMemo(() => extractYear(AOP_YEAR), [AOP_YEAR])
  const isDesign = capacityType === 'design'

  const fetchUnitCapacityData = useCallback(async () => {
    setLoading(true)
    try {
      const response = await TcsApiService.getTcsUnitCapacityData(
        keycloak,
        PLANT_ID,
        apiYear,
        capacityType,
        'KBPSD',
      )

      if (response?.results) {
        let tempData = response?.results?.map((item) => {
          return {
            ...item,
            value: item.jan || 0,
          }
        })
        setRows(tempData)
        setOriginalRows(tempData)
      } else {
        setRows([])
        setOriginalRows([])
      }
    } catch (err) {
      console.error(`Error fetching Unit Capacity data (${capacityType}):`, err)
      setSnackbarData({
        message: `Failed to load Unit Capacity data. Please try again.`,
        severity: 'error',
      })
      setSnackbarOpen(true)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [
    keycloak,
    PLANT_ID,
    apiYear,
    capacityType,
    setSnackbarData,
    setSnackbarOpen,
  ])

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      setModifiedCells({})
      fetchUnitCapacityData()
    }
  }, [PLANT_ID, apiYear, fetchUnitCapacityData])

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
  }, [isDesign, headerMap, valueFormat])

  const handleCustomItemChange = useCallback((event, setRowsFunc) => {
    const { dataItem, field, value } = event

    setRowsFunc((prevRows) =>
      prevRows.map((row) => {
        if (row.id === dataItem.id) {
          return {
            ...row,
            [field]: value,
            inEdit: true,
          }
        }
        return row
      }),
    )

    setModifiedCells((prev) => ({
      ...prev,
      [dataItem.id]: {
        ...dataItem,
        [field]: value,
        inEdit: true,
      },
    }))
  }, [])

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

      // Convert value to jan for design type before sending to API
      const payloadData = isDesign
        ? data.map((row) => {
            const { value, ...rest } = row
            return {
              ...rest,
              jan: value,
            }
          })
        : data

      const response = await TcsApiService.saveUnitCapacityData(
        keycloak,
        PLANT_ID,
        apiYear,
        capacityType,
        'KBPSD',
        payloadData,
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
  }, [modifiedCells, keycloak, setSnackbarData, setSnackbarOpen])

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

  // Handle remark cell click
  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
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
    <Box sx={{ width: '100%' }}>
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
          columns={columns}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
          setCurrentRowId={() => {}}
          customItemChange={handleCustomItemChange}
          saveChanges={saveChanges}
          handleRemarkCellClick={handleRemarkCellClick}
          title={title}
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          permissions={permissions}
          handleExcelUpload={handleExcelUpload}
          handleExport={handleExport}
          groupBy={['particulates']}
        />
      </Stack>
    </Box>
  )
}

export default UnitCapacitySimple
