import { useGridApiRef } from '@mui/x-data-grid'
import React, { useEffect, useState, useMemo, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { validateFields } from 'utils/validationUtils'
import KendoDataTables from './index'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { OptimizerDataApiService } from 'services/optimizer-api-service'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { DataService } from 'services/DataService'
const VcmStockbalance = ({ permissions }) => {
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

  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_${AOP_YEAR}`

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
  const [aopCalculation, setAopCalculation] = useState([])
  const valueFormat = ValueFormatterProduction()
  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const [lineDetails, setLineDetails] = useState([])
  const headerMap = generateHeaderNames(AOP_YEAR)

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const columns = [
    {
      field: 'gradeId',
      title: 'GradeId',
      editable: false,
      hidden: true,
      isVisible: false,
    },
    {
      field: 'plantId',
      title: 'PlantId',
      editable: false,
      hidden: true,
      isVisible: false,
    },
    {
      field: 'aopYear',
      title: 'AopYear',
      editable: false,
      hidden: true,
      isVisible: false,
    },
    {
      field: 'displayName',
      title: 'Particulars',
      editable: false,
      widthT: 300,
      minWidth: 150,
    },
    {
      field: 'apr',
      title: headerMap[4] || 'April',
      editable: false,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'may',
      title: headerMap[5],
      editable: false,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'jun',
      title: headerMap[6],
      editable: false,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'jul',
      title: headerMap[7],
      editable: false,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'aug',
      title: headerMap[8],
      editable: false,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'sep',
      title: headerMap[9],
      editable: false,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'oct',
      title: headerMap[10],
      editable: false,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'nov',
      title: headerMap[11],
      editable: false,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'dec',
      title: headerMap[12],
      editable: false,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'jan',
      title: headerMap[1],
      editable: false,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'feb',
      title: headerMap[2],
      editable: false,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'mar',
      title: headerMap[3],
      editable: false,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'remarks',
      title: 'Remark',
      editable: false,
      hidden: true,
      widthT: 300,
      minWidth: 120,
    },
  ]

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    setModifiedCells({})
    setLoading(true)
    try {
      const res = await OptimizerDataApiService.getVcmStockBalance(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      setAopCalculation(res?.data?.aopCalculation || [])
      if (res?.code === 200) {
        const mapped = (res?.data?.vcmStockBalance || []).map((item, index) => ({
          ...item,
          id: index,
          idFromApi: item.id,
          isEditable: false,
          remarks: item.remarks,
          originalRemark: item.remarks,
          normParameterFkId: item.normParameterFkId,
          uom: item.uom,
        }))
        setRows(mapped)
        setAopCalculation(res?.data?.aopCalculation || [])
      } else {
        setRows([])
        setAopCalculation([])
      }
    } catch (err) {
      console.error('Error fetching data:', err)
      setRows([])
      setAopCalculation([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, yearChanged, PLANT_ID, AOP_YEAR, lineDetails])

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
        apr: item.apr || null,
        may: item.may || null,
        jun: item.jun || null,
        jul: item.jul || null,
        aug: item.aug || null,
        sep: item.sep || null,
        oct: item.oct || null,
        nov: item.nov || null,
        dec: item.dec || null,
        jan: item.jan || null,
        feb: item.feb || null,
        mar: item.mar || null,
        UOM: '',
        auditYear: AOP_YEAR,
        normParameterFKId: item.normParameterFkId || item.NormParameter_FK_Id,
        remarks: item.remarks,
        id: item.idFromApi || null,
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
  const handleCalculate = useCallback(async () => {
    setRows([])
    setLoading(true)
    try {
      const data = await OptimizerDataApiService.calculateVcmStockBalance(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (data?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: data?.message || 'Data Refresh Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: error.message || 'An error occurred',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, fetchData])

  useEffect(() => {
    fetchData()
  }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak])
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
      saveWithRemark: false,
      saveBtn: false,
      customHeight: permissions?.customHeight,
      allAction: true,
      downloadExcelBtn: false,
      downloadExcelBtnFromUI: true,
      ExcelName: `${EXCEL_EXPORT_TITLE}_VCM_Stock_Balance`,
      uploadExcelBtn: false,
      showNoteWhileDeleting: false,
      showTitleNameBusiness: true,
      titleName: 'VCM Stock Balance',
      showCalculate: true,
      showCalculateVisibility: aopCalculation && aopCalculation.length > 0,
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
        handleCalculate={handleCalculate}
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
    </div>
  )
}

export default VcmStockbalance
