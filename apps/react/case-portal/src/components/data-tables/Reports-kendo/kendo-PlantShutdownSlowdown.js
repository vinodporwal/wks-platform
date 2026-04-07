import { Box } from '@mui/material/index'
import Notification from 'components/Utilities/Notification'
import React, { useEffect, useState, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { Backdrop, CircularProgress } from '@mui/material/index'

import KendoDataTablesReports from 'components/kendo-data-tables/index-reports'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import { ReportDataService } from 'services/ReportDataService'
import { validateFields } from 'utils/validationUtils'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

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
  const showSnackbar = useCallback((message, severity = 'info') => {
    setSnackbarData({ message, severity })
    setSnackbarOpen(true)
  }, [])

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [modifiedCells, setModifiedCells] = useState({})

  const valueFormatter = ValueFormatterProduction()
  function getPrevYearString(AOP_YEAR) {
    if (!AOP_YEAR) return ''
    const [start] = AOP_YEAR.split('-').map(Number)
    const prevStart = start - 1
    const prevEnd = (prevStart + 1) % 100
    return `${prevStart}-${prevEnd.toString().padStart(2, '0')}`
  }

  const columns = [
    {
      field: 'criticalActivity',
      title: 'Critical Routine Activity',
      widthT: 200,
      type: 'text',
      editable: true,
    },
    {
      title: 'Best achieved at site in the last 4 years',
      children: [
        {
          field: 'bestAchievedSiteFreq',
          title: 'Frequency',
          editable: true,
          type: 'numberNonGrey',
          format: valueFormatter,
        },
        {
          field: 'bestAchievedSiteDur',
          title: 'Duration',
          editable: true,
          type: 'numberNonGrey',
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
          editable: true,
          type: 'numberNonGrey',
          format: valueFormatter,
        },
        {
          field: 'bestAchievedGroupDur',
          title: 'Duration',
          editable: true,
          type: 'numberNonGrey',
          format: valueFormatter,
        },
      ],
    },
    {
      title: `Actual ${getPrevYearString(AOP_YEAR)}`,
      children: [
        {
          field: 'actualPrevYearFreq',
          title: 'Frequency',
          editable: true,
          type: 'numberNonGrey',
          format: valueFormatter,
        },
        {
          field: 'actualPrevYearDur',
          title: 'Duration',
          editable: true,
          type: 'numberNonGrey',
          format: valueFormatter,
        },
      ],
    },
    {
      title: `Budget ${AOP_YEAR}`,
      children: [
        {
          field: 'budgetNextYearFreq',
          title: 'Frequency',
          editable: true,
          type: 'numberNonGrey',
          format: valueFormatter,
        },
        {
          field: 'budgetNextYearDur',
          title: 'Duration',
          editable: true,
          type: 'numberNonGrey',
          format: valueFormatter,
        },
      ],
    },
    {
      field: 'clubbedActivities',
      title: 'Activities that can be clubbed with the critical activity',
      type: 'text',
      editable: true,
    },
    {
      field: 'explanationNotBest',
      title:
        'Explanation for not proposing the best achieved frequency / duration',
      type: 'text',
      editable: true,
    },
    {
      field: 'throughputReduction',
      title: 'Throughput reduction during the period',
      type: 'numberNonGrey',
      editable: true,
    },
    {
      field: 'lossRecoverable',
      title: 'Is the production Loss recoverable',
      type: 'text',
      editable: true,
    },
    // {
    //   field: 'remarks',
    //   title: 'Remarks',
    //   widthT: 200,
    //   editable: true,
    // },
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
        const responseData =
          res.data.plantShutdownSlowdownNormsDurationList || []

        const formattedData = responseData.map((item, index) => ({
          idFromApi: item.id,
          id: index,
          criticalActivity: item.criticalRoutineActivity,
          bestAchievedSiteFreq: item.bestAchievedLastYearFrequency,
          bestAchievedSiteDur: item.bestAchievedLastYearDuration,
          bestAchievedGroupFreq: item.bestAchievedGroupFrequency,
          bestAchievedGroupDur: item.bestAchievedGroupDuration,
          actualPrevYearFreq: item.actualFrequency,
          actualPrevYearDur: item.prevYearDuration,
          budgetNextYearFreq: item.budgetFrequency,
          budgetNextYearDur: item.currentYearDuration,
          clubbedActivities: item.activitiesClubbed,
          explanationNotBest: item.explanationNotProposing,
          throughputReduction: item.throughputReductionDuringPeriod,
          lossRecoverable: item.isProductionLossRecoverable,
          remarks: item.remarks,
          originalRemark: item.remarks,
        }))
        setRows(formattedData || responseData)
      } else {
        setRows([])
      }
    } catch (err) {
      console.error('Error fetching plant shutdown slowdown data:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [AOP_YEAR, PLANT_ID])

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveChanges = async () => {
    try {
      const data = Object.values(modifiedCells)
      if (data.length == 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        setLoading(false)
        return
      }
      const requiredFields = ['criticalActivity']

      const validationMessage = validateFields(data, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationMessage,
          severity: 'error',
        })
        setLoading(false)
        return
      }

      const rowsToUpdate = data.map((item) => ({
        id: item.idFromApi || null,
        criticalRoutineActivity: item.criticalActivity || null,
        bestAchievedLastYearFrequency: item.bestAchievedSiteFreq || null,
        bestAchievedLastYearDuration: item.bestAchievedSiteDur || null,
        bestAchievedGroupFrequency: item.bestAchievedGroupFreq || null,
        bestAchievedGroupDuration: item.bestAchievedGroupDur || null,
        actualFrequency: item.actualPrevYearFreq || null,
        prevYearDuration: item.actualPrevYearDur || null,
        budgetFrequency: item.budgetNextYearFreq || null,
        currentYearDuration: item.budgetNextYearDur || null,
        activitiesClubbed: item.clubbedActivities || null,
        explanationNotProposing: item.explanationNotBest || null,
        throughputReductionDuringPeriod: item.throughputReduction || null,
        isProductionLossRecoverable: item.lossRecoverable || null,
        remarks: item.remarks,
        updatedBy: keycloak?.userName || 'system',
      }))
      const res =
        await ReportDataService.savePlantShutdownSlowdownNormsDuration(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          rowsToUpdate,
        )

      // console.log(res)

      if (res?.code === 200) {
        showSnackbar('Data Saved Successfully!', 'success')
        setModifiedCells({})
        fetchData()
      } else {
        showSnackbar('Data Save Failed!', 'error')
      }
    } catch (err) {
      console.error('Error saving data:', err)
      showSnackbar(err.message || 'An error occurred', 'error')
    } finally {
      setLoading(false)
    }
  }

  const deleteRowData = async (paramsForDelete) => {
    try {
      const { idFromApi, id } = paramsForDelete
      const deleteId = id

      if (!idFromApi) {
        setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
      }

      if (idFromApi) {
        await ReportDataService.deletePlantShutdownSlowdownNormsDuration(
          idFromApi,
          keycloak,
        )
        setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Record Deleted successfully!',
          severity: 'success',
        })
        fetchPreviousYear()
      }
    } catch (error) {
      console.error('Error deleting Record!', error)
    }
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <KendoDataTablesReports
        rows={rows}
        setRows={setRows}
        title='Norms for Duration of Plant shutdown & Slowdown activities (T-19D)'
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        columns={columns}
        permissions={{
          allAction: true,
          textAlignment: 'center',
          remarksEditable: true,
          showCalculate: false,
          saveBtn: true,
          addButton: true,
          deleteButton: true,
          showWorkFlowBtns: true,
          showTitle: true,
          saveWithRemark: true,
          showFinalSubmit: false,
          editButton: true,
        }}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        deleteRowData={deleteRowData}
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
