import React, { useCallback, useEffect, useState } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import { validateFields } from 'utils/validationUtils'
import { QualityPackagingNormsApiService } from '../../../services/polyester/qualityPackagingNormsApiService'
import AdvanceKendoTable from '../../../common/AdvanceKendoTable'

const OtherCost = ({ refreshTrigger }) => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    oldYear,
    plantObject,
    year,
    isReleased,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const PLANT_NAME = plantObject?.name
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased

  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const valueFormat = ValueFormatterConsumption()

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  function getPreviousYear(aopYear) {
    if (!aopYear) return ''
    const [start, end] = aopYear.split('-').map((s) => s.trim())
    const prevStart = parseInt(start, 10) - 1
    const prevEnd = (parseInt(end, 10) === 0 ? 99 : parseInt(end, 10) - 1)
      .toString()
      .padStart(2, '0')
    return `${prevStart}-${prevEnd}`
  }
  const previousYear = getPreviousYear(AOP_YEAR)

  const columns = [
    {
      field: 'sno',
      title: 'S.no',
      minWidth: 70,
      type: 'number',
      format: '{0:n0}',
    },
    {
      field: 'sapMaterialCode',
      title: 'SAP Material Code',
      editable: false,
      minWidth: 100,
    },
    {
      field: 'name',
      title: 'Name of Item',
      editable: false,
      minWidth: 100,
    },
    {
      field: 'unit',
      title: 'Unit',
      minWidth: 100,
    },
    {
      field: 'budget',
      title: `Budget ${previousYear}`,
      editable: true,
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'actual',
      title: `Actual ${previousYear}`,
      editable: true,
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'proposedNorm',
      title: `Proposed Cost ${AOP_YEAR}`,
      editable: true,
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'remark',
      title: 'Remark',
      type: 'textarea',
      editable: true,
      minWidth: 100,
    },
  ]

  const fetchOtherCostsRows = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) {
      setRows([])
      return
    }
    setLoading(true)
    try {
      const res = await QualityPackagingNormsApiService.getOtherCost(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (res?.code === 200 && Array.isArray(res?.data?.Data)) {
        const mappedRows = res.data.Data.map((item, idx) => ({
          id: item.id || idx,
          sno: idx + 1,
          materialId: item.materialId,
          name: item.displayName,
          unit: item.uom,
          budget: item.prevBudget,
          actual: item.prevActual,
          proposedNorm: item.proposedNorm,
          sapMaterialCode: item.sapMaterialCode,
          normParameterTypeName: item.normTypeName,
          Particulars: item.normTypeName,
          originalRemark: item.remark,
          remark: item.remark,
        }))
        setRows(mappedRows)
      } else {
        setRows([])
      }
    } catch (err) {
      console.error('fetchOtherCostsRows error', err)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  useEffect(() => {
    fetchOtherCostsRows()
  }, [fetchOtherCostsRows, refreshTrigger])

  const saveChanges = useCallback(async () => {
    try {
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

      const otherCostDTOList = data.map((row) => ({
        id: typeof row.id === 'number' ? null : row.id,
        materialId: row.materialId,
        displayName: row.name,
        uom: row.unit,
        prevBudget: row.budget,
        prevActual: row.actual,
        proposedNorm: row.proposedNorm,
        plantId: PLANT_ID,
        aopYear: AOP_YEAR,
        remark: row.remark || '',
      }))

      setLoading(true)
      const res = await QualityPackagingNormsApiService.saveOtherCost(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        otherCostDTOList,
      )

      if (res?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchOtherCostsRows()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Save Failed!',
          severity: 'error',
        })
      }
    } catch (err) {
      console.error('Error while saving other costs data', err)
      setSnackbarOpen(true)
      setSnackbarData({ message: err.message, severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, keycloak, PLANT_ID, AOP_YEAR, fetchOtherCostsRows])

  const handleExcelUpload = async (rawFile) => {
    setLoading(true)
    try {
      const response = await QualityPackagingNormsApiService.importOtherCost(
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
        setModifiedCells({})
        fetchOtherCostsRows()
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
        link.setAttribute('download', `Error File - OtherCost.xlsx`)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchOtherCostsRows()
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
      setLoading(false)
    }
  }

  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })
    try {
      const EXCEL_EXPORT_TITLE = `${vertName}_${PLANT_NAME}_Other_Costs`
      await QualityPackagingNormsApiService.exportOtherCost(
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
    setCurrentRemark(row.remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }, [READ_ONLY])

  const permissions = {
    allAction: true,
    saveBtn: true,
    showTitleNameBusiness: true,
    titleName: 'Other Costs',
    showExport: true,
    showImport: true,
    ExcelName: `${lowerVertName}_Other_Costs`,
    addButton: false,
    deleteButton: false,
    showTitle: true,
  }

  return (
    <Box>
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        title='Other Costs'
        loading={loading}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        permissions={permissions}
        saveChanges={saveChanges}
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

export default OtherCost
