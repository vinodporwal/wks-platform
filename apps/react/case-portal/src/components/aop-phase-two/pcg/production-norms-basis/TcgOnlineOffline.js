import React, { useState, useEffect, useMemo } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { generateHeaderNames } from '../../common/utilities/generateHeaders'
import { customValueFormatterPhaseTwo } from '../../common/ValueFormatterPhaseTwo'
import { validateRowDataWithRemarks } from '../../common/commonUtilityFunctions'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { ProductionNormsApiService } from 'components/aop-phase-two/services/pcg/productionNormsApiService'

// Converts whatever the dropdown API returns into [{ value, label }]
// Supports: [...], { data: [...] }, and items like
// { name, displayName } (your API) | 'P-4' | { value, label } | { id, name } | { key, text }
const normalizeOptions = (raw) => {
  const list = Array.isArray(raw) ? raw : raw?.data || []
  return list
    .map((item) => {
      if (item === null || item === undefined) return null
      if (typeof item !== 'object') {
        return { value: String(item), label: String(item) }
      }
      // API shape: { name: 'TGC-1 Online', displayName: 'TGC-1 Online' }
      const value = item.value ?? item.name ?? item.id ?? item.key ?? item.code
      const label =
        item.displayName ?? item.label ?? item.name ?? item.text ?? value
      if (value === undefined || value === null) return null
      return { value: String(value), label: String(label) }
    })
    .filter(Boolean)
}

const monthColumns = [
  { field: 'apr', headerIndex: 4 },
  { field: 'may', headerIndex: 5 },
  { field: 'jun', headerIndex: 6 },
  { field: 'jul', headerIndex: 7 },
  { field: 'aug', headerIndex: 8 },
  { field: 'sep', headerIndex: 9 },
  { field: 'oct', headerIndex: 10 },
  { field: 'nov', headerIndex: 11 },
  { field: 'dec', headerIndex: 12 },
  { field: 'jan', headerIndex: 1 },
  { field: 'feb', headerIndex: 2 },
  { field: 'mar', headerIndex: 3 },
]

const TcgOnlineOffline = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear

  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [dropdownOptions, setDropdownOptions] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const valueFormat = useMemo(() => customValueFormatterPhaseTwo(5), [])
  const headerMap = useMemo(() => generateHeaderNames(AOP_YEAR), [AOP_YEAR])

  const showError = (message) => {
    setSnackbarOpen(true)
    setSnackbarData({ message, severity: 'error' })
  }

  const columns = useMemo(
    () => [
      {
        field: 'id',
        title: 'Id',
        minWidth: 200,
        type: 'text',
        editable: false,
        locked: true,
        hidden: true,
      },
      {
        field: 'productName',
        title: 'Particulars',
        minWidth: 200,
        type: 'text',
        editable: false,
        locked: true,
      },
      {
        field: 'TypeDisplayName',
        title: 'Type',
        minWidth: 200,
        type: 'text',
        editable: false,
        locked: true,
        hidden: true,
      },
      ...monthColumns.map((month) => ({
        field: month.field,
        title: headerMap[month.headerIndex],
        minWidth: 120,
        type: 'select',
        options: dropdownOptions, // from the dropdown API
        displayMode: 'label',
        editable: true,
        format: valueFormat,
      })),
      {
        field: 'remarks',
        title: 'Remark',
        width: 300,
        minWidth: 250,
        widthT: 300,
        type: 'textarea',
        editable: true,
      },
    ],
    [dropdownOptions, headerMap, valueFormat],
  )

  const fetchDropdownData = async () => {
    try {
      const response =
        await ProductionNormsApiService.getTcgOnlineofflineDropdownData(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      setDropdownOptions(normalizeOptions(response))
    } catch (error) {
      console.error('Error fetching dropdown data:', error)
      setDropdownOptions([])
      showError('Error fetching dropdown options')
    }
  }

  const fetchData = async () => {
    setLoading(true)
    try {
      const response = await ProductionNormsApiService.getTcgOnlineofflineData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      // API returns { data: [...], code: 200, message: '...' } or an array
      const tableData = Array.isArray(response) ? response : response?.data || []

      // Map API fields to what the grid expects, and add the inEdit flag.
      // The grid needs a unique `id` per row (dataItemKey), so use normparameterFkId.
      const dataWithEditFlag = tableData.map((row) => ({
        ...row,
        id: row.normparameterFkId,
        productName: row.displayName || row.name,
        TypeDisplayName: row.normParameterType,
        UOM: row.uom,
        inEdit: false,
      }))

      setRows(dataWithEditFlag)
      setOriginalRows(dataWithEditFlag)
    } catch (error) {
      console.error('Error fetching data:', error)
      showError('Error fetching data')
    } finally {
      setLoading(false)
    }
  }

  // Load dropdown options and table data when plant / year changes
  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchDropdownData()
      fetchData()
    }
  }, [PLANT_ID, AOP_YEAR])

  const saveChanges = async () => {
    setLoading(true)

    const modifiedData = Object.values(modifiedCells)
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

    // Remark validation: a changed month value must come with a remark
    const fieldsToCheck = monthColumns.map((m) => m.field)
    const validationError = validateRowDataWithRemarks(
      data,
      originalRows,
      fieldsToCheck,
      'productName',
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

    // Send only the required fields
    const convertedData = data.map((row) => ({
      apr: row.apr,
      may: row.may,
      jun: row.jun,
      jul: row.jul,
      aug: row.aug,
      sep: row.sep,
      oct: row.oct,
      nov: row.nov,
      dec: row.dec,
      jan: row.jan,
      feb: row.feb,
      mar: row.mar,
      uom: row.uom || '',
      normparameterFkId: row.normparameterFkId,
      remarks: row.remarks || '',
    }))

    try {
      await ProductionNormsApiService.saveTcgOnlineoffline(
        keycloak,
        AOP_YEAR,
        PLANT_ID,
        convertedData,
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Data saved successfully!',
        severity: 'success',
      })
      setModifiedCells({})
      // Reload the table; fetchData also resets originalRows and the loader
      await fetchData()
    } catch (error) {
      console.error('Error saving data:', error)
      setLoading(false)
      showError('Error saving data!')
    }
  }

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showExport: false,
    showImport: false,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'TGC Online/Offline',
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
        customHeight={70}
        paginationConfig={{
          threshold: 100,
          buttonCount: 5,
          pageSizes: [10, 20, 50, 100],
          defaultPageSize: 100,
        }}
      />
    </Box>
  )
}

export default TcgOnlineOffline