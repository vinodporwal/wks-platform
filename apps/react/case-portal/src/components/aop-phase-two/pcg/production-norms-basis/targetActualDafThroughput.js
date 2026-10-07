import { useState, useEffect, useCallback, useMemo, useRef } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from '../../common/utilities/excelNameUtil'
import { downloadBase64Excel } from '../../common/utilities/downloadBase64Excel'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'

const TargetActualDafThroughput = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, screenTitle } = dataGridStore
  const PLANT_ID = plantObject?.plantId
  const AOP_YEAR = year

  const apiRef = useRef(null)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [open1, setOpen1] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'success',
  })
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // Column definitions
  const columns = useMemo(
    () => [
      {
        field: 'particulars',
        header: 'Particulars',
        type: 'text',
        isEditable: false,
        flex: 1.5,
        minWidth: 200,
      },
      {
        field: 'uom',
        header: 'UOM',
        type: 'text',
        isEditable: false,
        flex: 1,
        minWidth: 120,
      },
      {
        field: 'targetValue',
        header: 'Target Value',
        type: 'number',
        isEditable: true,
        flex: 1.2,
        minWidth: 140,
      },
      {
        field: 'range',
        header: 'Range',
        type: 'number',
        isEditable: true,
        flex: 1.2,
        minWidth: 140,
      },
      {
        field: 'remark',
        header: 'Remarks',
        type: 'text',
        isEditable: true,
        flex: 1.5,
        minWidth: 160,
      },
    ],
    [],
  )

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
      ExcelName: `Target_Actual_DAF_Throughput_${AOP_YEAR || ''}`,
      showImport: false,
      showTitleNameBusiness: true,
      showTitle: true,
      titleName: 'Target Actual DAF Throughput',
      showCalculate: false,
      calculateDisabled: true,
    }),
    [AOP_YEAR],
  )

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    setModifiedCells({})
    try {
      // Mock / initial structure - can be replaced with API service call
      const defaultData = [
        {
          id: 0,
          particulars: 'DAF Throughput',
          uom: 'TPH',
          targetValue: 0,
          range: 0,
          remark: '',
          inEdit: false,
        },
      ]
      setRows(defaultData)
      setOriginalRows(defaultData)
    } catch (error) {
      console.error('Error fetching Target Actual DAF Throughput data:', error)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, AOP_YEAR])

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchData()
    }
  }, [PLANT_ID, AOP_YEAR, fetchData])

  const saveChanges = useCallback(async () => {
    const data = Object.values(modifiedCells)
    const valid = validateRowDataWithRemarks(
      data,
      rows,
      setSnackbarData,
      setSnackbarOpen,
    )
    if (!valid) return

    setLoading(true)
    try {
      setSnackbarData({
        message: 'Saved successfully',
        severity: 'success',
      })
      setSnackbarOpen(true)
      setModifiedCells({})
      fetchData()
    } catch (error) {
      setSnackbarData({
        message: error.message || 'Failed to save changes',
        severity: 'error',
      })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, rows, fetchData])

  const handleRemarkCellClick = useCallback((id, remark) => {
    setCurrentRowId(id)
    setCurrentRemark(remark || '')
    setRemarkDialogOpen(true)
  }, [])

  const handleExport = useCallback(() => {
    const excelName = generateExcelName(
      screenTitle || 'Target Actual DAF Throughput',
      AOP_YEAR,
      plantObject?.plantCode,
    )
    downloadBase64Excel(rows, columns, excelName)
  }, [screenTitle, AOP_YEAR, plantObject, rows, columns])

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

export default TargetActualDafThroughput
