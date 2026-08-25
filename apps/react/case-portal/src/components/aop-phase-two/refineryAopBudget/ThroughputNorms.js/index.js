import React, { useState, useEffect, useCallback } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { generateHeaderNames } from '../../common/utilities/generateHeaders'
import { customValueFormatterPhaseTwo } from '../../common/ValueFormatterPhaseTwo'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { ThroughputNormsApiService } from 'components/aop-phase-two/services/crude/throughputNormsApiService'
import {
  formatUnitDropdownOptions,
  formatMaterialDropdownOptions,
  getMaterialOptions,
  getUnitOptions,
  formatNormsInitialRows,
} from './helpers'
import { ThroughputNormsSelectCellEditor } from './ThroughputNormsSelectCellEditor'

const ThroughputNormsScreen = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID =
    siteObject?.id ||
    siteObject?.value ||
    plantObject?.siteId ||
    plantObject?.siteFKId
  const AOP_YEAR = year?.selectedYear || year
  const VERTICAL_ID = verticalObject?.id

  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [unitDropdown, setUnitDropdown] = useState([])
  const [materialOptions, setMaterialOptions] = useState([])

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const valueFormat = customValueFormatterPhaseTwo(5)
  const headerMap = generateHeaderNames(AOP_YEAR)

  const unitDropdownRef = React.useRef([])

  // 1. API Call: Fetch Unit dropdown options
  const fetchUnitDropdown = useCallback(async () => {
    if (!SITE_ID) return []
    try {
      const response = await ThroughputNormsApiService.getDropdownUnit(
        keycloak,
        SITE_ID,
        'SEZ',
      )
      const data = response?.data || response?.result || response || []
      const formattedOptions = formatUnitDropdownOptions(data)
      unitDropdownRef.current = formattedOptions
      setUnitDropdown(formattedOptions)
      return formattedOptions
    } catch (error) {
      console.error('Error fetching unit dropdown options:', error)
      return []
    }
  }, [keycloak, SITE_ID])

  // 2. API Call: Fetch Material Code dropdown options
  const fetchMaterialDropdown = useCallback(async () => {
    try {
      const response = await ThroughputNormsApiService.getNormsMaterialDropdown(
        keycloak,
        'SEZ',
        SITE_ID || '',
      )
      const data = response?.data || response?.result || response || []
      const rawList = Array.isArray(data) ? data : []
      const formatted = formatMaterialDropdownOptions(rawList)
      setMaterialOptions(formatted)
      return formatted
    } catch (error) {
      console.error('Error fetching material dropdown options:', error)
      return []
    }
  }, [keycloak, SITE_ID])

  // Load Dropdowns when SITE_ID changes
  useEffect(() => {
    if (SITE_ID) {
      fetchUnitDropdown()
      fetchMaterialDropdown()
    }
  }, [SITE_ID, fetchUnitDropdown, fetchMaterialDropdown])

  // 3. API Call: Fetch Table Data (GET)
  const fetchTableData = useCallback(async () => {
    if (!SITE_ID || !PLANT_ID || !VERTICAL_ID || !AOP_YEAR) return

    setLoading(true)
    try {
      const response = await ThroughputNormsApiService.getThroughputNorms(
        keycloak,
        'SEZ',
        AOP_YEAR,
        SITE_ID || '',
      )
      const data = response?.data || []
      const formattedData = formatNormsInitialRows(data, unitDropdownRef.current)
      setRows(formattedData)
      setOriginalRows(formattedData)
    } catch (error) {
      console.error('Error fetching SEZ JobWork Norms data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error fetching data',
        severity: 'error',
      })
      setRows([])
      setOriginalRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, SITE_ID, PLANT_ID, VERTICAL_ID, AOP_YEAR])

  // Load Table Data when SITE_ID / AOP_YEAR changes
  useEffect(() => {
    fetchTableData()
  }, [fetchTableData])

  const columns = [
    {
      field: 'id',
      title: 'Id',
      widthT: 250,
      minWidth: 200,
      type: 'text',
      editable: false,
      locked: true,
      hidden: true,
    },
    {
      field: 'unit',
      title: 'Unit',
      widthT: 250,
      minWidth: 200,
      type: 'customSelect',
      cellEditor: ThroughputNormsSelectCellEditor,
      dynamicOptions: true,
      getOptions: (dataItem) => getUnitOptions(dataItem, unitDropdown, rows),
      editable: true,
      locked: true,
    },
    {
      field: 'displayName',
      title: 'Material Code',
      widthT: 250,
      minWidth: 200,
      type: 'customSelect',
      cellEditor: ThroughputNormsSelectCellEditor,
      dynamicOptions: true,
      getOptions: (dataItem) =>
        getMaterialOptions(dataItem, materialOptions, null, rows),
      editable: true,
      locked: true,
    },
    {
      field: 'uom',
      title: 'UOM',
      widthT: 120,
      minWidth: 120,
      type: 'text',
      editable: false,
      locked: true,
    },
    {
      field: 'apr',
      title: headerMap[4],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'may',
      title: headerMap[5],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'jun',
      title: headerMap[6],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'jul',
      title: headerMap[7],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'aug',
      title: headerMap[8],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'sep',
      title: headerMap[9],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'oct',
      title: headerMap[10],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'nov',
      title: headerMap[11],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'dec',
      title: headerMap[12],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'jan',
      title: headerMap[1],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'feb',
      title: headerMap[2],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'mar',
      title: headerMap[3],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    // {
    //   field: 'remarks',
    //   title: 'Remarks',
    //   widthT: 250,
    //   minWidth: 180,
    //   type: 'text',
    //   editable: true,
    // },
  ]

  // Add new empty row
  const customAddRow = () => {
    const uniqueId = `new_row_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
    const newRow = {
      id: uniqueId,
      materialId: '',
      unitId: '',
      profitId: '',
      unit: '',
      displayName: '',
      uom: '',
      UOM: '',
      isNew: true,
      isEditable: true,
      apr: '',
      may: '',
      jun: '',
      jul: '',
      aug: '',
      sep: '',
      oct: '',
      nov: '',
      dec: '',
      jan: '',
      feb: '',
      mar: '',
      remarks: '',
    }
    setRows((prev) => [newRow, ...prev])
  }

  const handleCustomItemChange = async (
    e,
    setRowsState,
    setModifiedCellsState,
  ) => {
    const { dataItem, field, value } = e
    if (!dataItem) return

    const rowId = dataItem.id

    if (field === 'unit') {
      const valStr =
        typeof value === 'object'
          ? value?.value || value?.label || ''
          : String(value || '')
      const selectedObj = unitDropdown.find(
        (opt) =>
          String(opt.value || '').toLowerCase() === valStr.toLowerCase() ||
          String(opt.label || '').toLowerCase() === valStr.toLowerCase() ||
          String(opt.id || '').toLowerCase() === valStr.toLowerCase() ||
          String(opt.unitId || '').toLowerCase() === valStr.toLowerCase(),
      )

      if (selectedObj) {
        const unitIdVal =
          selectedObj.unitId || selectedObj.profitId || selectedObj.id
        const unitLabel = selectedObj.label || selectedObj.value || valStr

        // Check if this (Unit + current Material) already exists in another row
        const currentMaterial = String(dataItem.displayName || '').trim().toLowerCase()
        if (currentMaterial) {
          const isDuplicate = rows.some((r) =>
            String(r.id) !== String(rowId) &&
            String(r.unit || '').trim().toLowerCase() === String(unitLabel).trim().toLowerCase() &&
            String(r.displayName || '').trim().toLowerCase() === currentMaterial
          )
          if (isDuplicate) {
            setSnackbarOpen(true)
            setSnackbarData({
              message: `"${dataItem.displayName}" is already added under Unit "${unitLabel}"! Each Unit + Material Code combination must be unique.`,
              severity: 'warning',
            })
            return
          }
        }

        const updatedRow = {
          ...dataItem,
          unit: unitLabel,
          unitId: unitIdVal,
          profitId: unitIdVal,
          inEdit: true,
        }

        setRowsState((prev) =>
          prev.map((r) => (String(r.id) === String(rowId) ? updatedRow : r)),
        )

        setModifiedCellsState((prev) => ({
          ...prev,
          [rowId]: {
            ...(prev[rowId] || {}),
            ...updatedRow,
          },
        }))
      }
    } else if (field === 'displayName') {
      const valStr =
        typeof value === 'object'
          ? value?.value || value?.label || ''
          : String(value || '')

      const selectedMaterial = materialOptions.find(
        (opt) =>
          String(opt.value || '').toLowerCase() === valStr.toLowerCase() ||
          String(opt.label || '').toLowerCase() === valStr.toLowerCase() ||
          String(opt.id || '').toLowerCase() === valStr.toLowerCase(),
      )

      if (selectedMaterial) {
        const mId = selectedMaterial.materialId || selectedMaterial.id
        const dName = selectedMaterial.label || selectedMaterial.value || valStr
        const uomVal = selectedMaterial.uom || dataItem.uom || ''

        // Check if this (Unit + selected Material) already exists in another row
        const targetUnit = String(dataItem.unit || '').trim().toLowerCase()
        if (targetUnit) {
          const isDuplicate = rows.some((r) =>
            String(r.id) !== String(rowId) &&
            String(r.unit || '').trim().toLowerCase() === targetUnit &&
            String(r.displayName || '').trim().toLowerCase() === String(dName).trim().toLowerCase()
          )
          if (isDuplicate) {
            setSnackbarOpen(true)
            setSnackbarData({
              message: `"${dName}" is already added under Unit "${dataItem.unit}"! Each Unit + Material Code combination must be unique.`,
              severity: 'warning',
            })
            return
          }
        }

        const updatedRow = {
          ...dataItem,
          materialId: mId,
          displayName: dName,
          uom: uomVal,
          UOM: uomVal,
          inEdit: true,
        }

        setRowsState((prev) =>
          prev.map((r) => (String(r.id) === String(rowId) ? updatedRow : r)),
        )

        setModifiedCellsState((prev) => ({
          ...prev,
          [rowId]: {
            ...(prev[rowId] || {}),
            ...updatedRow,
          },
        }))
      }
    }
  }

  // 4. API Call: Save / Update Data (POST)
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

    // Validation 1: Material Name (displayName) and Unit are mandatory
    const invalidRow = data.find(
      (row) =>
        !row.unit || !row.displayName || String(row.displayName).trim() === '',
    )
    if (invalidRow) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Material Name (Display Name) and Unit are mandatory to fill!',
        severity: 'error',
      })
      setLoading(false)
      return
    }

    // Validation 2: Remarks mandatory for modified rows (Commented)
    // for (const row of data) {
    //   const remarksVal = (row.remarks || '').trim()
    //   const desc = row.displayName || row.unit || 'Row'
    //   if (!remarksVal) {
    //     setSnackbarOpen(true)
    //     setSnackbarData({
    //       message: `Remarks is mandatory for: "${desc}". Please provide remarks!`,
    //       severity: 'warning',
    //     })
    //     setLoading(false)
    //     return
    //   }
    // }

    // Validation 3: Check for duplicate (Unit + Material Code)
    const seenCombos = new Set()
    for (const r of rows) {
      const uKey = String(r.unit || '').trim().toLowerCase()
      const mKey = String(r.displayName || '').trim().toLowerCase()
      if (uKey && mKey) {
        const comboKey = `${uKey}___${mKey}`
        if (seenCombos.has(comboKey)) {
          setSnackbarOpen(true)
          setSnackbarData({
            message: `Duplicate detected: "${r.displayName}" under Unit "${r.unit}". Each Unit + Material Code combination must be unique!`,
            severity: 'warning',
          })
          setLoading(false)
          return
        }
        seenCombos.add(comboKey)
      }
    }

    const payloadData = data.map((row) => {
      const origId =
        row.originalMaterialId ||
        (!String(row.id).startsWith('new_row_') && !String(row.id).startsWith('row_') ? row.id : '')
      const currentMaterialId = row.materialId || origId || ''
      return {
        id: origId || null,
        materialId: currentMaterialId,
        unitId: row.unitId || row.profitId,
        unit: row.unit,
        displayName: row.displayName,
        uom: row.uom || row.UOM || '',
        jan: Number(row.jan || 0),
        feb: Number(row.feb || 0),
        mar: Number(row.mar || 0),
        apr: Number(row.apr || 0),
        may: Number(row.may || 0),
        jun: Number(row.jun || 0),
        jul: Number(row.jul || 0),
        aug: Number(row.aug || 0),
        sep: Number(row.sep || 0),
        oct: Number(row.oct || 0),
        nov: Number(row.nov || 0),
        dec: Number(row.dec || 0),
        remarks: row.remarks || '',
      }
    })

    try {
      await ThroughputNormsApiService.saveThroughputNorms(
        keycloak,
        payloadData,
        AOP_YEAR,
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Data saved successfully!',
        severity: 'success',
      })
      setModifiedCells({})
      setOriginalRows([])
      setRows([])
      await fetchTableData()
    } catch (error) {
      console.error('Error saving SEZ JobWork Norms data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error saving data!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const deleteRowData = async (dataItem) => {
    if (!dataItem) return
    if (dataItem.isNew || String(dataItem.id).startsWith('new_row_')) {
      setRows((prev) => prev.filter((r) => r.id !== dataItem.id))
      setModifiedCells((prev) => {
        const copy = { ...prev }
        delete copy[dataItem.id]
        return copy
      })
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Row removed successfully!',
        severity: 'success',
      })
      return
    }

    try {
      setLoading(true)
      const materialId = dataItem.materialId || dataItem.id
      const unitId = dataItem.unitId || dataItem.profitId
      await ThroughputNormsApiService.deleteThroughputNorms(
        keycloak,
        materialId,
        unitId,
        AOP_YEAR,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Record deleted successfully!',
        severity: 'success',
      })
      await fetchTableData()
    } catch (error) {
      console.error('Error deleting record:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to delete record. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const permissions = {
    showAction: true,
    addButton: true,
    addBtnName: 'Add Item',
    deleteButton: true,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showExport: false,
    showImport: false,
    showCalculate: false,
    ExcelName: `SEZ JobWork Norms_${AOP_YEAR}`,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'SEZ JobWork Norms',
    showDropdown: false,
    remarksEditable: true,
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
        deleteRowData={deleteRowData}
        customAddRow={customAddRow}
        customItemChange={handleCustomItemChange}
        title={permissions.showTitle ? permissions.titleName : ''}
        permissions={permissions}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        saveChanges={saveChanges}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
      />
    </Box>
  )
}

export default ThroughputNormsScreen
