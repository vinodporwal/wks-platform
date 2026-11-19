import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react'
import { Box } from '@mui/material'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from '../../../utilities/excelNameUtil'
import { downloadBase64Excel } from '../../../utilities/downloadBase64Excel'
import { ProductionNormsApiService } from 'components/aop-phase-two/services/naphthasplitter/productionNormsApiService'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import usePlantContext from '../../../hooks/usePlantContext'
import AdvanceKendoTable from '../../../AdvanceKendoTable/index'

const DataFiltersTab = ({
  refreshData,
  onLoadNormCalculation,
  plantContext,
  variantConfig = {},
}) => {
  const keycloak = useSession()
  const fallbackContext = usePlantContext()
  const context = plantContext || fallbackContext

  const requestIdRef = useRef(0)
  const apiRef = useRef(null)

  const {
    plantId: PLANT_ID,
    aopYear: AOP_YEAR,
    isOldYear: IS_OLD_YEAR,
    isReleased: IS_RELEASED,
    dataGridStore,
  } = context

  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const apiService = variantConfig?.apiService || ProductionNormsApiService

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [open1, setOpen1] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const columns = useMemo(() => {
    const baseColumns = [
      {
        field: 'particulars',
        title: 'Particulars',
        type: 'text',
        editable: false,
        widthT: 220,
        minWidth: 200,
      },
      {
        field: 'UOM',
        title: 'UOM',
        type: 'text',
        editable: false,
        widthT: 120,
        minWidth: 100,
      },
      {
        field: 'targetValue',
        title: 'Target Value',
        type: 'number',
        format: '{0:0.00}',
        editable: !READ_ONLY,
        widthT: 160,
        minWidth: 140,
      },
      {
        field: 'range',
        title: 'Range (+/-) %',
        type: 'number',
        format: '{0:0.00}',
        editable: !READ_ONLY,
        widthT: 160,
        minWidth: 140,
      },
      {
        field: 'remarks',
        title: 'Remarks',
        type: 'text',
        editable: !READ_ONLY,
        widthT: 200,
        minWidth: 160,
      },
    ]

    return typeof variantConfig?.getColumns === 'function'
      ? variantConfig.getColumns(baseColumns, context)
      : baseColumns
  }, [READ_ONLY, variantConfig, context])

  const permissions = useMemo(() => {
    const basePermissions = {
      showAction: true,
      addButton: false,
      addBtnName: 'Add Item',
      deleteButton: false,
      editButton: false,
      saveBtn: !READ_ONLY,
      allAction: true,
      showExport: false,
      ExcelName: `Data_Filter_${AOP_YEAR || ''}`,
      showImport: false,
      showTitleNameBusiness: true,
      showTitle: true,
      titleName: 'Data Filter',
      showCalculate: false,
      calculateDisabled: true,
    }

    return typeof variantConfig?.getPermissions === 'function'
      ? variantConfig.getPermissions(basePermissions, context)
      : basePermissions
  }, [AOP_YEAR, READ_ONLY, variantConfig, context])

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    const requestId = ++requestIdRef.current
    setLoading(true)
    setModifiedCells({})
    try {
      const response = await apiService.getDataFilterData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (requestId !== requestIdRef.current) return

      const list = Array.isArray(response)
        ? response
        : response?.data || response?.result || []

      if (Array.isArray(list) && list.length > 0) {
        const formattedRows = list.map((item, index) => {
          const targetVal =
            item?.targetValue !== null && item?.targetValue !== undefined
              ? item.targetValue
              : 0
          const rangeVal =
            item?.range !== null && item?.range !== undefined ? item.range : 0
          const remarksVal = item?.remarks || item?.remark || ''

          return {
            ...item,
            id: item?.id || index + 1,
            idFromApi: item?.id || null,
            normParameterId: item?.normParameterId || null,
            particulars: item?.displayName,
            displayName: item?.displayName,
            UOM: item?.UOM,
            targetValue: targetVal,
            range: rangeVal,
            remarks: remarksVal,
            remark: remarksVal,
            aopYear: item?.aopYear || AOP_YEAR,
            plantId: item?.plantId || PLANT_ID,
            isEditable: !READ_ONLY,
            inEdit: false,
          }
        })
        setRows(formattedRows)
        setOriginalRows(formattedRows)
      } else {
        setRows([])
        setOriginalRows([])
      }
    } catch (error) {
      if (requestId !== requestIdRef.current) return
      console.error('Error fetching Data Filter data:', error)
      setRows([])
      setOriginalRows([])
    } finally {
      if (requestId === requestIdRef.current) {
        setLoading(false)
      }
    }
  }, [PLANT_ID, AOP_YEAR, keycloak, READ_ONLY, apiService])

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchData()
    }
  }, [PLANT_ID, AOP_YEAR, refreshData, fetchData])

  const saveChanges = useCallback(async () => {
    const data = Object.values(modifiedCells)
    if (data.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No changes to save.',
        severity: 'info',
      })
      return
    }

    const validationError = validateRowDataWithRemarks(
      data,
      originalRows,
      ['targetValue', 'range'],
      'particulars',
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

    setLoading(true)
    try {
      const payload = (rows || []).map((row) => {
        const modifiedRow = modifiedCells[row.id] || row
        return {
          normParameterId: modifiedRow.normParameterId || null,
          displayName:
            modifiedRow.displayName ||
            modifiedRow.particulars ||
            'DAF Throughput',
          targetValue:
            modifiedRow.targetValue !== '' &&
            modifiedRow.targetValue !== null &&
            modifiedRow.targetValue !== undefined
              ? Number(modifiedRow.targetValue)
              : 0,
          range:
            modifiedRow.range !== '' &&
            modifiedRow.range !== null &&
            modifiedRow.range !== undefined
              ? Number(modifiedRow.range)
              : 0,
          remarks: modifiedRow.remarks || modifiedRow.remark || '',
          aopYear: modifiedRow.aopYear || AOP_YEAR,
          plantId: modifiedRow.plantId || PLANT_ID,
        }
      })

      const res = await apiService.saveDataFilterData(
        keycloak,
        AOP_YEAR,
        payload,
      )

      if (
        res?.code === 200 ||
        res?.message?.toLowerCase().includes('success') ||
        res?.status === 200
      ) {
        setSnackbarData({
          message: res?.message || 'Data Filter saved successfully!',
          severity: 'success',
        })
        setSnackbarOpen(true)
        setModifiedCells({})
        await fetchData()
        if (
          typeof onLoadNormCalculation === 'function' &&
          variantConfig?.triggerNormCalculationOnSave
        ) {
          onLoadNormCalculation()
        }
      } else {
        setSnackbarData({
          message: res?.message || 'Failed to save Data Filter.',
          severity: 'error',
        })
        setSnackbarOpen(true)
      }
    } catch (error) {
      console.error('Error saving Data Filter:', error)
      setSnackbarData({
        message: error.message || 'Failed to save changes',
        severity: 'error',
      })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [
    modifiedCells,
    rows,
    originalRows,
    keycloak,
    AOP_YEAR,
    PLANT_ID,
    apiService,
    fetchData,
    onLoadNormCalculation,
    variantConfig,
  ])

  const handleRemarkCellClick = useCallback(
    (param1, param2) => {
      if (READ_ONLY) return
      if (typeof param1 === 'object' && param1 !== null) {
        setCurrentRowId(param1?.id)
        setCurrentRemark(param1?.remarks || param1?.remark || '')
      } else {
        setCurrentRowId(param1)
        setCurrentRemark(param2 || '')
      }
      setRemarkDialogOpen(true)
    },
    [READ_ONLY],
  )

  const handleExport = useCallback(() => {
    const excelName = generateExcelName(dataGridStore, 'Data_Filter')
    downloadBase64Excel(rows, columns, excelName)
  }, [dataGridStore, rows, columns])

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        setRows={setRows}
        columns={columns}
        rows={rows}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        apiRef={apiRef}
        open1={open1}
        setOpen1={setOpen1}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        handleRemarkCellClick={handleRemarkCellClick}
        fetchData={fetchData}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        permissions={permissions}
        saveChanges={saveChanges}
        title={permissions.showTitle ? permissions.titleName : ''}
        handleExport={handleExport}
        groupBy={variantConfig?.groupBy ?? null}
        paginationConfig={{
          threshold: 100,
          buttonCount: 5,
          pageSizes: [10, 20, 50, 100],
          defaultPageSize: 100,
        }}
      />
    </Box>
  )
}

export default DataFiltersTab

