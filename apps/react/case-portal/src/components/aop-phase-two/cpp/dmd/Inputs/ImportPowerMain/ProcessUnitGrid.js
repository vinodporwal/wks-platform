import { useState, useCallback, useMemo, useEffect } from 'react'
import { Box, Tooltip, IconButton } from '@mui/material'
import EditIcon from '@mui/icons-material/Edit'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo, {
  customValueFormatterPhaseTwo,
} from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { InputApiService } from 'components/aop-phase-two/services/cpp/jmd/inputApiService'
import { UtilityPlantApiServiceV2 } from 'components/aop-phase-two/services/cpp/jmd/utilityPlantApiServiceV2'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { useDebounce } from 'hooks/useDebounce'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import DeleteDialog from 'components/aop-phase-two/common/AdvanceKendoTable/components/DeleteDialog'
import AddProcessUnitDialog from './components/AddProcessUnitDialog'
import { downloadBase64Excel } from 'components/aop-phase-two/common/utilities/downloadBase64Excel'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'

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

const ProcessUnitGrid = ({ importData }) => {
  const keycloak = useSession()

  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, year, screenTitle, jmdSelectedPlants } =
    dataGridStore

  const AOP_YEAR = year?.selectedYear
  const headerMap = generateHeaderNames(AOP_YEAR)
  const valueFormat = customValueFormatterPhaseTwo(2)
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Process_Unit')
  const lowerSiteName = siteObject?.name?.toLowerCase()
  const selectedPlantId = plantObject?.id

  const PLANT_ID_LIST = plantObject?.id
  // useMemo(
  //   () =>
  //     lowerSiteName === 'jmd'
  //       ? jmdSelectedPlants?.map((plant) => plant.id) || []
  //       : [selectedPlantId],
  //   [jmdSelectedPlants, selectedPlantId],
  // )

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [sourceRows, setSourceRows] = useState([])
  const [plantRequirementData, setPlantRequirementData] = useState([])

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const [addRowDialogOpen, setAddRowDialogOpen] = useState(false)
  const [editRowData, setEditRowData] = useState(null)

  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [rowToDelete, setRowToDelete] = useState(null)

  const columns = [
    { field: 'id', title: 'ID', hidden: true },
    {
      field: 'procurementPlant',
      title: 'Plant',
      width: 150,
      minWidth: 150,
      type: 'text',
      editable: false,
    },
    {
      field: 'utility',
      title: 'Utility',
      width: 180,
      minWidth: 180,
      type: 'text',
      editable: false,
    },
    {
      field: 'material',
      title: 'Material',
      width: 180,
      minWidth: 180,
      type: 'text',
      editable: false,
    },
    {
      field: 'uom',
      title: 'UOM',
      width: 100,
      minWidth: 100,
      type: 'text',
      editable: false,
    },
    {
      field: 'processUnit',
      title: 'Process Unit',
      width: 180,
      minWidth: 180,
      type: 'text',
      // editable: true,
      // selectOptions: PROCESS_UNIT_OPTIONS,
    },
    // Monthly columns with sub-columns: Power Qty, Illustrative Qty, Balance
    ...MONTH_FIELDS.map((m) => ({
      title: headerMap[MONTH_TO_INDEX[m]],
      children: [
        {
          field: `source${m.charAt(0).toUpperCase() + m.slice(1)}`,
          title: 'Power Qty',
          minWidth: 130,
          editable: false,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: m,
          title: 'Illustrative Qty',
          minWidth: 130,
          editable: true,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: `balance${m.charAt(0).toUpperCase() + m.slice(1)}`,
          title: 'Balance',
          minWidth: 130,
          editable: false,
          type: 'number1',
          format: valueFormat,
        },
      ],
    })),
    {
      field: 'remarks',
      title: 'Remarks',
      width: 250,
      type: 'textarea',
      editable: true,
      minWidth: 250,
    },
  ]

  // ── Fetch ──────────────────────────────────────────────────────────────────

  const fetchProcessUnitData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await InputApiService.getProcessUnitAllocations(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )

      const allocations = (res?.data?.processUnitAllocations || []).map(
        (row) => ({
          ...row,
          remarks: row.remarks || '',
        }),
      )

      if (allocations.length === 0) {
        setRows([])
        setOriginalRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        setLoading(false)
        return
      }

      // sourceRows is set from importData (parent component) via useEffect
      // Use current sourceRows ref to compute balances
      const rowsWithBalance = computeBalanceRows(allocations, sourceRows)
      const finalRows = buildTotalRows(rowsWithBalance)

      setRows(finalRows)
      setOriginalRows(finalRows)
    } catch (error) {
      console.error('Error fetching process unit data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR, sourceRows])

  const fetchPlantRequirementData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await UtilityPlantApiServiceV2.getPlantRequirementData(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )
      if (res?.length === 0) {
        setPlantRequirementData([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        setLoading(false)
        return
      }

      console.log('res', res)
      const formattedData = res?.map((item, index) => ({
        ...item,
        remarks: item.remarks || '',
        id: item?.id || index + 1,
        total: MONTH_FIELDS.reduce((sum, key) => sum + (item[key] || 0), 0),
      }))
      setPlantRequirementData(formattedData)
    } catch (error) {
      console.error('Error fetching plant requirement data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR])

  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && AOP_YEAR) {
        fetchProcessUnitData()
        fetchPlantRequirementData()
        setModifiedCells({})
      }
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR, fetchProcessUnitData, fetchPlantRequirementData],
  )

  useEffect(() => setSourceRows(importData), [importData])

  // ── Balance / total helpers ────────────────────────────────────────────────

  const computeBalanceRows = (allocations, sources) => {
    // Build source monthly totals per sourceId.
    // For existing rows (from API): each row already has sourceApr, sourceMay, etc.
    // For newly added rows: fall back to parent sourceRows prop.
    const sourceFromProp = {}
    sources.forEach((s) => {
      sourceFromProp[s.id] = {}
      MONTH_FIELDS.forEach((m) => {
        sourceFromProp[s.id][m] = parseFloat(s[m]) || 0
      })
    })

    const sourceMonthly = {}
    allocations.forEach((a) => {
      if (sourceMonthly[a.sourceId]) return
      sourceMonthly[a.sourceId] = {}
      MONTH_FIELDS.forEach((m) => {
        const srcKey = `source${m.charAt(0).toUpperCase() + m.slice(1)}`
        const fromRow = parseFloat(a[srcKey])
        sourceMonthly[a.sourceId][m] = !isNaN(fromRow)
          ? fromRow
          : sourceFromProp[a.sourceId]?.[m] || 0
      })
    })

    // Sum allocated per source per month
    const allocatedBySource = {}
    allocations.forEach((a) => {
      if (!allocatedBySource[a.sourceId]) {
        allocatedBySource[a.sourceId] = {}
        MONTH_FIELDS.forEach((m) => {
          allocatedBySource[a.sourceId][m] = 0
        })
      }
      MONTH_FIELDS.forEach((m) => {
        allocatedBySource[a.sourceId][m] += parseFloat(a[m]) || 0
      })
    })

    return allocations.map((a) => {
      const updated = { ...a }
      MONTH_FIELDS.forEach((m) => {
        const srcCap = `source${m.charAt(0).toUpperCase() + m.slice(1)}`
        updated[srcCap] = sourceMonthly[a.sourceId]?.[m] || 0
        updated[`balance${m.charAt(0).toUpperCase() + m.slice(1)}`] =
          (sourceMonthly[a.sourceId]?.[m] || 0) -
          (allocatedBySource[a.sourceId]?.[m] || 0)
      })
      return updated
    })
  }

  const buildTotalRows = (dataRows) => {
    if (!dataRows || dataRows.length === 0) {
      return []
    }

    const plantGroups = {}
    dataRows.forEach((row) => {
      if (!plantGroups[row.procurementPlant]) {
        plantGroups[row.procurementPlant] = []
      }
      plantGroups[row.procurementPlant].push(row)
    })

    const result = []
    Object.keys(plantGroups).forEach((plant) => {
      result.push(...plantGroups[plant])
      const totalRow = {
        id: `TOTAL_${plant}`,
        procurementPlant: plant,
        utility: '',
        material: '',
        uom: '',
        processUnit: '',
        remarks: 'Total specific allocation',
        isTotal: true,
        isEditable: false,
      }
      MONTH_FIELDS.forEach((m) => {
        totalRow[m] = plantGroups[plant].reduce(
          (sum, r) => sum + (parseFloat(r[m]) || 0),
          0,
        )
        totalRow[`source${m.charAt(0).toUpperCase() + m.slice(1)}`] = ''
        totalRow[`balance${m.charAt(0).toUpperCase() + m.slice(1)}`] = ''
      })
      result.push(totalRow)
    })

    const grandRow = {
      id: 'GRAND_TOTAL',
      procurementPlant: 'Total',
      title: 'Total',
      utility: '',
      material: '',
      uom: '',
      processUnit: '',
      remarks: 'JMD level specific allocation',
      isTotal: true,
      isEditable: false,
    }
    MONTH_FIELDS.forEach((m) => {
      grandRow[m] = dataRows.reduce(
        (sum, r) => sum + (parseFloat(r[m]) || 0),
        0,
      )
      grandRow[`source${m.charAt(0).toUpperCase() + m.slice(1)}`] = ''
      grandRow[`balance${m.charAt(0).toUpperCase() + m.slice(1)}`] = ''
    })
    result.push(grandRow)

    return result
  }

  const recalcRows = (currentRows) => {
    const dataRows = currentRows.filter((row) => !row.isTotal)
    const rowsWithBalance = computeBalanceRows(dataRows, sourceRows)
    return buildTotalRows(rowsWithBalance)
  }

  // Custom item change — recalculates balances and totals
  const customItemChange = useCallback(
    (event, setRowsFunc) => {
      const { field } = event
      if (
        !MONTH_FIELDS.includes(field) &&
        field !== 'processUnit' &&
        field !== 'remarks'
      ) {
        return
      }

      setRowsFunc((currentRows) => recalcRows(currentRows))
    },
    [sourceRows],
  )

  // ── Row actions ────────────────────────────────────────────────────────────

  const handleAddRow = (newRow) => {
    setRows((currentRows) => {
      const dataRows = currentRows.filter((row) => !row.isTotal)
      const updated = [...dataRows, { ...newRow, inEdit: true }]
      return recalcRows(updated)
    })

    setModifiedCells((prev) => ({
      ...prev,
      [newRow.id]: { ...newRow, inEdit: true },
    }))
  }

  const handleEditRow = (updatedRow) => {
    setRows((currentRows) => {
      const dataRows = currentRows
        .filter((row) => !row.isTotal)
        .map((row) => (row.id === updatedRow.id ? updatedRow : row))
      return recalcRows(dataRows)
    })

    setModifiedCells((prev) => ({
      ...prev,
      [updatedRow.id]: { ...updatedRow, inEdit: true },
    }))
  }

  const handleDeleteRow = (row) => {
    setRows((currentRows) => {
      const dataRows = currentRows.filter((r) => !r.isTotal && r.id !== row.id)
      return recalcRows(dataRows)
    })

    setModifiedCells((prev) => {
      const next = { ...prev }
      delete next[row.id]
      return next
    })
  }

  // ── Edit action cell ───────────────────────────────────────────────────────

  const EditActionCell = ({ dataItem, tdProps }) => {
    if (dataItem?.isTotal) {
      return <td {...tdProps} />
    }
    return (
      <td
        {...tdProps}
        style={{
          ...tdProps?.style,
          textAlign: 'center',
          verticalAlign: 'middle',
        }}
      >
        <Box sx={{ display: 'flex', justifyContent: 'center' }}>
          <Tooltip title='Edit Allocation'>
            <IconButton
              size='small'
              onClick={() => {
                setEditRowData(dataItem)
                setAddRowDialogOpen(true)
              }}
            >
              <EditIcon fontSize='small' />
            </IconButton>
          </Tooltip>

          <Tooltip title='Delete Allocation'>
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
  }

  // ── Permissions ────────────────────────────────────────────────────────────

  const permissions = {
    showAction: true,
    addButton: true,
    addBtnName: 'Add Process Unit',
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showImport: true,
    showExport: true,
    ExcelName: `Process Unit Allocation - ${AOP_YEAR}`,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: screenTitle?.title,
  }

  // ── Validation ─────────────────────────────────────────────────────────────

  const validateAllocations = () => {
    const dataRows = rows.filter((row) => !row.isTotal)

    for (const month of MONTH_FIELDS) {
      const allocatedBySource = {}
      dataRows.forEach((a) => {
        allocatedBySource[a.sourceId] =
          (allocatedBySource[a.sourceId] || 0) + (parseFloat(a[month]) || 0)
      })

      for (const sourceId of Object.keys(allocatedBySource)) {
        const source = sourceRows.find((s) => s.id === sourceId)
        const total = parseFloat(source?.[month]) || 0
        if (allocatedBySource[sourceId] > total) {
          return `Allocated qty for ${source?.material || sourceId} (${source?.procurementPlant || ''}) in ${month} exceeds source total ${total}`
        }
      }
    }
    return null
  }

  // ── Save ───────────────────────────────────────────────────────────────────

  const saveChanges = async () => {
    setLoading(true)

    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
      setLoading(false)
      return
    }

    const validationError = validateAllocations()
    if (validationError) {
      setSnackbarOpen(true)
      setSnackbarData({ message: validationError, severity: 'error' })
      setLoading(false)
      return
    }

    const remarksError = validateRowDataWithRemarks(
      modifiedData,
      originalRows,
      MONTH_FIELDS,
      'processUnit',
    )
    if (remarksError) {
      setSnackbarOpen(true)
      setSnackbarData({ message: remarksError, severity: 'error' })
      setLoading(false)
      return
    }

    // Body = direct array of allocation records (plantIds + aopYear sent as query params)
    const processUnitAllocations = modifiedData.map((row) => {
      const stripped = { ...row }
      // Remove UI-only fields
      delete stripped.inEdit
      delete stripped.isTotal
      delete stripped.isEditable

      // Remove source_* fields (UI display only — not needed in DB)
      MONTH_FIELDS.forEach((m) => {
        delete stripped[`source_${m}`]
      })

      // For new rows id is null (CREATE) — remove it so backend sees null → INSERT
      // For existing rows id is a real UUID (UPDATE) — keep it
      if (!stripped.id) {
        delete stripped.id
      }

      // Enrich with balance* (camelCase) fields from rows state.
      // modifiedCells stores the row as it was when added/edited (no balance).
      // rows state always has the latest computed balances via computeBalanceRows().
      const computedRow = rows.find((r) => r.id === row.id)
      MONTH_FIELDS.forEach((m) => {
        const balKey = `balance${m.charAt(0).toUpperCase() + m.slice(1)}`
        stripped[balKey] = computedRow?.[balKey] ?? 0
      })

      return stripped
    })

    console.log('SAVE PAYLOAD', {
      plantIds: PLANT_ID_LIST,
      aopYear: AOP_YEAR,
      processUnitAllocations,
    })

    try {
      await InputApiService.saveProcessUnitAllocations(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
        processUnitAllocations,
      )
      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Saved successfully!',
        severity: 'success',
      })
      fetchProcessUnitData()
    } catch (error) {
      console.error('Error saving process unit data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to save changes. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  // ── Excel Export ───────────────────────────────────────────────────────────

  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({ message: 'Excel download started!', severity: 'info' })
    try {
      await InputApiService.exportProcessUnitAllocationsExcel(
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
      console.error('Error exporting Process Unit data:', error)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  // ── Excel Import ───────────────────────────────────────────────────────────

  const handleExcelUpload = async (file) => {
    if (!file) return
    setLoading(true)
    try {
      const response = await InputApiService.saveProcessUnitAllocationsExcel(
        file,
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Excel file imported successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        await fetchProcessUnitData()
      } else if (response?.code === 400 && response?.data) {
        downloadBase64Excel(
          response.data,
          'Error File - Process Unit Allocation.xlsx',
        )

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        await fetchProcessUnitData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'Upload Failed!', severity: 'error' })
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

  // ── Delete ─────────────────────────────────────────────────────────────────

  const handleConfirmDelete = async () => {
    if (!rowToDelete) return

    setDeleteDialogOpen(false)
    setLoading(true)

    try {
      // Call real DELETE API — use the row's actual DB UUID.
      // For rows not yet saved (id is null), only remove locally.
      if (rowToDelete.id != null) {
        await InputApiService.deleteProcessUnitAllocation(
          keycloak,
          rowToDelete.id,
        )
      }
      handleDeleteRow(rowToDelete)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Allocation deleted successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error deleting allocation:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to delete allocation. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
      setRowToDelete(null)
    }
  }

  // ── Remark dialog ──────────────────────────────────────────────────────────

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title='Process Unit Allocation'
        permissions={permissions}
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={() => { }}
        handleExport={handleExport}
        handleExcelUpload={handleExcelUpload}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        customItemChange={customItemChange}
        groupBy={['procurementPlant']}
        customAddRow={() => {
          setEditRowData(null)
          setAddRowDialogOpen(true)
        }}
        customActionCell={EditActionCell}
      />

      <AddProcessUnitDialog
        open={addRowDialogOpen}
        onClose={() => {
          setAddRowDialogOpen(false)
          setEditRowData(null)
        }}
        onSuccess={(row) => {
          if (editRowData) {
            handleEditRow({ ...editRowData, ...row, id: editRowData.id })
          } else {
            handleAddRow(row)
          }
          setAddRowDialogOpen(false)
          setEditRowData(null)
        }}
        editRowData={editRowData}
        sourceRows={sourceRows}
        plantRequirementData={plantRequirementData}
        financialYear={AOP_YEAR}
        existingRows={rows}
      />

      <DeleteDialog
        openDeleteDialogeBox={deleteDialogOpen}
        setOpenDeleteDialogeBox={setDeleteDialogOpen}
        deleteTheRecord={handleConfirmDelete}
        message='Are you sure you want to delete this allocation?'
        confirmButtonText='Delete'
      />
    </Box>
  )
}

export default ProcessUnitGrid
