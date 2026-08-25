import React, { useState, useEffect, useRef, useCallback } from 'react'
import { Box, Backdrop, CircularProgress } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { generateHeaderNames } from '../../common/utilities/generateHeaders'
import ValueFormatterPhaseTwo, {
  customValueFormatterPhaseTwo,
} from '../../common/ValueFormatterPhaseTwo'
import { validateRowDataWithRemarks } from '../../common/commonUtilityFunctions'
import { SteadyStateConsumptionApiService } from '../../services/crude/steadyStateConsumptionApiService'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { JswBudgetSourceAPIService } from 'components/aop-phase-two/services/crude/jwBudgetSourceAPIService'
import { getUnitOptions } from './helpers'
import { JwSelectCellEditor } from './JwSelectCellEditor'

const JwBudgetScreen = () => {
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
      cellEditor: JwSelectCellEditor,
      dynamicOptions: true,
      getOptions: (dataItem) => getUnitOptions(dataItem, unitDropdown, rows),
      editable: true,
      locked: true,
    },
    {
      field: 'normParameterTypeDisplayName',
      title: 'Type',
      widthT: 250,
      minWidth: 200,
      type: 'text',
      editable: false,
      locked: true,
      hidden: true,
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

  const dummyRows = []

  useEffect(() => {
    if (SITE_ID) {
      fetchUnitDropdown()
    }
  }, [SITE_ID])

  const fetchUnitDropdown = async () => {
    try {
      const response = await JswBudgetSourceAPIService.getDropdownUnit(
        keycloak,
        SITE_ID,
        'SEZ',
      )
      const data = response?.data || response?.result || response || []
      const formattedOptions = Array.isArray(data)
        ? data.map((item) => ({
            label:
              item?.Unit || item?.unit || item?.name || item?.displayName || '',
            value: item?.Unit || item?.unit || item?.id || item?.Id || '',
            id: item?.Id || item?.id,
            unitId: item?.Id || item?.id,
            uom: item?.UOM || item?.uom || '',
          }))
        : []
      setUnitDropdown(formattedOptions)
    } catch (error) {
      console.error('Error fetching unit dropdown options:', error)
    }
  }

  // Ensure every new row gets a unique ID so multiple blank rows never collide
  const customAddRow = () => {
    const uniqueId = `new_row_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
    const newRow = {
      id: uniqueId,
      unit: '',
      normParameterTypeDisplayName: '',
      uom: '',
      UOM: '',
      unitId: '',
      unitFKId: '',
      normParameterFKId: '',
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

  const handleCustomItemChange = (e, setRowsState, setModifiedCellsState) => {
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
        const uomVal = selectedObj.uom || ''
        const unitIdVal = selectedObj.unitId || selectedObj.id || ''
        const unitLabel = selectedObj.label || selectedObj.value || valStr

        const updatedRow = {
          ...dataItem,
          unit: unitLabel,
          uom: uomVal,
          UOM: uomVal,
          unitId: unitIdVal,
          unitFKId: unitIdVal,
          normParameterFKId: unitIdVal,
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
      const response = await JswBudgetSourceAPIService.getJswBudgetSourceData(
        keycloak,
        SITE_ID,
        AOP_YEAR,
        'SEZ',
      )
      const data = response?.data || []
      const formattedData = data?.map((item, index) => ({
        ...item,
        unit: item.unit || item.Unit || '',
        uom: item.uom || item.UOM || '',
        UOM: item.UOM || item.uom || '',
        remarks: item.remarks || '',
        id: item?.id || index + 1,
        isEditable: true,
      }))
      setRows(formattedData)
      setOriginalRows(formattedData)
    } catch (error) {
      console.error('Error fetching SEZ JobWork Throughput data:', error)
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

    const payloadData = data.map((row) => {
      const matched = unitDropdown.find(
        (opt) =>
          opt.value === row.unit ||
          opt.label === row.unit ||
          opt.id === row.unit ||
          opt.unitId === row.unitId,
      )
      return {
        ...row,
        id: row.id || matched?.unitId || matched?.id,
        uom: row.uom || row.UOM || matched?.uom || '',
        jan: row.jan || 0,
        feb: row.feb || 0,
        mar: row.mar || 0,
        apr: row.apr || 0,
        may: row.may || 0,
        jun: row.jun || 0,
        jul: row.jul || 0,
        aug: row.aug || 0,
        sep: row.sep || 0,
        oct: row.oct || 0,
        nov: row.nov || 0,
        dec: row.dec || 0,
      }
    })

    try {
      await JswBudgetSourceAPIService.saveJswBudgetSourceData(
        payloadData,
        keycloak,
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
      console.error('Error saving SEZ JobWork Throughput data:', error)
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

    setLoading(true)
    try {
      await JswBudgetSourceAPIService.deleteJwBudgetData(
        keycloak,
        dataItem.id,
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
    ExcelName: `SEZ JobWork Throughput_${AOP_YEAR}`,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'SEZ JobWork Throughput',
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
        setCurrentRowId={() => {}}
        saveChanges={saveChanges}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        //customHeight={70}
      />
    </Box>
  )
}

export default JwBudgetScreen
