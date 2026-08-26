import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from './index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import Notification from 'components/Utilities/Notification'
import { ProductionNormsApiService } from 'services/production-norms-api-service'
import { getRoleName } from 'services/role-service'
import { validateFields } from 'utils/validationUtils'

const MxoRegeneration = ({ permissions }) => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)

  const {
    oldYear,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
    isReleased,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const SCREEN_NAME = screenTitle?.title || 'MXO Regeneration'

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

  const colDefs = useMemo(
    () => [
      {
        field: 'id',
        title: 'Id',
        width: 110,
        minWidth: 90,
        editable: false,
        hidden: true,
      },
      {
        field: 'monthLabel',
        title: 'Month',
        width: 100,
        minWidth: 90,
        editable: false,
      },
      {
        field: 'mode',
        title: 'Mode',
        width: 110,
        minWidth: 100,
        editable: false,
      },
      {
        field: 'mXOGeneration_tph',
        title: 'MXO Generation in tph',
        width: 220,
        minWidth: 220,
        type: 'number',
        align: 'right',
        format: '{0:0.00}',
        editable: false,
      },
      {
        field: 'mXODowntime_hrs',
        title: 'Downtime in hrs',
        width: 170,
        minWidth: 170,
        type: 'number',
        align: 'right',
        format: '{0:0.00}',
        editable: !READ_ONLY,
      },
      {
        field: 'mXOgeneration_TPM',
        title: 'MXO generation (TPM)',
        width: 220,
        minWidth: 220,
        type: 'number',
        align: 'right',
        format: '{0:0.00}',
        editable: false,
      },
      {
        field: 'onstream_hrs',
        title: 'Onstream in hrs',
        width: 170,
        minWidth: 170,
        type: 'number',
        align: 'right',
        format: '{0:0.00}',
        editable: false,
      },
      {
        field: 'maxMXOReprocessingRate_tph',
        title: 'Max MXO Reprocessing rate in tph',
        width: 320,
        minWidth: 320,
        type: 'number',
        align: 'right',
        format: '{0:0.00}',
        editable: !READ_ONLY,
      },
      {
        field: 'remarks',
        title: 'Remark',
        width: 140,
        minWidth: 130,
        editable: !READ_ONLY,
      },
    ],
    [READ_ONLY],
  )

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || row.Remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  // Fetch MXO data formatted with exact backend response keys
  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await ProductionNormsApiService.getMxoRegenerationData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        const rawData =
          response?.data?.Data ||
          response?.data?.data ||
          (Array.isArray(response?.data) ? response?.data : [])

        const formattedData = (rawData || []).map((item, index) => {
          const monthVal =
            item?.month ?? item?.Month ?? item?.months ?? item?.moth ?? ''

          return {
            ...item,
            idFromApi: item?.id,
            id: index,
            monthLabel: monthVal,
            month: monthVal,
            mode: item?.mode ?? item?.Mode ?? '',
            mXOGeneration_tph:
              item?.mXOGeneration_tph ??
              item?.mxoGeneration_tph ??
              item?.MXOGeneration_tph ??
              0,
            mXODowntime_hrs:
              item?.mXODowntime_hrs ??
              item?.mxoDowntime_hrs ??
              item?.MXODowntime_hrs ??
              0,
            mXOgeneration_TPM:
              item?.mXOgeneration_TPM ??
              item?.mxoGeneration_TPM ??
              item?.MXOGeneration_TPM ??
              0,
            onstream_hrs: item?.onstream_hrs ?? item?.Onstream_hrs ?? 0,
            maxMXOReprocessingRate_tph:
              item?.maxMXOReprocessingRate_tph ??
              item?.MaxMXOReprocessingRate_tph ??
              0,
            mXODowntimeInHrsId:
              item?.mXODowntimeInHrsId ??
              item?.mxoDowntimeInHrsId ??
              item?.['MXO Downtime in hrs Id'] ??
              null,
            maxMXOReprocessingRateInTphId:
              item?.maxMXOReprocessingRateInTphId ??
              item?.['Max MXO Reprocessing rate in tph Id'] ??
              null,
            remarks: item?.remarks ?? item?.Remarks ?? '',
            originalRemark: item?.remarks ?? item?.Remarks ?? '',
            isEditable: item?.isEditable || item?.IsEditable || !READ_ONLY,
          }
        })
        setRows(formattedData)
      } else {
        setRows([])
      }
    } catch (error) {
      console.error('Error fetching MXO Regeneration data:', error)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, READ_ONLY])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  // Save payload cleaned to match MXOReprocessingDTO 1:1
  const saveChanges = useCallback(async () => {
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) return

    const requiredFields = ['Remarks']
    const validationData = modifiedData.map((row) => ({
      ...row,
      Remarks: row.Remarks || row.remarks || '',
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
        month: row.month ?? row.monthLabel ?? '',
        mode: row.mode ?? '',
        mXOGeneration_tph: Number(
          row.mXOGeneration_tph ?? row.mxoGeneration_tph ?? 0,
        ),
        onstream_hrs: Number(row.onstream_hrs ?? 0),
        mXOgeneration_TPM: Number(
          row.mXOgeneration_TPM ?? row.mxoGeneration_TPM ?? 0,
        ),
        mXODowntime_hrs: Number(
          row.mXODowntime_hrs ?? row.mxoDowntime_hrs ?? 0,
        ),
        maxMXOReprocessingRate_tph: Number(
          row.maxMXOReprocessingRate_tph ?? row.MaxMXOReprocessingRate_tph ?? 0,
        ),
        aopYear: row.aopYear || AOP_YEAR,
        mXODowntimeInHrsId:
          row.mXODowntimeInHrsId ||
          row.mxoDowntimeInHrsId ||
          row['MXO Downtime in hrs Id'] ||
          null,
        maxMXOReprocessingRateInTphId:
          row.maxMXOReprocessingRateInTphId ||
          row['Max MXO Reprocessing rate in tph Id'] ||
          null,
        remarks: row.remarks ?? row.Remarks ?? '',
      }))

      const response = await ProductionNormsApiService.saveMxoRegenerationData(
        PLANT_ID,
        payload,
        keycloak,
        AOP_YEAR,
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
      console.error('Error saving MXO Regeneration data:', error)
      setSnackbarData({ message: 'Error saving data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, keycloak, PLANT_ID, AOP_YEAR, fetchData])

  const getAdjustedPermissions = (perms, isOld) => {
    if (isOld != 1) return perms
    return {
      ...perms,
      showAction: false,
      addButton: false,
      deleteButton: false,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOld,
      allAction: false,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: permissions?.showAction ?? true,
      saveWithRemark: permissions?.saveWithRemark ?? true,
      saveBtn: true,
      allAction: true,
      showTitleNameBusiness: true,
      titleName: 'Mxo Regeneration',
      downloadExcelBtn: false,
      downloadExcelBtnFromUI: true,
      uploadExcelBtn: false,
      ...permissions,
    },
    IS_OLD_YEAR,
  )

  return (
    <div>
      <LoaderBackdrop open={!!loading} />

      <KendoDataTables
        rows={rows}
        setRows={setRows}
        columns={colDefs}
        permissions={adjustedPermissions}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title={SCREEN_NAME}
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
    </div>
  )
}

export default MxoRegeneration
