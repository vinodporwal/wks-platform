import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import TargetActualDafThroughput from './targetActualDafThroughput'
import { ProductionNormsApiService } from 'components/aop-phase-two/services/pcg/productionNormsApiService'

// Fallback G-Operation dropdown options
export const G_OPERATION_OPTIONS = [
  { label: '3G Operation', value: '3G Operation' },
  { label: '4G Operation', value: '4G Operation' },
  { label: '5G Operation', value: '5G Operation' },
  { label: '6G Operation', value: '6G Operation' },
]

// Fallback G-Configuration mapping based on (M3+M4+M5) combinations all in one line separated by /
export const G_CONFIGURATION_MAPPING = {
  '6G Operation': '(2+2+2)',
  '5G Operation': '(2+2+1)/(1+2+2)/(2+1+2)',
  '4G Operation': '(1+1+1)/(1+2+1)/(1+1+2)/(2+1+1)/(0+2+2)/(2+0+2)/(2+2+0)',
  '3G Operation': '(1+1+1)/(2+1+0)/(1+2+0)/(0+1+2)/(2+0+1)/(1+0+2)/(0+2+1)',
}

// Helper to get configuration string dynamically for a given G-Operation
export const getGConfigurationString = (gOperation) => {
  if (!gOperation) return ''
  const trimmed = String(gOperation).trim()
  if (G_CONFIGURATION_MAPPING[trimmed]) {
    return G_CONFIGURATION_MAPPING[trimmed]
  }
  const match = trimmed.match(/^([3-6])G/i)
  if (match) {
    const key = `${match[1]}G Operation`
    return G_CONFIGURATION_MAPPING[key] || ''
  }
  return ''
}

const GasifierOperation = () => {
  const [rows, setRows] = useState([])
  const [dropdownList, setDropdownList] = useState([])
  const [loading, setLoading] = useState(false)
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [open1, setOpen1] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const apiRef = useGridApiRef()
  const keycloak = useSession()

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, oldYear, isReleased } = dataGridStore
  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, isReleased)

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  // Get config string for a selected G-Operation
  const getConfigForOperation = useCallback(
    (gOp) => {
      if (!gOp) return ''
      const found = dropdownList.find(
        (d) =>
          (d.name || d.displayName)?.trim().toLowerCase() ===
          String(gOp).trim().toLowerCase(),
      )
      if (found?.configuration) return found.configuration
      return getGConfigurationString(gOp)
    },
    [dropdownList],
  )

  // Dynamic G-Operation dropdown options from API with fallback
  const gOperationOptions = useMemo(() => {
    if (dropdownList.length > 0) {
      return dropdownList.map((item) => ({
        label: item.displayName || item.name,
        value: item.name || item.displayName,
      }))
    }
    return G_OPERATION_OPTIONS
  }, [dropdownList])

  // Column definitions for AdvanceKendoTable
  const columns = useMemo(
    () => [
      {
        field: 'particulars',
        title: 'Particulars',
        widthT: 250,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'gOperation',
        title: 'G-Operation',
        widthT: 200,
        minWidth: 180,
        type: 'select',
        editable: !READ_ONLY,
        options: gOperationOptions,
        displayMode: 'label',
      },
      {
        field: 'gConfiguration',
        title: 'G-Configuration',
        widthT: 480,
        minWidth: 380,
        type: 'text',
        editable: false,
      },
    ],
    [gOperationOptions, READ_ONLY],
  )

  // Permissions configuration for the table toolbar and actions
  const permissions = useMemo(
    () => ({
      showAction: true,
      addButton: false,
      addBtnName: 'Add Item',
      deleteButton: false,
      editButton: false,
      saveBtn: !READ_ONLY,
      allAction: true,
      showExport: false,
      ExcelName: `Gasifier_Operation_${AOP_YEAR || ''}`,
      showImport: false,
      showTitleNameBusiness: true,
      showTitle: true,
      titleName: 'Target Gasifier Filter',
      showCalculate: false,
      calculateDisabled: true,
    }),
    [AOP_YEAR, READ_ONLY],
  )

  // Custom item change handler to sync dependent dropdown
  const handleCustomItemChange = useCallback(
    (e, setRowsTable, setModifiedCellsTable) => {
      const { dataItem, field, value } = e
      if (field === 'gOperation') {
        const nextConfig = getConfigForOperation(value)

        setRowsTable((prev) =>
          prev.map((r) =>
            r.id === dataItem.id
              ? {
                  ...r,
                  gOperation: value,
                  gConfiguration: nextConfig,
                }
              : r,
          ),
        )

        setModifiedCellsTable((prev) => {
          const current = prev[dataItem.id] || { ...dataItem }
          return {
            ...prev,
            [dataItem.id]: {
              ...current,
              gOperation: value,
              gConfiguration: nextConfig,
              inEdit: true,
            },
          }
        })
      }
    },
    [getConfigForOperation],
  )

  // Fetch Dropdown and Saved Target Gasifier Filters
  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    setModifiedCells({})
    try {
      // 1. Fetch dropdown options
      let currentDropdowns = []
      try {
        const ddRes =
          await ProductionNormsApiService.getGasifierDropdownAopBasis(
            keycloak,
            PLANT_ID,
            AOP_YEAR,
          )
        currentDropdowns = Array.isArray(ddRes) ? ddRes : ddRes?.data || []
        if (Array.isArray(currentDropdowns) && currentDropdowns.length > 0) {
          setDropdownList(currentDropdowns)
        }
      } catch (ddErr) {
        console.error('Error fetching gasifier dropdown:', ddErr)
      }

      // 2. Fetch saved target gasifier filter record
      const response =
        await ProductionNormsApiService.getTargetGasifierFilters(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      const list = Array.isArray(response) ? response : response?.data || []

      if (Array.isArray(list) && list.length > 0) {
        const formattedRows = list.map((item, index) => {
          const opName = item?.gOperation || ''
          const matchingDd = (currentDropdowns.length > 0
            ? currentDropdowns
            : dropdownList
          ).find(
            (d) =>
              (d.name || d.displayName)?.trim().toLowerCase() ===
              opName.trim().toLowerCase(),
          )
          const configStr =
            matchingDd?.configuration || getGConfigurationString(opName)

          return {
            ...item,
            id: item?.id || index + 1,
            idFromApi: item?.id || null,
            particulars: 'Gasifier Operation',
            gOperation: opName,
            gConfiguration: configStr,
            plantId: item?.plantId || PLANT_ID,
            aopYear: item?.aopYear || AOP_YEAR,
            normParameterId: item?.normParameterId || null,
            isEditable: !READ_ONLY,
            inEdit: false,
          }
        })
        setRows(formattedRows)
      } else {
        const defaultOp = currentDropdowns[0]?.name || '4G Operation'
        const defaultConf =
          currentDropdowns[0]?.configuration ||
          getGConfigurationString(defaultOp)
        setRows([
          {
            id: 1,
            idFromApi: null,
            particulars: 'Gasifier Operation',
            gOperation: defaultOp,
            gConfiguration: defaultConf,
            plantId: PLANT_ID,
            aopYear: AOP_YEAR,
            normParameterId: null,
            isEditable: !READ_ONLY,
            inEdit: false,
          },
        ])
      }
    } catch (error) {
      console.error('Error fetching Target Gasifier Filters:', error)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak, dropdownList, READ_ONLY])

  useEffect(() => {
    fetchData()
  }, [PLANT_ID, AOP_YEAR])

  // Handler for save button
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

    const targetRow = data[0] || rows[0]
    if (!targetRow) return

    setLoading(true)
    try {
      const payload = {
        id: targetRow.idFromApi || null,
        gOperation: targetRow.gOperation || '',
        plantId: targetRow.plantId || PLANT_ID,
        aopYear: AOP_YEAR,
        normParameterId: targetRow.normParameterId || null,
      }

      const res = await ProductionNormsApiService.saveTargetGasifierFilters(
        keycloak,
        AOP_YEAR,
        payload,
      )

      if (
        res?.code === 200 ||
        res?.message?.toLowerCase().includes('success')
      ) {
        setSnackbarOpen(true)
        setSnackbarData({
          message:
            res?.message || 'Target Gasifier Filters Saved Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        await fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: res?.message || 'Failed to save Target Gasifier Filters.',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error saving Target Gasifier Filters:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Data save failed. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, rows, PLANT_ID, AOP_YEAR, keycloak, fetchData])

  const handleExport = useCallback(() => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel export completed successfully!',
      severity: 'success',
    })
  }, [])

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <Box>
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
          permissions={permissions}
          saveChanges={saveChanges}
          customItemChange={handleCustomItemChange}
          title={permissions.showTitle ? permissions.titleName : ''}
          handleExport={handleExport}
          paginationConfig={{
            threshold: 100,
            buttonCount: 5,
            pageSizes: [10, 20, 50, 100],
            defaultPageSize: 100,
          }}
        />
      </Box>

      {/* Target Actual DAF Throughput Grid */}
      <Box sx={{ mt: 3 }}>
        <TargetActualDafThroughput />
      </Box>
    </Box>
  )
}

export default GasifierOperation
