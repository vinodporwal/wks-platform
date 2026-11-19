import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import TargetActualDafThroughput from './targetActualDafThroughput'
import { ProductionNormsApiService } from 'components/aop-phase-two/services/pcg/productionNormsApiService'

const GasifierOperation = () => {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [modifiedCells, setModifiedCells] = useState({})
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const keycloak = useSession()

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year } = dataGridStore
  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear || year || ''
  const verticalName = verticalObject?.name || ''
  const siteName = siteObject?.name || ''
  const plantName = plantObject?.name || ''

  // Column definitions: only DisplayName and GConfiguration in UI
  const columns = useMemo(
    () => [
      {
        field: 'displayName',
        title: 'G Operation',
        width: 400,
        widthT: 400,
        minWidth: 350,
        type: 'text',
        editable: false,
      },
      {
        field: 'gConfiguration',
        title: 'G Configuration',
        width: 250,
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'actualDAFThroughput',
        title: 'Actual DAF Throughput',
        width: 250,
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'minDAFThroughput',
        title: 'Min DAF Throughput',
        width: 250,
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'maxDAFThroughput',
        title: 'Max DAF Throughput',
        width: 250,
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
    ],
    [],
  )

  // Permissions configuration: read-only table without SAVE button, with UI Excel Export
  const permissions = useMemo(
    () => ({
      showAction: false,
      addButton: false,
      deleteButton: false,
      editButton: false,
      saveBtn: false,
      allAction: true,
      downloadExcelBtnFromUI: true,
      showExport: false,
      ExcelName: `${verticalName}_${siteName}_${plantName}_${AOP_YEAR}_Target Gasifier Filter`,
      showImport: false,
      showTitleNameBusiness: true,
      showTitle: true,
      titleName: 'Target Gasifier Filter',
      showCalculate: false,
      calculateDisabled: true,
    }),
    [verticalName, siteName, plantName, AOP_YEAR],
  )

  // Fetch Target Gasifier Filters directly from GET API
  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response =
        await ProductionNormsApiService.getTargetGasifierFilters(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      const list = Array.isArray(response)
        ? response
        : response?.data || response?.result || []

      if (Array.isArray(list) && list.length > 0) {
        const formattedRows = list.map((item, index) => ({
          ...item,
          id: item?.id || item?.Id || `row_${index + 1}`,
          displayName:
            item?.displayName ||
            item?.DisplayName ||
            item?.operationName ||
            item?.OperationName ||
            '',
          gConfiguration: item?.gConfiguration || item?.GConfiguration || '',
          isEditable: false,
          inEdit: false,
        }))
        setRows(formattedRows)
      } else {
        setRows([])
      }
    } catch (error) {
      console.error('Error fetching Target Gasifier Filters:', error)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <Box>
        <TargetActualDafThroughput />
      </Box>

      {/* Target Actual DAF Throughput Grid */}
      <Box sx={{ mt: 3 }}>
        <AdvanceKendoTable
          columns={columns}
          rows={rows}
          setRows={setRows}
          loading={loading}
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          permissions={permissions}
          title={permissions.showTitle ? permissions.titleName : ''}
          fetchData={fetchData}
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          pagable={false}
          customHeight={30}
        />
      </Box>
    </Box>
  )
}

export default GasifierOperation
