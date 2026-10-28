import { useEffect, useState, useMemo, useCallback } from 'react'
import { Box, IconButton, Tooltip } from '@mui/material'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { InputApiService } from 'components/aop-phase-two/services/cpp/jmd/inputApiService'
import { TabAccessApiService } from 'components/aop-phase-two/services/common/tabAccessApiService'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import DeleteDialog from 'components/aop-phase-two/common/AdvanceKendoTable/components/DeleteDialog'
import { useDebounce } from 'hooks/useDebounce'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

const MONTH_TO_INDEX = {
  apr: 4,
  may: 5,
  jun: 6,
  jul: 7,
  aug: 8,
  sep: 9,
  oct: 10,
  nov: 11,
  dec: 12,
  jan: 1,
  feb: 2,
  mar: 3,
}

const MONTH_FIELDS = [
  'apr',
  'may',
  'jun',
  'jul',
  'aug',
  'sep',
  'oct',
  'nov',
  'dec',
  'jan',
  'feb',
  'mar',
]

// Parse the keycloak token `plants` claim into an allowed-map:
// allowedMap[verticalId][siteId] = [plantId, ...]
const parseAllowed = (raw) => {
  if (!Array.isArray(raw)) return {}
  return raw.reduce((map, vObj) => {
    const vid = Object.keys(vObj)[0]
    if (!vid) return map
    map[vid] = vObj[vid].reduce((siteMap, siteObj) => {
      const sid = Object.keys(siteObj)[0]
      if (sid) siteMap[sid] = siteObj[sid]
      return siteMap
    }, {})
    return map
  }, {})
}

const InterSitePowerTransferGrid = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
    jmdSelectedPlants,
  } = dataGridStore
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(
    dataGridStore,
    'Inter_Site_Power_Transfer',
  )

  // Multi-plant list (selected plants) — used for save/fetch query params
  const PLANT_ID_LIST = useMemo(
    () => jmdSelectedPlants?.map((plant) => plant.id) ?? [],
    [jmdSelectedPlants],
  )

  const headerMap = generateHeaderNames(AOP_YEAR)
  const valueFormat = ValueFormatterPhaseTwo()

  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [rowToDelete, setRowToDelete] = useState(null)

  // All plants available in the current vertical (CPP) + site (JMD),
  // filtered by the user's allowed plants from the keycloak token.
  const [plantOptions, setPlantOptions] = useState([])

  const fetchPlantOptions = useCallback(async () => {
    if (!verticalObject?.id || !siteObject?.id) return
    try {
      const fullDetails =
        await TabAccessApiService.getPlantSiteVertical(keycloak)
      if (!Array.isArray(fullDetails)) {
        setPlantOptions([])
        return
      }

      let allowedMap = {}
      try {
        const parsed = JSON.parse(keycloak?.idTokenParsed?.plants || '[]')
        allowedMap = parseAllowed(parsed)
      } catch (e) {
        console.error('Token parse error (plants claim):', e)
      }

      const vertObj = fullDetails.find((v) => v.id === verticalObject.id)
      const siteObj = vertObj?.sites?.find((s) => s.id === siteObject.id)
      const allowedPlants =
        allowedMap[verticalObject.id]?.[siteObject.id] || null

      const list = (siteObj?.plants || [])
        .filter((p) => (allowedPlants ? allowedPlants.includes(p.id) : true))
        .map((p) => ({
          value: p.id,
          label: p.displayName || p.name || p.id,
        }))

      setPlantOptions(list)
    } catch (error) {
      console.error('Error fetching plant options:', error)
      setPlantOptions([])
    }
  }, [keycloak, verticalObject, siteObject])

  useEffect(() => {
    fetchPlantOptions()
  }, [fetchPlantOptions])

  // To Plant options exclude the selected From Plant for a given row
  const getToPlantOptionsForRow = useCallback(
    (dataItem) => plantOptions.filter((p) => p.value !== dataItem?.fromPlantId),
    [plantOptions],
  )

  const monthBaseColumnConfig = {
    editable: true,
    widthT: 100,
    minWidth: 100,
    align: 'left',
    headerAlign: 'left',
    type: 'number1',
    format: valueFormat,
  }

  const MONTH_COLUMNS = MONTH_FIELDS.map((mon) => ({
    ...monthBaseColumnConfig,
    field: mon,
    title: headerMap[MONTH_TO_INDEX[mon]],
  }))

  const ActionCell = ({ dataItem, tdProps }) => (
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
          <span>
            <IconButton
              size='medium'
              color='error'
              onClick={() => {
                setRowToDelete(dataItem)
                setDeleteDialogOpen(true)
              }}
            >
              <DeleteOutlineIcon fontSize='medium' />
            </IconButton>
          </span>
        </Tooltip>
      </Box>
    </td>
  )

  const columns = [
    {
      field: 'fromPlantName',
      title: 'From Plant',
      type: 'select',
      options: plantOptions,
      displayMode: 'label',
      returnFullObject: true,
      editable: true,
      locked: true,
      minWidth: 180,
    },
    {
      field: 'toPlantName',
      title: 'To Plant',
      type: 'select',
      dynamicOptions: true,
      getOptions: getToPlantOptionsForRow,
      displayMode: 'label',
      returnFullObject: true,
      editable: true,
      locked: true,
      minWidth: 180,
    },
    {
      field: 'uom',
      title: 'UOM',
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 100,
    },
    ...MONTH_COLUMNS,
    {
      field: 'remarks',
      title: 'Remarks',
      widthT: 250,
      type: 'textarea',
      editable: true,
      minWidth: 250,
    },
    {
      field: 'customActions',
      title: 'Action',
      type: 'customAction',
      minWidth: 100,
      className: 'k-text-center',
      cell: ActionCell,
      locked: true,
      lockPosition: 'right',
    },
  ]

  // Fetch inter site power transfer data for the grid
  const fetchData = useCallback(async () => {
    if (!PLANT_ID_LIST.length || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await InputApiService.getInterSitePowerTransfer(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )
      const data = response?.data?.interSitePowerTransfers || []

      if (!data || data.length === 0) {
        setRows([])
        setOriginalRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        return
      }

      const rowsWithId = data.map((row, index) => ({
        ...row,
        id: row.id || `row_${index}`,
        remarks: row.remarks || '',
      }))
      setRows(rowsWithId)
      setOriginalRows(rowsWithId)
    } catch (error) {
      // Backend endpoint may not exist yet — start with empty grid
      console.error('Error fetching inter site power transfer data:', error)
      setRows([])
      setOriginalRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR])

  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && AOP_YEAR) {
        fetchData()
      }
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR],
  )

  useEffect(() => {
    setModifiedCells({})
  }, [PLANT_ID_LIST, AOP_YEAR])

  // Custom item change handler for cascading selects.
  // With returnFullObject: true, value is the full option object { value, label }.
  // When fromPlant changes, populate fromPlantId/fromPlantName.
  // When toPlant changes, populate toPlantId/toPlantName + duplicate check.
  const handleCustomItemChange = useCallback(
    (e, setRowsState, setModifiedCellsState) => {
      const { dataItem, field, value } = e
      if (!dataItem || !field) return

      // With returnFullObject, value is { value, label } or null/'' when cleared
      const isObject = value && typeof value === 'object'
      const selectedId = isObject ? value.value : value
      const selectedLabel = isObject ? value.label : value

      const updates = { [field]: selectedLabel }

      if (field === 'fromPlantName') {
        updates.fromPlantId = selectedId || ''
        updates.fromPlantName = selectedLabel || ''
      } else if (field === 'toPlantName') {
        updates.toPlantId = selectedId || ''
        updates.toPlantName = selectedLabel || ''

        // Duplicate check: From → To combination must be unique across rows
        if (selectedId && dataItem.fromPlantId) {
          const duplicate = rows.some(
            (r) =>
              r.id !== dataItem.id &&
              r.fromPlantId === dataItem.fromPlantId &&
              r.toPlantId === selectedId,
          )
          if (duplicate) {
            const fromName =
              plantOptions.find((o) => o.value === dataItem.fromPlantId)
                ?.label || dataItem.fromPlantName
            const toName = selectedLabel
            setSnackbarOpen(true)
            setSnackbarData({
              message: `A transfer from "${fromName}" to "${toName}" already exists. Please choose a different combination.`,
              severity: 'error',
            })
            // Clear the toPlant selection
            updates.toPlantId = ''
            updates.toPlantName = ''
          }
        }
      }

      // Update rows
      setRowsState((prevRows) =>
        prevRows.map((row) =>
          row.id === dataItem.id ? { ...row, ...updates } : row,
        ),
      )

      // Update modifiedCells
      setModifiedCellsState((prev) => {
        const existing = prev[dataItem.id] || {}
        return {
          ...prev,
          [dataItem.id]: {
            ...existing,
            ...updates,
            id: dataItem.id,
            inEdit: true,
          },
        }
      })
    },
    [plantOptions, rows],
  )

  const permissions = {
    showAction: true,
    addButton: true,
    addBtnName: 'Add Item',
    editButton: false,
    saveBtn: true,
    allAction: true,
    showExport: true,
    ExcelName: EXCEL_NAME,
    showImport: true,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Inter Site Power Transfer',
  }

  const saveChanges = async () => {
    setLoading(true)
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      setLoading(false)
      return
    }

    const data = modifiedData.filter((row) => row.inEdit)
    if (data.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      setLoading(false)
      return
    }

    // Validate: From Plant and To Plant are required and must differ
    for (const row of data) {
      if (!row.fromPlantId) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Please select a From Plant for the new row.',
          severity: 'error',
        })
        setLoading(false)
        return
      }
      if (!row.toPlantId) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Please select a To Plant for the new row.',
          severity: 'error',
        })
        setLoading(false)
        return
      }
      if (row.fromPlantId === row.toPlantId) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'To Plant must be different from From Plant.',
          severity: 'error',
        })
        setLoading(false)
        return
      }
    }

    // Validate: no duplicate From Plant + To Plant combinations
    const seenCombos = new Set()
    for (const row of data) {
      const combo = `${row.fromPlantId}__${row.toPlantId}`
      if (seenCombos.has(combo)) {
        const fromName =
          plantOptions.find((o) => o.value === row.fromPlantId)?.label ||
          row.fromPlantName ||
          row.fromPlantId
        const toName =
          plantOptions.find((o) => o.value === row.toPlantId)?.label ||
          row.toPlantName ||
          row.toPlantId
        setSnackbarOpen(true)
        setSnackbarData({
          message: `Duplicate transfer from "${fromName}" to "${toName}". Each From Plant to To Plant combination must be unique.`,
          severity: 'error',
        })
        setLoading(false)
        return
      }
      seenCombos.add(combo)
    }

    // Check against existing (non-modified) rows in the grid
    const existingCombos = new Set(
      rows
        .filter(
          (r) =>
            !r.inEdit &&
            r.fromPlantId &&
            r.toPlantId &&
            r.id !== undefined &&
            !String(r.id).startsWith('new_row_'),
        )
        .map((r) => `${r.fromPlantId}__${r.toPlantId}`),
    )
    for (const row of data) {
      const combo = `${row.fromPlantId}__${row.toPlantId}`
      if (existingCombos.has(combo)) {
        const fromName =
          plantOptions.find((o) => o.value === row.fromPlantId)?.label ||
          row.fromPlantName ||
          row.fromPlantId
        const toName =
          plantOptions.find((o) => o.value === row.toPlantId)?.label ||
          row.toPlantName ||
          row.toPlantId
        setSnackbarOpen(true)
        setSnackbarData({
          message: `A transfer from "${fromName}" to "${toName}" already exists.`,
          severity: 'error',
        })
        setLoading(false)
        return
      }
    }

    const validationError = validateRowDataWithRemarks(
      data,
      originalRows,
      MONTH_FIELDS,
      'fromPlantName',
    )

    if (validationError) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: validationError,
        severity: 'error',
      })
      setLoading(false)
      return
    }

    // Validate: no reverse combination (A→B and B→A) with values in the same month.
    // Build a map of all rows (new + existing) keyed by "fromId__toId" with the
    // set of months that have a non-zero value. Then check if the reverse combo
    // shares any active month.
    const allRows = [
      ...data,
      ...rows.filter((r) => !r.inEdit && r.fromPlantId && r.toPlantId),
    ]
    const comboMonthMap = {}
    for (const row of allRows) {
      const key = `${row.fromPlantId}__${row.toPlantId}`
      if (!comboMonthMap[key]) comboMonthMap[key] = new Set()
      MONTH_FIELDS.forEach((m) => {
        const val = parseFloat(row[m])
        if (!isNaN(val) && val > 0) {
          comboMonthMap[key].add(m)
        }
      })
    }
    for (const row of data) {
      const forward = `${row.fromPlantId}__${row.toPlantId}`
      const reverse = `${row.toPlantId}__${row.fromPlantId}`
      const forwardMonths = comboMonthMap[forward] || new Set()
      const reverseMonths = comboMonthMap[reverse]
      if (reverseMonths) {
        const conflictMonths = [...forwardMonths].filter((m) =>
          reverseMonths.has(m),
        )
        if (conflictMonths.length > 0) {
          const fromName =
            plantOptions.find((o) => o.value === row.fromPlantId)?.label ||
            row.fromPlantName ||
            row.fromPlantId
          const toName =
            plantOptions.find((o) => o.value === row.toPlantId)?.label ||
            row.toPlantName ||
            row.toPlantId
          const monthLabels = conflictMonths
            .map((m) => m.charAt(0).toUpperCase() + m.slice(1))
            .join(', ')
          setSnackbarOpen(true)
          setSnackbarData({
            message: `Conflict: "${fromName}" → "${toName}" and reverse "${toName}" → "${fromName}" both have values in ${monthLabels}. A plant cannot send and receive power in the same month.`,
            severity: 'error',
          })
          setLoading(false)
          return
        }
      }
    }

    try {
      const payload = modifiedData.map((item) => {
        const {
          inEdit,
          isNew,
          isEditable,
          fromPlantName,
          toPlantName,
          ...rest
        } = item
        const sanitized = {
          ...rest,
          id: isNew ? null : rest.id,
          financialYear: AOP_YEAR,
        }
        // Convert empty strings to null
        Object.keys(sanitized).forEach((key) => {
          if (sanitized[key] === '' || sanitized[key] === undefined) {
            sanitized[key] = null
          }
        })
        return sanitized
      })

      await InputApiService.saveInterSitePowerTransfer(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
        payload,
      )

      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved ${modifiedData.length} changes!`,
        severity: 'success',
      })
      fetchData()
    } catch (error) {
      console.error('Error saving inter site power transfer data:', error)
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
      const response = await InputApiService.saveInterSitePowerTransferExcel(
        file,
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Excel file imported successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        await fetchData()
      } else if (response?.code === 400 && response?.data) {
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
          link.download = `Inter_Site_Power_Transfer_Errors_${new Date().getTime()}.xlsx`
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
        } catch (downloadError) {
          console.error('Error downloading error file:', downloadError)
          setSnackbarOpen(true)
          setSnackbarData({
            message: 'Import failed but could not download error file.',
            severity: 'error',
          })
        }
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Failed to import Excel file.',
          severity: 'error',
        })
      }
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
      await InputApiService.exportInterSitePowerTransferExcel(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
        EXCEL_NAME,
      )
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting inter site power transfer data:', error)
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

  const deleteRowData = (row) => {
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

    const isExistingRecord =
      typeof rowToDelete.id === 'string' && rowToDelete.id.length === 36

    if (isExistingRecord) {
      try {
        await InputApiService.deleteInterSitePowerTransfer(
          keycloak,
          rowToDelete.id,
        )
      } catch (error) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Failed to delete record from server.',
          severity: 'error',
        })
        setRowToDelete(null)
        return
      }
    }

    deleteRowData(rowToDelete)
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Record deleted successfully!',
      severity: 'success',
    })
    setRowToDelete(null)
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title={permissions.showTitle ? permissions.titleName : ''}
        permissions={permissions}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={() => {}}
        saveChanges={saveChanges}
        handleExcelUpload={handleExcelUpload}
        handleExport={handleExport}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        initialFieldValues={{
          fromPlantName: plantObject?.name || '',
          fromPlantId: plantObject?.id || '',
          toPlantName: '',
          toPlantId: '',
          uom: 'MW',
          remarks: '',
          ...MONTH_FIELDS.reduce((acc, m) => ({ ...acc, [m]: 0 }), {}),
        }}
        customItemChange={handleCustomItemChange}
        paginationConfig={{
          threshold: 100,
          buttonCount: 5,
          pageSizes: [10, 20, 50, 100],
          defaultPageSize: 100,
        }}
      />
      <DeleteDialog
        openDeleteDialogeBox={deleteDialogOpen}
        setOpenDeleteDialogeBox={setDeleteDialogOpen}
        deleteTheRecord={handleConfirmDelete}
        message='Are you sure you want to delete this Inter Site Power Transfer record?'
        confirmButtonText='Delete'
      />
    </Box>
  )
}

export default InterSitePowerTransferGrid
