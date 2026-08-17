import React, { useEffect, useState, useCallback } from 'react'
import { useGridApiRef } from '@mui/x-data-grid'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { validateFields } from 'utils/validationUtils'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { useSession } from 'SessionStoreContext'
import { PlantsCapacitiesApiService } from 'components/aop-phase-two/services/crude/plantsCapacitiesApiService'
import { useSelector } from 'react-redux'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'

const PlantCapacities = ({ permissions }) => {
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
  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const columns = [
    {
      field: 'particulars',
      title: 'Particulars',
      editable: false,
      widthT: 300,
      minWidth: 180,
      hidden: true,
    },
    {
      field: 'pimsCode',
      title: 'PIMS Code',
      editable: true,
      minWidth: 130,
      hidden: true,
    },
    {
      field: 'sapProductId',
      title: 'SAP Product_ID',
      editable: true,
      minWidth: 150,
      hidden: true,
    },
    {
      field: 'siteName',
      title: 'Site',
      editable: false, // "Should be unique and fix"
      minWidth: 120,
    },
    {
      field: 'plantName',
      title: 'Plant',
      editable: false, // "Should be unique and fix"
      minWidth: 120,
    },
    {
      field: 'uom',
      title: 'UOM',
      editable: false, // "Should be unique and fix"
      minWidth: 100,
    },
    {
      field: 'min',
      title: 'Min',
      editable: true, // "Manual in Digital AOP"
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      minWidth: 100,
    },
    {
      field: 'max',
      title: 'Max (MCU)',
      editable: true, // "From MCU"
      align: 'right',
      headerAlign: 'right',
      type: 'number',
      minWidth: 100,
    },
    {
      field: 'remarks',
      title: 'Remarks', // "Manual in Digital AOP"
      editable: true,
      widthT: 250,
      minWidth: 120,
    },
  ]

  const fetchData = useCallback(async () => {
    setModifiedCells({})
    setLoading(true)
    try {
      const resp = await PlantsCapacitiesApiService.getPlantsCapacitiesData(keycloak, PLANT_ID, AOP_YEAR)
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
          originalRemark: item.remarks,
          transactionId: item.transactionId,
          masterId: item.masterId,
          Particulars: item.normParameterTypeDispla || item.Particulars,
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
      ['min', 'max'],
      'Particulars',
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
    // Check if max is greater than min
    const invalidMinMaxRows = modifiedData.filter((row) => {
      const minVal = row.min !== undefined && row.min !== null && row.min !== '' ? Number(row.min) : null
      const maxVal = row.max !== undefined && row.max !== null && row.max !== '' ? Number(row.max) : null
      return minVal !== null && maxVal !== null && maxVal <= minVal
    })

    if (invalidMinMaxRows.length > 0) {
      const displayNames = invalidMinMaxRows
        .map((row) => row.Particulars || `Row ${row.id}`)
        .join(', ')
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Max  must be greater than Min for: ${displayNames}`,
        severity: 'error',
      })
      return
    }

    setLoading(true)
    try {
      const payload = modifiedData.map((row) => ({
        transactionId: row.transactionId,
        masterId: row.masterId,
        siteName: row.siteName,
        plantName: row.plantName,
        min: row.min,
        max: row.max,
        remarks: row.remarks,
        aopYear: AOP_YEAR,
      }))

      const response =
        await PlantsCapacitiesApiService.savePlantsCapacitiesData(
          PLANT_ID,
          payload,
          keycloak,
          null,
          AOP_YEAR,
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
      const EXCEL_NAME = `Refinery_Plant_Capacities.xlsx`
      await PlantsCapacitiesApiService.exportPlantsCapacities(
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
      console.error('Error exporting Plant Capacities plan:', error)
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
        const response = await PlantsCapacitiesApiService.importPlantsCapacities(
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
          link.setAttribute('download', 'Error File - Plant Capacities.xlsx')
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
    addButton: false,
    deleteButton: false,
    showAction: true,
    remarksEditable: true,
    showCalculate: false,
    showExport: true,
    ExcelName: `Plant Capacities`,
    showImport: true,
    saveBtnForRemark: true,
    saveBtn: true,
    showWorkFlowBtns: false,
    showTitle: true,
    filterable: false,
    makePagable: false,
  }

  return (
    <div>
      <LoaderBackdrop open={!!loading} />

      <AdvanceKendoTable
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title='Plant Capacities'
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
      />
    </div>
  )
}

export default PlantCapacities
