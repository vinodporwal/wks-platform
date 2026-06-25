import { Box } from '@mui/material/index'
import Notification from 'components/Utilities/Notification'
import React, { useEffect, useState } from 'react'
import { useSelector } from 'react-redux'
import { Backdrop, CircularProgress } from '@mui/material/index'

import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import { ReportDataService } from 'services/ReportDataService'
import KendoDataTables from 'components/kendo-data-tables/index'
import { validateFields } from 'utils/validationUtils'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

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
      field: 'id',
      title: 'ID',
      widthT: 50,
      editable: false,
      hidden: true,
    },
    {
      field: '_month',
      title: 'Month',
      editable: false,
      isDisabled: true,
    },
    {
      field: 'totalAvailableHrs',
      title: 'Total available Hrs',
      editable: true,
      type: 'number',
      format: valueFormatter,
      isDisabled: false,
    },
    {
      field: 'plannedTurnaroundHrs',
      title: 'Planned Turnaround Hrs',
      editable: true,
      type: 'number',
      format: valueFormatter,
      isDisabled: false,
    },
    {
      field: 'plannedShutdownOtherThanTurnaroundHrs',
      title: 'Planned shutdown other than Turnaround Hrs',
      editable: true,
      type: 'number',
      format: valueFormatter,
      isDisabled: false,
    },
    {
      field: 'routineShutdownHrs',
      title: 'Routine shutdown Hrs',
      editable: true,
      type: 'number',
      format: valueFormatter,
      isDisabled: false,
    },
    {
      field: 'slowdownHrs',
      title: 'Slowdown Hrs',
      editable: true,
      type: 'number',
      format: valueFormatter,
      isDisabled: false,
    },
    {
      field: 'netOperatingHours',
      title: 'Net operating Hours',
      editable: true,
      type: 'number',
      format: valueFormatter,
      isDisabled: false,
    },
    // {
    //   field: 'remarks',
    //   title: 'Remarks',
    //   widthT: 200,
    //   editable: true,
    // },
  ]

  const [groupBy, setGroupBy] = useState(null)

  const fetchData = async () => {
    try {
      setLoading(true)
      const res = await ReportDataService.getMonthwiseOperatingHours(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (res?.code === 200) {
        const hasGroupBy = (res?.data.monthwiseOperatingHoursList || []).some(
          (item) => Object.prototype.hasOwnProperty.call(item, 'groupBy'),
        )
        if (hasGroupBy) {
          setGroupBy('groupBy')
        } else {
          setGroupBy(null)
        }
        let formattedData
        formattedData = (res?.data.monthwiseOperatingHoursList || []).map(
          (item, index) => ({
            ...item,
            id: item.id,
            originalRemark: item.remarks,
            _month: item.month,
            isEditable: false,
          }),
        )
        setRows(formattedData)
      } else {
        setRows([])
        setGroupBy(null)
      }
    } catch (err) {
      console.error('Error fetching operating hours data:', err)
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
    setCurrentRemark(row.remarks || '')
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
      const requiredFields = ['remarks']

      const validationMessage = validateFields(dataToSave, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationMessage,
          severity: 'error',
        })
        setLoading(false)
        return
      }

      const res = await ReportDataService.saveMonthwiseOperatingHours(
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
      <LoaderBackdrop open={!!loading} />

      <KendoDataTables
        rows={rows}
        setRows={setRows}
        title='Monthwise Operating Hours (T-20)'
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        columns={columns}
        groupBy={groupBy}
        permissions={{
          allAction: true,
          textAlignment: 'center',
          remarksEditable: true,
          showCalculate: false,
          saveBtn: false,
          showWorkFlowBtns: true,
          showTitle: true,
          showTitleNameBusiness: true,
          titleName: 'Monthwise Operating Hours (T-20)',
        }}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
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
