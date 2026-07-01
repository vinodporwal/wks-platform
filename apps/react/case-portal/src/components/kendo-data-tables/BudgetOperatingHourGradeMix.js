import { useGridApiRef } from '@mui/x-data-grid'
import React, { useEffect, useState, useMemo, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { PlantAopReportApiService } from 'services/plant-aop-report-api-service'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { validateFields } from 'utils/validationUtils'
import KendoDataTables from './index'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { OptimizerDataApiService } from 'services/optimizer-api-service'
import CalculatedBusinessProposed from './ProprosedBusinessGradeOptimizer'
import { DataService } from 'services/DataService'
import AopTabs from 'components/AopTabs'
import { Box } from '@mui/material'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
const BudgetOperatingHour = ({ permissions, saveTrigger }) => {
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
  const [tabIndex, setTabIndex] = useState(0)
  const valueFormat = ValueFormatterProduction()
  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const [tabs, setTabs] = useState([])
  const [lineDetails, setLineDetails] = useState([])
  const headerMap = generateHeaderNames(AOP_YEAR)
  const [aopCalculation, setAopCalculation] = useState([])
  const [refreshSignal, setRefreshSignal] = useState(0)
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
      field: 'lineId',
      title: 'LineId',
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
      editable: true,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'may',
      title: headerMap[5],
      editable: true,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'jun',
      title: headerMap[6],
      editable: true,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'jul',
      title: headerMap[7],
      editable: true,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'aug',
      title: headerMap[8],
      editable: true,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'sep',
      title: headerMap[9],
      editable: true,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'oct',
      title: headerMap[10],
      editable: true,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'nov',
      title: headerMap[11],
      editable: true,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'dec',
      title: headerMap[12],
      editable: true,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'jan',
      title: headerMap[1],
      editable: true,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'feb',
      title: headerMap[2],
      editable: true,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'mar',
      title: headerMap[3],
      editable: true,
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'remarks',
      title: 'Remark',
      editable: true,
      widthT: 300,
      minWidth: 120,
    },
  ]

  // --- Fetch the list of lines for this plant/year (NEW) ---
  const fetchLineDetails = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      const response = await DataService.getLineDetails(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.code === 200 && Array.isArray(response?.data)) {
        setLineDetails(response.data)
        setTabs(response.data.map((line) => line.displayName))
      } else {
        setLineDetails([])
        setTabs([])
      }
    } catch (err) {
      console.error('Error fetching line details:', err)
      setLineDetails([])
      setTabs([])
    }
  }, [PLANT_ID, AOP_YEAR, keycloak])

  useEffect(() => {
    fetchLineDetails()
  }, [fetchLineDetails])

  // Reset to first tab whenever the plant/year changes so we don't stay
  // on a stale tabIndex from a plant that had more lines
  useEffect(() => {
    setTabIndex(0)
  }, [PLANT_ID, AOP_YEAR])

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    const lineId = lineDetails[tabIndex]?.id
    if (!lineId) return // wait until lineDetails has loaded
    setModifiedCells({})
    setLoading(true)
    try {
      const res =
        await OptimizerDataApiService.getGradewiseBudgetOperatingHours(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          lineId,
        )

      setAopCalculation(res?.data?.aopCalculation || [])

      if (res?.code === 200) {
        const mapped = (res?.data.budgetedOperatingHoursData || []).map(
          (item, index) => ({
            ...item,
            id: index,
            idFromApi: item.id || null,
            isEditable: item?.isEditable,
            remarks: item.remarks,
            originalRemark: item.remarks,
            normParameterFkId: item.normParameterFkId,
            uom: item.uom,
          }),
        )
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
  }, [keycloak, yearChanged, PLANT_ID, AOP_YEAR, lineDetails, tabIndex])

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
        gradeId: item.gradeId,
        normParameterFkId: item.normParameterFkId || item.NormParameter_FK_Id,
        remarks: item.remarks,
        id: item.idFromApi || null,
      }))
      const lineId = lineDetails[tabIndex]?.id
      const response =
        await OptimizerDataApiService.saveGradeBudgetOperatingHours(
          PLANT_ID,
          payload,
          keycloak,
          AOP_YEAR,
          lineId,
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

  useEffect(() => {
    if (lineDetails.length > 0 && lineDetails[tabIndex]) {
      fetchData()
    }
  }, [
    PLANT_ID,
    AOP_YEAR,
    oldYear,
    yearChanged,
    keycloak,
    lineDetails,
    tabIndex,
  ])

  useEffect(() => {
    if (saveTrigger > 0) {
      fetchData()
    }
  }, [saveTrigger, fetchData])

  const handleCalculate = async () => {
    setLoading(true)
    try {
      const data =
        await OptimizerDataApiService.calculateGradeMixBudgetOpeartingHours(
          PLANT_ID,
          AOP_YEAR,
          keycloak,
        )
      if (data || data === 0 || data?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        fetchData()
        setRefreshSignal((prev) => prev + 1)
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Refresh Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error saving refresh data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: error.message || 'An error occurred',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const downloadExcelForConfiguration = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })
    const EXCEL_NAME = `${EXCEL_EXPORT_TITLE}_Budget_Operating_Hours`
    try {
      await OptimizerDataApiService.budgetOperatingLineExport(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_NAME,
      )
    } catch (error) {
      console.error('Error downloading Excel:', error)
      setSnackbarData({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
    } finally {
      setSnackbarOpen(true)
    }
  }

  const handleExcelUpload = (rawFile) => {
    uploadBudgetOperatingHour(rawFile)
  }

  const uploadBudgetOperatingHour = async (rawFile) => {
    setLoading(true)

    try {
      let response

      response = await OptimizerDataApiService.budgetOperatingHourImport(
        rawFile,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Uploaded Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData()
      } else if (response?.code === 400 && response?.data) {
        const byteCharacters = atob(response.data)
        const byteNumbers = Array.from(byteCharacters, (char) =>
          char.charCodeAt(0),
        )
        const byteArray = new Uint8Array(byteNumbers)

        const blob = new Blob([byteArray], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        })

        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', 'Error File - Budget Operating Hour.xlsx')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
          severity: 'error',
        })
      }

      return response
    } catch (error) {
      console.error('Error uploading xcel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

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
      downloadExcelBtn: true,
      uploadExcelBtn: true,
      showNoteWhileDeleting: false,
      showTitleNameBusiness: true,
      titleName: 'Gradewise Monthwise Budgeted Operating Hours',
      showCalculate: true,
      showCalculateVisibility: aopCalculation.length > 0,
    },
    isOldYear,
  )

  return (
    <div>
      <LoaderBackdrop open={!!loading} />
      {/* Line tabs */}
      {tabs.length > 0 && (
        <Box display='flex' alignItems='center' sx={{ mb: 1, mt: 1 }}>
          <AopTabs tabIndex={tabIndex} setTabIndex={setTabIndex} tabs={tabs} />
        </Box>
      )}

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
        handleCalculate={handleCalculate}
        currentRowId={currentRowId}
        permissions={adjustedPermissions}
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        handleExcelUpload={handleExcelUpload}
        disableRedHighlight={true}
        screenType='shutdown'
      />
      <CalculatedBusinessProposed
        permissions={permissions}
        lineDetails={lineDetails}
        tabIndex={tabIndex}
        refreshSignal={refreshSignal}
      />
    </div>
  )
}

export default BudgetOperatingHour
