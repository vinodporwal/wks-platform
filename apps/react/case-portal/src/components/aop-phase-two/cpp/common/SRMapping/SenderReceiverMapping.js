import { useState, useMemo, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { UtilityPlantApiServiceV2 } from 'components/aop-phase-two/services/cpp/jmd/utilityPlantApiServiceV2'
import { Box, Stack, Tooltip, IconButton } from '@mui/material'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import DeleteDialog from 'components/aop-phase-two/common/AdvanceKendoTable/components/DeleteDialog'
import { useDebounce } from 'hooks/useDebounce'

// ── Constants ────────────────────────────────────────────────────────────────

const PREFIXES = ['sender', 'receiver']

const GROUP_FIELDS = {
  Plant: ['Name', 'Id', 'Code'],
  CostCenter: ['Name', 'Id', 'Code'],
  Utility: ['Name', 'Id', 'Code', 'UOM'],
}

const getFields = (prefix, group) =>
  GROUP_FIELDS[group].map((s) => `${prefix}${group}${s}`)

const getAllFields = (prefix) =>
  Object.keys(GROUP_FIELDS).flatMap((group) => getFields(prefix, group))

// Comprehensive dependency reset mapping
const DEPENDENCY_RESETS = {
  cppPlantId: [
    ...PREFIXES.flatMap((p) => getAllFields(p)),
    // Also reset utilities specifically
    ...PREFIXES.flatMap((p) => getFields(p, 'Utility')),
  ],
  senderPlantName: getFields('sender', 'Utility'),
  receiverPlantName: getFields('receiver', 'Utility'),
  senderCostCenterName: [],
  receiverCostCenterName: [],
  // When utility changes, no dependent fields to reset
}

const DROPDOWN_FIELD_CONFIG = {
  senderPlantName: {
    source: 'plantsDropdown',
    prefix: 'sender',
    group: 'Plant',
    valueField: 'value',
    labelField: 'label',
    codeField: 'code',
    idField: 'plantId',
  },
  receiverPlantName: {
    source: 'plantsDropdown',
    prefix: 'receiver',
    group: 'Plant',
    valueField: 'value',
    labelField: 'label',
    codeField: 'code',
    idField: 'plantId',
  },
  senderCostCenterName: {
    source: 'costCentersDropdown',
    prefix: 'sender',
    group: 'CostCenter',
    valueField: 'value',
    labelField: 'label',
    codeField: 'code',
    idField: 'costCenterId',
  },
  receiverCostCenterName: {
    source: 'costCentersDropdown',
    prefix: 'receiver',
    group: 'CostCenter',
    valueField: 'value',
    labelField: 'label',
    codeField: 'code',
    idField: 'costCenterId',
  },
  senderUtilityName: {
    source: 'normParameters',
    prefix: 'sender',
    group: 'Utility',
    normTypeFkId: 2,
    valueField: 'displayName',
    labelField: 'displayName',
    codeField: 'sapMaterialCode',
    idField: 'id',
    uomField: 'uom',
  },
  receiverUtilityName: {
    source: 'normParameters',
    prefix: 'receiver',
    group: 'Utility',
    normTypeFkId: 1,
    valueField: 'displayName',
    labelField: 'displayName',
    codeField: 'sapMaterialCode',
    idField: 'id',
    uomField: 'uom',
  },
}

// ── Component ────────────────────────────────────────────────────────────────

const SenderReceiverMapping = () => {
  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [plantsDropdown, setPlantsDropdown] = useState([])
  const [costCentersDropdown, setCostCentersDropdown] = useState([])
  const [normParameters, setNormParameters] = useState([])

  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [rowToDelete, setRowToDelete] = useState(null)

  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalObject,
    siteObject,
    plantObject,
    year,
    screenTitle,
    jmdSelectedPlants,
  } = dataGridStore
  const PLANT_ID = plantObject?.id
  const lowerSiteName = siteObject?.name?.toLowerCase()
  const AOP_YEAR = year?.selectedYear

  const PLANT_ID_LIST = useMemo(
    () =>
      lowerSiteName === 'jmd'
        ? jmdSelectedPlants?.map((plant) => plant.id) || []
        : [PLANT_ID],
    [jmdSelectedPlants, lowerSiteName, PLANT_ID],
  )

  const cppPlantList = useMemo(
    () =>
      lowerSiteName === 'jmd'
        ? jmdSelectedPlants?.map((plant) => ({
            value: plant.id?.toLowerCase(),
            label: plant.name,
          }))
        : [
            {
              value: plantObject?.id?.toLowerCase(),
              label: plantObject?.name,
            },
          ],
    [jmdSelectedPlants, plantObject, lowerSiteName],
  )

  // ── Data Fetching ──────────────────────────────────────────────────────────

  const fetchNormParameters = useCallback(async () => {
    if (!PLANT_ID) return
    try {
      const res = await UtilityPlantApiServiceV2.getNormParameters(
        keycloak,
        PLANT_ID,
      )
      const data = res?.data || []
      setNormParameters(data)
    } catch (error) {
      console.error('Error fetching norm parameters:', error)
      setNormParameters([])
    }
  }, [keycloak, PLANT_ID])

  const fetchAssociatedFieldIds = useCallback(async () => {
    try {
      const plantsResponse = await UtilityPlantApiServiceV2.getSRMappingPlants(
        keycloak,
        PLANT_ID_LIST,
      )
      const plantsData = plantsResponse?.data || []
      const plantsOptions = plantsData.map((plant) => ({
        plantId: plant.plantId.toLowerCase(),
        value: plant.plantName,
        label: plant.plantName || plant.plantCode || 'Unknown Plant',
        code: plant.plantCode || '',
        sourceName: plant.sourceName?.toLowerCase() || '',
      }))
      setPlantsDropdown(plantsOptions)

      const costCentersResponse =
        await UtilityPlantApiServiceV2.getSRMappingCostCenters(
          keycloak,
          PLANT_ID_LIST,
        )
      const costCentersData = costCentersResponse?.data || []
      const costCentersOptions = costCentersData.map((cc) => ({
        costCenterId: cc.id.toLowerCase(),
        value: cc.costCenterName,
        label: cc.costCenterName || '',
        code: cc.costCenterCode || '',
        cppPlantFkId: cc.cppPlantFkId.toLowerCase() || '',
      }))
      setCostCentersDropdown(costCentersOptions)
    } catch (error) {
      console.error('Error fetching dropdown options:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error loading dropdown options',
        severity: 'error',
      })
    }
  }, [keycloak, PLANT_ID_LIST])

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await UtilityPlantApiServiceV2.getSRMappingByPlant(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )

      const apiRows = res?.data?.SRMappingResponse || res?.data || []

      if (!apiRows || apiRows.length === 0) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        setLoading(false)
        return
      }

      const formattedData = apiRows.map((item, index) => ({
        ...item,
        remarks: item?.remarks || '',
        id: item?.id || index + 1,
        apiId: item?.id,
      }))
      setRows(formattedData)
      setOriginalRows(formattedData)
    } catch (error) {
      console.error('Error fetching SR mapping data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR])

  useDebounce(
    () => {
      setRows([])
      setOriginalRows([])
      setModifiedCells({})

      if (PLANT_ID_LIST?.length && AOP_YEAR) {
        fetchData()
        fetchAssociatedFieldIds()
        fetchNormParameters()
      }
    },
    1000,
    [
      PLANT_ID_LIST,
      AOP_YEAR,
      fetchData,
      fetchAssociatedFieldIds,
      fetchNormParameters,
    ],
  )

  // ── Filter Functions ──────────────────────────────────────────────────────

  const getFilteredSenderUtilities = useCallback(
    (dataItem) => {
      const selectedPlantId = dataItem?.senderPlantId
      if (!selectedPlantId) return []
      const filtered = normParameters.filter(
        (np) =>
          np.normTypeFkId === 2 &&
          np.plantFkId?.toLowerCase() === selectedPlantId?.toLowerCase(),
      )
      return filtered.map((np) => ({
        value: np.displayName || np.name,
        label: np.displayName || np.name,
        sapMaterialCode: np.sapMaterialCode || '',
        uom: np.uom || '',
      }))
    },
    [normParameters],
  )

  const getFilteredReceiverUtilities = useCallback(
    (dataItem) => {
      const selectedPlantId = dataItem?.receiverPlantId
      if (!selectedPlantId) return []
      const filtered = normParameters.filter(
        (np) =>
          np.normTypeFkId === 1 &&
          np.plantFkId?.toLowerCase() === selectedPlantId?.toLowerCase(),
      )
      return filtered.map((np) => ({
        value: np.displayName || np.name,
        label: np.displayName || np.name,
        sapMaterialCode: np.sapMaterialCode || '',
        uom: np.uom || '',
      }))
    },
    [normParameters],
  )

  const getFilteredCostCenters = useCallback(
    (dataItem) => {
      const selectedCppPlantId = dataItem?.cppPlantId
      if (!selectedCppPlantId) return []

      const filtered = costCentersDropdown.filter(
        (cc) =>
          cc.cppPlantFkId?.toLowerCase() === selectedCppPlantId?.toLowerCase(),
      )

      return filtered.length ? filtered : []
    },
    [costCentersDropdown],
  )

  const getFilteredPlants = useCallback(
    (dataItem) => {
      const selectedCppPlantId = dataItem?.cppPlantId
      if (!selectedCppPlantId) return []

      const filtered = plantsDropdown.filter(
        (p) =>
          p.sourceName?.toLowerCase() === selectedCppPlantId?.toLowerCase(),
      )
      console.log('filtered', filtered)
      return filtered.length ? filtered : []
    },
    [plantsDropdown],
  )

  // ── Core Update Logic ──────────────────────────────────────────────────────

  const applyRowUpdate = useCallback((rowId, changedField, updates) => {
    // Get fields to reset based on the changed field
    const resetFields = DEPENDENCY_RESETS[changedField] || []

    // Build reset object with empty strings
    const resetUpdates = Object.fromEntries(
      resetFields.map((field) => [field, '']),
    )

    // Combine updates and resets
    const allUpdates = {
      ...updates,
      ...resetUpdates,
      // Track that this row has been modified
      inEdit: true,
    }

    // Update rows
    setRows((prevRows) =>
      prevRows.map((row) =>
        row.id === rowId ? { ...row, ...allUpdates } : row,
      ),
    )

    // Update modifiedCells
    setModifiedCells((prev) => ({
      ...prev,
      [rowId]: {
        ...prev[rowId],
        ...allUpdates,
      },
    }))
  }, [])

  // ── Custom Item Change Handler ───────────────────────────────────────────

  const handleCustomItemChange = useCallback(
    (e, setRows) => {
      const { dataItem, field, value } = e

      if (!dataItem || !field) return

      // Handle cppPlantId change - reset everything
      if (field === 'cppPlantId') {
        applyRowUpdate(dataItem.id, field, { cppPlantId: value })
        return
      }

      // Handle text field changes
      const config = DROPDOWN_FIELD_CONFIG[field]
      if (!config) {
        // It's a text field or remark
        applyRowUpdate(dataItem.id, field, { [field]: value })
        return
      }

      // Handle dropdown changes
      if (!value) {
        // Clear the field
        applyRowUpdate(dataItem.id, field, { [field]: value })
        return
      }

      // Get the appropriate dropdown data source
      let dropdownSource
      switch (config.source) {
        case 'plantsDropdown':
          dropdownSource = plantsDropdown
          break
        case 'costCentersDropdown':
          dropdownSource = costCentersDropdown
          break
        case 'normParameters':
          dropdownSource = normParameters
          break
        default:
          return
      }

      // Find the selected item
      let selectedItem = dropdownSource.find(
        (item) =>
          (item[config.valueField] || item.value)?.toLowerCase() ===
          value?.toLowerCase(),
      )

      // For utility fields, also filter by normTypeFkId
      if (config.normTypeFkId && selectedItem) {
        const filteredItems = dropdownSource.filter(
          (item) => item.normTypeFkId === config.normTypeFkId,
        )
        const isValid = filteredItems.some(
          (item) =>
            (item[config.valueField] || item.value)?.toLowerCase() ===
            value?.toLowerCase(),
        )
        if (!isValid) {
          selectedItem = null
        }
      }

      if (!selectedItem) return

      // Build updates based on field type
      let updates = {}

      if (config.group === 'Utility') {
        // Utility fields
        updates = {
          [`${config.prefix}UtilityName`]:
            selectedItem[config.labelField] || value,
          [`${config.prefix}UtilityId`]: selectedItem[config.idField] || '',
          [`${config.prefix}UtilityCode`]: selectedItem[config.codeField] || '',
          [`${config.prefix}UtilityUOM`]: selectedItem[config.uomField] || '',
        }
      } else {
        // Plant or Cost Center fields
        updates = {
          [`${config.prefix}${config.group}Name`]:
            selectedItem[config.labelField] || value,
          [`${config.prefix}${config.group}Id`]:
            selectedItem[config.idField] || '',
          [`${config.prefix}${config.group}Code`]:
            selectedItem[config.codeField] || '',
        }
      }

      applyRowUpdate(dataItem.id, field, updates)
    },
    [plantsDropdown, costCentersDropdown, normParameters, applyRowUpdate],
  )

  // ── Column Definitions ────────────────────────────────────────────────────

  const columns = [
    {
      field: 'cppPlantId',
      title: 'CPP Plant',
      widthT: 200,
      minWidth: 200,
      editable: true,
      type: 'select',
      searchable: true,
      displayMode: 'label',
      options: cppPlantList,
    },
    {
      field: 'senderCostCenterName',
      title: 'Sender Cost Center',
      widthT: 200,
      minWidth: 200,
      type: 'select',
      dynamicOptions: true,
      displayMode: 'label',
      getOptions: getFilteredCostCenters,
      editable: true,
    },
    {
      field: 'senderCostCenterCode',
      title: 'Sender Cost Center Code',
      widthT: 200,
      minWidth: 200,
      type: 'text',
      editable: false,
    },
    {
      field: 'senderPlantName',
      title: 'Sender Plant',
      widthT: 200,
      minWidth: 200,
      type: 'select',
      dynamicOptions: true,
      displayMode: 'label',
      getOptions: getFilteredPlants,
      editable: true,
    },
    {
      field: 'senderPlantCode',
      title: 'Sender Plant Code',
      widthT: 200,
      minWidth: 200,
      type: 'text',
      editable: false,
    },
    {
      field: 'senderUtilityName',
      title: 'Utility',
      widthT: 150,
      minWidth: 150,
      type: 'select',
      dynamicOptions: true,
      displayMode: 'label',
      getOptions: getFilteredSenderUtilities,
      editable: true,
    },
    {
      field: 'senderUtilityCode',
      title: 'Utility Code',
      widthT: 150,
      minWidth: 150,
      type: 'text',
      editable: false,
    },
    {
      field: 'senderUtilityUOM',
      title: 'Utility UOM',
      widthT: 150,
      minWidth: 150,
      type: 'text',
      editable: false,
    },
    {
      field: 'receiverCostCenterName',
      title: 'Receiver Cost Center',
      widthT: 180,
      minWidth: 180,
      editable: true,
      type: 'select',
      dynamicOptions: true,
      displayMode: 'label',
      getOptions: getFilteredCostCenters,
    },
    {
      field: 'receiverCostCenterCode',
      title: 'Receiver Cost Center Code',
      widthT: 200,
      minWidth: 200,
      type: 'text',
      editable: false,
    },
    {
      field: 'receiverPlantName',
      title: 'Receiver Plant',
      widthT: 200,
      minWidth: 200,
      type: 'select',
      dynamicOptions: true,
      displayMode: 'label',
      getOptions: getFilteredPlants,
      editable: true,
    },
    {
      field: 'receiverPlantCode',
      title: 'Receiver Plant Code',
      widthT: 200,
      minWidth: 200,
      type: 'text',
      editable: false,
    },
    {
      field: 'receiverUtilityName',
      title: 'Receiver Utility',
      widthT: 150,
      minWidth: 150,
      type: 'select',
      dynamicOptions: true,
      displayMode: 'label',
      getOptions: getFilteredReceiverUtilities,
      editable: true,
      hidden: false,
    },
    {
      field: 'receiverUtilityCode',
      title: 'Receiver Utility Code',
      widthT: 180,
      minWidth: 180,
      type: 'text',
      editable: false,
      hidden: false,
    },
    {
      field: 'receiverUtilityUOM',
      title: 'Receiver Utility UOM',
      widthT: 180,
      minWidth: 180,
      type: 'text',
      editable: false,
    },
    {
      field: 'remarks',
      title: 'Remarks',
      widthT: 200,
      minWidth: 200,
      type: 'textarea',
      editable: true,
    },
  ]

  // ── Delete Logic ──────────────────────────────────────────────────────────

  const handleDeleteRow = (row) => {
    setRows((prev) => prev.filter((r) => r.id !== row.id))
    setOriginalRows((prev) => prev.filter((r) => r.id !== row.id))
    setModifiedCells((prev) => {
      const next = { ...prev }
      delete next[row.id]
      return next
    })
  }

  const handleConfirmDelete = async () => {
    if (!rowToDelete) return

    setDeleteDialogOpen(false)
    setLoading(true)

    try {
      if (rowToDelete.apiId != null) {
        await UtilityPlantApiServiceV2.deleteSRMapping(
          keycloak,
          rowToDelete.apiId,
        )
      }
      handleDeleteRow(rowToDelete)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'SR Mapping deleted successfully!',
        severity: 'success',
      })
      fetchData()
    } catch (error) {
      console.error('Error deleting SR mapping:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to delete SR Mapping. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
      setRowToDelete(null)
    }
  }

  const DeleteActionCell = ({ dataItem, tdProps }) => (
    <td
      {...tdProps}
      style={{
        ...tdProps?.style,
        textAlign: 'center',
        verticalAlign: 'middle',
      }}
    >
      <Box sx={{ display: 'flex', justifyContent: 'center' }}>
        <Tooltip title='Delete Row'>
          <IconButton
            size='small'
            color='error'
            onClick={() => {
              setRowToDelete(dataItem)
              setDeleteDialogOpen(true)
            }}
          >
            <DeleteOutlineIcon fontSize='small' />
          </IconButton>
        </Tooltip>
      </Box>
    </td>
  )

  // ── Permissions ───────────────────────────────────────────────────────────

  const permissions = {
    showAction: true,
    addButton: true,
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    downloadExcelBtnFromUI: false,
    ExcelName: 'Sender Receiver Mapping',
    showImport: false,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName:
      screenTitle?.title || 'Sender Receiver Mapping (Utility for Utility)',
  }

  // ── Save / Export / Import ───────────────────────────────────────────────

  const saveChanges = async () => {
    setLoading(true)

    const modifiedData = Object.values(modifiedCells)
    const dataToSave = modifiedData.filter((row) => row.inEdit || row.isNew)

    if (!dataToSave || dataToSave.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
      setLoading(false)
      return
    }

    const payload = dataToSave.map((item) => {
      const { inEdit, isNew, saveStatus, errDescription, ...rest } = item
      return {
        ...rest,
        aopYear: AOP_YEAR,
        ...(isNew ? { id: null } : {}),
      }
    })

    try {
      const response = await UtilityPlantApiServiceV2.updateSRMappingsByPlant(
        keycloak,
        payload,
        AOP_YEAR,
      )

      const isSuccess = response?.code === 200

      setSnackbarOpen(true)
      setSnackbarData({
        message: isSuccess
          ? response?.message || 'Successfully saved changes!'
          : response?.message || 'Failed to save changes. Please try again.',
        severity: isSuccess ? 'success' : 'error',
      })

      if (isSuccess) {
        setModifiedCells({})
        await fetchData()
      }

      return response
    } catch (error) {
      console.error('Error saving SR mapping:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to save changes. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleExcelUpload = async (file) => {
    if (!file) return

    setLoading(true)
    try {
      const response = await UtilityPlantApiServiceV2.importSRMappingExcel(
        file,
        keycloak,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Excel file imported successfully!',
          severity: 'success',
        })
        await fetchData()
        return
      }

      if (response?.code === 400 && response?.data) {
        try {
          const base64Data = response.data
          const binaryString = window.atob(base64Data)
          const bytes = new Uint8Array(binaryString.length)
          for (let i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i)
          }
          const blob = new Blob([bytes], {
            type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          })
          const url = window.URL.createObjectURL(blob)
          const link = document.createElement('a')
          link.href = url
          link.download = `SRMapping_Errors_${new Date().getTime()}.xlsx`
          document.body.appendChild(link)
          link.click()
          document.body.removeChild(link)
          window.URL.revokeObjectURL(url)

          setSnackbarOpen(true)
          setSnackbarData({
            message:
              response?.message ||
              'Import failed with errors. Please check the downloaded file.',
            severity: 'error',
          })
          await fetchData()
          return
        } catch (downloadError) {
          console.error('Error downloading error file:', downloadError)
          setSnackbarOpen(true)
          setSnackbarData({
            message: 'Import failed but could not download error file.',
            severity: 'error',
          })
          return
        }
      }

      setSnackbarOpen(true)
      setSnackbarData({
        message: response?.message || 'Failed to import Excel file.',
        severity: 'error',
      })
    } catch (error) {
      console.error('Error uploading Excel file:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Failed to import Excel file: ${error.message}`,
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
      severity: 'info',
    })

    try {
      await UtilityPlantApiServiceV2.exportSRMappingExcel(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting SR mapping:', error)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <Stack sx={{ mt: 2 }}>
        <AdvanceKendoTable
          columns={columns}
          rows={rows}
          setRows={setRows}
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          title={permissions.showTitle ? permissions.titleName : ''}
          permissions={permissions}
          handleExport={handleExport}
          handleExcelUpload={handleExcelUpload}
          saveChanges={saveChanges}
          fetchData={fetchData}
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          customHeight={80}
          customItemChange={handleCustomItemChange}
          paginationConfig={{
            threshold: 100,
            buttonCount: 5,
            pageSizes: [10, 20, 50, 100],
            defaultPageSize: 100,
          }}
          handleRemarkCellClick={handleRemarkCellClick}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
          setCurrentRowId={() => {}}
          customActionCell={DeleteActionCell}
        />
      </Stack>

      <DeleteDialog
        openDeleteDialogeBox={deleteDialogOpen}
        setOpenDeleteDialogeBox={setDeleteDialogOpen}
        deleteTheRecord={handleConfirmDelete}
        message='Are you sure you want to delete this SR Mapping record?'
        confirmButtonText='Delete'
      />
    </Box>
  )
}

export default SenderReceiverMapping
