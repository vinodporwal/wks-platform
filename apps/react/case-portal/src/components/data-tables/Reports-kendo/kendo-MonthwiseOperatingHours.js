import { Box } from '@mui/material/index'
import Notification from 'components/Utilities/Notification'
import React, { useEffect, useState } from 'react'
import { useSelector } from 'react-redux'
import { Backdrop, CircularProgress } from '@mui/material/index'

import KendoDataTablesReports from 'components/kendo-data-tables/index-reports'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import { AOPWorkFlowService } from 'services/AOPWorkFlowService'

const MonthwiseOperatingHours = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { oldYear, plantObject, year, isReleased } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const IS_RELEASED = isReleased
  const IS_OLD_YEAR = oldYear?.oldYear
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [modifiedCells, setModifiedCells] = useState({})

  const valueFormatter = ValueFormatterProduction()

  const columns = [
    {
      field: 'Month',
      title: 'Month',
      widthT: 120,
      editable: false,
    },
    {
      field: 'TotalAvailableHrs',
      title: 'Total available Hrs',
      widthT: 150,
      editable: false,
      type: 'number',
      format: valueFormatter,
    },
    {
      field: 'PlannedTurnaroundHrs',
      title: 'Planned Turnaround Hrs',
      widthT: 180,
      editable: true,
      type: 'number',
      format: valueFormatter,
    },
    {
      field: 'PlannedShutdownOtherThanTurnaroundHrs',
      title: 'Planned shutdown other than Turnaround Hrs',
      widthT: 250,
      editable: true,
      type: 'number',
      format: valueFormatter,
    },
    {
      field: 'RoutineShutdownHrs',
      title: 'Routine shutdown Hrs',
      widthT: 180,
      editable: true,
      type: 'number',
      format: valueFormatter,
    },
    {
      field: 'SlowdownHrs',
      title: 'Slowdown Hrs',
      widthT: 150,
      editable: true,
      type: 'number',
      format: valueFormatter,
    },
    {
      field: 'NetOperatingHours',
      title: 'Net operating Hours',
      widthT: 180,
      editable: false,
      type: 'number',
      format: valueFormatter,
    },
    {
      field: 'Remark',
      title: 'Remarks',
      widthT: 200,
      editable: true,
    },
  ]

  const fetchData = async () => {
    try {
      setLoading(true)
      const res = await AOPWorkFlowService.getMonthwiseOperatingHours(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (res?.code === 200) {
        setRows(
          (res?.data || []).map((item, index) => ({
            ...item,
            id: item.id ?? index,
          })),
        )
      } else {
        setRows([])
        setSnackbarData({
          message: res?.message || 'Failed to fetch data',
          severity: 'error',
        })
        setSnackbarOpen(true)
      }
    } catch (err) {
      console.error('Error fetching operating hours data:', err)
      setSnackbarData({
        message: 'Failed to fetch data',
        severity: 'error',
      })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchData()
    }
  }, [AOP_YEAR, PLANT_ID])

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.Remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveChanges = async () => {
    try {
      setLoading(true)
      const dataToSave = Object.values(modifiedCells)
      if (dataToSave.length === 0) {
        setSnackbarData({
          message: 'No changes to save',
          severity: 'info',
        })
        setSnackbarOpen(true)
        return
      }

      const res = await AOPWorkFlowService.saveMonthwiseOperatingHours(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        dataToSave,
      )

      if (res?.code === 200) {
        setSnackbarData({
          message: 'Data Saved Successfully!',
          severity: 'success',
        })
        setSnackbarOpen(true)
        setModifiedCells({})
        fetchData()
      } else {
        setSnackbarData({
          message: res?.message || 'Failed to save data',
          severity: 'error',
        })
        setSnackbarOpen(true)
      }
    } catch (err) {
      console.error('Error saving operating hours data:', err)
      setSnackbarData({
        message: 'Failed to save data',
        severity: 'error',
      })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box>
      <Backdrop
        sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
        open={loading}
      >
        <CircularProgress color='inherit' />
      </Backdrop>

      <KendoDataTablesReports
        rows={rows}
        setRows={setRows}
        title='Monthwise Operating Hours (T-20)'
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        columns={columns}
        permissions={{
          textAlignment: 'center',
          remarksEditable: true,
          showCalculate: false,
          saveBtn: !READ_ONLY,
          showWorkFlowBtns: true,
          showTitle: true,
        }}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
      />

      <Notification
        open={snackbarOpen}
        message={snackbarData.message}
        severity={snackbarData.severity}
        onClose={() => setSnackbarOpen(false)}
      />
    </Box>
  )
}

export default MonthwiseOperatingHours
