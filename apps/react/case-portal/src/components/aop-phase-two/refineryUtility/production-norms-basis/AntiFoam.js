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

// single source of truth for the value column
const VALUE_FIELD = 'april'

const AntiFoam = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, oldYear, isReleased } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, isReleased)
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

  const showMessage = (message, severity) => {
    setSnackbarData({ message, severity })
    setSnackbarOpen(true)
  }

  const fetchUtilityData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await ProductionNormsApiService.getAntiFoamData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.code === 200) {
        const formattedData = (response?.data || [])
          .map((item) => ({
            ...item,
            id: item.normParameterFKId,
            idFromApi: item.normParameterFKId,
            ParticularG: item.normType,
            Type: item.normType,
            displayName: item.displayName || item.name,
            [VALUE_FIELD]: item[VALUE_FIELD] ?? null,
            remarks: item.remarks ?? '',
            originalRemark: item.remarks ?? '',
            isEditable: item.isEditable ?? true,
          }))
          .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
        setRows(formattedData)
      } else {
        setRows([])
      }
    } catch (error) {
      console.error('Error fetching AntiFoam data:', error)
      setRows([])
      showMessage('Error fetching data', 'error')
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  useEffect(() => {
    setRows([])
    setModifiedCells({})
    fetchUtilityData()
  }, [fetchUtilityData])

  const columns = useMemo(
    () => [
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
        field: 'displayName',
        title: 'Particulars',
        editable: false,
        width: 300,
        minWidth: 250,
        widthT: 300,
      },
      {
        field: 'uom',
        title: 'UOM',
        editable: false,
        width: 100,
        minWidth: 80,
        widthT: 100,
      },
      {
        field: VALUE_FIELD,
        title: 'Value',
        width: 150,
        minWidth: 120,
        widthT: 150,
        type: 'number',
        format: valueFormat,
        editable: !READ_ONLY,
      },
      {
        field: 'remarks',
        title: 'Remark',
        editable: !READ_ONLY,
        width: 300,
        minWidth: 250,
        widthT: 300,
      },
    ],
    [valueFormat, READ_ONLY],
  )

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY || IS_OLD_YEAR) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveChanges = useCallback(async () => {
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) return

    const validationMessage = validateFields(
      modifiedData.map((row) => ({ ...row })),
      ['remarks'],
    )
    if (validationMessage) {
      showMessage(validationMessage, 'error')
      return
    }

    const payload = modifiedData
      .filter((row) => row.isEditable !== false)
      .map((row) => {
        const raw = row[VALUE_FIELD]
        return {
          normParameterFKId: row.normParameterFKId || row.idFromApi,
          [VALUE_FIELD]:
            raw !== undefined && raw !== null && raw !== ''
              ? Number(raw)
              : null,
          remarks: row.remarks || '',
        }
      })

    console.log('AntiFoam save payload:', payload) // verify in DevTools

    setLoading(true)
    try {
      const response = await ProductionNormsApiService.saveAntiFoamData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        payload,
      )
      if (response?.code === 200 || response?.success) {
        showMessage('Saved Successfully!', 'success')
        setModifiedCells({})
        fetchUtilityData()
      } else {
        showMessage(
          response?.message || 'Save Failed!',
          response?.code === 400 ? 'warning' : 'error',
        )
      }
    } catch (error) {
      console.error('Error saving AntiFoam data:', error)
      showMessage('Error saving data', 'error')
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, PLANT_ID, AOP_YEAR, keycloak, fetchUtilityData])

  const adjustedPermissions = useMemo(() => {
    const base = {
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
        ...base,
        showAction: false,
        saveWithRemark: false,
        saveBtn: false,
        allAction: false,
      }
    }
    return base
  }, [READ_ONLY, IS_OLD_YEAR])

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        rows={rows}
        setRows={setRows}
        columns={columns}
        permissions={adjustedPermissions}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title='AntiFoam'
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

export default AntiFoam