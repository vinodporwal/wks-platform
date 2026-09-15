import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Box } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useSession } from 'SessionStoreContext'
import { SiteReportDataService } from 'services/SiteReportDataService'
import KendoDataTables from '../index'
import { useSelector } from 'react-redux'
import { validateFields } from 'utils/validationUtils'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

export default function ConversionVariableCost({
  permissions,
  tabDisplayName,
}) {
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

  function getAopShortYears(aopYear) {
    if (!aopYear) return { prev: '', next: '' }
    const match = aopYear.match(/(\d{4})-(\d{2})/)
    if (match) {
      const prev = match[1].slice(-2)
      const next = match[2]
      return { prev, next }
    }
    const year = String(aopYear).slice(-2)
    return { prev: year, next: String(Number(year) + 1).padStart(2, '0') }
  }
  const { prev, next } = getAopShortYears(AOP_YEAR)

  const conversionVariableColumns = [
    {
      field: 'sno',
      title: 'S.No',
      minWidth: 30,
      editable: false,
      align: 'right',
      format: '{0:0}',
      locked: true,
    },

    {
      field: 'costType',
      title: 'Cost Head',
      minWidth: 100,
      editable: false,
      locked: true,
    },
    {
      field: 'previousAop',
      title: `FY${prev} AOP`,
      editable: false,
      type: 'number',
      minWidth: 150,
      format: '{0:0.00}',
    },
    {
      field: 'previousActual',
      title: `FY${prev} Actual`,
      editable: false,
      type: 'number',
      minWidth: 150,
      format: '{0:0.00}',
    },
    {
      field: 'currentAop',
      title: `FY${next} AOP`,
      editable: false,
      type: 'number',
      minWidth: 150,
      format: '{0:0.00}',
    },
    {
      field: 'remark',
      title: 'Rationale/ Reasons',
      minWidth: 350,
      editable: true,
    },
  ]

  const columns = useMemo(() => {
    return conversionVariableColumns
  }, [AOP_YEAR, prev, next])

  const fetchData = useCallback(async () => {
    if (!SITE_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const res = await SiteReportDataService.getConversionVariableCost(
        keycloak,
        SITE_ID,
        AOP_YEAR,
      )

      if (res?.code === 200) {
        // Handle variations of data wrapping
        const dataList = Array.isArray(res?.data)
          ? res.data
          : Array.isArray(res?.data?.Data)
            ? res.data.Data
            : []

        const mapped = dataList.map((item, index) => ({
          ...item,
          id: item?.id || null,
          sno: index + 1,
          idFromApi: item?.id || null,
          originalRemark: item.remark,
          remark: item.remark,
          plantName: item?.plantName || item?.plant || item?.Plant || '',
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

      const requiredFields = ['remark']

      const validationMessage = validateFields(data, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({ message: validationMessage, severity: 'error' })
        setLoading(false)
        return
      }

      // Send only id and remark in payload
      const payload = data.map((item) => ({
        id: item?.id || null,
        remark: item?.remark ?? item?.remarks ?? '',
      }))

      const response = await SiteReportDataService.saveConversionVariableCost(
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

  const deleteRowData = async (paramsForDelete) => {
    setLoading(true)

    try {
      const { idFromApi, id } = paramsForDelete
      const deleteId = id

      if (!idFromApi) {
        setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
      }

      if (idFromApi) {
        await SiteReportDataService.deleteTechnicalAvailability(
          idFromApi,
          keycloak,
        )
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

  const handleLoad = async () => {
    if (!SITE_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const data = await SiteReportDataService.loadConversionVariableCost(
        keycloak,
        SITE_ID,
        AOP_YEAR,
      )
      if (data?.code === 200 || data === 0 || data?.data >= 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data loaded successfully!',
          severity: 'success',
        })
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: data?.message || 'Data Load Failed!',
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
      showLoadBtn: false,
      isOldYear: isOldYear,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      allAction: true,
      saveBtn: true,
      showTitleNameBusiness: true,
      titleName: tabDisplayName || 'Conversion, Contribution & Variable Cost',
      adjustedPermissions: true,
      ExcelName: `${lowerVertName}_Conversion_Contribution_Variable_Cost_${AOP_YEAR}`,
      makePagable: false,
      showLoadBtn: true,
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
        title='Conversion, Contribution & Variable Cost'
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
        handleLoad={handleLoad}
        handleRemarkCellClick={handleRemarkCellClick}
        permissions={adjustedPermissions}
        deleteRowData={deleteRowData}
        groupBy={'plantName'}
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
