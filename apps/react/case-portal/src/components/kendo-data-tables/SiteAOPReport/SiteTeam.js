import React, { useEffect, useState, useCallback } from 'react'
import { useSelector } from 'react-redux'
import getSiteAOPReportColumns from './columns/SiteReportColumns'
import { SiteTeamDataService } from './data-service/SiteTeamDataService'
import { useSession } from 'SessionStoreContext'
import { validateFields } from 'utils/validationUtils'
import KendoDataTables from '../index'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import Notification from 'components/Utilities/Notification'

const SiteTeam = ({ permissions, tabDisplayName }) => {
  const [modifiedCells, setModifiedCells] = useState({})
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    oldYear,
    plantObject,
    siteObject,
    verticalObject,
    verticalChange,
    year,
    isReleased,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const vertName = verticalChange?.selectedVertical
  const SITE_NAME =
    siteObject?.name || siteObject?.siteName || siteObject?.displayName || ''
  const EXCEL_EXPORT_TITLE = `${SITE_NAME ? `${SITE_NAME}_` : ''}${tabDisplayName || 'Site Team'}_${AOP_YEAR}`

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
  const columns = getSiteAOPReportColumns({ AOP_YEAR, valueFormat, prev, next })

  const fetchData = useCallback(async () => {
    if (!SITE_ID || !AOP_YEAR) return

    setModifiedCells({})
    setLoading(true)
    try {
      const data = await SiteTeamDataService.getSiteTeamDetails(
        keycloak,
        SITE_ID,
        AOP_YEAR,
      )

      const formattedData = (data?.data?.Data || []).map((item, idx) => ({
        ...item,
        id: item.id || `temp_${item.masterId || idx + 1}`,
        idFromAPI: item.id || null,
        sno: idx + 1,
      }))

      setRows(formattedData)
    } catch (error) {
      console.error('Error fetching Site Team data:', error)
    } finally {
      setLoading(false)
    }
  }, [keycloak, SITE_ID, AOP_YEAR])

  const parseOptionalInt = (val) => {
    if (val === undefined || val === null || val === '') return null
    const num = Number(val)
    return isNaN(num) ? null : num
  }

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
        return
      }

      const requiredFields = ['functions', 'jobRole']
      const validationMessage = validateFields(data, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationMessage,
          severity: 'error',
        })
        return
      }

      const payload = data.map((item) => ({
        id:
          item.id && !String(item.id).startsWith('temp_')
            ? item.id
            : item.idFromAPI || null,
        masterId: item.masterId || null,
        functions: item.functions,
        jobRole: item.jobRole,
        name: item.name || null,
        age: parseOptionalInt(item.age),
        teamSize: parseOptionalInt(item.teamSize),
        siteId: item.siteId || SITE_ID,
        aopYear: item.aopYear || AOP_YEAR,
      }))

      const response = await SiteTeamDataService.saveSiteTeam(
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

  const downloadExcelForConfiguration = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })

    try {
      await SiteTeamDataService.SiteTeamExport(
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
      const response = await SiteTeamDataService.ImportSiteTeamExcel(
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
      showAction: permissions?.showAction ?? true,
      showUnit: permissions?.showUnit ?? false,
      saveWithRemark: false,
      saveBtn: permissions?.saveBtn ?? true,
      customHeight: permissions?.customHeight,
      allAction: true,
      downloadExcelBtn: true,
      uploadExcelBtn: true,
      showNoteWhileDeleting: false,
      showTitleNameBusiness: true,
      titleName: tabDisplayName || 'Site Team',
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
      <KendoDataTables
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        setRows={setRows}
        columns={columns.siteTeam}
        rows={rows}
        fetchData={fetchData}
        saveChanges={saveChanges}
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

export default SiteTeam
