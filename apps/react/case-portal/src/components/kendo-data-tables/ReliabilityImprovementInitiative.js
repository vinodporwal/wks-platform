import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Box } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from './index'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { useSelector } from 'react-redux'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { PlantAopReportApiService } from 'services/plant-aop-report-api-service'
import { validateFields } from 'utils/validationUtils'

export default function ReliabilityImprovementInitiative() {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { year, verticalChange, yearChanged, oldYear, plantObject, siteObject } = dataGridStore
  const AOP_YEAR = year?.selectedYear
  const PLANT_ID = plantObject?.id
  const SITE_NAME_NO_CASE = siteObject?.name?.toLowerCase()
  const PLANT_NAME_NO_CASE = plantObject?.name?.toLowerCase()
  const thisYear = AOP_YEAR

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [modifiedCells, setModifiedCells] = useState({})
  const [enableSaveAddBtn, setEnableSaveAddBtn] = useState(false)
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
        field: 'sNo',
        title: 'S.No',
        widthT: 70,
        editable: false,
        minWidth: 70,
      },
      {
        field: 'initiativeDescription',
        title: 'Initiative',
        editable: true,
        widthT: 300,
        minWidth: 100,
      },
      {
        field: 'outcome',
        title: 'Outcome',
        widthT: 80,
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
        field: 'remark',
        title: 'Remark',
        widthT: 60,
        editable: false,
        minWidth: 100,
      },
    ],
    [PLANT_ID, yearChanged],
  )
  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await PlantAopReportApiService.getReliabilityImprovementInitiative(keycloak, PLANT_ID, AOP_YEAR)
      if (res?.code === 200) {
        const mapped = (res?.data?.Data || []).map((item, index) => ({
          ...item,
          id: index,
          idFromApi: item?.id,
          sNo: index + 1,
          isEditable: item?.isEditable,
          originalRemark: item.remark,
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
  }, [keycloak, yearChanged, PLANT_ID])

  useEffect(() => {
    fetchData()
  }, [fetchData, yearChanged, PLANT_ID])

  function toLocalDateString(date) {
    if (!date) return null
    const d = new Date(date)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}` // "2026-09-24"
  }

  const saveChanges = useCallback(async () => {
    try {
      setLoading(true)
      const data = Object.values(modifiedCells)
      if (!data.length) {
        setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
        setSnackbarOpen(true)
        return
      }
      // adjust to whichever fields are actually mandatory on this grid
      const requiredFields = ['remark']

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

      const payload = data.map((item) => ({
        id: item.idFromApi || null,
        initiativeDescription: item.initiativeDescription,
        outcome: item.outcome,
        recommendation: item.recommendation,
        targetDate: toLocalDateString(item.targetDate),
        remark: item.remark,
        aopYear: AOP_YEAR,
        plant_FK_Id: PLANT_ID,
        isEditable: item.isEditable ?? true,
        isVisible: item.isVisible ?? true,
        displayOrder: item.displayOrder ?? null,
      }))

      const response = await PlantAopReportApiService.saveReliabilityImprovementInitiative(
        keycloak,
        payload,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Save failed!',
          severity: 'error',
        })
      }

    } catch (e) {
      console.log(e)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error while saving!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells])


  const handleCalculate = () => { }

  const handleRemarkCellClick = useCallback((row) => {
    setCurrentRemark(row.remark || '')
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
      titleName: 'Reliability Improvement Initiative',
      adjustedPermissions: true,
      // downloadExcelBtnFromUI: true,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      ExcelName: `${lowerVertName}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_${AOP_YEAR}_Plant_AOP_Report_Reliability_Improvement_Initiative`,
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
        title='Reliability Improvement Initiative'
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
