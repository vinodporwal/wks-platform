import { useEffect, useState, useMemo, useCallback } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { validateNestedRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import { InputApiService } from 'components/aop-phase-two/services/cpp/jmd/inputApiService'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import { downloadBase64Excel } from 'components/aop-phase-two/common/utilities/downloadBase64Excel'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { useDebounce } from 'hooks/useDebounce'

// ========================|| Component ||=====================================//

const EdRpoMapping = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    plantObject,
    siteObject,
    year,
    jmdSelectedPlants,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const IS_JMD = siteObject?.name?.toLowerCase() === 'jmd'
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'EdRpoMapping')

  const PLANT_ID_LIST = useMemo(
    () =>
      IS_JMD
        ? jmdSelectedPlants?.map((plant) => plant.id) ?? []
        : PLANT_ID
          ? [PLANT_ID]
          : [],
    [plantObject, jmdSelectedPlants, siteObject, IS_JMD, PLANT_ID],
  )

  // ── Column definitions ──────────────────────────────────────────────────────
  const columns = useMemo(
    () => [
      {
        field: 'cppPlantName',
        title: 'CPP Plant',
        widthT: 150,
        minWidth: 150,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'cppPlantCode',
        title: 'CPP Code',
        widthT: 120,
        minWidth: 120,
        type: 'text',
        editable: false,
        hidden: true,
      },
      {
        field: 'generatingPlantName',
        title: 'Generating Plant',
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'generatingPlantCode',
        title: 'Plant Code',
        widthT: 140,
        minWidth: 140,
        type: 'text',
        editable: false,
        hidden: false,
      },
        {
        field: 'issuingPlantName',
        title: 'Issuing Plant',
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'issuingPlantCode',
        title: 'Plant Code',
        widthT: 140,
        minWidth: 140,
        type: 'text',
        editable: false,
        hidden: false,
      }, 
      {
        field: 'utilityName',
        title: 'Utility',
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'utilitySAPMaterialCode',
        title: 'Utility Code',
        widthT: 140,
        minWidth: 140,
        type: 'text',
        editable: false,
        hidden: true,
      },
      {
        field: 'materialName',
        title: 'Material',
        widthT: 200,
        minWidth: 200,
        type: 'text',
        editable: false,
      },
      {
        field: 'materialSAPMaterialCode',
        title: 'Material Code',
        widthT: 140,
        minWidth: 140,
        type: 'text',
        editable: false,
        hidden: true,
      },
      {
        field: 'exempt',
        title: 'Exempt',
        widthT: 100,
        minWidth: 100,
        type: 'boolean',
        editable: true,
      },
      {
        field: 'remark',
        title: 'Remark',
        widthT: 250,
        minWidth: 250,
        type: 'textarea',
        editable: true,
      },
    ],
    [],
  )

  // ── State ───────────────────────────────────────────────────────────────────
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [customModifiedCells, setCustomModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({ message: '', severity: 'info' })

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // ── Fetch ───────────────────────────────────────────────────────────────────
  const fetchData = useCallback(async () => {
    if (!AOP_YEAR || !PLANT_ID_LIST || PLANT_ID_LIST.length === 0) return
    setLoading(true)
    try {
      const res = await InputApiService.getEdRpoMapping(keycloak, PLANT_ID_LIST, AOP_YEAR)

      if (!res?.data || res.data.length === 0) {
        setRows([])
        setOriginalRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        return
      }

      const formatted = res.data.map((item, index) => ({
        ...item,
        id: item.id || `edrpo_map_${index}`,
        exempt: Boolean(item.exempt),
        remark: item.remark || '',
      }))

      setRows(formatted)
      setOriginalRows(formatted)
      setModifiedCells({})
      setCustomModifiedCells({})
    } catch (error) {
      console.error('[EdRpoMapping] Error fetching ED RPO Mapping data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to fetch ED RPO Mapping data. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [AOP_YEAR, PLANT_ID_LIST, keycloak])

  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && AOP_YEAR) {
        fetchData()
      }
    },
    300,
    [PLANT_ID_LIST, AOP_YEAR],
  )

  // ── Cell modification tracker ───────────────────────────────────────────────
  const handleItemChange = (e) => {
    const { dataItem, field, value } = e
    const rowId = dataItem.id

    const originalRow = originalRows.find((r) => r.id === rowId)
    const originalValue = originalRow ? originalRow[field] : undefined

    let isModified = false
    if (field === 'exempt') {
      isModified = Boolean(value) !== Boolean(originalValue)
    } else {
      isModified = (value ?? '') !== (originalValue ?? '')
    }

    setCustomModifiedCells((prev) => {
      const rowMods = { ...(prev[rowId] || {}) }
      if (isModified) {
        rowMods[field] = true
      } else {
        delete rowMods[field]
      }
      const next = { ...prev }
      if (Object.keys(rowMods).length > 0) {
        next[rowId] = rowMods
      } else {
        delete next[rowId]
      }
      return next
    })
  }

  // ── Save ────────────────────────────────────────────────────────────────────
  const saveChanges = async () => {
    const dirtyRowIds = Object.keys(customModifiedCells)

    if (dirtyRowIds.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({ message: 'No changes to save.', severity: 'info' })
      return
    }

    // fieldsToCheck: only the two editable fields
    const validationError = validateNestedRowDataWithRemarks(
      rows,
      originalRows,
      ['exempt', 'remark'],
      'issuingPlantName',
      'remark',
    )

    if (validationError) {
      setSnackbarOpen(true)
      setSnackbarData({ message: validationError, severity: 'error' })
      return
    }

    const payload = rows
      .filter((row) => dirtyRowIds.includes(String(row.id)))
      .map((row) => ({
        id: row.id,
        cppPlantFkId: row.cppPlantFkId,
        normheaderfkid: row.normheaderfkid,
        aopYear: AOP_YEAR,
        exempt: Boolean(row.exempt),
        remark: row.remark || '',
      }))

    setLoading(true)
    try {
      const res = await InputApiService.saveEdRpoMapping(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
        payload,
      )

      if (res?.code === 200 || res?.success) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: res?.message || 'Data saved successfully!',
          severity: 'success',
        })
        fetchData()
      } else if (res?.code === 207) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: res?.message || 'Partial data saved.',
          severity: 'warning',
        })
        fetchData()
      } else {
        throw new Error(res?.message || 'Save failed')
      }
    } catch (error) {
      console.error('[EdRpoMapping] Error saving data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: error.message || 'Error saving data. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  // ── Import Excel ────────────────────────────────────────────────────────────
  const handleExcelUpload = async (file) => {
    if (!file) return
    setLoading(true)
    try {
      const res = await InputApiService.saveEdRpoMappingExcel(
        file,
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )

      if (res?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: res?.message || 'Excel imported successfully!',
          severity: 'success',
        })
        fetchData()
      } else if (res?.code === 400 && res?.data) {
        downloadBase64Excel(res.data, `${EXCEL_NAME}_Errors.xlsx`)
        setSnackbarOpen(true)
        setSnackbarData({
          message: res?.message || 'Import completed with errors. Error file downloaded.',
          severity: 'warning',
        })
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: res?.message || 'Import failed. Please check the file.',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('[EdRpoMapping] Error importing Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to import Excel file. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  // ── Export Excel ────────────────────────────────────────────────────
  const handleExport = async () => {
    try {
      await InputApiService.exportEdRpoMappingExcel(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
        EXCEL_NAME,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('[EdRpoMapping] Error exporting Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  // ── Remark dialog ───────────────────────────────────────────────────────────
  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  // ── Permissions ─────────────────────────────────────────────────────────────
  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showTitleNameBusiness: true,
    titleName: 'ED / RPO Mapping',
    showTitle: true,
    showImport: true,
    showExport: true,
    ExcelName: EXCEL_NAME,
  }

  // ── Render ──────────────────────────────────────────────────────────────────
  return (
    <Box sx={{ mt: 2 }}>
      <LoaderBackdrop open={!!loading} />

      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        customModifiedCells={customModifiedCells}
        setCustomModifiedCells={setCustomModifiedCells}
        title={permissions.titleName}
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
        customHeight={65}
        groupBy={['cppPlantName', 'generatingPlantName']}
        customItemChange={handleItemChange}
        pagable={false}
      />
    </Box>
  )
}

export default EdRpoMapping
