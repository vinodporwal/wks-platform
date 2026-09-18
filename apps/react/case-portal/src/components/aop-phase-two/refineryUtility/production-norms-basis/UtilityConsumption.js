import { Box } from '@mui/material'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
import { ProductionNormsApiService } from 'components/aop-phase-two/services/refineryUtility/productionNormsApiService'
import { validateFields } from 'utils/validationUtils'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import Notification from 'components/Utilities/Notification'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'

const UtilityConsumption = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year, oldYear, isReleased } =
    dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const valueFormat = ValueFormatterPhaseTwo()
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

  const fetchMatbalData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await ProductionNormsApiService.getUtilityConsumptionData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.code === 200) {
        const formattedData = (response?.data || []).map((item, index) => ({
          ...item,
          idFromApi: item?.normParameterFKId,
          id: index,
          Type: item?.TypeDisplayName || 'Utility Consumption',
          remarks: item?.remarks || '',
          originalRemark: item?.remarks || '',
          isEditable: item?.isEditable ?? true,
          ParticularG: item?.TypeDisplayName || 'Utility Consumption',
        }))
        setRows(formattedData)
      } else {
        setRows([])
      }
    } catch (error) {
      console.error('Error fetching Matbal data:', error)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  useEffect(() => {
    setRows([])
    setModifiedCells({})
    fetchMatbalData()
  }, [fetchMatbalData, PLANT_ID, AOP_YEAR])

  const colDefs = useMemo(() => {
    return [
      {
        field: 'productName',
        title: 'Particulars',
        editable: false,
        width: 300,
        minWidth: 300,
      },
      {
        field: 'UOM',
        title: 'UOM',
        editable: false,
        width: 80,
        minWidth: 80,
      },
      {
        field: 'apr',
        title: 'Summer',
        width: 120,
        type: 'number',
        format: valueFormat,
        editable: true,
      },
      {
        field: 'may',
        title: 'Winter',
        width: 120,
        type: 'number',
        format: valueFormat,
        editable: true,
      },
      {
        field: 'remarks',
        title: 'Remark',
        editable: true,
        width: 100,
        minWidth: 100,
      },
    ]
  }, [])

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveChanges = useCallback(async () => {
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) return
    const requiredFields = ['remarks']
    const validationData = modifiedData.map((row) => ({
      ...row,
    }))
    const validationMessage = validateFields(validationData, requiredFields)
    if (validationMessage) {
      setSnackbarData({ message: validationMessage, severity: 'error' })
      setSnackbarOpen(true)
      return
    }

    setLoading(true)
    try {
      // Payload matches colDefs: only 'apr' (Winter) and 'may' (Summer) are editable
      const payload = modifiedData.map((row) => ({
        normParameterFKId: row.normParameterFKId,
        apr: row.apr !== undefined && row.apr !== '' ? Number(row.apr) : null,
        may: row.may !== undefined && row.may !== '' ? Number(row.may) : null,
        remarks: row.remarks || '',
        auditYear: row.auditYear || AOP_YEAR,
        UOM: row.UOM || '',
        TypeDisplayName: row.TypeDisplayName || 'Utility Consumption',
        isEditable: row.isEditable ?? true,
        productName: row.productName || '',
        productDisplayName: row.productDisplayName || '',
        productDisplayOrder: row.productDisplayOrder || ''
      }))

      const response = await ProductionNormsApiService.saveUtilityConsumptionData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        payload,
      )
      if (response) {
        setSnackbarData({ message: 'Saved Successfully!', severity: 'success' })
        setSnackbarOpen(true)
        setModifiedCells({})
        fetchMatbalData()
      } else {
        setSnackbarData({ message: 'Save Failed!', severity: 'error' })
        setSnackbarOpen(true)
      }
    } catch (error) {
      console.error('Error saving Matbal data:', error)
      setSnackbarData({ message: 'Error saving data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, PLANT_ID, AOP_YEAR, keycloak, fetchMatbalData])

  const handleExcelUpload = async (file) => {
    setLoading(true)
    try {
      const response =
        await ProductionNormsApiService.saveProductionDemandExcel(
          file,
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      if (response?.code === 200) {
        setSnackbarData({
          message: 'Imported Successfully!',
          severity: 'success',
        })
        setSnackbarOpen(true)
        fetchMatbalData()
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
        link.setAttribute('download', 'Error File - Production Demand.xlsx')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchMatbalData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error importing Matbal data:', error)
      setSnackbarData({ message: 'Error importing data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }

  const downloadExcelForConfiguration = async () => {
    try {
      const excelName = `${verticalObject?.name}_${siteObject?.name}_${plantObject?.name}_Material_Balance`
      await ProductionNormsApiService.productionDemandExport(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        excelName,
      )
      setSnackbarData({ message: 'Export Started!', severity: 'success' })
      setSnackbarOpen(true)
    } catch (error) {
      console.error('Error exporting Matbal data:', error)
      setSnackbarData({ message: 'Error exporting data', severity: 'error' })
      setSnackbarOpen(true)
    }
  }

  // Simplified: no vertical/site conditions — same permissions apply to everyone.
  // If it's an old year, everything gets locked down via the overrides below.
  const adjustedPermissions = useMemo(() => {
    const basePermissions = {
      showAction: true,
      saveWithRemark: true,
      saveBtn: true,
      allAction: true,
      showTitleNameBusiness: true,
      showExport: false,
      ExcelName: `PIMS_THROUGHPUT_${AOP_YEAR}`,
      showImport: false,
      showCalculate: false,
      showCalculateVisibility: true,
    }

    if (isOldYear) {
      return {
        ...basePermissions,
        showAction: false,
        addButton: false,
        deleteButton: false,
        downloadExcelBtn: false,
        uploadExcelBtn: false,
        editButton: false,
        showUnit: false,
        saveWithRemark: false,
        saveBtn: false,
        isOldYear: true,
        allAction: false,
      }
    }

    return basePermissions
  }, [isOldYear])

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        rows={rows}
        setRows={setRows}
        columns={colDefs}
        permissions={adjustedPermissions}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title='Utility Consumption'
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        handleExcelUpload={handleExcelUpload}
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        plantID={PLANT_ID}
        groupBy='ParticularG'
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

export default UtilityConsumption
