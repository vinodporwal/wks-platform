import { Box } from '@mui/material'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
import { validateFields } from 'utils/validationUtils'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import Notification from 'components/Utilities/Notification'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import KendoDataTables from './index'
import { ProductionNormsApiService } from 'services/production-norms-api-service'

const ModeSelectionCracker = () => {
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

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await ProductionNormsApiService.getTreatmentVendorData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.code === 200) {
        const formattedData = (response?.data || []).map((item, index) => ({
          ...item,
          idFromApi: item?.normParameterFKId,
          id: index,
          isEditable: item?.isEditable ?? true,
          ParticularG: item?.normTypeName || 'Vendors',
        }))
        setRows(formattedData)
      } else {
        setRows([])
      }
    } catch (error) {
      console.error('Error fetching Matbal data:', error)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  useEffect(() => {
    setRows([])
    setModifiedCells({})
    fetchData()
  }, [fetchData, PLANT_ID, AOP_YEAR])

  const colDefs = useMemo(() => {
    const columns = [
      {
        field: 'ParticularG',
        title: 'Particulars',
        editable: false,
        width: 300,
        minWidth: 250,
        widthT: 300,
        locked: true,
        hidden: true,
      },
      {
        field: 'DisplayName',
        title: 'Mode Name',
        editable: false,
        width: 300,
        minWidth: 250,
        widthT: 300,
      },
      {
        field: 'isChecked',
        title: 'Is Active',
        width: 150,
        minWidth: 120,
        widthT: 150,
        type: 'checkbox',
        editable: true,
      }
    ]

    return columns
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
        isChecked: row.isChecked,
        remarks: row.remarks || '',
        auditYear: row.auditYear || AOP_YEAR,
        uom: row.UOM || '',
        TypeDisplayName: row.TypeDisplayName || 'Treatment Vendor',
        isEditable: row.isEditable ?? true,
        DisplayName: row.DisplayName || '',
        Name: row.Name || '',
      }))

      const response = await ProductionNormsApiService.saveTreatmentVendorData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        payload,
      )
      if (response) {
        setSnackbarData({ message: 'Saved Successfully!', severity: 'success' })
        setSnackbarOpen(true)
        setModifiedCells({})
        fetchData()
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
  }, [modifiedCells, PLANT_ID, AOP_YEAR, keycloak, fetchData])


  const adjustedPermissions = useMemo(() => {
    const basePermissions = {
      allAction: true,
      saveBtn: true,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      titleName: 'Mode Selection',
      showTitleNameBusiness: true,
      isTotalFooterActive: false,
    }

    if (isOldYear) {
      return {
        ...basePermissions,
        allAction: false,
        addButton: false,
        deleteButton: false,
        editButton: false,
        showUnit: false,
        saveWithRemark: false,
        saveBtn: false,
        isOldYear: isOldYear,
        downloadExcelBtn: false,
        uploadExcelBtn: false,
      }
    }

    return basePermissions
  }, [isOldYear])

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <KendoDataTables
        rows={rows}
        setRows={setRows}
        columns={colDefs}
        permissions={adjustedPermissions}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title='Mode Selection'
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
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

export default ModeSelectionCracker
