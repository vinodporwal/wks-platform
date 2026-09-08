import React, { useEffect, useState, useCallback } from 'react'
import { useSelector } from 'react-redux'
import getSiteAOPReportColumns from 'components/colums/SiteReportColums'
import { SiteReportDataService } from 'services/SiteReportDataService'
import { useSession } from 'SessionStoreContext'
import { validateFields } from 'utils/validationUtils'
import KendoDataTables from '../index'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import PerformanceHighlights from './Utilities/PerformanceHighlights'

const EnergyPerformance = ({ permissions }) => {
  const [modifiedCells, setModifiedCells] = useState({})
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  // Remark dialog state
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // Performance Highlights state
  const [performanceSummary, setPerformanceSummary] = useState('')
  const [performanceId, setPerformanceId] = useState(null)
  const [performanceHighlightsEdited, setPerformanceHighlightsEdited] =
    useState(false)

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    oldYear,
    plantObject,
    siteObject,
    verticalObject,
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

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  // --- Fetch & Save Energy Performance ---
  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !SITE_ID || !VERTICAL_ID || !AOP_YEAR) return

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
        sno: idx + 1,
        originalRemark: item.remark,
      }))

      setRows(formattedData)
    } catch (error) {
      console.error('Error fetching Energy Performance data:', error)
    } finally {
      setLoading(false)
    }
  }, [keycloak, SITE_ID, VERTICAL_ID, PLANT_ID, AOP_YEAR])

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

      const requiredFields = ['remark']
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
        id: item.id || null,
        plant: item.plant,
        uom: item.uom,
        aopValue: item.aopValue,
        actualValue: item.actualValue,
        planValue: item.planValue,
        remark: item.remark,
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

  // --- Performance Highlights Summary ---
  const getPerformanceHighlights = useCallback(async () => {
    if (!PLANT_ID || !SITE_ID || !AOP_YEAR) return

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
  }, [keycloak, SITE_ID, PLANT_ID, AOP_YEAR])

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
      isOldYear: isOld,
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
      titleName: 'Energy Performance',
      uploadExcelBtn: false,
    },
    isOldYear,
  )

  return (
    <>
      <LoaderBackdrop open={!!loading} />
      <KendoDataTables
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        columns={columns.energyPerformance}
        rows={rows}
        setRows={setRows}
        saveChanges={saveChanges}
        fetchData={fetchData}
        title='B3.4. Energy Performance'
        permissions={adjustedPermissions}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        snackbarData={snackbarData}
        setSnackbarData={setSnackbarData}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
      />
      <PerformanceHighlights
        performanceSummary={performanceSummary}
        setPerformanceSummary={setPerformanceSummary}
        performanceHighlightsEdited={performanceHighlightsEdited}
        setPerformanceHighlightsEdited={setPerformanceHighlightsEdited}
        savePerformanceHighlightsSummary={savePerformanceHighlightsSummary}
        readOnly={READ_ONLY}
      />
    </>
  )
}

export default EnergyPerformance
