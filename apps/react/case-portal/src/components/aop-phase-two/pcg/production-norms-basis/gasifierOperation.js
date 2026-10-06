import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'

// G-Operation dropdown options
export const G_OPERATION_OPTIONS = [
  { label: '3G Operation', value: '3G Operation' },
  { label: '4G Operation', value: '4G Operation' },
  { label: '5G Operation', value: '5G Operation' },
  { label: '6G Operation', value: '6G Operation' },
]

// G-Configuration mapping based on (M3+M4+M5) combinations all in one line separated by /
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

// Helper to get configuration dropdown options dynamically for a given G-Operation
export const getGConfigurationOptions = (gOperation) => {
  const configString = getGConfigurationString(gOperation)
  if (!configString) return []
  return [{ label: configString, value: configString }]
}

// Initial dummy data
const INITIAL_DUMMY_DATA = [
  {
    id: 1,
    particulars: 'Gasifier Operation',
    gOperation: '4G Operation',
    gConfiguration: '(1+1+1)/(1+2+1)/(1+1+2)/(2+1+1)/(0+2+2)/(2+0+2)/(2+2+0)',
    isEditable: true,
  },
]

const GasifierOperation = () => {
  const [rows, setRows] = useState(INITIAL_DUMMY_DATA)
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
        editable: true,
        options: G_OPERATION_OPTIONS,
        displayMode: 'label',
      },
      {
        field: 'gConfiguration',
        title: 'G-Configuration',
        widthT: 480,
        minWidth: 380,
        type: 'select',
        editable: true,
        dynamicOptions: true,
        getOptions: (dataItem) =>
          getGConfigurationOptions(dataItem?.gOperation),
        displayMode: 'label',
      },
    ],
    [],
  )

  // Permissions configuration for the table toolbar and actions
  const permissions = useMemo(
    () => ({
      showAction: true,
      addButton: false,
      addBtnName: 'Add Item',
      deleteButton: false,
      editButton: false,
      saveBtn: true,
      allAction: true,
      showExport: false,
      ExcelName: `Gasifier_Operation_${AOP_YEAR || ''}`,
      showImport: false,
      showTitleNameBusiness: true,
      showTitle: true,
      titleName: 'Configuartion',
      showCalculate: false,
      calculateDisabled: true,
    }),
    [AOP_YEAR],
  )

  // Custom item change handler to sync dependent dropdown
  const handleCustomItemChange = useCallback(
    (e, setRowsTable, setModifiedCellsTable) => {
      const { dataItem, field, value } = e
      if (field === 'gOperation') {
        // Automatically populate the corresponding combined configuration string in one line
        const nextConfig = getGConfigurationString(value)

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
    [],
  )

  // Handler to add a new row
  const customAddRow = useCallback(() => {
    const newId = `row_${Date.now()}`
    const defaultOp = '3G Operation'
    const newRow = {
      id: newId,
      particulars: `Gasifier Operation ${rows.length + 1}`,
      gOperation: defaultOp,
      gConfiguration: getGConfigurationString(defaultOp),
      isEditable: true,
      inEdit: true,
    }
    setRows((prev) => [...prev, newRow])
    setModifiedCells((prev) => ({
      ...prev,
      [newId]: newRow,
    }))
  }, [rows.length])

  // Handler for save button
  const saveChanges = useCallback(async () => {
    setLoading(true)
    try {
      if (Object.keys(modifiedCells).length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No changes to save.',
          severity: 'info',
        })
        return
      }

      const rowsToSave = Object.values(modifiedCells).filter(
        (row) => row.inEdit,
      )
      if (rowsToSave.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No changes to save.',
          severity: 'info',
        })
        return
      }

      // Simulate saving changes
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Saved Successfully!',
        severity: 'success',
      })
      setModifiedCells({})
    } catch (error) {
      console.error('Error saving Gasifier Operation data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Data save failed!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells])

  const fetchData = useCallback(async () => {
    setRows(INITIAL_DUMMY_DATA)
    setModifiedCells({})
  }, [])

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
          customAddRow={customAddRow}
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
    </Box>
  )
}

export default GasifierOperation
