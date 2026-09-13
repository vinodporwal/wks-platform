import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Box } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useSession } from 'SessionStoreContext'
import { SiteReportDataService } from 'services/SiteReportDataService'
import KendoDataTables from '../index'
import { useSelector } from 'react-redux'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from 'utils/excelNameUtil'
import { getRoleName } from 'services/role-service'

export default function FixedExpenses({ permissions, tabDisplayName }) {
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
    isReleased,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id

  const SCREEN_NAME = screenTitle?.title
  const AOP_YEAR = year?.selectedYear
  const thisYear = AOP_YEAR
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)

  const [modifiedCells, setModifiedCells] = useState({})
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const EXCEL_EXPORT_TITLE = generateExcelName(
    dataGridStore,
    tabDisplayName || 'Fixed Expenses',
  )

  function getAopShortYears(aopYear) {
    if (!aopYear) return { prev: '', next: '' }
    const match = aopYear.match(/(\d{4})-(\d{2})/)
    if (match) {
      const prev = match[1].slice(-2)
      const next = match[2]
      return { prev, next }
    }
    const y = String(aopYear).slice(-2)
    return { prev: y, next: String(Number(y) + 1).padStart(2, '0') }
  }
  const { prev, next } = getAopShortYears(AOP_YEAR)

  const columns = useMemo(
    () => [
      {
        field: 'id',
        title: 'ID',
        editable: false,
        hidden: true,
        isVisible: false,
      },
      {
        field: 'srNo',
        title: 'Sr. No.',
        width: 70,
        editable: false,
        align: 'right',
        headerAlign: 'right',
        format: '{0:0}',
        type: 'number',
      },
      {
        field: 'particular',
        title: 'Particulars',
        minWidth: 180,
        editable: false,
      },
      {
        field: 'fyPrevAOP',
        title: `FY${prev} AOP`,
        editable: true,
        type: 'number',
        format: '{0:0.00}',
        minWidth: 120,
        align: 'right',
        headerAlign: 'right',
      },
      {
        field: 'fyPrevActual',
        title: `FY${prev} Actual`,
        editable: true,
        type: 'number',
        format: '{0:0.00}',
        minWidth: 120,
        align: 'right',
        headerAlign: 'right',
      },
      {
        field: 'fyCurrAOP',
        title: `FY${next} AOP`,
        editable: true,
        type: 'number',
        format: '{0:0.00}',
        minWidth: 120,
        align: 'right',
        headerAlign: 'right',
      },
      {
        field: 'percentageChange',
        title: '% Change',
        editable: true,
        type: 'number',
        format: '{0:0.00}',
        minWidth: 110,
        align: 'right',
        headerAlign: 'right',
      },
      {
        field: 'variance',
        title: 'Variance',
        editable: true,
        type: 'number',
        format: '{0:0.00}',
        minWidth: 110,
        align: 'right',
        headerAlign: 'right',
      },
      {
        field: 'responsibility',
        title: 'Remarks',
        minWidth: 200,
        editable: true,
      },
    ],
    [prev, next],
  )

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setModifiedCells({})
    setLoading(true)
    try {
      const res = await SiteReportDataService.getFixedExpensesData(
        keycloak,
        SITE_ID,
        AOP_YEAR,
      )

      if (res?.code === 200) {
        const mapped = (res?.data?.Data || []).map((item, index) => ({
          id: item.id || index + 1,
          srNo: index + 1,
          particular: item.particulars,
          fyPrevAOP: item.fyPrevAOP,
          fyPrevActual: item.fyPrevActual,
          fyCurrAOP: item.fyCurrAOP,
          percentageChange: item.percentageChange,
          variance: item.variance,
          remarks: item.remarks,
          responsibility: item.remarks || '',
          siteId: item.siteId,
          aopYear: item.aopYear,
          updatedBy: item.updatedBy,
          updatedDate: item.updatedDate,
          idFromApi: item.id || null,
          isEditable: true,
          isdisabled: false,
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
  }, [keycloak, yearChanged, plantID, SITE_ID, AOP_YEAR])

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

      const payload = data.map((item) => ({
        id: item.idFromApi || null,
        particulars: item.particular,
        fyPrevAOP: item.fyPrevAOP,
        fyPrevActual: item.fyPrevActual,
        fyCurrAOP: item.fyCurrAOP,
        percentageChange: item.percentageChange,
        variance: item.variance,
        remarks:
          item.responsibility !== undefined
            ? item.responsibility
            : item.remarks || 'system generated',
        siteId: SITE_ID,
        aopYear: AOP_YEAR,
        updatedBy: keycloak?.userName || 'system',
        updatedDate: new Date().toISOString(),
      }))

      const response = await SiteReportDataService.saveFixedExpensesData(
        keycloak,
        SITE_ID,
        AOP_YEAR,
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
  }, [modifiedCells, keycloak, PLANT_ID, AOP_YEAR, SITE_ID, fetchData])

  const deleteRowData = async (paramsForDelete) => {
    setLoading(true)

    try {
      const { idFromApi, id } = paramsForDelete
      const deleteId = id

      if (!idFromApi) {
        setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
      }

      if (idFromApi) {
        await SiteReportDataService.deleteFixedExpensesData(idFromApi, keycloak)
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
    }
  }

  const downloadExcelForConfiguration = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })

    try {
      await SiteReportDataService.exportFixedExpensesData(
        keycloak,
        SITE_ID,
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
      const response = await SiteReportDataService.importFixedExpensesData(
        rawFile,
        keycloak,
        SITE_ID,
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
        link.setAttribute(
          'download',
          `Error_File_Fixed_Expenses_${AOP_YEAR}.xlsx`,
        )
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
      showLoadBtn: false,
      isOldYear: isOldYear,
      allAction: false,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: permissions?.showAction ?? true,
      showUnit: permissions?.showUnit ?? false,
      saveWithRemark: false,
      saveBtn: permissions?.saveBtn ?? true,
      allAction: true,
      downloadExcelBtn: permissions?.downloadExcelBtn ?? true,
      uploadExcelBtn: permissions?.uploadExcelBtn ?? true,
      showLoadBtn: false,
      showNoteWhileDeleting: false,
      showTitleNameBusiness: true,
      titleName: tabDisplayName || 'Fixed Expenses',
      ExcelName: `${lowerVertName}_Fixed_Expenses_${AOP_YEAR}`,
      addButton: false,
      deleteButton: false,
      disableColWidth: true,
      makePagable: false,
    },
    isOldYear,
  )

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <KendoDataTables
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        setRows={setRows}
        columns={columns}
        rows={rows}
        fetchData={fetchData}
        saveChanges={saveChanges}
        handleLoad={fetchData}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        permissions={adjustedPermissions}
        disableRedHighlight={true}
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        handleExcelUpload={handleExcelUpload}
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

