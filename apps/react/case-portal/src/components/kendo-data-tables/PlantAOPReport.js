import { useGridApiRef } from '@mui/x-data-grid'
import React, { useEffect, useState, useMemo, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { PlantAopReportApiService } from 'services/plant-aop-report-api-service'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { validateFields } from 'utils/validationUtils'
import KendoDataTables from './index'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AopTabs from 'components/AopTabs'
import SafetyImprovementInitiative from './SafetyImprovementInitiative'
const PlantAOPReport = ({ permissions }) => {
  const [_plantID, set_PlantID] = useState('')
  const [modifiedCells, setModifiedCells] = React.useState({})
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    yearChanged,
    oldYear,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const PLANT_NAME = plantObject?.name

  const SITE_ID = siteObject?.id
  const SITE_NAME = siteObject?.name

  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name

  const AOP_YEAR = year?.selectedYear
  const vertName = verticalChange?.selectedVertical
  const SCREEN_NAME = screenTitle?.title

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()

  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}`

  const lowerVertName = vertName?.toLowerCase()
  const lowerSiteName = SITE_NAME?.toLowerCase()
  const lowerPlantName = PLANT_NAME?.toLowerCase()
  const plantName = plantObject?.name
  const siteName = siteObject?.name
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const [open1, setOpen1] = useState(false)
  const [deleteId, setDeleteId] = useState(null)
  const apiRef = useGridApiRef()
  const [rowsSlowdown, setRowsSlowdown] = useState()

  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const keycloak = useSession()
  const [rows, setRows] = useState()
  const [tabIndex, setTabIndex] = useState(0)
  const defaultTabs = [
    'Plant Safety Performance & Targets',
    'Safety Improvement Initiative',
  ]
  function getAopShortYears(aopYear) {
    if (!aopYear) return { prev: '', next: '' }
    const match = aopYear.match(/(\d{4})-(\d{2})/)
    if (match) {
      const prev = match[1].slice(-2)
      const next = match[2]
      return { prev, next }
    }
    const year = String(aopYear).slice(-2)
    return { prev: year, next: String(Number(year) + 1).padStart(2, '0') }
  }
  const { prev, next } = getAopShortYears(AOP_YEAR)
  const valueFormat = ValueFormatterConsumption()

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const headerMap = generateHeaderNames(AOP_YEAR)
  const IS_PE_PP_VERTICAL = lowerVertName === 'pe' || lowerVertName === 'pp'
  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }
  const columns = [
    {
      field: 'id',
      title: 'Id',
      editable: false,
      hidden: true,
      isVisible: false,
    },
    {
      field: 'kpiName',
      title: 'KPI Name',
      editable: false,
      hidden: false,
      isVisible: true,
    },
    {
      field: 'uom',
      title: 'UOM',
      editable: false,
      hidden: false,
      isVisible: true,
    },
    {
      field: 'bestAchieved',
      title: 'Best Achieved',
      editable: true,
      hidden: false,
      isVisible: true,
    },
    {
      field: 'prevAOP',
      title: 'Prev AOP',
      editable: true,
      hidden: false,
      isVisible: true,
    },
    {
      field: 'prevActual',
      title: 'Prev Actual',
      editable: true,
      hidden: false,
      isVisible: true,
    },
    {
      field: 'currentPlan',
      title: 'Current Plan',
      editable: true,
      hidden: false,
      isVisible: true,
    },
    {
      field: 'remark',
      title: 'Remark',
      editable: true,
      hidden: false,
      isVisible: true,
    },
    {
      field: 'aopYear',
      title: 'AOP Year',
      hidden: true,
      isVisible: true,
    },
    {
      field: 'plant_FK_Id',
      title: 'Plant',
      hidden: true,
      isVisible: false,
    },
    {
      field: 'createdOn',
      title: 'Created On',
      hidden: true,
      isVisible: false,
    },
    {
      field: 'modifiedOn',
      title: 'Modified On',
      hidden: true,
      isVisible: false,
    },
    {
      field: 'updatedBy',
      title: 'Updated By',
      hidden: true,
      isVisible: false,
    },
    {
      field: 'isEditable',
      title: 'Is Editable',
      hidden: true,
      isVisible: false,
    },
    {
      field: 'isVisible',
      title: 'Is Visible',
      hidden: true,
      isVisible: false,
    },
    {
      field: 'displayOrder',
      title: 'Display Order',
      hidden: true,
      isVisible: true,
    },
  ]
  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setModifiedCells({})
    setLoading(true)
    try {
      const res = await PlantAopReportApiService.getPlantsafetyPerformance(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (res?.code === 200) {
        const mapped = (res?.data.Data || []).map((item, index) => ({
          ...item,
          id: item.id,
          isEditable: item?.isEditable,
          remark: item.remark,
          originalRemark: item.remark,
        }))
        setRows(mapped)
      } else {
        setRows([])
      }
    } catch (err) {
      console.error('Error fetching data:', err)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, yearChanged, PLANT_ID, AOP_YEAR])

  const saveChanges = React.useCallback(async () => {
    try {
      setLoading(true)
      const data = Object.values(modifiedCells)

      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
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
        id: item.id,
        kpiName: item.kpiName,
        uom: item.uom,
        bestAchieved: item.bestAchieved,
        prevAOP: item.prevAOP,
        prevActual: item.prevActual,
        currentPlan: item.currentPlan,
        remark: item.remark,
        aopYear: AOP_YEAR,
        plant_FK_Id: PLANT_ID,
        isEditable: item.isEditable ?? true,
        isVisible: item.isVisible ?? true,
        displayOrder: item.displayOrder ?? null,
      }))

      const response =
        await PlantAopReportApiService.savePlantsafetyPerformance(
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
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, keycloak, PLANT_ID, AOP_YEAR, fetchData])

  useEffect(() => {
    if (tabIndex === 0) {
      fetchData()
    }
  }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak, tabIndex])

  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
      allAction: false,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: permissions?.showAction ?? true,
      showUnit: permissions?.showUnit ?? false,
      saveWithRemark: permissions?.saveWithRemark ?? true,
      saveBtn: permissions?.saveBtn ?? true,
      customHeight: permissions?.customHeight,
      allAction: true,
      downloadExcelBtn: false,
      showNoteWhileDeleting: false,
      showTitleNameBusiness: true,
      titleName: 'Plant Safety Performance& Targets',

      uploadExcelBtn: false,
    },
    isOldYear,
  )

  return (
    <div>
      <LoaderBackdrop open={!!loading} />
      {defaultTabs?.length > 1 && (
        <AopTabs
          tabIndex={tabIndex}
          setTabIndex={setTabIndex}
          tabs={defaultTabs}
        />
      )}
      {tabIndex === 0 && (
        <KendoDataTables
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          setRows={setRows}
          columns={columns}
          rows={rows}
          fetchData={fetchData}
          saveChanges={saveChanges}
          paginationOptions={[100, 200, 300]}
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          apiRef={apiRef}
          deleteId={deleteId}
          open1={open1}
          setDeleteId={setDeleteId}
          setOpen1={setOpen1}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          handleRemarkCellClick={handleRemarkCellClick}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
          permissions={adjustedPermissions}
          disableRedHighlight={true}
          screenType='shutdown'
        />
      )}

      {tabIndex === 1 && <SafetyImprovementInitiative />}
    </div>
  )
}

export default PlantAOPReport
