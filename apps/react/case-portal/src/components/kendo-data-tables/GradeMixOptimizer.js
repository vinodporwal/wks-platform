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
import { OptimizerDataApiService } from 'services/optimizer-api-service'
import CalculatedBusinessProposed from './ProprosedBusinessGradeOptimizer'
import BudgetOperatingHour from './BudgetOperatingHourGradeMix'
import { DataService } from 'services/DataService'

const GradeMixOptimizer = ({ permissions }) => {
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
  const valueFormat = ValueFormatterConsumption()
  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }
  const columns = [
    {
      field: 'normParameterFkId',
      title: 'Particulars',
      editable: false,
      hidden: true,
      isVisible: false,
    },
    {
      field: 'normTypeName',
      title: 'Particulars',
      editable: false,
      hidden: true,
      isVisible: false,
    },
    {
      field: 'displayName',
      title: 'Particulars',
      editable: false,
      widthT: 300,
      minWidth: 120,
    },
    {
      field: 'uom',
      title: 'UOM',
      editable: false,
      widthT: 300,
      minWidth: 120,
    },
    {
      field: 'apr',
      title: 'Values',
      editable: true,
      align: 'left',
      headerAlign: 'left',
      type: 'number',
      format: '{0:#.##}',
      minWidth: 120,
    },
    {
      field: 'remarks',
      title: 'Remark',
      editable: true,
      widthT: 300,
      minWidth: 120,
    },
  ]

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setModifiedCells({})
    setLoading(true)
    try {
      const res = await OptimizerDataApiService.getGradeMixOptimizerConstant(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (res?.code === 200) {
        const mapped = (res?.data || []).map((item, index) => ({
          ...item,
          id: index,
          idFromApi: item.id || null,
          isEditable: item?.isEditable,
          remarks: item.remarks,
          originalRemark: item.remarks,
          normParameterFkId: item.normParameterFkId,
          uom: item.uom,
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
      const requiredFields = ['remarks']

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
      var payload = []
      payload = data.map((item) => ({
        apr: item.apr || item.ConstantValue || null,
        may: item.apr || null,
        jun: item.apr || null,
        jul: item.apr || null,
        aug: item.apr || null,
        sep: item.apr || null,
        oct: item.apr || null,
        nov: item.apr || null,
        dec: item.apr || null,
        jan: item.apr || null,
        feb: item.apr || null,
        mar: item.apr || null,
        UOM: '',
        auditYear: AOP_YEAR,
        normParameterFKId: item.normParameterFkId || item.NormParameter_FK_Id,
        remarks: item.remarks,
        id: null,
      }))

      const response = await DataService.saveCatalystData(
        PLANT_ID,
        payload,
        keycloak,
        AOP_YEAR,
      )

      if (response) {
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
      titleName: 'Constant',

      uploadExcelBtn: false,
    },
    isOldYear,
  )

  return (
    <div>
      <LoaderBackdrop open={!!loading} />

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
      <BudgetOperatingHour permissions={permissions} />
    </div>
  )
}

export default GradeMixOptimizer
