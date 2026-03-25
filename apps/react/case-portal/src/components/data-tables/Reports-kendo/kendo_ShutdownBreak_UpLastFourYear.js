import { Box } from '@mui/material'
import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import { useGridApiRef } from '@mui/x-data-grid'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import React, { useEffect, useState } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import { ReportDataService } from 'services/ReportDataService'
import KendoDataTables from 'components/kendo-data-tables/index'

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
  const [loading, setLoading] = useState(false)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const getColumns = [
    {
      field: 'lastFourYears',
      title: 'Year',
      editable: false,
    },
    {
      field: 'totalAvailableHours',
      title: 'Total Available Hours',
      editable: false,
      type: 'number',
    },
    {
      field: 'budgetedShutdownHours',
      title: 'Budgeted Shutdown Hours',
      editable: false,
      type: 'number',
    },
    {
      field: 'actualNoOfTurnaroundHrs',
      title: 'Actual No. of Turnaround Hours',
      editable: false,
      type: 'number',
    },
    {
      field: 'actualNoOfPlannedSD',
      title: 'Actual No of Planned Shutdowns other than TA',
      editable: false,
      type: 'number',
    },
    {
      field: 'actualNoOfRoutineSDHrs',
      title: 'Actual No of Routine SD Hrs',
      editable: false,
      type: 'number',
    },
    {
      field: 'totalActualPlannedSDHrs',
      title: 'Total (Actual) Planned Shutdown Hrs',
      editable: false,
      type: 'number',
    },
    {
      field: 'process',
      title: 'Process',
      editable: false,
      type: 'number',
    },
    {
      field: 'mech',
      title: 'Mech',
      editable: false,
      type: 'number',
    },
    {
      field: 'inst',
      title: 'Inst',
      editable: false,
      type: 'number',
    },
    {
      field: 'elect',
      title: 'Elect',
      editable: false,
      type: 'number',
    },
    {
      field: 'utility',
      title: 'Utility',
      editable: false,
      type: 'number',
    },
    {
      field: 'upStreamDownStream',
      title: 'Up Stream / Down Stream',
      editable: false,
      type: 'number',
    },
    {
      field: 'extFeedStock',
      title: 'Ext Feed Stock',
      editable: false,
      type: 'number',
    },
    {
      field: 'business',
      title: 'Business',
      editable: false,
      type: 'number',
    },
    {
      field: 'others',
      title: 'Others',
      editable: false,
      type: 'number',
    },
    {
      field: 'totalUnplannedSD',
      title: 'TOTAL Un-planned SD',
      editable: false,
      type: 'number',
    },
    {
      field: 'unplannedSlowdownHours',
      title: 'Unplanned Slowdown Hours',
      editable: false,
      type: 'number',
    },
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
          isEditable: false,
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
    setCurrentRemark(dataItem.remark || '')
    setCurrentRowId(dataItem.id)
    setRemarkDialogOpen(true)
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
      // showStepper:false,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: permissions?.showAction ?? false,

      saveWithRemark: permissions?.saveWithRemark ?? true,
      saveBtn: false,
      allAction: true,
      showTitleNameBusiness: true,
      titleName: 'shutdown details',
      ExcelName: `${EXCEL_NAME_GRID2}`,

      downloadExcelBtn: false,
      uploadExcelBtn: false,
      downloadExcelBtnFromUI: true,
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
      />
    </div>
  )
}

export default ShutdownSummaryReport
