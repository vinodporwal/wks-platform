import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import KendoDataTablesReports from 'components/kendo-data-tables/index-reports'
import { Backdrop, Box, CircularProgress } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useSession } from 'SessionStoreContext'
import { PlantAopReportApiService } from 'services/plant-aop-report-api-service'
import KendoDataTables from './index'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { useSelector } from 'react-redux'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { validateFields } from 'utils/validationUtils'
import { getRoleName } from 'services/role-service'

export default function PlantSafetyPerformanceTarget() {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    year,
    plantObject,
    verticalChange,
    yearChanged,
    oldYear,
    plantID: gridPlantID,
    isReleased,
  } = dataGridStore

  const PLANT_ID = plantObject?.id || gridPlantID
  const AOP_YEAR = year?.selectedYear
  const thisYear = AOP_YEAR

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)

  const [modifiedCells, setModifiedCells] = useState({})
  const [enableSaveAddBtn, setEnableSaveAddBtn] = useState(false)
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()

  // second grid states
  const [modifiedCellsP, setModifiedCellsP] = useState({})
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const unsavedChangesRef = useRef({ unsavedRows: {}, rowsBeforeChange: {} })

  function getAopShortYears(aopYear) {
    if (!aopYear) return { prev: '25', next: '26' }
    const match = aopYear.match(/(\d{4})-(\d{2})/)
    if (match) {
      const prev = match[1].slice(-2)
      const next = match[2]
      return { prev, next }
    }
    const yStr = String(aopYear).slice(-2)
    return { prev: yStr, next: String(Number(yStr) + 1).padStart(2, '0') }
  }

  const { prev, next } = getAopShortYears(AOP_YEAR)

  const columns = useMemo(
    () => [
      {
        field: 'serialNumber',
        title: 'S.No',
        width: 100,
        editable: false,
        minWidth: 100,
      },
      {
        field: 'kpiName',
        title: 'KPI',
        editable: false,
        widthT: 300,
        minWidth: 100,
      },
      {
        field: 'uom',
        title: 'UOM',
        width: 80,
        minWidth: 60,
        editable: false,
      },
      {
        field: 'prevAOP',
        title: `FY${prev} AOP`,
        editable: true,
        type: 'number',
        minWidth: 100,
      },
      {
        field: 'prevActuals',
        title: `FY${prev} ACT`,
        editable: true,
        type: 'number',
        minWidth: 100,
      },

      {
        field: 'currentPlan',
        title: `FY${next} Plan`,
        editable: true,
        type: 'number',
        minWidth: 100,
      },
      {
        field: 'responsibility',
        title: 'Remarks',
        widthT: 60,
        editable: true,
        minWidth: 100,
      },
    ],
    [PLANT_ID, yearChanged, AOP_YEAR, prev, next],
  )
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
        const mapped = (res?.data?.Data || []).map((item, index) => ({
          ...item,
          id: index,
          idFromAPI: item.id,
          masterId: item.masterId,
          serialNumber: item.displayOrder || index + 1,
          kpiName: item.kpiName || item.kpi,
          uom: item.uom,
          bestAchieved: item.bestAchieved ?? item.bestAchived ?? '',
          prevAOP: item.prevAOP ?? item.fyAop ?? '',
          prevActuals: item.prevActual ?? item.fyActual ?? '',
          currentPlan: item.currentPlan ?? item.fy26Plan ?? '',
          remark: item.remark ?? item.remarks ?? '',
          responsibility: item.remark ?? item.remarks ?? '',
          originalRemark: item.remark ?? item.remarks ?? '',
          isEditable: true,
          currentPlanEditable:
            item?.isEditable === true || item?.isEditable === 'true',
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
  }, [keycloak, yearChanged, PLANT_ID, AOP_YEAR])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const saveChanges = useCallback(async () => {
    try {
      setLoading(true)
      const data = Object.values(modifiedCells)
      if (!data.length) {
        setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
        setSnackbarOpen(true)
        return
      }

      const payload = data.map((item) => ({
        id: item.idFromAPI || null,
        masterId: item.masterId,
        kpiName: item.kpiName,
        uom: item.uom,
        bestAchieved:
          item.bestAchieved !== '' && item.bestAchieved != null
            ? Number(item.bestAchieved)
            : null,
        prevAOP:
          item.prevAOP !== '' && item.prevAOP != null
            ? Number(item.prevAOP)
            : null,
        prevActual:
          item.prevActuals !== '' && item.prevActuals != null
            ? Number(item.prevActuals)
            : null,
        currentPlan:
          item.currentPlan !== '' && item.currentPlan != null
            ? Number(item.currentPlan)
            : null,
        remark: item.responsibility ?? item.remark ?? item.remarks ?? '',
        aopYear: AOP_YEAR,
        plantFkId: PLANT_ID,
        isEditable: item.isEditable,
        isVisible: item.isVisible,
        displayOrder: item.displayOrder,
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
    } catch (err) {
      console.error('Error saving plant report data:', err)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, keycloak, AOP_YEAR, PLANT_ID, fetchData])

  const saveChangesP = useCallback(async () => {
    try {
      setLoading(true)
      const data = Object.values(modifiedCellsP)
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
  }, [modifiedCellsP])

  const handleCalculate = () => {}
  const handleCalculateP = () => {}
  const handleLoad = async () => {
    setLoading(true) 
    try {
      const data = await PlantAopReportApiService.handleLoadPlantSafetyTarget(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (data || data == 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Refresh Falied!',
          severity: 'error',
        })
      }

      return data
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: error.message || 'An error occurred',
        severity: 'error',
      })
      console.error('Error!', error)
    } finally {
      setLoading(false)
    }
  }

  const handleRemarkCellClickP = useCallback((row) => {
    setCurrentRemarkP(row.remarks || '')
    setCurrentRowIdP(row.id)
    setRemarkDialogOpenP(true)
  }, [])

  const getAdjustedPermissionsP = (permissions, isOldYear) => {
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
  const getAdjustedPermissions3 = (permissions, isOldYear) => {
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
  const getAdjustedPermissions4 = (permissions, isOldYear) => {
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

  const adjustedPermissionsP = getAdjustedPermissionsP(
    {
      saveBtn: true,
      allAction: true,
      showTitleNameBusiness: true,
      titleName: 'Procurment Budget',
      adjustedPermissions: true,
      // downloadExcelBtnFromUI: true,
      downloadExcelBtn: true,
      uploadExcelBtn: true,
      ExcelName: `${lowerVertName}_Monthly Procurment Budget`,
    },
    isOldYear,
  )
  const adjustedPermissions3 = getAdjustedPermissions3(
    {
      saveBtn: true,
      allAction: true,
      showTitleNameBusiness: true,
      titleName:
        'Major Incidents FY25 (Fatality, PSE Tier 1 & 2, LWC, High Severity, Process Fires)',
      adjustedPermissions: true,
      // downloadExcelBtnFromUI: true,
      downloadExcelBtn: true,
      uploadExcelBtn: true,
      // ExcelName: `${lowerVertName}_Monthly Procurment Budget`,
    },
    isOldYear,
  )
  const adjustedPermissions4 = getAdjustedPermissions4(
    {
      saveBtn: true,
      allAction: true,
      showTitleNameBusiness: true,
      titleName: 'Safety Improvement Initiative',
      adjustedPermissions: true,
      // downloadExcelBtnFromUI: true,
      downloadExcelBtn: true,
      uploadExcelBtn: true,
      // ExcelName: `${lowerVertName}_Monthly Procurment Budget`,
    },
    isOldYear,
  )
  const downloadExcelForConfiguration = async () => {
    setLoading(true)
    const EXCEL_NAME = `${lowerVertName}_Plant_Safety_Performance_Targets_${AOP_YEAR}.xlsx`
    try {
      await PlantAopReportApiService.exportPlantReport(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_NAME,
      )

      setSnackbarData({ message: 'Export started!', severity: 'success' })
      setSnackbarOpen(true)
    } catch (err) {
      console.error('Export error', err)
      setSnackbarData({ message: 'Export failed!', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }

  const handleExcelUpload = async (file) => {
    if (!file) return
    setLoading(true)
    try {
      const res = await PlantAopReportApiService.importPlantReport(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        file,
      )

      if (
        res?.code === 200 ||
        res?.status === 200 ||
        res?.message === 'Success' ||
        res?.status === 'success' ||
        (res && res.ok !== false && !res.error && res.code !== 500)
      ) {
        setSnackbarData({
          message: 'Uploaded Successfully!',
          severity: 'success',
        })
        setSnackbarOpen(true)
        fetchData()
      } else {
        setSnackbarData({
          message: res?.message || 'Import failed!',
          severity: 'error',
        })
        setSnackbarOpen(true)
      }
    } catch (err) {
      console.error('Import error', err)
      setSnackbarData({ message: 'Import failed!', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }

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
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      isOldYear: isOldYear,
    }
  }

  const adjustedPermissionsC = getAdjustedPermissionsC(
    {
      allAction: true,
      saveBtn: true,
      showTitleNameBusiness: true,
      titleName: 'Plant Safety Performance & Targets',
      adjustedPermissions: true,
      // downloadExcelBtnFromUI: true,
      downloadExcelBtn: true,
      uploadExcelBtn: true,
      ExcelName: `${lowerVertName}_Plant Safety Performance & Targets`,
      disableColWidth: true,
      showLoadBtn: true,
    },
    isOldYear,
  )

  const commonGridProps = {
    columns,
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <KendoDataTables
        rows={rows}
        setRows={setRows}
        title='Plant Safety Performance & Targets'
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        enableSaveAddBtn={enableSaveAddBtn}
        saveChanges={saveChanges}
        handleCalculate={handleCalculate}
        handleLoad={handleLoad}
        permissions={adjustedPermissionsC}
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        handleExcelUpload={handleExcelUpload}
        // groupBy='Particulars'
        {...commonGridProps}
      />

      {/* <KendoDataTables
        rows={rowsP}
        setRows={setRowsP}
        title='Procurement Budget'
        modifiedCells={modifiedCellsP}
        setModifiedCells={setModifiedCellsP}
        remarkDialogOpen={remarkDialogOpenP}
        setRemarkDialogOpen={setRemarkDialogOpenP}
        currentRemark={currentRemarkP}
        setCurrentRemark={setCurrentRemarkP}
        currentRowId={currentRowIdP}
        setCurrentRowId={setCurrentRowIdP}
        enableSaveAddBtn={enableSaveAddBtnP}
        saveChanges={saveChangesP}
        handleCalculate={handleCalculateP}
        handleRemarkCellClick={handleRemarkCellClickP}
        permissions={adjustedPermissionsP}
        columns={columns3}
      /> */}
      {/* <KendoDataTables
        rows={rows3}
        setRows={setRows3}
        title='Procurement Budget'
        modifiedCells={modifiedCells3}
        setModifiedCells={setModifiedCells3}
        remarkDialogOpen={remarkDialogOpen3}
        setRemarkDialogOpen={setRemarkDialogOpen3}
        currentRemark={currentRemark3}
        setCurrentRemark={setCurrentRemark3}
        currentRowId={currentRowId3}
        setCurrentRowId={setCurrentRowId3}
        enableSaveAddBtn={enableSaveAddBtn3}
        // saveChanges={saveChanges3}
        // handleCalculate={handleCalculate3}
        // handleRemarkCellClick={handleRemarkCellClick3}
        permissions={adjustedPermissions3}
        columns={columns3}
      /> */}

      {/* <KendoDataTables
        rows={rows4}
        setRows={setRows4}
        title='Procurement Budget'
        modifiedCells={modifiedCells4}
        setModifiedCells={setModifiedCells4}
        remarkDialogOpen={remarkDialogOpen4}
        setRemarkDialogOpen={setRemarkDialogOpen4}
        currentRemark={currentRemark4}
        setCurrentRemark={setCurrentRemark4}
        currentRowId={currentRowId4}
        setCurrentRowId={setCurrentRowId4}
        enableSaveAddBtn={enableSaveAddBtn4}
        // saveChanges={saveChanges4}
        // handleCalculate={handleCalculate4}
        // handleRemarkCellClick={handleRemarkCellClick4}
        permissions={adjustedPermissions4}
        columns={columns4}
      /> */}

      <Notification
        open={snackbarOpen}
        message={snackbarData.message}
        severity={snackbarData.severity}
        onClose={() => setSnackbarOpen(false)}
      />
    </Box>
  )
}
