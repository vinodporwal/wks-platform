import React, { useEffect, useState, useMemo, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from '../index'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import Notification from 'components/Utilities/Notification'
import { SiteSafetyPerformanceTargetDataService } from './data-service/SiteSafetyPerformanceTargetDataService'
import { SiteReportDataService } from 'services/SiteReportDataService'
import PerformanceHighlights from './Utilities/PerformanceHighlights'

const SiteSafetyPerformanceTarget = ({ permissions, tabDisplayName }) => {
  const [modifiedCells, setModifiedCells] = useState({})
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  // Performance Highlights state
  const [performanceSummary, setPerformanceSummary] = useState('')
  const [performanceId, setPerformanceId] = useState(null)
  const [performanceHighlightsEdited, setPerformanceHighlightsEdited] =
    useState(false)

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    oldYear,
    siteObject,
    verticalObject,
    year,
    isReleased,
  } = dataGridStore

  const SITE_ID = siteObject?.id
  const AOP_YEAR = year?.selectedYear
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const vertName = verticalChange?.selectedVertical
  const SITE_NAME =
    siteObject?.name || siteObject?.siteName || siteObject?.displayName || ''
  const EXCEL_EXPORT_TITLE = `${SITE_NAME ? `${SITE_NAME}_` : ''}${tabDisplayName || 'Safety Performance & Targets'}_${AOP_YEAR}`

  const keycloak = useSession()
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

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
        title: 'Id',
        editable: false,
        hidden: true,
        isVisible: false,
      },
      {
        field: 'masterId',
        title: 'Master Id',
        editable: false,
        hidden: true,
        isVisible: false,
      },
      {
        field: 'serialNumber',
        title: 'S.No.',
        width: 80,
        editable: false,
        minWidth: 70,
        align: 'right',
        headerAlign: 'right',
      },
      {
        field: 'kpiName',
        title: 'KPI',
        editable: false,
        minWidth: 220,
      },
      {
        field: 'uom',
        title: 'UOM',
        width: 90,
        minWidth: 70,
        editable: false,
      },
      {
        field: 'prevAOP',
        title: `FY${prev} AOP`,
        editable: true,
        type: 'number',
        minWidth: 120,
        align: 'right',
        headerAlign: 'right',
      },
      {
        field: 'prevActual',
        title: `FY${prev} ACT`,
        editable: true,
        type: 'number',
        minWidth: 120,
        align: 'right',
        headerAlign: 'right',
      },
      {
        field: 'currentPlan',
        title: `FY${next} Plan`,
        editable: true,
        type: 'number',
        minWidth: 120,
        align: 'right',
        headerAlign: 'right',
      },
      {
        field: 'responsibility',
        title: 'Remarks',
        editable: true,
        minWidth: 180,
      },
    ],
    [AOP_YEAR, prev, next],
  )

  const fetchData = useCallback(async () => {
    if (!SITE_ID || !AOP_YEAR) return
    setModifiedCells({})
    setLoading(true)
    try {
      const res =
        await SiteSafetyPerformanceTargetDataService.getSiteSafetyPerformanceTargets(
          keycloak,
          SITE_ID,
          AOP_YEAR,
        )

      if (res?.code === 200) {
        const mapped = (res?.data?.Data || []).map((item, index) => {
          const isActEditable =
            item?.isEditable === true ||
            item?.isEditable === 'true' ||
            item?.isEditable === 1
          return {
            ...item,
            id: item.id || `temp-${index + 1}`,
            idFromAPI: item.id || null,
            masterId: item.masterId,
            serialNumber: item.displayOrder || index + 1,
            kpiName: item.kpiName || item.kpi,
            uom: item.uom,
            prevAOP: item.prevAOP ?? '',
            prevActual: item.prevActual ?? '',
            currentPlan: item.currentPlan ?? '',
            remark: item.remark ?? item.remarks ?? '',
            responsibility: item.remark ?? item.remarks ?? '',
            isEditable: true,
            currentPlanEditable: isActEditable,
          }
        })
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
  }, [keycloak, SITE_ID, AOP_YEAR])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const parseOptionalNumber = (val) => {
    if (val === undefined || val === null || val === '') return null
    const num = Number(val)
    return isNaN(num) ? null : num
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

      const payload = data.map((item) => ({
        id:
          item.idFromAPI ||
          (typeof item.id === 'string' && item.id.startsWith('temp-')
            ? null
            : item.id) ||
          null,
        masterId: item.masterId,
        kpiName: item.kpiName,
        uom: item.uom,
        prevAOP: parseOptionalNumber(item.prevAOP),
        prevActual: parseOptionalNumber(item.prevActual),
        currentPlan: parseOptionalNumber(item.currentPlan),
        remark: item.responsibility ?? item.remark ?? '',
        siteFkId: SITE_ID,
        aopYear: AOP_YEAR,
      }))

      const response =
        await SiteSafetyPerformanceTargetDataService.saveSiteSafetyPerformanceTargets(
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
      console.error('Error saving Site Safety data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, keycloak, SITE_ID, AOP_YEAR, fetchData])

  const handleLoad = async () => {
    setLoading(true)
    try {
      const data =
        await SiteSafetyPerformanceTargetDataService.handleLoadSiteSafetyTarget(
          keycloak,
          SITE_ID,
          AOP_YEAR,
        )
      if (data?.code === 200 || data === 0 || data?.data >= 0) {
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
        message: error.message || 'An error occurred during load',
        severity: 'error',
      })
      console.error('Error in handleLoad:', error)
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
      await SiteSafetyPerformanceTargetDataService.SiteSafetyPerformanceExport(
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
    } finally {
      setSnackbarOpen(true)
    }
  }

  const handleExcelUpload = async (rawFile) => {
    setLoading(true)
    try {
      const response =
        await SiteSafetyPerformanceTargetDataService.ImportSiteSafetyPerformanceExcel(
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
        link.setAttribute('download', `Error File - ${EXCEL_EXPORT_TITLE}.xlsx`)
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

  // --- Performance Highlights Summary ---
  const getPerformanceHighlights = useCallback(async () => {
    if (!SITE_ID || !AOP_YEAR) return

    try {
      setPerformanceSummary('')
      setPerformanceId(null)

      const res = await SiteReportDataService.getPerformanceHighlightsSummary(
        keycloak,
        SITE_ID,
        AOP_YEAR,
      )

      if (res?.code === 200 && res?.data?.Data?.length > 0) {
        const record = res.data.Data[0]
        setPerformanceSummary(record.summary || '')
        setPerformanceId(record.id || null)
      } else {
        setPerformanceSummary('')
        setPerformanceId(null)
      }
    } catch (error) {
      setPerformanceSummary('')
      setPerformanceId(null)
      console.error('Error fetching summary:', error)
    }
  }, [keycloak, SITE_ID, AOP_YEAR])

  const savePerformanceHighlightsSummary = async () => {
    try {
      const payload = [
        {
          id: performanceId,
          summary: performanceSummary,
          saveStatus: null,
        },
      ]

      const res = await SiteReportDataService.savePerformanceHighlightsSummary(
        keycloak,
        SITE_ID,
        AOP_YEAR,
        payload,
      )

      if (res?.code === 200 || res?.code === 207) {
        setSnackbarData({
          message:
            res?.code === 200
              ? 'Saved Successfully!'
              : 'Saved with minor issues',
          severity: res?.code === 200 ? 'success' : 'warning',
        })
        setPerformanceHighlightsEdited(false)
        setSnackbarOpen(true)
      } else {
        setSnackbarData({
          message: 'Save Failed!',
          severity: 'error',
        })
        setSnackbarOpen(true)
      }
    } catch (error) {
      setSnackbarData({
        message: 'Error saving summary!',
        severity: 'error',
      })
      setSnackbarOpen(true)
    }
  }

  useEffect(() => {
    fetchData()
    getPerformanceHighlights()
  }, [fetchData, getPerformanceHighlights])

  const getAdjustedPermissions = (perms, isOld) => {
    if (isOld != 1) return perms
    return {
      ...perms,
      showAction: false,
      addButton: false,
      deleteButton: false,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      showLoadBtn: false,
      isOldYear: isOld,
      allAction: false,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: permissions?.showAction ?? true,
      showUnit: permissions?.showUnit ?? false,
      saveWithRemark: false,
      saveBtn: permissions?.saveBtn ?? true,
      customHeight: permissions?.customHeight,
      allAction: true,
      downloadExcelBtn: true,
      uploadExcelBtn: true,
      showLoadBtn: true,
      showNoteWhileDeleting: false,
      showTitleNameBusiness: true,
      titleName: tabDisplayName || 'Site Safety Performance & Targets',
      ExcelName: EXCEL_EXPORT_TITLE,
      addButton: false,
      deleteButton: false,
      disableColWidth: true,
      makePagable: false,
    },
    isOldYear,
  )

  return (
    <>
      <LoaderBackdrop open={!!loading} />
      <PerformanceHighlights
        performanceSummary={performanceSummary}
        setPerformanceSummary={setPerformanceSummary}
        performanceHighlightsEdited={performanceHighlightsEdited}
        setPerformanceHighlightsEdited={setPerformanceHighlightsEdited}
        savePerformanceHighlightsSummary={savePerformanceHighlightsSummary}
        readOnly={READ_ONLY}
      />
      <KendoDataTables
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        setRows={setRows}
        columns={columns}
        rows={rows}
        fetchData={fetchData}
        saveChanges={saveChanges}
        handleLoad={handleLoad}
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
    </>
  )
}

export default SiteSafetyPerformanceTarget
