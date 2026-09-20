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

// List of plants (VERTICAL_SITE_PLANT) that require 2 columns (Summer: April, Winter: October)
const TWO_COLUMN_PLANTS = [
  'Refinery Utility_DTA_PCG ASU',
]

const UtilityConsumption = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year, oldYear, isReleased } =
    dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
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

  const currentPlantKey = `${verticalObject?.name}_${siteObject?.name}_${plantObject?.name}`
  const isTwoColumnPlant = TWO_COLUMN_PLANTS.includes(currentPlantKey)

  const fetchUtilityData = useCallback(async () => {
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
      console.error('Error fetching Utility Consumption data:', error)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  useEffect(() => {
    setRows([])
    setModifiedCells({})
    fetchUtilityData()
  }, [fetchUtilityData, PLANT_ID, AOP_YEAR])

  const colDefs = useMemo(() => {
    const columns = [
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
    ]

    if (isTwoColumnPlant) {
      columns.push(
        {
          field: 'apr',
          title: 'Summer',
          width: 120,
          type: 'number',
          format: valueFormat,
          editable: true,
        },
        {
          field: 'oct',
          title: 'Winter',
          width: 120,
          type: 'number',
          format: valueFormat,
          editable: true,
        },
      )
    } else {
      columns.push({
        field: 'apr',
        title: 'Value',
        width: 120,
        type: 'number',
        format: valueFormat,
        editable: true,
      })
    }

    columns.push({
      field: 'remarks',
      title: 'Remark',
      editable: true,
      width: 100,
      minWidth: 100,
    })

    return columns
  }, [isTwoColumnPlant, valueFormat])

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
      const payload = modifiedData.map((row) => ({
        normParameterFKId: row.normParameterFKId,
        apr: row.apr !== undefined && row.apr !== '' ? Number(row.apr) : null,
        oct: row.oct !== undefined && row.oct !== '' ? Number(row.oct) : null,
        remarks: row.remarks || '',
        auditYear: row.auditYear || AOP_YEAR,
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
        fetchUtilityData()
      } else {
        setSnackbarData({ message: 'Save Failed!', severity: 'error' })
        setSnackbarOpen(true)
      }
    } catch (error) {
      console.error('Error saving Utility Consumption data:', error)
      setSnackbarData({ message: 'Error saving data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, PLANT_ID, AOP_YEAR, keycloak, fetchUtilityData])

  const adjustedPermissions = useMemo(() => {
    const basePermissions = {
      showAction: true,
      saveWithRemark: true,
      saveBtn: true,
      allAction: true,
      showTitleNameBusiness: true,
      showExport: false,
      showImport: false,
      showCalculate: false,
      showCalculateVisibility: false,
    }

    if (READ_ONLY || IS_OLD_YEAR) {
      return {
        ...basePermissions,
        showAction: false,
        saveWithRemark: false,
        saveBtn: false,
        allAction: false,
      }
    }

    return basePermissions
  }, [READ_ONLY, IS_OLD_YEAR])

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
