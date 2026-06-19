import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import KendoDataTablesReports from 'components/kendo-data-tables/index-reports'
import { Backdrop, Box, CircularProgress } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from './index'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { useSelector } from 'react-redux'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

export default function SafetyImprovementInitiative() {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { year } = dataGridStore
  const AOP_YEAR = year?.selectedYear
  const thisYear = AOP_YEAR

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [modifiedCells, setModifiedCells] = useState({})
  const [enableSaveAddBtn, setEnableSaveAddBtn] = useState(false)
  const { verticalChange, yearChanged, oldYear, plantID } = dataGridStore
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()

  const headerMap = generateHeaderNames(AOP_YEAR)

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const unsavedChangesRef = useRef({ unsavedRows: {}, rowsBeforeChange: {} })

  const oldYearLabel = useMemo(() => {
    if (!thisYear || !thisYear.includes('-')) return ''
    const [start, end] = thisYear.split('-').map(Number)
    return `${start - 1}-${(end - 1).toString().slice(-2)}`
  }, [thisYear])

  const columns = useMemo(
    () => [
      {
        field: 'serialNumber',
        title: 'S.No',
        widthT: 70,
        editable: false,
        minWidth: 70,
      },
      {
        field: 'kpi',
        title: 'KPI',
        editable: true,
        widthT: 300,
        minWidth: 100,
      },
      {
        field: 'uom',
        title: 'UOM',
        widthT: 80,
        editable: true,
        minWidth: 100,
      },
      {
        field: 'bestAchived',
        title: 'Best Achived',

        editable: true,
        minWidth: 100,
      },
      {
        field: 'fyAop',
        title: 'FY25 AOP',

        editable: true,
        minWidth: 100,
      },
      {
        field: 'fyActual',
        title: 'FY25 Actual',

        editable: true,
        minWidth: 100,
      },
      {
        field: 'fyActual',
        title: 'FY26 Plan',

        editable: true,
        minWidth: 100,
      },

      {
        field: 'remarks',
        title: 'Remark',
        widthT: 60,
        editable: false,
        minWidth: 100,
      },
    ],
    [plantID, yearChanged],
  )
  const columns4 = useMemo(
    () => [
      {
        field: 'serialNumber',
        title: 'S.No',
        widthT: 70,
        editable: false,
        minWidth: 70,
      },
      {
        field: 'initiative',
        title: 'Initiative',
        editable: true,
        widthT: 250,
        minWidth: 100,
      },
      {
        field: 'outcome',
        title: 'Outcome',
        editable: true,
        minWidth: 100,
      },
      {
        field: 'recommendation',
        title: 'Recommendation',
        editable: true,
        minWidth: 100,
      },
      {
        field: 'targetDate',
        title: 'Target Date',
        editable: true,
        minWidth: 100,
      },
      {
        field: 'responsible',
        title: 'Resp.',
        editable: true,
        widthT: 120,
        minWidth: 100,
      },
    ],
    [plantID, yearChanged],
  )

  const columns3 = useMemo(
    () => [
      {
        field: 'serialNumber',
        title: 'S.No',
        widthT: 70,
        editable: false,
        minWidth: 70,
      },
      {
        field: 'incidentDescription',
        title: 'Incident Description',
        editable: true,
        widthT: 250,
        minWidth: 100,
      },
      {
        field: 'rootCauses',
        title: 'Root Causes',
        editable: true,
        minWidth: 100,
      },
      {
        field: 'recommendation',
        title: 'Recommendation',
        editable: true,
      },
      {
        field: 'targetDate',
        title: 'Target Date',
        editable: true,
        minWidth: 100,
      },
      {
        field: 'responsible',
        title: 'Resp.',
        editable: true,
        widthT: 120,
        minWidth: 100,
      },
    ],
    [plantID, yearChanged],
  )
  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      // var res = await DataService.getMonthWiseSummary(keycloak)
      

      if (res?.code === 200) {
        const mapped = res?.data?.map((item, index) => ({
          ...item,
          id: index,
          isEditable: item?.isEditable,
          originalRemark: item.remarks,
        }))
       
        setRows(mapped)
      } else {
        setRows([])
      }
    } catch (err) {
      console.error('fetchData error', err)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, yearChanged, plantID])

  useEffect(() => {
    fetchData()
  }, [fetchData, yearChanged, plantID])

  const saveChanges = useCallback(async () => {
    try {
      setLoading(true)
      const data = Object.values(modifiedCells)
      if (!data.length) {
        setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
        setSnackbarOpen(true)
        return
      }
      // save logic...
    } finally {
      setSnackbarOpen(true)
      setLoading(false)
    }
  }, [modifiedCells])


  const handleCalculate = () => {}

  const handleRemarkCellClick = useCallback((row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }, [])




  const getAdjustedPermissionsC = (permissions, isOldYear) => {
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

  const adjustedPermissionsC = getAdjustedPermissionsC(
    {
      allAction: true,
      saveBtn: true,
      showTitleNameBusiness: true,
      titleName: 'Safety Improvement Initiative',
      adjustedPermissions: true,
      // downloadExcelBtnFromUI: true,
      downloadExcelBtn: true,
      uploadExcelBtn: true,
      ExcelName: `${lowerVertName}_Safety Improvement Initiative`,
    },
    isOldYear,
  )



  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

     <KendoDataTables
  rows={rows}
  setRows={setRows}
  columns={columns}  
  title='Safety Improvement Initiative'
  modifiedCells={modifiedCells}
  setModifiedCells={setModifiedCells}
  remarkDialogOpen={remarkDialogOpen}
  setRemarkDialogOpen={setRemarkDialogOpen}
  currentRemark={currentRemark}
  setCurrentRemark={setCurrentRemark}
  currentRowId={currentRowId}
  setCurrentRowId={setCurrentRowId}
  enableSaveAddBtn={enableSaveAddBtn}
  saveChanges={saveChanges}
  handleCalculate={handleCalculate}
  handleRemarkCellClick={handleRemarkCellClick}
  permissions={adjustedPermissionsC}
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
