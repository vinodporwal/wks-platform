import { Box } from '@mui/material'
import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import { useGridApiRef } from '@mui/x-data-grid'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import React, { useEffect, useState, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import { ReportDataService } from 'services/ReportDataService'
import KendoDataTables from 'components/kendo-data-tables/index'
import { validateFields } from 'utils/validationUtils'

const ShutdownSummaryReport = ({ permissions }) => {
  const [modifiedCells, setModifiedCells] = React.useState({})
  const keycloak = useSession()

  const [open1, setOpen1] = useState(false)
  const [deleteId, setDeleteId] = useState(null)
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    yearChanged,
    oldYear,
    plantID,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
  } = dataGridStore
  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id

  const AOP_YEAR = year?.selectedYear
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const lowerSiteName = siteObject?.name.toLowerCase()

  const SCREEN_NAME = screenTitle?.title

  const PLANT_NAME = plantObject?.name?.toUpperCase()
  const SITE_NAME = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME = verticalObject?.name?.toUpperCase()

  const EXCEL_NAME = `${VERTICAL_NAME}_${SITE_NAME}_${PLANT_NAME}_Business_Demand_${AOP_YEAR}`
  const EXCEL_NAME_GRID2 = `${VERTICAL_NAME}_${SITE_NAME}_${PLANT_NAME}_${SCREEN_NAME}_${AOP_YEAR}`

  const apiRef = useGridApiRef()
  const [rows, setRows] = useState()
  const headerMap = generateHeaderNames(AOP_YEAR)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const showSnackbar = useCallback((message, severity = 'info') => {
    setSnackbarData({ message, severity })
    setSnackbarOpen(true)
  }, [])
  const [loading, setLoading] = useState(false)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const getColumns = [
    {
      field: 'year',
      title: 'Year',
      editable: true,
      widthT: 100,
    },
    {
      field: 'totalAvailableHours',
      title: 'Total Available Hours',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    {
      field: 'budgetedShutdownHours',
      title: 'Budgeted Shutdown Hours',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    {
      field: 'actualNoOfTurnaroundHrs',
      title: 'Actual No. of Turnaround Hours',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    {
      field: 'actualNoOfPlannedSD',
      title: 'Actual No of Planned Shutdowns other than TA',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    {
      field: 'actualNoOfRoutineSDHrs',
      title: 'Actual No of Routine SD Hrs',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    {
      field: 'totalActualPlannedSDHrs',
      title: 'Total (Actual) Planned Shutdown Hrs',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    {
      field: 'process',
      title: 'Process',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    {
      field: 'mech',
      title: 'Mech',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    {
      field: 'inst',
      title: 'Inst',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    {
      field: 'elect',
      title: 'Elect',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    {
      field: 'utility',
      title: 'Utility',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    {
      field: 'upStreamDownStream',
      title: 'Up Stream / Down Stream',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    {
      field: 'extFeedStock',
      title: 'Ext Feed Stock',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    {
      field: 'business',
      title: 'Business',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    {
      field: 'others',
      title: 'Others',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    {
      field: 'totalUnplannedSD',
      title: 'TOTAL Un-planned SD',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    {
      field: 'unplannedSlowdownHours',
      title: 'Unplanned Slowdown Hours',
      editable: true,
      type: 'number',
      widthT: 100,
    },
    // {
    //   field: 'remarks',
    //   title: 'Remarks',
    //   editable: true,
    //   widthT: 200,
    // },
  ]

  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    console.log('Fetching data with:', { PLANT_ID, AOP_YEAR })

    setLoading(true)
    try {
      const res = await ReportDataService.getShutdownSummaryLastFourYearData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      const formattedData = res?.data?.shutdownSummaryLastFourYearList?.map(
        (item, index) => ({
          ...item,
          idFromApi: item?.id,
          id: index,
          aopYears: item.lastFourYears,
          totalAvailableHours: item.totalAvailableHours,
          budgetedShutdownHours: item.budgetedShutdownHours,
          actualNoOfTurnaroundHrs: item.actualNoOfTurnaroundHrs,
          actualNoOfPlannedSD: item.actualNoOfPlannedSD,
          actualNoOfRoutineSDHrs: item.actualNoOfRoutineSDHrs,
          totalActualPlannedSDHrs: item.totalActualPlannedSDHrs,
          process: item.process,
          mech: item.mech,
          inst: item.inst,
          elect: item.elect,
          utility: item.utility,
          upStreamDownStream: item.upStreamDownStream,
          extFeedStock: item.extFeedStock,
          business: item.business,
          others: item.others,
          totalUnplannedSD: item.totalUnplannedSD,
          unplannedSlowdownHours: item.unplannedSlowdownHours,
          year: item.lastFourYears,
          remarks: item.remarks,
          originalRemark: item.remarks,
        }),
      )

      setRows(formattedData)

      setLoading(false)
    } catch (error) {
      console.error('Error fetching data:', error)
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak])

  const handleRemarkCellClick = (dataItem) => {
    // if (!dataItem?.isEditable) return
    if (READ_ONLY) return
    setCurrentRemark(dataItem.remarks || '')
    setCurrentRowId(dataItem.id)
    setRemarkDialogOpen(true)
  }
  const saveChanges = useCallback(async () => {
    try {
      const data = Object.values(modifiedCells)
      if (data.length === 0) {
        showSnackbar('No Records to Save!', 'info')
        return
      }

      const validationMessage = validateFields(data, ['year'])
      if (validationMessage) {
        showSnackbar(validationMessage, 'error')
        return
      }

      const payload = data.map((row) => ({
        id: row?.idFromApi || null,
        lastFourYears: row.year, // or row.lastFourYears if you use that key in your UI
        totalAvailableHours: row.totalAvailableHours || null,
        budgetedShutdownHours: row.budgetedShutdownHours || null,
        actualNoOfTurnaroundHrs: row.actualNoOfTurnaroundHrs || null,
        actualNoOfPlannedSD: row.actualNoOfPlannedSD || null,
        actualNoOfRoutineSDHrs: row.actualNoOfRoutineSDHrs || null,
        totalActualPlannedSDHrs: row.totalActualPlannedSDHrs || null,
        process: row.process || null,
        mech: row.mech || null,
        inst: row.inst || null,
        elect: row.elect || null,
        utility: row.utility || null,
        upStreamDownStream: row.upStreamDownStream || null,
        extFeedStock: row.extFeedStock || null,
        business: row.business || null,
        others: row.others || null,
        totalUnplannedSD: row.totalUnplannedSD || null,
        unplannedSlowdownHours: row.unplannedSlowdownHours || null,
        remarks: row.remarks || '',
      }))

      setLoading(true)
      const res = await ReportDataService.saveShutdownSummaryLastFourYearData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        payload,
      )

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
  }, [modifiedCells, keycloak, PLANT_ID, AOP_YEAR, fetchData, showSnackbar])

  const deleteRowData = async (paramsForDelete) => {
    try {
      const { idFromApi, id } = paramsForDelete
      const deleteId = id

      if (!idFromApi) {
        setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
      }

      if (idFromApi) {
        await ReportDataService.deleteShutdownLastFourYears(idFromApi, keycloak)
        setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Record Deleted successfully!',
          severity: 'success',
        })
        fetchData()
      }
    } catch (error) {
      console.error('Error deleting Record!', error)
    }
  }

  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: true,
      saveBtn: true,
      allAction: true,
      showTitleNameBusiness: true,
      titleName: 'Shutdown Breakup For Last 4 Years (19-C)',
      ExcelName: `${EXCEL_NAME_GRID2}`,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      downloadExcelBtnFromUI: false,
      addButton: false,
      deleteButton: false,
      saveWithRemark: true,
    },
    isOldYear,
  )

  return (
    <div>
      <Backdrop
        sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
        open={!!loading}
      >
        <CircularProgress color='inherit' />
      </Backdrop>

      <KendoDataTables
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        setRows={setRows}
        columns={getColumns}
        rows={rows || []}
        title='Business Demand'
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        apiRef={apiRef}
        deleteId={deleteId}
        setDeleteId={setDeleteId}
        setOpen1={setOpen1}
        open1={open1}
        fetchData={fetchData}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        handleRemarkCellClick={handleRemarkCellClick}
        permissions={adjustedPermissions}
        saveChanges={saveChanges}
        deleteRowData={deleteRowData}
      />
    </div>
  )
}

export default ShutdownSummaryReport
