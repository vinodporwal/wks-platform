import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import KendoDataTablesReports from 'components/kendo-data-tables/index-reports'
import { Backdrop, Box, CircularProgress } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useSession } from 'SessionStoreContext'
import { DataService } from 'services/DataService'
import { SiteReportDataService } from 'services/SiteReportDataService'
import KendoDataTables from './index'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { useSelector } from 'react-redux'
import { add } from 'lodash'
import { validateFields } from 'utils/validationUtils'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from 'utils/excelNameUtil'
import { formatDate } from 'utils/dateUtils'
export default function Capex({ permissions, tabDisplayName }) {
  const keycloak = useSession()
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

  const SCREEN_NAME = screenTitle?.title
  const AOP_YEAR = year?.selectedYear
  const thisYear = AOP_YEAR
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [modifiedCells, setModifiedCells] = useState({})
  const [enableSaveAddBtn, setEnableSaveAddBtn] = useState(false)
  const [deleteId, setDeleteId] = useState(null)
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
  const EXCEL_EXPORT_TITLE = generateExcelName(
    dataGridStore,
    tabDisplayName || 'Capex/PIO Plan',
  )

  const unsavedChangesRef = useRef({ unsavedRows: {}, rowsBeforeChange: {} })

  const capexPlanColumns = [
    {
      field: 'id',
      title: 'ID',
      editable: false,
      hidden: true,
    },
    {
      field: 'sno',
      title: 'S.No',
      widthT: 60,
      editable: false,
      align: 'right',
      format: '{0:0}',
    },
    { field: 'proposal', title: 'Proposal', editable: true },
    { field: 'category', title: 'Category', editable: true },
    {
      field: 'justification',
      title: 'Justification',
      editable: true,
    },
    {
      field: 'costRsCr',
      title: 'Cost (Rs Cr)',
      editable: true,
      type: 'number',
    },
    {
      field: 'benefitRsCr',
      title: 'Benefit (Rs Cr)',
      editable: true,
      type: 'number',
    },
    {
      field: 'targetPlan',
      title: 'Target',
      editable: true,
      type: 'date',
    },
    { field: 'statusPlan', title: 'Status', editable: true },
    // { field: 'remarks', title: 'Remarks', widthT: 100, editable: true },
  ]

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const res = await SiteReportDataService.getCapexData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (res?.code === 200) {
        const mapped = res?.data?.Data?.map((item, index) => ({
          id: item.id || null,
          sno: index + 1,
          proposal: item.proposal,
          category: item.category,
          justification: item.justification,
          costRsCr: item.costRsCr,
          benefitRsCr: item.benefitRsCr,
          targetPlan: item.targetPlan,
          statusPlan: item.statusPlan,
          // remarks: item.remarks,
          siteId: item.siteId,
          aopYear: item.aopYear,
          updatedBy: item.updatedBy,
          updatedDate: item.updatedDate,
          idFromApi: item.id || null,
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
  }, [keycloak, yearChanged, PLANT_ID])

  useEffect(() => {
    fetchData()
  }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak])

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

      const requiredFields = ['proposal']

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
        id: item.id || null,
        proposal: item.proposal,
        category: item.category,
        justification: item.justification,
        costRsCr: item.costRsCr || 0,
        benefitRsCr: item.benefitRsCr,
        targetPlan: item.targetPlan ? formatDate(item.targetPlan) : '',
        statusPlan: item.statusPlan,
        remarks: item.remarks || 'system generated',
        siteId: SITE_ID,
        aopYear: AOP_YEAR,
        updatedBy: keycloak?.userName || 'system',
        updatedDate: new Date().toISOString(),
      }))

      // 3. Save to API
      const response = await SiteReportDataService.saveCapexData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        payload,
      )

      // 4. Handle API response
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

  const deleteRowData = async (paramsForDelete) => {
    setLoading(true)

    try {
      const { idFromApi, id } = paramsForDelete
      const deleteId = id

      if (!idFromApi) {
        setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
      }

      if (idFromApi) {
        await SiteReportDataService.deleteCapex(keycloak, idFromApi)
        setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Record Deleted successfully!',
          severity: 'success',
        })
        fetchData()
      } else {
        setLoading(false)
      }
    } catch (error) {
      console.error('Error deleting Record!', error)
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

    try {
      await SiteReportDataService.exportCapexData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_EXPORT_TITLE,
      )
    } catch (error) {
      console.error('Error downloading Excel:', error)
      setSnackbarData({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
      setSnackbarOpen(true)
    }
  }

  const handleExcelUpload = async (rawFile) => {
    setLoading(true)
    try {
      const response = await SiteReportDataService.importCapexData(
        rawFile,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Uploaded Successfully!',
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
        link.setAttribute('download', `Error_File_Capex_Plan_${AOP_YEAR}.xlsx`)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message:
            response?.message || 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        setModifiedCells({})
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Upload Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error uploading excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred during upload!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleRemarkCellClick = useCallback((row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }, [])

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
      allAction: true,
      showTitleNameBusiness: true,
      titleName: tabDisplayName || 'Capex/PIO Plan',
      adjustedPermissions: true,
      ExcelName: `${lowerVertName}_Capex_Plan_${AOP_YEAR}`,
      saveBtn: permissions?.saveBtn ?? true,
      addButton: permissions?.addButton ?? true,
      deleteButton: permissions?.deleteButton ?? true,
      downloadExcelBtn: permissions?.downloadExcelBtn ?? true,
      uploadExcelBtn: permissions?.uploadExcelBtn ?? true,
    },
    isOldYear,
  )

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <KendoDataTables
        columns={capexPlanColumns}
        rows={rows}
        setRows={setRows}
        title='Capex Plan'
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
        handleRemarkCellClick={handleRemarkCellClick}
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        handleExcelUpload={handleExcelUpload}
        deleteRowData={deleteRowData}
        deleteId={deleteId}
        setDeleteId={setDeleteId}
        permissions={adjustedPermissions}
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
