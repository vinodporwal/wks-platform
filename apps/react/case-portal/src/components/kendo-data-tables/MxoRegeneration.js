import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from './index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import Notification from 'components/Utilities/Notification'
import { ProductionNormsApiService } from 'services/production-norms-api-service'
import { getRoleName } from 'services/role-service'
import { validateFields } from 'utils/validationUtils'

const monthOrder = {
  apr: 1,
  may: 2,
  jun: 3,
  jul: 4,
  aug: 5,
  sep: 6,
  oct: 7,
  nov: 8,
  dec: 9,
  jan: 10,
  feb: 11,
  mar: 12,
}

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

  const [rowsStock, setRowsStock] = useState([])
  const [loadingStock, setLoadingStock] = useState(false)
  const [modifiedCellsStock, setModifiedCellsStock] = useState({})

  const colDefsStock = useMemo(
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
        minWidth: 95,
        editable: false,
      },
      {
        field: 'mxoOpeningStock_MT',
        title: 'MXO opening stock in MT',
        width: 215,
        minWidth: 250,
        type: 'number',
        align: 'right',
        format: '{0:0.00}',
        editable: true,
      },
      {
        field: 'mxoGeneration_TPM',
        title: 'MXO generation (TPM)',
        width: 215,
        minWidth: 250,
        type: 'number',
        align: 'right',
        format: '{0:0.00}',
        editable: false,
      },
      {
        field: 'mxoReprocessing_TPM',
        title: 'MXO Reprocessing (TPM)',
        width: 215,
        minWidth: 250,
        type: 'number',
        align: 'right',
        format: '{0:0.00}',
        editable: false,
      },
      {
        field: 'mxoClosingStock_MT',
        title: 'MXO Closing stock in MT',
        width: 215,
        minWidth: 250,
        type: 'number',
        align: 'right',
        format: '{0:0.00}',
        editable: false,
      },
      {
        field: 'mxoOpeningStock_Id',
        title: 'MXOOpeningStock_Id',
        width: 180,
        minWidth: 170,
        editable: false,
        hidden: true,
      },
      {
        field: 'mxoClosingStock_Id',
        title: 'MXOClosingStock_Id',
        width: 180,
        minWidth: 170,
        editable: false,
        hidden: true,
      },
    ],
    [READ_ONLY],
  )

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
        minWidth: 95,
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
        width: 215,
        minWidth: 210,
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
        editable: true,
      },
      {
        field: 'mXOgeneration_TPM',
        title: 'MXO generation (TPM)',
        width: 215,
        minWidth: 210,
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
        width: 310,
        minWidth: 305,
        type: 'number',
        align: 'right',
        format: '{0:0.00}',
        editable: true,
      },
      {
        field: 'remarks',
        title: 'Remark',
        width: 175,
        minWidth: 170,
        editable: true,
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

        formattedData.sort((a, b) => {
          const monthA = a.monthLabel
            ? a.monthLabel.substring(0, 3).toLowerCase()
            : ''
          const monthB = b.monthLabel
            ? b.monthLabel.substring(0, 3).toLowerCase()
            : ''
          return (monthOrder[monthA] || 99) - (monthOrder[monthB] || 99)
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

  const fetchStockData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoadingStock(true)
    try {
      const response = await ProductionNormsApiService.getMxoStockData(
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
            mxoOpeningStock_MT:
              item?.mXOOpeningStockInMT ??
              item?.mxoOpeningStock_MT ??
              item?.['MXO opening stock in MT'] ??
              0,
            mxoGeneration_TPM:
              item?.mXOGeneration ??
              item?.mxoGeneration_TPM ??
              item?.['MXO generation (TPM)'] ??
              0,
            mxoReprocessing_TPM:
              item?.mXOReprocessing ??
              item?.mxoReprocessing_TPM ??
              item?.['MXO Reprocessing (TPM)'] ??
              0,
            mxoClosingStock_MT:
              item?.mXOClosingStockInMT ??
              item?.mxoClosingStock_MT ??
              item?.['MXO Closing stock in MT'] ??
              0,
            mxoOpeningStock_Id:
              item?.mXOOpeningStockId ??
              item?.mxoOpeningStock_Id ??
              item?.MXOOpeningStock_Id ??
              null,
            mxoClosingStock_Id:
              item?.mXOClosingStockId ??
              item?.mxoClosingStock_Id ??
              item?.MXOClosingStock_Id ??
              null,
            isEditable: !READ_ONLY && monthVal.toLowerCase().startsWith('apr'),
          }
        })

        formattedData.sort((a, b) => {
          const monthA = a.monthLabel
            ? a.monthLabel.substring(0, 3).toLowerCase()
            : ''
          const monthB = b.monthLabel
            ? b.monthLabel.substring(0, 3).toLowerCase()
            : ''
          return (monthOrder[monthA] || 99) - (monthOrder[monthB] || 99)
        })

        setRowsStock(formattedData)
      } else {
        setRowsStock([])
      }
    } catch (error) {
      console.error('Error fetching MXO Stock data:', error)
      setSnackbarData({
        message: 'Error fetching stock data',
        severity: 'error',
      })
      setSnackbarOpen(true)
    } finally {
      setLoadingStock(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, READ_ONLY])

  useEffect(() => {
    fetchData()
    fetchStockData()
  }, [fetchData, fetchStockData])

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
        aopYear: AOP_YEAR,
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

  const saveChangesStock = useCallback(async () => {
    const modifiedData = Object.values(modifiedCellsStock)
    if (modifiedData.length === 0) return

    setLoadingStock(true)
    try {
      const payload = modifiedData.map((row) => ({
        month: row.month ?? row.monthLabel ?? '',
        mXOOpeningStockInMT: Number(row.mxoOpeningStock_MT ?? 0),
        mXOGeneration: Number(row.mxoGeneration_TPM ?? 0),
        mXOReprocessing: Number(row.mxoReprocessing_TPM ?? 0),
        mXOClosingStockInMT: Number(row.mxoClosingStock_MT ?? 0),
        aopYear: AOP_YEAR,
        MXOOpeningStockId:
          row.mxoOpeningStock_Id || row.MXOOpeningStock_Id || null,
        MXOClosingStockId:
          row.mxoClosingStock_Id || row.MXOClosingStock_Id || null,
      }))

      const response = await ProductionNormsApiService.saveMxoStockData(
        PLANT_ID,
        payload,
        keycloak,
        AOP_YEAR,
      )

      if (response) {
        setSnackbarData({
          message: 'Saved Stock Successfully!',
          severity: 'success',
        })
        setSnackbarOpen(true)
        setModifiedCellsStock({})
        fetchStockData()
      } else {
        setSnackbarData({ message: 'Save Stock Failed!', severity: 'error' })
        setSnackbarOpen(true)
      }
    } catch (error) {
      console.error('Error saving MXO Stock data:', error)
      setSnackbarData({ message: 'Error saving stock data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoadingStock(false)
    }
  }, [modifiedCellsStock, keycloak, PLANT_ID, AOP_YEAR, fetchStockData])

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
      titleName: 'MXO Regeneration',
      downloadExcelBtn: false,
      downloadExcelBtnFromUI: true,
      ExcelName: `${verticalObject?.name}_${siteObject?.name}_${AOP_YEAR}_Mxo_Regeneration`,
      uploadExcelBtn: false,
      ...permissions,
    },
    IS_OLD_YEAR,
  )

  const adjustedPermissionsStock = getAdjustedPermissions(
    {
      showAction: permissions?.showAction ?? true,
      saveWithRemark: false,
      saveBtn: true,
      allAction: true,
      showTitleNameBusiness: true,
      titleName: 'MXO Stock',
      downloadExcelBtn: false,
      downloadExcelBtnFromUI: true,
      uploadExcelBtn: false,
      ExcelName: `${verticalObject?.name}_${siteObject?.name}_${AOP_YEAR}_Mxo_Stock`,
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

      <br />

      <LoaderBackdrop open={!!loadingStock} />

      <KendoDataTables
        rows={rowsStock}
        setRows={setRowsStock}
        columns={colDefsStock}
        permissions={adjustedPermissionsStock}
        modifiedCells={modifiedCellsStock}
        setModifiedCells={setModifiedCellsStock}
        title={'MXO Stock'}
        saveChanges={saveChangesStock}
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
