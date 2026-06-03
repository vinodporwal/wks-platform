import React, { useCallback, useEffect, useState } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import { validateFields } from 'utils/validationUtils'
import { QualityPackagingNormsApiService } from '../../../services/polyester/qualityPackagingNormsApiService'
import AdvanceKendoTable from '../../../common/AdvanceKendoTable'

const PriceDifferential = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    oldYear,
    plantObject,
    siteObject,
    year,
    isReleased,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const PLANT_NAME = plantObject?.name
  const SITE_NAME = siteObject?.name
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased

  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const valueFormat = ValueFormatterConsumption()

  const [priceDiffRows, setPriceDiffRows] = useState([])
  const [priceDiffLoading, setPriceDiffLoading] = useState(false)
  const [modifiedCellsDiff, setModifiedCellsDiff] = useState({})
  const [remarkDialogOpenDiff, setRemarkDialogOpenDiff] = useState(false)
  const [currentRemarkDiff, setCurrentRemarkDiff] = useState('')
  const [currentRowIdDiff, setCurrentRowIdDiff] = useState(null)

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const priceDiffColumns = [
    {
      field: 'id',
      title: 'ID',
      editable: false,
      hidden: true,
    },
    {
      field: 'qualityType',
      title: 'Quality Type',
      editable: false,
      minWidth: 150,
    },
    {
      field: 'percentage',
      title: 'Value (%)',
      editable: true,
      type: 'number',
      format: valueFormat,
      minWidth: 120,
    },
    {
      field: 'remark',
      title: 'Remark',
      type: 'textarea',
      editable: true,
      minWidth: 100,
    },
  ]

  const fetchPriceDifferential = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) {
      setPriceDiffRows([])
      return
    }
    setPriceDiffLoading(true)
    try {
      const res = await QualityPackagingNormsApiService.getPriceDifferential(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      const priceDiffSource = Array.isArray(res?.data)
        ? res.data
        : Array.isArray(res?.data?.data)
          ? res.data.data
          : Array.isArray(res?.data?.Data)
            ? res.data.Data
            : []
      const mappedPriceDiffRows = priceDiffSource.map((item, idx) => ({
        id: item.id || idx,
        materialId: item.materialId,
        qualityType: item.displayName,
        percentage: item.percentage,
        normParameterTypeName: item.normParameterTypeName,
        originalRemark: item.remark,
        remark: item.remark,
        Particulars: item.normParameterTypeName,
        unit: '%',
      }))
      setPriceDiffRows(mappedPriceDiffRows)
    } catch (err) {
      console.error('fetchPriceDifferential error', err)
      setPriceDiffRows([])
    } finally {
      setPriceDiffLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  useEffect(() => {
    fetchPriceDifferential()
  }, [fetchPriceDifferential])

  const savePriceDiffChanges = useCallback(async () => {
    try {
      const data = Object.values(modifiedCellsDiff)
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

      const priceDifferentialDTOList = data.map((row) => ({
        id: typeof row.id === 'number' ? null : row.id,
        displayName: row.qualityType,
        percentage: row.percentage,
        plantId: PLANT_ID,
        aopYear: AOP_YEAR,
        remark: row.remark || 'system gen',
        normParameterTypeName: 'Quality',
        materialId: row.materialId,
      }))

      setPriceDiffLoading(true)
      const res = await QualityPackagingNormsApiService.savePriceDifferential(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        priceDifferentialDTOList,
      )

      if (res?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Successfully!',
          severity: 'success',
        })
        setModifiedCellsDiff({})
        fetchPriceDifferential()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Save Failed!',
          severity: 'error',
        })
      }
    } catch (err) {
      console.error('Error while saving price differential', err)
      setSnackbarOpen(true)
      setSnackbarData({ message: err.message, severity: 'error' })
    } finally {
      setPriceDiffLoading(false)
    }
  }, [modifiedCellsDiff, keycloak, PLANT_ID, AOP_YEAR, fetchPriceDifferential])

  const handleExcelUpload = async (rawFile) => {
    setPriceDiffLoading(true)
    try {
      const response = await QualityPackagingNormsApiService.importPriceDifferential(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        rawFile,
      )
      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Uploaded Successfully!',
          severity: 'success',
        })
        setModifiedCellsDiff({})
        fetchPriceDifferential()
      } else if (response?.code === 400 && response?.data) {
        const byteCharacters = atob(response.data)
        const byteNumbers = Array.from(byteCharacters, (char) => char.charCodeAt(0))
        const byteArray = new Uint8Array(byteNumbers)
        const blob = new Blob([byteArray], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `Error File - PriceDifferential.xlsx`)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchPriceDifferential()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error uploading excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setPriceDiffLoading(false)
    }
  }

  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })
    try {
      const EXCEL_EXPORT_TITLE = `${vertName}_${SITE_NAME}_${PLANT_NAME}_Price_Differential`
      await QualityPackagingNormsApiService.exportPriceDifferential(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_EXPORT_TITLE,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel downloaded successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error downloading Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
    }
  }

  const handleRemarkCellClick = useCallback((row) => {
    if (READ_ONLY) return
    setCurrentRemarkDiff(row.remark || '')
    setCurrentRowIdDiff(row.id)
    setRemarkDialogOpenDiff(true)
  }, [READ_ONLY])

  const permissions = {
    saveBtn: true,
    allAction: true,
    showTitleNameBusiness: true,
    titleName: 'Price Differential As Percentage wrt Quality',
    showExport: true,
    showImport: true,
    ExcelName: `${lowerVertName}_Price_Differential`,
    addButton: false,
    deleteButton: false,
    showTitle: true,
  }

  return (
    <Box>
      <AdvanceKendoTable
        columns={priceDiffColumns}
        rows={priceDiffRows}
        setRows={setPriceDiffRows}
        title='Price Differential As Percentage wrt Quality'
        loading={priceDiffLoading}
        modifiedCells={modifiedCellsDiff}
        setModifiedCells={setModifiedCellsDiff}
        permissions={permissions}
        remarkDialogOpen={remarkDialogOpenDiff}
        setRemarkDialogOpen={setRemarkDialogOpenDiff}
        currentRemark={currentRemarkDiff}
        setCurrentRemark={setCurrentRemarkDiff}
        currentRowId={currentRowIdDiff}
        setCurrentRowId={setCurrentRowIdDiff}
        saveChanges={savePriceDiffChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        handleExport={handleExport}
        handleExcelUpload={handleExcelUpload}
        groupBy='Particulars'
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        snackbarData={snackbarData}
        setSnackbarData={setSnackbarData}
      />
    </Box>
  )
}

export default PriceDifferential
