import { Box, Backdrop, CircularProgress, Stack } from '@mui/material'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { TcsOutputApiService } from 'components/aop-phase-two/services/tcs/tcsOutputApiService'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { convertFromKBPSD } from './uomConversionUtils'
import {
  extractYear,
  generateCalendarYearHeaders,
} from 'components/aop-phase-two/common/utilities/generateHeaders'

const UnitCapacityGridRowwise = ({
  capacityType,
  title,
  SITE_ID,
  VERTICAL_ID,
  PLANT_ID,
  AOP_YEAR,
  snackbarData,
  setSnackbarData,
  snackbarOpen,
  setSnackbarOpen,
  userRole,
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
  // Fetch Unit Capacity data for this capacity type
  const fetchUnitCapacityData = useCallback(async () => {
    if (!SITE_ID || !VERTICAL_ID || !AOP_YEAR) return
    try {
      setLoading(true)

      const response = await TcsOutputApiService.getTcsUnitCapacityData(
        keycloak,
        SITE_ID,
        VERTICAL_ID,
        apiYear,
        capacityType,
      )

      let transformedData = []
      if (response?.results && Array.isArray(response.results)) {
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
          }

          return [kbpsdRow, ktpdRow]
        })
      }

      setRows(transformedData)
      setOriginalRows(transformedData)
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
    SITE_ID,
    VERTICAL_ID,
    AOP_YEAR,
    capacityType,
    isDesign,
    setSnackbarData,
    setSnackbarOpen,
  ])

  // Fetch capacity data when dropdown selection changes
  useEffect(() => {
    if (SITE_ID && VERTICAL_ID && AOP_YEAR) {
      fetchUnitCapacityData()
    }
  }, [SITE_ID, VERTICAL_ID, AOP_YEAR, fetchUnitCapacityData])

  // Column definitions - static configuration (flat values, no KTPD)
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
              field: 'jan',
              title: 'Value',
              editable: false,
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
          editable: false,
        },
      ]
    } else {
      const monthColumns = [
        {
          field: 'jan',
          title: headerMap[1],
          editable: false,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'feb',
          title: headerMap[2],
          editable: false,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'mar',
          title: headerMap[3],
          editable: false,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'apr',
          title: headerMap[4],
          editable: false,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'may',
          title: headerMap[5],
          editable: false,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'jun',
          title: headerMap[6],
          editable: false,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'jul',
          title: headerMap[7],
          editable: false,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'aug',
          title: headerMap[8],
          editable: false,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'sep',
          title: headerMap[9],
          editable: false,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'oct',
          title: headerMap[10],
          editable: false,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'nov',
          title: headerMap[11],
          editable: false,
          widthT: 100,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: 'dec',
          title: headerMap[12],
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
          editable: false,
        },
      ]
    }
  }, [isDesign, valueFormat, headerMap])

  // Export handler
  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'info',
    })

    try {
      await TcsOutputApiService.exportUnitCapacityExcel(
        keycloak,
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

  // Handle remark cell click
  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const permissions = useMemo(
    () => ({
      customHeight: { mainBox: '32vh', otherBox: '100%' },
      textAlignment: 'center',
      allAction: true,
      showExport: true,
      showTitle: true,
      showDropdown: false,
      approveBtn: false,
    }),
    [userRole],
  )

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
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          permissions={permissions}
          readonly={true}
          handleExport={handleExport}
          groupBy={['particulates']}
        />
      </Stack>
    </Box>
  )
}

export default UnitCapacityGridRowwise
