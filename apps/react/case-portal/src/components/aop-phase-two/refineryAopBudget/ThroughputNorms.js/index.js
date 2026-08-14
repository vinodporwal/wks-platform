import React, { useState, useEffect, useRef, useCallback } from 'react'
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
  formatNormsInitialRows,
  getUniqueUnitsToFetch,
} from './helpers'

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

  const isFetchedRef = useRef(false)

  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [unitDropdown, setUnitDropdown] = useState([])
  const [materialDropdownMap, setMaterialDropdownMap] = useState({})
  const materialDropdownMapRef = useRef({})

  useEffect(() => {
    materialDropdownMapRef.current = materialDropdownMap
  }, [materialDropdownMap])

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

  // Fetch Unit dropdown using getDropdownUnit API
  const fetchUnitDropdown = useCallback(async () => {
    if (!SITE_ID) return []
    try {
      const response = await ThroughputNormsApiService.getDropdownUnit(
        keycloak,
        SITE_ID,
      )
      const data = response?.data || response?.result || response || []
      const formattedOptions = formatUnitDropdownOptions(data)
      setUnitDropdown(formattedOptions)
      return formattedOptions
    } catch (error) {
      console.error('Error fetching unit dropdown options:', error)
      return []
    }
  }, [keycloak, SITE_ID])

  // Fetch Material dropdown for a specific profitId (unitId) or unitName using getNormsMaterialDropdown API
  const fetchMaterialDropdownForUnit = useCallback(
    async (profitId, unitName) => {
      if ((!profitId && !unitName) || !SITE_ID) return []
      const cacheKey = profitId || unitName
      if (materialDropdownMapRef.current[cacheKey]) {
        return materialDropdownMapRef.current[cacheKey]
      }
      try {
        const response =
          await ThroughputNormsApiService.getNormsMaterialDropdown(
            keycloak,
            SITE_ID,
            profitId || '',
          )
        const data = response?.data || response?.result || response || []
        const rawList = Array.isArray(data) ? data : []

        const formatted = formatMaterialDropdownOptions(
          rawList,
          profitId,
          unitName,
        )

        setMaterialDropdownMap((prev) => {
          const next = { ...prev }
          if (profitId) next[profitId] = formatted
          if (unitName) next[unitName] = formatted
          return next
        })
        if (profitId) materialDropdownMapRef.current[profitId] = formatted
        if (unitName) materialDropdownMapRef.current[unitName] = formatted
        return formatted
      } catch (error) {
        console.error(
          'Error fetching material dropdown for profitId:',
          profitId,
          error,
        )
        return []
      }
    },
    [keycloak, SITE_ID],
  )

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
      type: 'select',
      options: unitDropdown,
      editable: true,
      locked: true,
    },
    {
      field: 'displayName',
      title: 'Material Code',
      widthT: 250,
      minWidth: 200,
      type: 'select',
      dynamicOptions: true,
      getOptions: (dataItem) =>
        getMaterialOptions(
          dataItem,
          materialDropdownMap,
          materialDropdownMapRef,
          rows,
        ),
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
  ]

  useEffect(() => {
    if (SITE_ID) {
      fetchUnitDropdown()
    }
  }, [SITE_ID, fetchUnitDropdown])

  const handleCustomItemChange = async (
    e,
    setRowsState,
    setModifiedCellsState,
  ) => {
    const { dataItem, field, value } = e
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

        // Fetch materials for selected unitId / profitId
        if (unitIdVal) {
          await fetchMaterialDropdownForUnit(unitIdVal, unitLabel)
        }

        dataItem.unit = unitLabel
        dataItem.unitId = unitIdVal
        dataItem.profitId = unitIdVal
        dataItem.displayName = ''
        dataItem.uom = ''
        dataItem.UOM = ''

        setRowsState((prev) =>
          prev.map((r) =>
            String(r.id) === String(dataItem.id)
              ? {
                  ...r,
                  unit: unitLabel,
                  unitId: unitIdVal,
                  profitId: unitIdVal,
                  displayName: '',
                  uom: '',
                  UOM: '',
                }
              : r,
          ),
        )

        setModifiedCellsState((prev) => {
          const rowId = dataItem.id
          const previousModified = prev[rowId] || {}
          const updatedRow = {
            ...dataItem,
            ...previousModified,
            unit: unitLabel,
            unitId: unitIdVal,
            profitId: unitIdVal,
            displayName: '',
            uom: '',
            UOM: '',
          }
          return { ...prev, [rowId]: updatedRow }
        })
      }
    } else if (field === 'displayName') {
      const valStr =
        typeof value === 'object'
          ? value?.value || value?.label || ''
          : String(value || '')
      const profitId =
        dataItem?.unitId ||
        dataItem?.profitId ||
        dataItem?.profitCenter_FK_Id ||
        dataItem?.profitCenterFkId
      const unitName = dataItem?.unit || dataItem?.Unit
      const options =
        materialDropdownMapRef.current[profitId] ||
        materialDropdownMapRef.current[unitName] ||
        []

      const selectedMaterial = options.find(
        (opt) =>
          String(opt.value || '').toLowerCase() === valStr.toLowerCase() ||
          String(opt.label || '').toLowerCase() === valStr.toLowerCase() ||
          String(opt.id || '').toLowerCase() === valStr.toLowerCase(),
      )

      if (selectedMaterial) {
        const mId = selectedMaterial.materialId || selectedMaterial.id
        const dName = selectedMaterial.label || selectedMaterial.value || valStr
        const uId = selectedMaterial.unitId || profitId
        const uName = selectedMaterial.unit || dataItem.unit
        const uomVal = selectedMaterial.uom || ''

        // Set materialId as id for inserted record
        dataItem.id = mId
        dataItem.materialId = mId
        dataItem.displayName = dName
        dataItem.unitId = uId
        dataItem.profitId = uId
        dataItem.unit = uName
        dataItem.uom = uomVal
        dataItem.UOM = uomVal

        setRowsState((prev) =>
          prev.map((r) =>
            String(r.id) === String(e.dataItem.id) || r === dataItem
              ? {
                  ...r,
                  id: mId,
                  materialId: mId,
                  displayName: dName,
                  unitId: uId,
                  profitId: uId,
                  unit: uName,
                  uom: uomVal,
                  UOM: uomVal,
                }
              : r,
          ),
        )

        setModifiedCellsState((prev) => {
          const rowKey = mId || dataItem.id
          const previousModified = prev[e.dataItem.id] || prev[rowKey] || {}
          const updatedRow = {
            ...dataItem,
            ...previousModified,
            id: mId,
            materialId: mId,
            displayName: dName,
            unitId: uId,
            profitId: uId,
            unit: uName,
            uom: uomVal,
            UOM: uomVal,
          }
          const copy = { ...prev }
          if (e.dataItem.id !== mId) {
            delete copy[e.dataItem.id]
          }
          copy[rowKey] = updatedRow
          return copy
        })
      }
    }
  }

  useEffect(() => {
    if (SITE_ID && AOP_YEAR && !isFetchedRef.current) {
      isFetchedRef.current = true
      fetchData()
    }
  }, [SITE_ID, AOP_YEAR])

  const fetchData = useCallback(async () => {
    if (!SITE_ID || !PLANT_ID || !VERTICAL_ID || !AOP_YEAR) return

    setLoading(true)
    try {
      const unitsList = await fetchUnitDropdown()

      const response = await ThroughputNormsApiService.getThroughputNorms(
        keycloak,
        SITE_ID,
        AOP_YEAR,
      )
      const data = response?.data || []
      const formattedData = formatNormsInitialRows(data, unitsList)

      // Pre-fetch material options for all distinct units in the loaded rows
      const uniqueUnitsToFetch = getUniqueUnitsToFetch(formattedData)

      await Promise.all(
        uniqueUnitsToFetch.map((u) =>
          fetchMaterialDropdownForUnit(u.profitId, u.unitName),
        ),
      )

      setRows(formattedData)
      setOriginalRows(formattedData)
    } catch (error) {
      console.error('Error fetching Job Work Norms Budget data:', error)
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

  useEffect(() => {
    fetchData()
  }, [fetchData])

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

    // Validation: Material Name (displayName) and Unit are mandatory to fill
    const invalidRow = data.find(
      (row) =>
        !row.unit || !row.displayName || String(row.displayName).trim() === '',
    )
    if (invalidRow) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Material Name (Display Name) is mandatory to fill!',
        severity: 'error',
      })
      setLoading(false)
      return
    }

    const payloadData = data.map((row) => ({
      id: row.id || row.materialId,
      materialId: row.materialId || row.id,
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
    }))

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
      isFetchedRef.current = false
      await fetchData()
    } catch (error) {
      console.error('Error saving Job Work Norms Budget data:', error)
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
        message: 'Record deleted successfully!',
        severity: 'success',
      })
      return
    }

    const materialId = dataItem.materialId || dataItem.id
    const unitId = dataItem.unitId || dataItem.profitId

    setLoading(true)
    try {
      await ThroughputNormsApiService.deleteThroughputNormsData(
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
      isFetchedRef.current = false
      await fetchData()
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
    deleteButton: true,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showExport: false,
    showImport: false,
    showCalculate: false,
    ExcelName: `Job Work Norms Budget_${AOP_YEAR}`,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Job Work Norms Budget',
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
        customItemChange={handleCustomItemChange}
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
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
      />
    </Box>
  )
}

export default ThroughputNormsScreen
