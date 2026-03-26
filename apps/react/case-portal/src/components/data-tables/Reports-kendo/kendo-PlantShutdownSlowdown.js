import { Box } from '@mui/material/index'
import Notification from 'components/Utilities/Notification'
import React, { useEffect, useState } from 'react'
import { useSelector } from 'react-redux'
import { Backdrop, CircularProgress } from '@mui/material/index'

import KendoDataTablesReports from 'components/kendo-data-tables/index-reports'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import { MockPlantShutdownSlowdownAPI } from './MockPlantShutdownSlowdownAPI'

const PlantShutdownSlowdown = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { oldYear, plantObject, year, isReleased } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const IS_RELEASED = isReleased
  const IS_OLD_YEAR = oldYear?.oldYear
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const [rows, setRows] = useState([])
  const [columns, setColumns] = useState([])
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

  const fetchData = async () => {
    try {
      setLoading(true)
      const { columns: cols, data } =
        await MockPlantShutdownSlowdownAPI.getReport({
          valueFormat: valueFormatter,
        })
      setColumns(cols)
      setRows(data)
    } catch (err) {
      console.error('Error fetching plant shutdown slowdown data:', err)
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
    fetchData()
  }, [AOP_YEAR, PLANT_ID])

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.Remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveChanges = async () => {
    setSnackbarData({
      message: 'Data Saved Successfully (Mock)!',
      severity: 'success',
    })
    setSnackbarOpen(true)
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
        title='Norms for Duration of Plant shutdown & Slowdown activities (T-19D)'
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        columns={columns}
        permissions={{
          textAlignment: 'center',
          remarksEditable: false,
          showCalculate: false,
          saveBtn: false,
          showWorkFlowBtns: false,
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

export default PlantShutdownSlowdown
