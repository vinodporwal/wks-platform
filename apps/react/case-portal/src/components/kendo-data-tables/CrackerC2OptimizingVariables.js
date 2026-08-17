import React, { useState, useEffect, useCallback, useRef } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from './index'
import { OptimizingVariablesApiService } from 'services/optimizing-variables-api-service'
import { getRoleName } from 'services/role-service'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import Notification from 'components/Utilities/Notification'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { validateFields } from 'utils/validationUtils'

const MONTH_FIELDS = [
  'april',
  'may',
  'june',
  'july',
  'august',
  'september',
  'october',
  'november',
  'december',
  'january',
  'february',
  'march',
]

const CrackerC2OptimizingVariables = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    plantObject,
    year,
    oldYear,
    verticalObject,
    siteObject,
    isReleased,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, isReleased)

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}`

  const valueFormat = ValueFormatterProduction()
  const headerMap = generateHeaderNames(AOP_YEAR)

  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [dropdownOptions, setDropdownOptions] = useState([])
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const unsavedChangesRef = useRef({
    unsavedRows: {},
    rowsBeforeChange: {},
  })

  const dropdownOptionsRef = useRef([])

  const handleSetRows = useCallback((updateFn) => {
    setRows((prev) => (typeof updateFn === 'function' ? updateFn(prev) : updateFn))
  }, [])

  // Month columns use 'feedTypeOrNumeric' type — the FeedTypeOrNumericEditor
  // renders a dropdown when UOM === '#', or a numeric input otherwise.
  const monthColumns = MONTH_FIELDS.map((monthField, idx) => {
    const calendarMonth = ((idx + 3) % 12) + 1
    return {
      field: monthField,
      title: headerMap[calendarMonth] || monthField,
      editable: true,
      type: 'feedTypeOrNumeric',
      minWidth: 100,
      format: valueFormat,
      isRightAlligned: 'numeric',
    }
  })

  const columns = [
    {
      field: 'DisplayName',
      title: 'Particulars',
      editable: false,
      minWidth: 250,
      locked: true,
    },
    {
      field: 'UOM',
      title: 'UOM',
      editable: false,
      minWidth: 80,
      locked: true,
    },
    ...monthColumns,
    {
      field: 'remarks',
      title: 'Remarks',
      editable: true,
      minWidth: 200,
      widthT: 200,
    },
  ]

  // Format raw API data into grid rows
  const formatApiData = (data, options = []) => {
    const defValue = options[0]?.name || null
    return data.map((item, index) => {
      const isFirst = index === 0
      const getVal = (val) => {
        if (!isFirst) return val
        if (
          val === null ||
          val === undefined ||
          val === 0 ||
          val === '0' ||
          typeof val !== 'string' ||
          val.trim() === ''
        ) {
          return defValue
        }
        return val
      }
      return {
        ...item,
        idFromApi: item.Id || item.id,
        id: index,
        DisplayName: item.DisplayName || item.displayName,
        UOM: item.UOM || item.uom,
        originalRemark: item.Remarks || item.remarks || '',
        remarks: item.Remarks || item.remarks || '',
        srNo: index + 1,

        april: getVal(item.April ?? item.april ?? null),
        may: getVal(item.May ?? item.may ?? null),
        june: getVal(item.June ?? item.june ?? null),
        july: getVal(item.July ?? item.july ?? null),
        august: getVal(item.August ?? item.august ?? null),
        september: getVal(item.September ?? item.september ?? null),
        october: getVal(item.October ?? item.october ?? null),
        november: getVal(item.November ?? item.november ?? null),
        december: getVal(item.December ?? item.december ?? null),
        january: getVal(item.January ?? item.january ?? null),
        february: getVal(item.February ?? item.february ?? null),
        march: getVal(item.March ?? item.march ?? null),

        normParameterTypeFKId:
          item.NormParameterType_FK_Id || item.normParameterTypeFKId || null,
      }
    })
  }

  // Fetch data
  const fetchData = useCallback(async (currentDropdownOptions = dropdownOptionsRef.current) => {
    if (!PLANT_ID || !AOP_YEAR) return
    setRows([])
    setLoading(true)
    try {
      const res = await OptimizingVariablesApiService.getCrackerC2OptimizingVariables(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (res?.code !== 200) {
        setRows([])
        return
      }

      setRows(formatApiData(res?.data || [], currentDropdownOptions))
    } catch (error) {
      console.error('Error fetching Cracker C2 Optimizing Variables:', error)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  const fetchDropdownOptions = useCallback(async () => {
    try {
      const res = await OptimizingVariablesApiService.getCrackerC2OptimizingVariablesDropdown(keycloak)
      if (res?.code === 200 && Array.isArray(res.data)) {
        const mapped = res.data
          .map((opt) => ({
            name: opt.name || opt.Name,
            value: opt.value !== undefined ? Number(opt.value) : Number(opt.Value),
          }))
          .sort((a, b) => a.value - b.value)
        setDropdownOptions(mapped)
        dropdownOptionsRef.current = mapped
        return mapped
      } else {
        setDropdownOptions([])
        dropdownOptionsRef.current = []
        return []
      }
    } catch (e) {
      console.error('Failed to fetch cracker C2 dropdown', e)
      setDropdownOptions([])
      dropdownOptionsRef.current = []
      return []
    }
  }, [keycloak])

  useEffect(() => {
    const init = async () => {
      const opts = await fetchDropdownOptions()
      await fetchData(opts)
    }
    init()
  }, [fetchDropdownOptions, fetchData])

  // Save handler
  const saveData = async (editedRows) => {
    setLoading(true)
    try {
      const payload = editedRows.map((row) => ({
        id: row.idFromApi || null,
        name: row.Name || row.name || '',
        displayName: row.DisplayName || row.displayName || '',
        uom: row.UOM || row.uom || '',
        normParameterTypeFKId: row.normParameterTypeFKId || null,
        isEditable: row.isEditable,
        isVisible: row.isVisible ?? true,
        displayOrder: row.displayOrder || null,
        remarks: row.remarks || '',
        april: row.april !== undefined && row.april !== null ? String(row.april) : null,
        may: row.may !== undefined && row.may !== null ? String(row.may) : null,
        june: row.june !== undefined && row.june !== null ? String(row.june) : null,
        july: row.july !== undefined && row.july !== null ? String(row.july) : null,
        august: row.august !== undefined && row.august !== null ? String(row.august) : null,
        september: row.september !== undefined && row.september !== null ? String(row.september) : null,
        october: row.october !== undefined && row.october !== null ? String(row.october) : null,
        november: row.november !== undefined && row.november !== null ? String(row.november) : null,
        december: row.december !== undefined && row.december !== null ? String(row.december) : null,
        january: row.january !== undefined && row.january !== null ? String(row.january) : null,
        february: row.february !== undefined && row.february !== null ? String(row.february) : null,
        march: row.march !== undefined && row.march !== null ? String(row.march) : null,
      }))

      const response = await OptimizingVariablesApiService.saveCrackerC2OptimizingVariables(
        PLANT_ID,
        payload,
        keycloak,
        AOP_YEAR,
      )

      if (response?.code === 200 || response) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Save Failed!',
          severity: 'error',
        })
      }
      return response
    } catch (error) {
      console.error('Error saving data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error saving data!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const saveChanges = useCallback(async () => {
    try {
      const data = Object.values(modifiedCells)
      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        return
      }

      const requiredFields = ['remarks']
      const validationMessage = validateFields(data, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationMessage,
          severity: 'error',
        })
        return
      }

      saveData(data)
    } catch (error) {
      console.error('Error in saveChanges:', error)
    }
  }, [modifiedCells])

  // Permissions
  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
      showCalculate: false,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: false,
      allAction: true,
      showTitleNameBusiness: true,
      titleName: 'Pilot furnace details',
      saveWithRemark: true,
      saveBtn: true,
      showCalculate: false,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      makePagable: false,
      feedTypeOptions: dropdownOptions,
    },
    IS_OLD_YEAR,
  )

  return (
    <div>
      <LoaderBackdrop open={!!loading} />
      <Box>
        <KendoDataTables
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          columns={columns}
          setRows={handleSetRows}
          rows={rows}
          saveChanges={saveChanges}
          fetchData={fetchData}
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
          handleRemarkCellClick={handleRemarkCellClick}
          unsavedChangesRef={unsavedChangesRef}
          permissions={adjustedPermissions}
        />
      </Box>
      <Notification
        open={snackbarOpen}
        message={snackbarData?.message || ''}
        severity={snackbarData?.severity || 'info'}
        onClose={() => setSnackbarOpen(false)}
      />
    </div>
  )
}

export default CrackerC2OptimizingVariables
