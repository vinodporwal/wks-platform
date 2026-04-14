import { Box, Backdrop, CircularProgress, Stack } from '@mui/material'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { TcsApiService } from 'components/aop-phase-two/services/tcs/tcsApiService'
import { useSession } from 'SessionStoreContext'
import { convertFromKBPSD } from './UnitCapacityComponents/uomConversionUtils'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import {
  extractYear,
  generateCalendarYearHeaders,
} from 'components/aop-phase-two/common/utilities/generateHeaders'

const NetUnitCapacity = ({
  title,
  PLANT_ID,
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
  // Fetch Unit Capacity data for this capacity type
  const fetchNetCapacityData = useCallback(async () => {
    if (!AOP_YEAR) return
    try {
      setLoading(true)

      const response = await TcsApiService.getTcsNetCapacityData(
        keycloak,
        PLANT_ID,
        apiYear,
        'maxAchieved',
      )

      let transformedData = []
      if (response?.results && Array.isArray(response.results)) {
        transformedData = response.results
          .filter((item) => Object.keys(item).length > 0)
          .flatMap((item, index) => {
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
              isKBPSD: true,
              isEditable: false,
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
              isEditable: false,
            }

            return [kbpsdRow, ktpdRow]
          })
      }

      if (response?.headers && response?.keys) {
        setApiMetadata({ headers: response.headers, keys: response.keys })
      }

      setRows(transformedData)
      setOriginalRows(transformedData)
    } catch (err) {
      console.error(`Error fetching Net Unit Capacity data:`, err)
      setSnackbarData({
        message: `Failed to load Unit Capacity data. Please try again.`,
        severity: 'error',
      })
      setSnackbarOpen(true)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, setSnackbarData, setSnackbarOpen])

  // Fetch capacity data when dropdown selection changes
  useEffect(() => {
    if (AOP_YEAR) {
      setModifiedCells({})
      fetchNetCapacityData()
    }
  }, [AOP_YEAR])

  // Column definitions - static configuration with KBPSD editable and KTPD read-only
  const columns = useMemo(() => {
    const monthColumns = [
      {
        field: 'jan',
        title: headerMap[1] || 'Jan',
        editable: false,
        widthT: 100,
        type: 'number1',
        format: valueFormat,
      },
      {
        field: 'feb',
        title: headerMap[2] || 'Feb',
        editable: false,
        widthT: 100,
        type: 'number1',
        format: valueFormat,
      },
      {
        field: 'mar',
        title: headerMap[3] || 'Mar',
        editable: false,
        widthT: 100,
        type: 'number1',
        format: valueFormat,
      },
      {
        field: 'apr',
        title: headerMap[4] || 'Apr',
        editable: false,
        widthT: 100,
        type: 'number1',
        format: valueFormat,
      },
      {
        field: 'may',
        title: headerMap[5] || 'May',
        editable: false,
        widthT: 100,
        type: 'number1',
        format: valueFormat,
      },
      {
        field: 'jun',
        title: headerMap[6] || 'Jun',
        editable: false,
        widthT: 100,
        type: 'number1',
        format: valueFormat,
      },
      {
        field: 'jul',
        title: headerMap[7] || 'Jul',
        editable: false,
        widthT: 100,
        type: 'number1',
        format: valueFormat,
      },
      {
        field: 'aug',
        title: headerMap[8] || 'Aug',
        editable: false,
        widthT: 100,
        type: 'number1',
        format: valueFormat,
      },
      {
        field: 'sep',
        title: headerMap[9] || 'Sep',
        editable: false,
        widthT: 100,
        type: 'number1',
        format: valueFormat,
      },
      {
        field: 'oct',
        title: headerMap[10] || 'Oct',
        editable: false,
        widthT: 100,
        type: 'number1',
        format: valueFormat,
      },
      {
        field: 'nov',
        title: headerMap[11] || 'Nov',
        editable: false,
        widthT: 100,
        type: 'number1',
        format: valueFormat,
      },
      {
        field: 'dec',
        title: headerMap[12] || 'Dec',
        editable: false,
        widthT: 100,
        type: 'number1',
        format: valueFormat,
      },
    ]

    return [
      { field: 'id', title: 'ID', hidden: true },
      {
        field: 'particulates',
        title: 'Particulates',
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
    ]
  }, [headerMap, valueFormat])

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
    remarksEditable: false,
    showCalculate: false,
    downloadExcelBtnFromUI: true,
    ExcelName: `Net_Unit_Capacity_${apiYear}`,
    showImport: false,
    saveBtnForRemark: false,
    saveBtn: false,
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
          fetchData={() => fetchNetCapacityData()}
          title={title}
          handleRemarkCellClick={handleRemarkCellClick}
          columns={columns}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
          setCurrentRowId={() => {}}
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          permissions={permissions}
          groupBy={['particulates']}
        />
      </Stack>
    </Box>
  )
}

export default NetUnitCapacity
