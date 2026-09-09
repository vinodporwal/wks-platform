import React, { useEffect, useState, useCallback, useMemo } from 'react'
import { useSelector } from 'react-redux'
import getSiteAOPReportColumns from './columns/SiteReportColumns'
import { SiteReportDataService } from 'services/SiteReportDataService'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from '../index'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import Notification from 'components/Utilities/Notification'

const EnergyPerformance = ({ permissions, tabDisplayName }) => {
  const [modifiedCells, setModifiedCells] = useState({})
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { oldYear, siteObject, verticalChange, year, isReleased } =
    dataGridStore

  const SITE_ID = siteObject?.id
  const AOP_YEAR = year?.selectedYear
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || 'site'
  const SITE_NAME =
    siteObject?.name || siteObject?.siteName || siteObject?.displayName || ''
  const EXCEL_EXPORT_TITLE = `${SITE_NAME ? `${SITE_NAME}_` : ''}${tabDisplayName || 'Energy Performance'}_${AOP_YEAR}`

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
  const valueFormat = ValueFormatterConsumption()
  const columns = useMemo(() => {
    return getSiteAOPReportColumns({ AOP_YEAR, valueFormat, prev, next })
      .energyPerformance
  }, [AOP_YEAR, valueFormat, prev, next])

  // --- Fetch & Save Energy Performance ---
  const fetchData = useCallback(async () => {
    if (!SITE_ID || !AOP_YEAR) return

    setModifiedCells({})
    setLoading(true)
    try {
      const data = await SiteReportDataService.getEnergyPerformanceDetails(
        keycloak,
        SITE_ID,
        AOP_YEAR,
      )

      const formattedData = (data?.data?.Data || []).map((item, idx) => ({
        ...item,
        id: item.id || `temp-${idx + 1}`,
        idFromApi: item.id || null,
        masterId: item.masterId,
        sno: idx + 1,
        plant: item.plant,
        uom: item.uom,
        aopValue: item.aopValue ?? '',
        actualValue: item.actualValue ?? '',
        planValue: item.planValue ?? '',
        remark: item.remark ?? item.remarks ?? '',
        responsibility: item.remark ?? item.remarks ?? '',
        isEditable: true,
      }))

      setRows(formattedData)
    } catch (error) {
      console.error('Error fetching Energy Performance data:', error)
    } finally {
      setLoading(false)
    }
  }, [keycloak, SITE_ID, AOP_YEAR])

  const saveChanges = useCallback(async () => {
    try {
      setLoading(true)
      const data = Object.values(modifiedCells)
      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        setLoading(false)
        return
      }

      const payload = data.map((item) => ({
        id:
          item.idFromApi ||
          (item.id && !String(item.id).startsWith('temp-') ? item.id : null),
        masterId: item.masterId || null,
        plant: item.plant,
        uom: item.uom,
        aopValue:
          item.aopValue !== undefined &&
          item.aopValue !== null &&
          item.aopValue !== ''
            ? Number(item.aopValue)
            : null,
        actualValue:
          item.actualValue !== undefined &&
          item.actualValue !== null &&
          item.actualValue !== ''
            ? Number(item.actualValue)
            : null,
        planValue:
          item.planValue !== undefined &&
          item.planValue !== null &&
          item.planValue !== ''
            ? Number(item.planValue)
            : null,
        remark:
          item.responsibility !== undefined
            ? item.responsibility
            : item.remark || item.remarks || '',
        siteId: SITE_ID,
        aopYear: AOP_YEAR,
      }))

      const response = await SiteReportDataService.saveEnergyPerformance(
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
  }, [modifiedCells, keycloak, SITE_ID, AOP_YEAR, fetchData])

  // --- Excel Export & Import ---
  const downloadExcelForConfiguration = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })

    try {
      await SiteReportDataService.exportEnergyPerformance(
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
      const response = await SiteReportDataService.importEnergyPerformance(
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
        link.setAttribute('download', `Error_File_${EXCEL_EXPORT_TITLE}.xlsx`)
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

  useEffect(() => {
    fetchData()
  }, [fetchData])

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
      isOldYear: isOld,
      allAction: false,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: false,
      addButton: false,
      deleteButton: false,
      showUnit: permissions?.showUnit ?? false,
      saveWithRemark: false,
      saveBtn: permissions?.saveBtn ?? true,
      customHeight: permissions?.customHeight,
      allAction: true,
      downloadExcelBtn: permissions?.downloadExcelBtn ?? true,
      uploadExcelBtn: permissions?.uploadExcelBtn ?? true,
      showNoteWhileDeleting: false,
      showTitleNameBusiness: true,
      titleName: tabDisplayName || 'Energy Performance',
      ExcelName: EXCEL_EXPORT_TITLE,
      disableColWidth: true,
      makePagable: false,
    },
    isOldYear,
  )

  return (
    <>
      <LoaderBackdrop open={!!loading} />
      <KendoDataTables
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        columns={columns}
        rows={rows}
        setRows={setRows}
        saveChanges={saveChanges}
        fetchData={fetchData}
        title='B3.4. Energy Performance'
        permissions={adjustedPermissions}
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

export default EnergyPerformance
