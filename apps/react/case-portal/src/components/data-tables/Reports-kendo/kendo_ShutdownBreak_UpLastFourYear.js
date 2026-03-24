import React, { useEffect, useState } from 'react'
import { Box, Backdrop } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import KendoDataTablesReports from 'components/kendo-data-tables/index-reports'
import { useSession } from 'SessionStoreContext'
import { useSelector } from 'react-redux'
import { DataService } from 'services/DataService'
import {
  CircularProgress,
  Typography,
} from '../../../../node_modules/@mui/material/index'

// ─── Dummy Data ───────────────────────────────────────────────────

export default function ShutdownSummaryReport() {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { yearChanged, oldYear, plantObject, verticalObject, year } =
    dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear

  const [loading, setLoading] = useState(false) // ✅ init with dummy
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const keycloak = useSession()
  const getColumns = [
    {
      field: 'AopYears',
      title: 'Year',
      editable: false,
      widthT: 100,
    },
    {
      field: 'TotalAvailableHours',
      title: 'Total Available Hours',
      editable: false,
      widthT: 120,
      type: 'number',
    },
    {
      field: 'BudgetedShutdownHours',
      title: 'Budgeted Shutdown Hours',
      editable: false,
      widthT: 120,
      type: 'number',
    },
    {
      field: 'ActualNoOfTurnaroundHrs',
      title: 'Actual No. of Turnaround Hours',
      editable: false,
      widthT: 130,
      type: 'number',
    },
    {
      field: 'ActualNoOfPlannedSD',
      title: 'Actual No of Planned Shutdowns other than TA',
      editable: false,
      widthT: 150,
      type: 'number',
    },
    {
      field: 'ActualNoOfRoutineSDHrs',
      title: 'Actual No of Routine SD Hrs',
      editable: false,
      widthT: 130,
      type: 'number',
    },
    {
      field: 'TotalActualPlannedSDHrs',
      title: 'Total (Actual) Planned Shutdown Hrs',
      editable: false,
      widthT: 140,
      type: 'number',
    },
    {
      field: 'Process',
      title: 'Process',
      editable: false,
      widthT: 100,
      type: 'number',
    },
    {
      field: 'Mech',
      title: 'Mech',
      editable: false,
      widthT: 100,
      type: 'number',
    },
    {
      field: 'Inst',
      title: 'Inst',
      editable: false,
      widthT: 100,
      type: 'number',
    },
    {
      field: 'Elect',
      title: 'Elect',
      editable: false,
      widthT: 100,
      type: 'number',
    },
    {
      field: 'Utility',
      title: 'Utility',
      editable: false,
      widthT: 100,
      type: 'number',
    },
    {
      field: 'UpStreamDownStream',
      title: 'Up Stream / Down Stream',
      editable: false,
      widthT: 130,
      type: 'number',
    },
    {
      field: 'ExtFeedStock',
      title: 'Ext Feed Stock',
      editable: false,
      widthT: 110,
      type: 'number',
    },
    {
      field: 'Business',
      title: 'Business',
      editable: false,
      widthT: 100,
      type: 'number',
    },
    {
      field: 'Others',
      title: 'Others',
      editable: false,
      widthT: 100,
      type: 'number',
    },
    {
      field: 'TotalUnplannedSD',
      title: 'TOTAL Un-planned SD',
      editable: false,
      widthT: 120,
      type: 'number',
    },
    {
      field: 'UnplannedSlowdownHours',
      title: 'Unplanned Slowdown Hours',
      editable: false,
      widthT: 130,
      type: 'number',
    },
  ]
  const DUMMY_DATA = [
    {
      id: 1,
      AopYears: '2024-25',
      TotalAvailableHours: 8760,
      BudgetedShutdownHours: 544,
      ActualNoOfTurnaroundHrs: 528,
      ActualNoOfPlannedSD: 0,
      ActualNoOfRoutineSDHrs: 0,
      TotalActualPlannedSDHrs: 528,
      Process: 0,
      Mech: 312,
      Inst: 0,
      Elect: 0,
      Utility: 0,
      UpStreamDownStream: 67,
      ExtFeedStock: 0,
      Business: 0,
      Others: 0,
      TotalUnplannedSD: 379,
      UnplannedSlowdownHours: 0,
    },
    {
      id: 2,
      AopYears: '2023-24',
      TotalAvailableHours: 8760,
      BudgetedShutdownHours: 304,
      ActualNoOfTurnaroundHrs: 0,
      ActualNoOfPlannedSD: 312,
      ActualNoOfRoutineSDHrs: 0,
      TotalActualPlannedSDHrs: 312,
      Process: 0,
      Mech: 0,
      Inst: 15,
      Elect: 0,
      Utility: 0,
      UpStreamDownStream: 0,
      ExtFeedStock: 0,
      Business: 0,
      Others: 0,
      TotalUnplannedSD: 15,
      UnplannedSlowdownHours: 0,
    },
    {
      id: 3,
      AopYears: '2022-23',
      TotalAvailableHours: 8784,
      BudgetedShutdownHours: 48,
      ActualNoOfTurnaroundHrs: 0,
      ActualNoOfPlannedSD: 0,
      ActualNoOfRoutineSDHrs: 0,
      TotalActualPlannedSDHrs: 0,
      Process: 0,
      Mech: 0,
      Inst: 6,
      Elect: 0,
      Utility: 0,
      UpStreamDownStream: 0,
      ExtFeedStock: 0,
      Business: 0,
      Others: 0,
      TotalUnplannedSD: 6,
      UnplannedSlowdownHours: 77,
    },
    {
      id: 4,
      AopYears: '2021-22',
      TotalAvailableHours: 8760,
      BudgetedShutdownHours: 312,
      ActualNoOfTurnaroundHrs: 0,
      ActualNoOfPlannedSD: 312,
      ActualNoOfRoutineSDHrs: 0,
      TotalActualPlannedSDHrs: 312,
      Process: 144,
      Mech: 0,
      Inst: 3,
      Elect: 0,
      Utility: 375,
      UpStreamDownStream: 106,
      ExtFeedStock: 0,
      Business: 0,
      Others: 17,
      TotalUnplannedSD: 643,
      UnplannedSlowdownHours: 0,
    },
  ]

  // ─── Helper: map rows with id ─────────────────────────────────────
  const mapRows = (data) =>
    data.map((item, index) => ({
      ...item,
      id: index + 1,
    }))
  const [rows, setRows] = useState(mapRows(DUMMY_DATA))
  // ─── Fetch Data ───────────────────────────────────────────────────
  const loadData = async () => {
    if (!PLANT_ID || !AOP_YEAR) {
      setRows(mapRows(DUMMY_DATA)) // ✅ fallback dummy if no plant/year
      return
    }
    setLoading(true)
    try {
      const res = await DataService.getShutdownSummary(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      const data = res?.data?.length > 0 ? res.data : DUMMY_DATA
      //  setRows(mapRows(data))
      setRows(mapRows(DUMMY_DATA))
    } catch (error) {
      console.error('Error loading shutdown summary:', error)
      setRows(mapRows(DUMMY_DATA)) // ✅ fallback dummy on error
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [keycloak, AOP_YEAR, PLANT_ID])

  return (
    <Box sx={{ width: '100%' }}>
      <Backdrop
        sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
        open={!!loading}
      >
        <CircularProgress color='inherit' />
      </Backdrop>

      {/* ✅ Shutdown Summary Grid */}
      <KendoDataTablesReports
        columns={getColumns}
        rows={rows}
        title='Shutdown Break-up for last 4 Years'
        setRows={setRows}
        permissions={{
          textAlignment: 'center',
          showCalculate: false,
          showFinalSubmit: false,
          showTitle: true,
        }}
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
