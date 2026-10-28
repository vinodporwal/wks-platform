import { Box } from '@mui/material'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
import { ProductionNormsApiService } from 'components/aop-phase-two/services/refineryUtility/productionNormsApiService'
import { validateFields } from 'utils/validationUtils'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import Notification from 'components/Utilities/Notification'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'

const OwnerSelection = () => {
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
  const [dropdownOptions, setDropdownOptions] = useState([])
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const fetchData = useCallback(async () => {
    if (!PLANT_ID) return
    setLoading(true)
    try {
      const [dropdownRes, dataRes] = await Promise.all([
        ProductionNormsApiService.getPlantOwnerDropdown(keycloak, PLANT_ID),
        ProductionNormsApiService.getSelectedPlantOwner(keycloak, PLANT_ID),
      ])

      if (dropdownRes?.data) {
        const options = dropdownRes.data.map((item) => ({
          value: item?.plantOwner || item,
          label: item?.plantOwner || item,
          id: item?.id,
        }))
        setDropdownOptions(options)
      }

      if (dataRes?.code === 200) {
        const formattedData = (dataRes?.data || []).map((item, index) => ({
          ...item,
          id: item?.normParameterId ?? (item?.id || index),
          idFromApi: item?.id,
          plantOwner: 'Plant Owner',
          remarks: item?.remarks || '',
          originalRemark: item?.remarks || '',
          isEditable: item?.isEditable ?? true,
        }))
        setRows(formattedData)
      } else {
        setRows([])
      }
    } catch (error) {
      console.error('Error fetching Owner Selection data:', error)
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
        field: 'plantOwner',
        title: 'Plant Owner',
        editable: false,
        width: 300,
        minWidth: 250,
        widthT: 300,
      },
      {
        field: 'plantOwnerSelection',
        title: 'Plant Owner Selection',
        width: 300,
        minWidth: 250,
        widthT: 300,
        type: 'select',
        editable: true,
        options: dropdownOptions,
      },
      {
        field: 'remarks',
        title: 'Remark',
        editable: true,
        width: 300,
        minWidth: 250,
        widthT: 300,
      },
    ]

    return columns
  }, [dropdownOptions])

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
      const payload = modifiedData.map((row) => {
        const selectedOption = dropdownOptions.find(
          (opt) => opt.value === row.plantOwnerSelection
        )
        return {
          id: selectedOption?.id || row.idFromApi || undefined,
          remarks: row.remarks || '',
          normParameterId: row.normParameterId,
        }
      })

      const response = await ProductionNormsApiService.saveSelectedPlantOwner(
        keycloak,
        PLANT_ID,
        payload,
      )

      if (response && response.code === 200) {
        setSnackbarData({ message: 'Saved Successfully!', severity: 'success' })
        setSnackbarOpen(true)
        setModifiedCells({})
        fetchData()
      } else if (response && response.code === 400) {
        setSnackbarData({
          message: 'Partial Data Updated',
          severity: 'warning',
        })
        setSnackbarOpen(true)
        setModifiedCells({})
        fetchData()
      } else {
        setSnackbarData({ message: 'Save Failed!', severity: 'error' })
        setSnackbarOpen(true)
      }
    } catch (error) {
      console.error('Error saving Owner Selection data:', error)
      setSnackbarData({ message: 'Error saving data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, PLANT_ID, AOP_YEAR, keycloak, fetchData])

  const adjustedPermissions = useMemo(() => {
    const basePermissions = {
      showAction: true,
      saveWithRemark: true,
      saveBtn: true,
      allAction: true,
      showTitleNameBusiness: true,
      showExport: false,
      ExcelName: `Owner Selection_${AOP_YEAR}`,
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
        showExport: false,
        showImport: false,
        editButton: false,
        showUnit: false,
        saveWithRemark: false,
        saveBtn: false,
        isOldYear: true,
        allAction: false,
      }
    }

    return basePermissions
  }, [isOldYear, AOP_YEAR])

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
        title='Owner Selection'
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        plantID={PLANT_ID}
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

export default OwnerSelection
