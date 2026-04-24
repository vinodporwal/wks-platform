import { Box, Backdrop, CircularProgress } from '@mui/material'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { TcsOutputApiService } from 'components/aop-phase-two/services/tcs/tcsOutputApiService'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { generateCalendarYearHeaders } from 'components/aop-phase-two/common/utilities/generateHeaders'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
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
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // Fetch PCG Outlook Data (read-only)
  const fetchPcgOutlookData = useCallback(async () => {
    if (!SITE_ID || !AOP_YEAR) return
    try {
      setLoading(true)
      let transformedData = []

      const response = await TcsOutputApiService.getPcgOutlookData(
        keycloak,
        VERTICAL_ID,
        SITE_ID,
        AOP_YEAR,
      )
      console.log('PCG Outlook (Output) Response:', response)

      if (response?.length > 0 && Array.isArray(response)) {
        transformedData = response.map((item, index) => ({
          id: item.id || `row_${index}`,
          ...item,
          remarks: item.remarks || '',
          inEdit: false,
          isEditable: false,
        }))
      }

      setRows(transformedData)
    } catch (err) {
      console.error('Error fetching PCG Outlook (Output) data:', err)
      setSnackbarData({
        message: `Failed to load PCG Outlook data. Please try again.`,
        severity: 'error',
      })
      setSnackbarOpen(true)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [
    keycloak,
    AOP_YEAR,
    SITE_ID,
    VERTICAL_ID,
    currentTab.id,
    setSnackbarData,
    setSnackbarOpen,
  ])

  // Fetch data on mount or when dependencies change
  useEffect(() => {
    if (SITE_ID && AOP_YEAR) {
      fetchPcgOutlookData()
    }
  }, [SITE_ID, AOP_YEAR, fetchPcgOutlookData])

  // Generate calendar-year header map
  const headerMap = useMemo(
    () => generateCalendarYearHeaders(AOP_YEAR),
    [AOP_YEAR],
  )

  // Column configuration — grouped headers (read-only: editable: false on all leaf columns)
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
            editable: false,
            width: 100,
            minWidth: 100,
            type: 'number1',
            format: valueFormat,
          },
          {
            field: 'gasifierAvailabilitySez',
            title: 'SEZ',
            editable: false,
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
            editable: false,
            width: 100,
            minWidth: 100,
            type: 'number1',
            format: valueFormat,
          },
          {
            field: 'synGasProductionSez',
            title: 'SEZ',
            editable: false,
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
        editable: false,
        width: 100,
        minWidth: 100,
        type: 'number1',
        format: valueFormat,
      },
      {
        field: 'remarks',
        title: 'Remark',
        editable: false,
        // width: 250,
        // minWidth: 250,
        type: 'textarea',
      },
    ]
  }, [headerMap, valueFormat])

  // Handle remark cell click — no-op for read-only rows
  const handleRemarkCellClick = (row) => {
    if (!row?.isEditable && row?.isEditable !== undefined) {
      return
    }
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  // Export handler
  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'info',
    })

    try {
      await TcsOutputApiService.exportPcgOutlookExcel(
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
      console.error('Error exporting PCG Outlook (Output) data:', error)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  const permissions = {
    customHeight: { mainBox: '32vh', otherBox: '100%' },
    textAlignment: 'center',
    allAction: true,
    addButton: false,
    remarksEditable: false,
    showCalculate: false,
    showExport: true,
    ExcelName: `PCG_Outlook_Output_${AOP_YEAR}`,
    showImport: false,
    saveBtnForRemark: false,
    saveBtn: false,
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
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          permissions={permissions}
          readonly={true}
          handleExport={handleExport}
        />
      </Stack>
    </Box>
  )
}

export default PCGOutlookNew
