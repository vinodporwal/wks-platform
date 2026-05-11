import React, { useState, useEffect, useCallback } from 'react'
import { Box, Typography, Backdrop, CircularProgress } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
import KendoDataTables from '../../index'
import getEnhancedAOPColDefs from 'components/data-tables/CommonHeader/kendo_ConfigHeader'
import { DataService } from 'services/DataService'
import { getRoleName } from 'services/role-service'
import { RawMaterialNormsBasisApiService } from 'services/raw-material-norms-basis-api-service'
import { validateFields } from 'utils/validationUtils'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const IBINlossesGrid = ({
  summary,
  summaryEdited,
  setSummaryEdited,
  triggerGrid3Refresh,
}) => {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [open1, setOpen1] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const apiRef = useGridApiRef()

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    plantObject,
    year,
    oldYear,
    yearChanged,
    verticalObject,
    siteObject,
  } = dataGridStore
  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const keycloak = useSession()

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_IBIN_Losses`

  const FORMATE_VALUE = '{0:0.000}'

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const productionColumns = getEnhancedAOPColDefs({
    handleRemarkCellClick,
    configType: 'CatChem',
    FORMATE_VALUE,
  })

  // TODO: Replace with actual API call when backend is ready
  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    setModifiedCells({})

    try {
      setLoading(true)

      const response = await RawMaterialNormsBasisApiService.getData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        'losses',
      )

      const formattedData = response?.data?.catChemNormList?.map(
        (row, index) => ({
          ...row,
          id: row.id || index,
          particulars: row.displayName,
          uom: row.uom,
          value: parseFloat(row.apr) || 0,
          remarks: row.remarks,
          originalRemark: row.remarks || '',
          normParameterFKId: row.normParameterFKId,
          auditYear: row.auditYear,
          normTypeName: row.normTypeName,
          isEditable: row.isEditable,
          displayName: row.displayName,
          type: row.type,
        }),
      )

      setRows(formattedData || [])
    } catch (error) {
      console.error('Error fetching IBIN Losses data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error fetching data',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    setModifiedCells({})
    fetchData()
  }, [oldYear, yearChanged, keycloak, PLANT_ID, AOP_YEAR])

  const saveSummary = async (summary) => {
    try {
      const response = await DataService.saveSummaryAOPConsumptionNorm(
        PLANT_ID,
        AOP_YEAR,
        summary,
        keycloak,
      )

      if (response?.code == 200) {
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setSnackbarOpen(true)
      } else {
        setSnackbarData({
          message: 'Saved Failed!',
          severity: 'error',
        })
      }
      return response
    } catch (error) {
      console.error('Error saving Summary!', error)
    } finally {
      setLoading(false)
    }
  }

  const handleUpdate = async (updatedRows) => {
    setLoading(true)
    try {
      let payload = updatedRows?.map((row) => {
        const { id, inEdit, particulars, originalRemark, ...rest } = row
        const monthValue = row.value ?? 0
        return {
          normParameterFKId: row.normParameterFKId,
          jan: monthValue,
          feb: monthValue,
          mar: monthValue,
          apr: monthValue,
          may: monthValue,
          jun: monthValue,
          jul: monthValue,
          aug: monthValue,
          sep: monthValue,
          oct: monthValue,
          nov: monthValue,
          dec: monthValue,
          remarks: row.remarks,
          auditYear: row.auditYear,
          uom: row.uom,
          normTypeName: row.normTypeName,
          isEditable: row.isEditable,
          displayName: row.displayName,
          type: row.type ?? '',
        }
      })

      const response = await RawMaterialNormsBasisApiService.postData(
        keycloak,
        payload,
        PLANT_ID,
        AOP_YEAR,
      )
      console.log('payload', payload)

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Saved Successfully!',
        severity: 'success',
      })
      triggerGrid3Refresh()

      await fetchData()
      return { code: 200 }
    } catch (error) {
      console.error('Error updating data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Data Save failed!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const downloadExcelForConfiguration = async () => {
    try {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel download started!',
        severity: 'success',
      })

      // TODO: Replace with actual API call
      const response = await RawMaterialNormsBasisApiService.exportExcel(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_EXPORT_TITLE,
        'losses',
      )

      console.log('Excel export for:', EXCEL_EXPORT_TITLE)
      return { code: 200 }
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
      // TODO: Replace with actual API call
      const response = await RawMaterialNormsBasisApiService.importExcel(
        rawFile,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        'losses',
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
        link.setAttribute('download', 'Error File - IBIN Losses.xlsx')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        await fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'Upload Failed!', severity: 'error' })
      }

      return response
    } catch (error) {
      console.error('Error uploading excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const saveChanges = useCallback(async () => {
    setLoading(true)

    try {
      if (Object.keys(modifiedCells).length === 0) {
        if (summaryEdited) {
          await saveSummary(summary)
          setModifiedCells({})
          setSummaryEdited(false)
        }
        return
      }

      const rawData = Object.values(modifiedCells)
      const data = rawData.filter((row) => row.inEdit)

      if (data.length === 0) {
        setLoading(false)
        return
      }
      const requiredFields = ['remarks']
      const validationMessage = validateFields(data, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationMessage,
          severity: 'error',
        })
        return
      }

      await handleUpdate(data)
    } catch (error) {
      console.log('Error saving changes:', error)
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, summaryEdited, summary])

  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
      allAction: false,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: true,
      saveWithRemark: true,
      saveBtn: true,
      allAction: true,
      downloadExcelBtn: true,
      uploadExcelBtn: true,
      showTitleNameBusiness: true,
      titleName: 'IB,IN losses',
    },
    IS_OLD_YEAR,
  )

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <KendoDataTables
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        setRows={setRows}
        columns={productionColumns}
        rows={rows}
        paginationOptions={[100, 200, 300]}
        saveChanges={saveChanges}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        apiRef={apiRef}
        open1={open1}
        setOpen1={setOpen1}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        handleRemarkCellClick={handleRemarkCellClick}
        fetchData={fetchData}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        permissions={adjustedPermissions}
        summaryEdited={summaryEdited}
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        handleExcelUpload={handleExcelUpload}
      />
    </Box>
  )
}

export default IBINlossesGrid
