import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Box } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useSession } from 'SessionStoreContext'
import { SiteReportDataService } from 'services/SiteReportDataService'
import KendoDataTables from '../index'
import { useSelector } from 'react-redux'
import { formatDate } from 'utils/dateUtils'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from 'utils/excelNameUtil'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import getSiteAOPReportColumns from './columns/SiteReportColumns'

export default function MajorProfitInitiative({ permissions, tabDisplayName }) {
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
  const [plantOptions, setPlantOptions] = useState([])
  const [enableSaveAddBtn, setEnableSaveAddBtn] = useState(false)
  const [deleteId, setDeleteId] = useState(null)
  const isOldYear = false
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const ExcelExportTitle = generateExcelName(
    dataGridStore,
    tabDisplayName || 'Major Profit and Operability Improvement',
  )
  const valueFormat = ValueFormatterConsumption()

  const columns = useMemo(() => {
    const cols = getSiteAOPReportColumns({
      AOP_YEAR,
      valueFormat,
    }).majorProfitInitiative
    return cols
  }, [AOP_YEAR, valueFormat])

  // Fetch plant dropdown for this site
  const fetchPlantDropdown = useCallback(async () => {
    if (!SITE_ID) return
    try {
      const res = await SiteReportDataService.getPlantDropdownForSiteAOPReport(
        keycloak,
        SITE_ID,
      )
      const data = res?.data?.plants || res?.data?.Data || res?.data || []
      const formatted = Array.isArray(data)
        ? data.map((p) => ({
            id: p.id || p.Id,
            name:
              p.plantDisplayName ||
              p.name ||
              p.displayName ||
              p.plantName ||
              '',
            value:
              p.plantDisplayName ||
              p.name ||
              p.displayName ||
              p.plantName ||
              '',
            plantName: p.plantName || p.name || '',
            plantDisplayName: p.plantDisplayName || p.displayName || '',
          }))
        : []
      setPlantOptions(formatted)
    } catch (err) {
      console.error('Error fetching plant dropdown:', err)
      setPlantOptions([])
    }
  }, [keycloak, SITE_ID])

  const fetchData = useCallback(async () => {
    if (!SITE_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const res = await SiteReportDataService.getMajorProfitImprovement(
        keycloak,
        SITE_ID,
        AOP_YEAR,
      )

      if (res?.code === 200) {
        const mapped = res?.data?.map((item, index) => ({
          ...item,
          id: item.id || index + 1,
          sno: index + 1,
          idFromApi: item.id || null,
          responsibility: item?.remark || '',
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
    fetchPlantDropdown()
  }, [fetchPlantDropdown])

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

      const missing = data.some(
        (item) => !item.plant || !item.initiativeDescription,
      )
      if (missing) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Plant and Initiative Description are mandatory!',
          severity: 'error',
        })
        return
      }
      const payload = data.map((item) => {
        let matchedPlantId = null
        let matchedPlant = null
        if (item.plant && plantOptions.length > 0) {
          const plantSearch =
            typeof item.plant === 'string'
              ? item.plant.trim().toLowerCase()
              : ''
          matchedPlant = plantOptions.find(
            (p) =>
              p.name?.trim().toLowerCase() === plantSearch ||
              p.plantName?.trim().toLowerCase() === plantSearch ||
              p.plantDisplayName?.trim().toLowerCase() === plantSearch ||
              p.value?.trim().toLowerCase() === plantSearch,
          )
          if (matchedPlant) {
            matchedPlantId = matchedPlant.id
          }
        }
        if (!matchedPlantId) {
          matchedPlantId = item.plantId
        }
        return {
          id:
            item.idFromApi ||
            (typeof item.id === 'string' && item.id.startsWith('temp-')
              ? null
              : item.id) ||
            null,
          plantId: matchedPlantId || null,
          plantName: matchedPlant?.plantName || item?.plantName || '',
          plantDisplayName:
            matchedPlant?.plantDisplayName || item?.plantDisplayName || '',
          plant: matchedPlant?.name || item?.plant || '',
          initiativeDescription: item.initiativeDescription,
          category: item.category || '',
          outcome: item.outcome || '',
          cost: item.cost || 0,
          targetDate: item.targetDate ? formatDate(item.targetDate) : null,
          remark: item.responsibility || item.remark || '',
          siteFkId: SITE_ID,
          aopYear: AOP_YEAR,
        }
      })

      const response = await SiteReportDataService.saveMajorProfitImprovement(
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
      const response = await SiteReportDataService.deleteMajorProfitImprovement(
        keycloak,
        paramsForDelete?.id,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Deleted successfully',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Delete failed',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error deleting record:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to delete record.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleRemarkCellClick = useCallback((row) => {
    setCurrentRemark(row.responsibility || row.remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }, [])

  const downloadExcelForConfiguration = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })

    try {
      await SiteReportDataService.exportMajorProfitImprovement(
        keycloak,
        SITE_ID,
        AOP_YEAR,
        ExcelExportTitle,
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
      const response = await SiteReportDataService.importMajorProfitImprovement(
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
        link.setAttribute('download', `Error_File_Major_Profit_Initiative_${AOP_YEAR}.xlsx`)
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
      isOldYear: isOldYear,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      allAction: true,
      showTitleNameBusiness: true,
      titleName: tabDisplayName || 'Major Profit and Operability Improvement',
      adjustedPermissions: true,
      ExcelName: ExcelExportTitle,
      dynamicDropdownOptions: {
        plant: plantOptions,
      },
      disableColWidth: true,
      makePagable: false,
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
        columns={columns}
        rows={rows}
        setRows={setRows}
        title='B3.7. Major Profit Improvement and Operability Improvement Initiative FY27 (Max 5)'
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
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        handleExcelUpload={handleExcelUpload}
        deleteRowData={deleteRowData}
        deleteId={deleteId}
        setDeleteId={setDeleteId}
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
