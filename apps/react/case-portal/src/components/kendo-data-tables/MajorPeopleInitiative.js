import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Backdrop, Box, CircularProgress } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useSession } from 'SessionStoreContext'
import { SiteReportDataService } from 'services/SiteReportDataService'
import KendoDataTables from './index'
import { useSelector } from 'react-redux'
import { validateFields } from 'utils/validationUtils'
import getSiteAOPReportColumns from 'components/colums/SiteReportColums'
import { formatDate } from 'utils/dateUtils'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

export default function MajorPeopleInitiative() {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { verticalChange, siteObject, year } = dataGridStore

  const SITE_ID = siteObject?.id
  const AOP_YEAR = year?.selectedYear
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [modifiedCells, setModifiedCells] = useState({})
  const [enableSaveAddBtn, setEnableSaveAddBtn] = useState(false)
  const isOldYear = false
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const columns = useMemo(() => {
    const cols = getSiteAOPReportColumns({ AOP_YEAR }).majorPeopleInitiative
    return cols.map((col) => {
      return col
    })
  }, [AOP_YEAR])

  const fetchData = useCallback(async () => {
    if (!SITE_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const res = await SiteReportDataService.getMajorPeopleInitiative(
        keycloak,
        SITE_ID,
        AOP_YEAR,
      )

      if (res?.code === 200) {
        const mapped = res?.data?.majorPeopleInitiativeList?.map(
          (item, index) => ({
            ...item,
            id: item.id || index + 1,
            sno: index + 1,
            idFromApi: item.id || null,
          }),
        )
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

  const saveChanges = React.useCallback(async () => {
    try {
      setLoading(true)
      const data = Object.values(modifiedCells)
      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
        return
      }

      const requiredFields = ['initiativeDescription', 'remark']
      const validationMessage = validateFields(data, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({ message: validationMessage, severity: 'error' })
        setLoading(false)
        return
      }

      const payload = data.map((item) => {
        const {
          id,
          initiativeDescription,
          outcome,
          recommendation,
          remark,
          targetDate,
        } = item

        return {
          id,
          initiativeDescription,
          outcome,
          recommendation,
          remark,
          targetDate: formatDate(
            targetDate ? new Date(targetDate) : new Date(),
          ),
        }
      })

      const response = await SiteReportDataService.saveMajorPeopleInitiative(
        keycloak,
        SITE_ID,
        AOP_YEAR,
        payload,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'Saved Successfully!', severity: 'success' })
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

  const handleRemarkCellClick = useCallback((row) => {
    setCurrentRemark(row.remark || '')
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
      saveBtn: true,
      showTitleNameBusiness: true,
      titleName: 'Major People Initiative',
      adjustedPermissions: true,
      ExcelName: `${lowerVertName}_Major_People_Initiative_${AOP_YEAR}`,
      // addButton: true,
      // deleteButton: true,
    },
    isOldYear,
  )

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <KendoDataTables
        columns={columns}
        rows={rows}
        setRows={setRows}
        title='B5. Major People Initiative FY27 (Max 5)'
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
