import React, { useState, useEffect, useCallback } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
import KendoDataTables from './index'
import { DataService } from 'services/DataService'
import { getRoleName } from 'services/role-service'
import { validateFields } from 'utils/validationUtils'
import Notification from 'components/Utilities/Notification'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import CatChemReceipe from './CatChemReceipe'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'

const CatalystChecmicalsCalculationConstants = ({ onSaveOrImport }) => {
  const [loading, setLoading] = useState(false)
  const valueFormat = ValueFormatterConsumption()
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const apiRef = useGridApiRef()

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, oldYear, verticalObject, siteObject, isReleased } =
    dataGridStore
  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const keycloak = useSession()
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, isReleased)
  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}`

  const [productionRowsConstants, setProductionRowsConstants] = useState([])
  const [modifiedConstantCells, setModifiedConstantCells] = useState({})

  const [currentRemarkConstants, setCurrentRemarkConstants] = useState('')
  const [currentRowIdConstants, setCurrentRowIdConstants] = useState(null)
  const [remarkDialogOpenConstants, setRemarkDialogOpenConstants] =
    useState(false)
  const unsavedChangesRefConstants = React.useRef({
    unsavedRows: {},
    rowsBeforeChange: {},
  })

  // Grid 1: Constant Columns
  const colDefsConstants = [
    {
      field: 'DisplayName',
      title: 'Particulars',
      editable: false,
      minWidth: 350,
      locked: true,
    },
    {
      field: 'UOM',
      title: 'UOM',
      editable: false,
      minWidth: 90,
    },
    {
      field: 'ConstantValue',
      title: 'Value',
      editable: true,
      type: 'number',
      minWidth: 120,
      format: valueFormat,
    },
    {
      field: 'remarks',
      title: 'Remark',
      editable: false,
      type: 'string',
      minWidth: 150,
    },
  ]

  const handleRemarkCellClickConstants = (row) => {
    if (READ_ONLY) return
    setCurrentRemarkConstants(row.remarks || '')
    setCurrentRowIdConstants(row.id)
    setRemarkDialogOpenConstants(true)
  }

  const fetchConstantsData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    setProductionRowsConstants([])
    try {
      const constantsRes =
        await DataService.getCatalystSelectivityDataConstantsCatChem(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          true,
        )

      if (constantsRes?.code !== 200) {
        setProductionRowsConstants([])
        return
      }

      const data = constantsRes?.data
      const formattedData = data.map((item, index) => ({
        ...item,
        idFromApi: item.id,
        id: index,
        originalRemark: item.Remarks,
        srNo: index + 1,
        Particulars: item.NormTypeName,
        remarks: item.Remarks,
        isEditable: item.isEditable,
      }))

      setProductionRowsConstants(formattedData)
    } catch (error) {
      console.error('Error fetching constants data:', error)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  const saveCatalystData = async (newRow) => {
    setLoading(true)
    try {
      var payload = []

      payload = newRow.map((row) => ({
        apr: row.apr || row.ConstantValue || null,
        may: row.apr || row.ConstantValue || null,
        jun: row.apr || row.ConstantValue || null,
        jul: row.apr || row.ConstantValue || null,
        aug: row.apr || row.ConstantValue || null,
        sep: row.apr || row.ConstantValue || null,
        oct: row.apr || row.ConstantValue || null,
        nov: row.apr || row.ConstantValue || null,
        dec: row.apr || row.ConstantValue || null,
        jan: row.apr || row.ConstantValue || null,
        feb: row.apr || row.ConstantValue || null,
        mar: row.apr || row.ConstantValue || null,
        UOM: '',
        auditYear: AOP_YEAR,
        normParameterFKId: row.normParameterFKId || row.NormParameter_FK_Id,
        remarks: row.remarks,
        id: row.idFromApi || null,
      }))

      const response = await DataService.saveCatalystData(
        PLANT_ID,
        payload,
        keycloak,
        AOP_YEAR,
      )
      if (response) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setModifiedConstantCells({})
        setLoading(false)
        if (onSaveOrImport) onSaveOrImport()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Failed!',
          severity: 'error',
        })
      }

      return response
    } catch (error) {
      console.error('Error saving data:', error)
      setLoading(false)
    } finally {
      fetchConstantsData()
      setLoading(false)
    }
  }

  const saveChanges = React.useCallback(async () => {
    try {
      var data = Object.values(modifiedConstantCells)

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
      saveCatalystData(data)
    } catch (error) {
      // Handle error if necessary
    }
  }, [modifiedConstantCells])

  const downloadExcel = async (type, title) => {
    try {
      setSnackbarData({ message: 'Excel Export Started!', severity: 'success' })
      setSnackbarOpen(true)
      await DataService.getConfigurationExcelConstantsIsCatChem(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        `${EXCEL_EXPORT_TITLE}_${title}`,
        true,
      )
      setSnackbarData({
        message: 'Excel Export Successful!',
        severity: 'success',
      })
      setSnackbarOpen(true)
    } catch (e) {
      setSnackbarData({ message: 'Export Failed!', severity: 'error' })
      setSnackbarOpen(true)
    }
  }

  const handleExcelUpload = async (file) => {
    setLoading(true)
    try {
      const response =
        await DataService.saveConfigurationExcelConstantsIscatCam(
          file,
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          true,
        )
      if (response?.code == 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Upload Successfully!',
          severity: 'success',
        })
        setModifiedConstantCells({})
        setLoading(false)
        fetchConstantsData()
        if (onSaveOrImport) onSaveOrImport()
      } else if (response?.code === 400 && response?.data) {
        const byteCharacters = atob(response.data)
        const byteNumbers = new Array(byteCharacters.length)
        for (let i = 0; i < byteCharacters.length; i++) {
          byteNumbers[i] = byteCharacters.charCodeAt(i)
        }
        const byteArray = new Uint8Array(byteNumbers)
        const blob = new Blob([byteArray], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `Error File constant.xlsx`)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchConstantsData()
        if (onSaveOrImport) onSaveOrImport()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Failed!',
          severity: 'error',
        })
      }
      return response
    } catch (e) {
      setSnackbarData({ message: 'Import Failed!', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }

  const adjustedPermissionsConstant = () => ({
    showAction: true,
    saveWithRemark: true,
    saveBtn: true,
    allAction: true,
    downloadExcelBtn: true,
    uploadExcelBtn: true,
    showTitleNameBusiness: true,
    titleName: 'Constant',
    showCalculate: false,
  })

  useEffect(() => {
    fetchConstantsData()
  }, [fetchConstantsData])

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <LoaderBackdrop open={!!loading} />

      {/* Grid 1: Constant */}
      <KendoDataTables
        title='Constant'
        columns={colDefsConstants}
        setRows={setProductionRowsConstants}
        rows={productionRowsConstants}
        modifiedCells={modifiedConstantCells}
        setModifiedCells={setModifiedConstantCells}
        saveChanges={saveChanges}
        permissions={adjustedPermissionsConstant()}
        groupBy='Particulars'
        fetchData={fetchConstantsData}
        downloadExcelForConfiguration={() =>
          downloadExcel('constant', 'Constant')
        }
        handleExcelUpload={(file) => handleExcelUpload(file)}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        apiRef={apiRef}
        remarkDialogOpen={remarkDialogOpenConstants}
        setRemarkDialogOpen={setRemarkDialogOpenConstants}
        currentRemark={currentRemarkConstants}
        setCurrentRemark={setCurrentRemarkConstants}
        handleRemarkCellClick={handleRemarkCellClickConstants}
        currentRowId={currentRowIdConstants}
        unsavedChangesRef={unsavedChangesRefConstants}
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

export default CatalystChecmicalsCalculationConstants
