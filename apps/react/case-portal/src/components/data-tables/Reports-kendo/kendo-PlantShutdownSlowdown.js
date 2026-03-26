import { Box } from '@mui/material/index'
import Notification from 'components/Utilities/Notification'
import React, { useEffect, useState } from 'react'
import { useSelector } from 'react-redux'
import { Backdrop, CircularProgress } from '@mui/material/index'

import KendoDataTablesReports from 'components/kendo-data-tables/index-reports'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import { ReportDataService } from 'services/ReportDataService'

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
      field: 'sno',
      title: 'Sl no',
      widthT: 58,
      editable: false,
      align: 'right',
      type: 'number',
      format: '{0:0}',
    },
    {
      field: 'criticalActivity',
      title: 'Critical Routine Activity',
      widthT: 200,
      editable: false,
    },
    {
      title: 'Best achieved at site in the last 4 years',
      children: [
        {
          field: 'bestAchievedSiteFreq',
          title: 'Frequency',
          widthT: 150,
          editable: false,
          type: 'number',
          format: valueFormatter,
        },
        {
          field: 'bestAchievedSiteDur',
          title: 'Duration',
          widthT: 150,
          editable: false,
          type: 'number',
          format: valueFormatter,
        },
      ],
    },
    {
      title: 'Best achieved in the group',
      children: [
        {
          field: 'bestAchievedGroupFreq',
          title: 'Frequency',
          widthT: 150,
          editable: false,
          type: 'number',
          format: valueFormatter,
        },
        {
          field: 'bestAchievedGroupDur',
          title: 'Duration',
          widthT: 150,
          editable: false,
          type: 'number',
          format: valueFormatter,
        },
      ],
    },
    {
      title: 'Actual 2024-25',
      children: [
        {
          field: 'actualPrevYearFreq',
          title: 'Frequency',
          widthT: 150,
          editable: false,
          type: 'number',
          format: valueFormatter,
        },
        {
          field: 'actualPrevYearDur',
          title: 'Duration',
          widthT: 150,
          editable: false,
          type: 'number',
          format: valueFormatter,
        },
      ],
    },
    {
      title: 'Budget 2025-26',
      children: [
        {
          field: 'budgetNextYearFreq',
          title: 'Frequency',
          widthT: 150,
          editable: false,
          type: 'number',
          format: valueFormatter,
        },
        {
          field: 'budgetNextYearDur',
          title: 'Duration',
          widthT: 150,
          editable: false,
          type: 'number',
          format: valueFormatter,
        },
      ],
    },
    {
      field: 'clubbedActivities',
      title: 'Activities that can be clubbed with the critical activity',
      widthT: 150,
      editable: false,
    },
    {
      field: 'explanationNotBest',
      title:
        'Explanation for not proposing the best achieved frequency / duration',
      widthT: 150,
      editable: false,
    },
    {
      field: 'throughputReduction',
      title: 'Throughput reduction during the period',
      widthT: 150,
      type: 'number',
      editable: false,
    },
    {
      field: 'lossRecoverable',
      title: 'Is the production Loss recoverable',
      widthT: 150,
      editable: false,
    },
  ]
    
  const fetchData = async () => {
    try {
      setLoading(true)
      const res = await ReportDataService.getPlantShutdownSlowdownNormsDuration(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (res?.code === 200) {
        const { data } = res?.data || {}
        setRows(data || [])
      } else {
        setRows([])
        setSnackbarData({
          message: res?.message || 'Failed to fetch data',
          severity: 'error',
        })
        setSnackbarOpen(true)
      }
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
