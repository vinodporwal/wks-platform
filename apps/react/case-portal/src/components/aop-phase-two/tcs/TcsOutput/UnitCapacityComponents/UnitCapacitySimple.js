import { Box, Backdrop, CircularProgress, Stack } from '@mui/material'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { TcsOutputApiService } from 'components/aop-phase-two/services/tcs/tcsOutputApiService'
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
  userRole,
}) => {
  const keycloak = useSession()
  const valueFormat = ValueFormatterPhaseTwo()
  const headerMap = generateCalendarYearHeaders(AOP_YEAR)

  const [loading, setLoading] = useState(true)
  const [rows, setRows] = useState([])

  const apiYear = useMemo(() => extractYear(AOP_YEAR), [AOP_YEAR])
  const isDesign = capacityType === 'design'

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

      if (response?.results && Array.isArray(response.results)) {
        let tempData = response?.results?.map((item) => {
          return {
            ...item,
            value: item.jan || 0,
          }
        })
        setRows(tempData)
      } else {
        setRows([])
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
    SITE_ID,
    VERTICAL_ID,
    apiYear,
    capacityType,
    setSnackbarData,
    setSnackbarOpen,
  ])

  useEffect(() => {
    if (SITE_ID && VERTICAL_ID && AOP_YEAR) {
      fetchUnitCapacityData()
    }
  }, [SITE_ID, VERTICAL_ID, AOP_YEAR, fetchUnitCapacityData])

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
  }, [isDesign, headerMap, valueFormat])

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

  const permissions = {
    customHeight: { mainBox: '32vh', otherBox: '100%' },
    textAlignment: 'center',
    allAction: true,
    addButton: false,
    remarksEditable: false,
    showCalculate: false,
    showExport: true,
    ExcelName: `Unit_Capacity_${capacityType}_${AOP_YEAR}`,
    showImport: false,
    saveBtnForRemark: false,
    saveBtn: false,
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
          title={title}
          handleExport={handleExport}
          permissions={permissions}
          groupBy={['particulates']}
        />
      </Stack>
    </Box>
  )
}

export default UnitCapacitySimple
