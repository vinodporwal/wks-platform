import React, { useEffect, useState, useCallback } from 'react'
import { useGridApiRef } from '@mui/x-data-grid'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { useSession } from 'SessionStoreContext'
import { ShutdownApiService } from 'components/aop-phase-two/services/crude/shutdownApiService'
import { useSelector } from 'react-redux'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'

const Shutdown = ({ permissions }) => {
  const [modifiedCells, setModifiedCells] = useState({})
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [open1, setOpen1] = useState(false)
  const [deleteId, setDeleteId] = useState(null)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const apiRef = useGridApiRef()
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year } = dataGridStore
  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear
  const [siteDropdown, setSiteDropdown] = useState([])
  const [plantDropdown, setPlantDropdown] = useState([])

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const columns = [
    {
      field: 'siteName',
      title: 'Site',
      editable: true,
      minWidth: 150,
      type: 'dropdownSiteplant',
    },
    {
      field: 'plantName',
      title: 'Plant',
      editable: true,
      minWidth: 150,
      type: 'dropdownSiteplant',
    },
    {
      field: 'sdTotalDurationDays',
      title: 'SD Total duration in days',
      editable: true, // "Note that provide days specific to days in that month."
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      minWidth: 220,
    },
    {
      field: 'dateOfCommencement',
      title: 'Date of Commencement',
      editable: true, // "Manual in Digital AOP"
      minWidth: 220,
      type: 'date',
    },
    {
      field: 'remark',
      title: 'Purpose of Shutdown',
      editable: true,
      widthT: 250,
      minWidth: 200,
    },
  ]
  useEffect(() => {
    const loadSitePlantData = async () => {
      try {
        const resp = await ShutdownApiService.getSitePlantDropdownData(keycloak, PLANT_ID)
        const verticalData = resp?.sites
          ? resp
          : (resp?.data?.sites
            ? resp.data
            : (resp?.data?.data?.sites
              ? resp.data.data
              : {}))
        const sites = verticalData?.sites || []
        setSiteDropdown(sites)
        const plants = sites.flatMap((site) => site.plants || [])
        setPlantDropdown(plants)
      } catch (error) {
        console.error('Error fetching site data:', error)
      } finally {
        setLoading(false)
      }
    }
    loadSitePlantData()
  }, [])

  const fetchData = useCallback(async () => {
    setModifiedCells({})
    setLoading(true)
    try {
      const resp = await ShutdownApiService.getShutdownData(keycloak, PLANT_ID, AOP_YEAR)
      const rawData = Array.isArray(resp?.data)
        ? resp.data
        : Array.isArray(resp?.data?.data)
          ? resp.data.data
          : []
      const dataRows = rawData.map((item, index) => {
        return {
          ...item,
          idFromApi: item.id,
          id: `${index}`,
          originalRemark: item.remark,
          remark: item.remark,
          siteFkId: item.siteFkId,
          plantFkId: item.plantFkId,
          siteName: item.siteName,
          plantName: item.plantName,
          dateOfCommencement: item.dateOfCommencement ? new Date(item.dateOfCommencement) : null,
          isEditable: item.isEditable
        }
      })
      setRows(dataRows)
      setOriginalRows(JSON.parse(JSON.stringify(dataRows)))
    } catch (error) {
      console.error('Error fetching steady state consumption data:', error)
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak])

  // TODO: replace with the real SAVE API call once it's available.
  function addTimeOffset(dateTime) {
    if (!dateTime) return null
    const date = new Date(dateTime)
    date.setUTCHours(date.getUTCHours() + 5)
    date.setUTCMinutes(date.getUTCMinutes() + 30)
    return date
  }
  const saveChanges = useCallback(async () => {
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
      return
    }
    const validationError = validateRowDataWithRemarks(
      modifiedData,
      originalRows,
      ['sdTotalDurationDays', 'dateOfCommencement', 'remark'],
      'plantName',
      'remark',
    )

    if (validationError) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: validationError,
        severity: 'error',
      })
      setLoading(false)
      return
    }

    setLoading(true)
    try {
      const payload = modifiedData.map((row) => {
        const matchedSite = siteDropdown.find((s) => s.name === row.siteName)
        const matchedPlant = matchedSite?.plants?.find((p) => p.name === row.plantName)

        return {
          id: row.idFromApi ?? null,
          siteFkId: matchedSite ? matchedSite.id : (row.siteFkId ?? null),
          plantFkId: matchedPlant ? matchedPlant.id : (row.plantFkId ?? null),
          siteName: row.siteName,
          plantName: row.plantName,
          sdTotalDurationDays: row.sdTotalDurationDays,
          dateOfCommencement: addTimeOffset(row.dateOfCommencement),
          remark: row.remark,
          plantId: PLANT_ID,
          aopYear: AOP_YEAR,
        }
      })

      const response =
        await ShutdownApiService.saveShutdownData(
          payload,
          keycloak,
        )

      if (response) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'Saved Successfully!', severity: 'success' })
        setModifiedCells({})
        await fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'data not saved!', severity: 'error' })
      }
    } catch (error) {
      console.error('Error saving plant capacities:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Save failed, please try again!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [
    modifiedCells,
    originalRows,
    PLANT_ID,
    AOP_YEAR,
    keycloak,
    fetchData,
  ])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const handleExport = useCallback(async () => {
    setSnackbarOpen(true)
    setSnackbarData({ message: 'Excel download started!', severity: 'info' })
    try {
      const EXCEL_NAME = `Shutdown.xlsx`
      await ShutdownApiService.exportShutdownData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_NAME,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel downloaded successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting Shutdown plan:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  const handleExcelUpload = useCallback(
    async (file) => {
      if (!file) return
      setLoading(true)
      try {
        const response = await ShutdownApiService.importShutdownData(
          file,
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
          await fetchData()
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
        link.setAttribute('download', 'Error File - Shutdown.xlsx')
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
        console.error('Error importing slowdown plan:', error)
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Unexpected error during import.',
          severity: 'error',
        })
      } finally {
        setLoading(false)
      }
    },
    [keycloak, PLANT_ID, AOP_YEAR, fetchData],
  )

  const adjustedPermissions = {
    customHeight: { mainBox: '32vh', otherBox: '100%' },
    textAlignment: 'center',
    allAction: true,
    addButton: true,
    deleteButton: true,
    showAction: true,
    remarksEditable: true,
    showCalculate: false,
    showExport: true,
    ExcelName: `shutdown`,
    showImport: true,
    saveBtnForRemark: true,
    saveBtn: true,
    showWorkFlowBtns: false,
    showTitle: true,
    filterable: false,
  }

  return (
    <div>
      <LoaderBackdrop open={!!loading} />

      <AdvanceKendoTable
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title='Shutdown'
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
        currentRowId={currentRowId}
        permissions={adjustedPermissions}
        disableRedHighlight={true}
        handleExport={handleExport}
        handleExcelUpload={handleExcelUpload}
        screenType='pims-product-master'
        siteDropdown={siteDropdown}
        plantDropdown={plantDropdown}
      />
    </div>
  )
}

export default Shutdown
