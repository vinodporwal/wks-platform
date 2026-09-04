import { useState, useRef, useMemo, useCallback, useEffect } from 'react'
import { Box, Tooltip, IconButton } from '@mui/material'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo, {
  customValueFormatterPhaseTwo,
} from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { InputApiService } from 'components/aop-phase-two/services/cpp/jmd/inputApiService'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import DeleteDialog from 'components/aop-phase-two/common/AdvanceKendoTable/components/DeleteDialog'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import { useDebounce } from 'hooks/useDebounce'

const MAX_RECORDS_PER_PLANT = 3

const PRIORITY_OPTIONS = [
  { value: 1, label: '1' },
  { value: 2, label: '2' },
  { value: 3, label: '3' },
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

const SUB_COLUMN_FIELDS = [
  {
    field: 'CategoryName',
    title: 'Fuel Category',
    type: 'select',
    widthT: 150,
    minWidth: 150,
    showClearOption: true,
  },
  {
    field: 'FuelName',
    title: 'Compatible Fuel',
    type: 'select',
    widthT: 180,
    minWidth: 180,
    showClearOption: true,
  },
  {
    field: 'FuelCode',
    title: 'Fuel Code',
    type: 'text',
    editable: false,
    widthT: 120,
    minWidth: 120,
  },
  // {
  //   field: 'Uom',
  //   title: 'UOM',
  //   type: 'text',
  //   editable: false,
  //   widthT: 100,
  //   minWidth: 100,
  // },
  {
    field: 'Priority',
    title: 'Priority',
    type: 'select',
    widthT: 130,
    minWidth: 130,
  },
  {
    field: 'Quantity',
    title: 'QTY MMBTU',
    type: 'number1',
    widthT: 150,
    minWidth: 150,
  },
]

const PlantFuelAvailability = ({
  fuelOptions,
  setFuelOptions,
  setPlantFuelMap,
}) => {
  const keycloak = useSession()
  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { year, screenTitle, plantObject } = dataGridStore
  const AOP_YEAR = year?.selectedYear
  const PLANT_ID = plantObject?.id
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Fuel_Priority')
  const customFormat = customValueFormatterPhaseTwo(0)
  const headerMap = generateHeaderNames(AOP_YEAR)
  const PLANT_ID_LIST = useMemo(() => [PLANT_ID], [PLANT_ID])

  const plantOptions = useMemo(
    () => [
      {
        value: plantObject.name,
        label: plantObject.name,
      },
    ],
    [plantObject],
  )

  // Category dropdown options derived from the fuel master data. Each fuel
  // carries its categoryFkId + categoryDisplayName, so we collect the unique
  // categories that have at least one fuel mapped to them.
  const categoryOptions = useMemo(() => {
    const map = new Map()
    fuelOptions.forEach((f) => {
      if (f.categoryFkId && !map.has(f.categoryFkId)) {
        map.set(f.categoryFkId, {
          value: f.categoryFkId,
          label: f.categoryDisplayName || f.categoryName || f.categoryFkId,
        })
      }
    })
    return Array.from(map.values())
  }, [fuelOptions])

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const valueFormat = ValueFormatterPhaseTwo()

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [rowToDelete, setRowToDelete] = useState(null)

  const prevPlantFuelMapRef = useRef('')
  useEffect(() => {
    if (!setPlantFuelMap || !fuelOptions?.length) return
    const map = {}
    rows.forEach((row) => {
      if (!row.plantName) return
      if (!map[row.plantName]) map[row.plantName] = {}
      MONTH_FIELDS.forEach((mon) => {
        const fuelFkId = row[`${mon}FuelFkId`]
        if (!fuelFkId) return
        const option = fuelOptions.find(
          (o) => String(o.value) === String(fuelFkId),
        )
        if (!option) return
        if (!map[row.plantName][mon]) map[row.plantName][mon] = []
        // Avoid duplicates within same month
        if (!map[row.plantName][mon].some((o) => o.value === option.value)) {
          map[row.plantName][mon].push(option)
        }
      })
    })
    // Only push update if the map actually changed — prevents unnecessary
    // AssetFuelPriority re-renders caused by object reference churn
    const serialized = JSON.stringify(map)
    if (serialized !== prevPlantFuelMapRef.current) {
      prevPlantFuelMapRef.current = serialized
      setPlantFuelMap(map)
    }
  }, [rows, fuelOptions, setPlantFuelMap])

  useEffect(() => {
    const fetchFuelOptions = async () => {
      try {
        const res = await InputApiService.getFuelMaster(keycloak)
        const rawOptions = res?.data || []
        const formattedOptions = rawOptions.map((fuel) => ({
          fuelId: fuel.id,
          value: fuel.id,
          label: fuel.fuelName || fuel.fuelDisplayName || fuel.id,
          fuelName: fuel.fuelName || '',
          fuelDisplayName: fuel.fuelDisplayName || '',
          fuelCode: fuel.fuelCode || '',
          uom: fuel.uom || '',
          categoryFkId: fuel.categoryFkId || '',
          categoryName: fuel.categoryName || '',
          categoryDisplayName: fuel.categoryDisplayName || '',
        }))
        setFuelOptions(formattedOptions)
      } catch (error) {
        console.error('Error fetching fuel master options:', error)
      }
    }
    if (keycloak?.token) {
      fetchFuelOptions()
    }
  }, [keycloak])

  // Enrich existing rows (loaded from backend) with per-month category /
  // fuel code / UOM derived from the fuel master. The monthly API response
  // only carries fuelFkId + fuelName per month, so we look up the rest from
  // fuelOptions. Only fills fields that are not already set (so user edits
  // are preserved). Runs whenever fuelOptions become available.
  useEffect(() => {
    if (!fuelOptions?.length) return
    setRows((prev) => {
      if (!prev?.length) return prev
      let changed = false
      const next = prev.map((row) => {
        const enriched = { ...row }
        MONTH_FIELDS.forEach((mon) => {
          const fuelFkId = row[`${mon}FuelFkId`]
          if (!fuelFkId) return
          const fuel = fuelOptions.find(
            (f) => String(f.value) === String(fuelFkId),
          )
          if (!fuel) return
          if (!enriched[`${mon}CategoryFkId`] && fuel.categoryFkId) {
            enriched[`${mon}CategoryFkId`] = fuel.categoryFkId
            changed = true
          }
          const categoryDisplay =
            fuel.categoryDisplayName || fuel.categoryName || ''
          if (!enriched[`${mon}CategoryName`] && categoryDisplay) {
            enriched[`${mon}CategoryName`] = categoryDisplay
            changed = true
          }
          if (!enriched[`${mon}FuelCode`] && fuel.fuelCode) {
            enriched[`${mon}FuelCode`] = fuel.fuelCode
            changed = true
          }
          if (!enriched[`${mon}Uom`] && fuel.uom) {
            enriched[`${mon}Uom`] = fuel.uom
            changed = true
          }
        })
        return enriched
      })
      return changed ? next : prev
    })
  }, [fuelOptions, rows])

  const MONTH_COLUMNS = useMemo(
    () =>
      MONTH_FIELDS.map((mon) => ({
        title: headerMap[MONTH_TO_INDEX[mon]],
        children: SUB_COLUMN_FIELDS.map((sub) => {
          const baseConfig = {
            field: `${mon}${sub.field}`,
            title: sub.title,
            widthT: sub.widthT,
            minWidth: sub.minWidth,
            editable: sub.editable !== false,
          }
          if (sub.type === 'select') {
            if (sub.field === 'CategoryName') {
              return {
                ...baseConfig,
                type: 'select',
                options: categoryOptions,
                displayMode: 'label',
                showClearOption: true,
                returnFullObject: true,
              }
            } else if (sub.field === 'FuelName') {
              return {
                ...baseConfig,
                type: 'select',
                dynamicOptions: true,
                getOptions: (dataItem) => {
                  const catId = dataItem?.[`${mon}CategoryFkId`]
                  if (!catId) return []
                  return fuelOptions.filter((f) => f.categoryFkId === catId)
                },
                displayMode: 'label',
                showClearOption: true,
                returnFullObject: true,
              }
            } else {
              return {
                ...baseConfig,
                type: 'select',
                options: PRIORITY_OPTIONS,
                displayMode: 'label',
                format: customFormat,
                showClearOption: true,
              }
            }
          }
          return {
            ...baseConfig,
            type: sub.type,
            format: valueFormat,
          }
        }),
      })),
    [headerMap, fuelOptions, categoryOptions, customFormat, valueFormat],
  )

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

  const columns = useMemo(
    () => [
      { field: 'id', title: 'ID', hidden: true },
      {
        field: 'plantName',
        title: 'Plant',
        widthT: 150,
        minWidth: 150,
        type: 'select',
        options: plantOptions,
        displayMode: 'label',
        editable: true,
        locked: true,
      },
      ...MONTH_COLUMNS,
      {
        field: 'remarks',
        title: 'Remarks',
        widthT: 250,
        type: 'textarea',
        editable: true,
        minWidth: 250,
        locked: true,
        lockPosition: 'right',
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
    ],
    [MONTH_COLUMNS, plantOptions],
  )

  const fetchFuelPriorityData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await InputApiService.getPlantFuelAvailabilityMonthly(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )
      const rawList = res?.data ?? res
      if (!rawList || (Array.isArray(rawList) && rawList.length === 0)) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        return
      }
      const tempRes = (Array.isArray(rawList) ? rawList : []).map(
        (item, index) => ({
          ...item,
          id: item.id || index + 1,
          remarks: item.remarks || '',
        }),
      )
      setRows(tempRes)
      setOriginalRows(tempRes)
    } catch (error) {
      console.error(
        'Error fetching plant fuel availability monthly data:',
        error,
      )
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR])

  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && AOP_YEAR) {
        fetchFuelPriorityData()
        setModifiedCells({})
      }
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR, fetchFuelPriorityData],
  )

  const hasValue = (val) => val !== null && val !== undefined && val !== ''

  const validateFuelPriorityData = (data) => {
    // Validate priority range and QTY for each month
    for (const row of data) {
      for (const mon of MONTH_FIELDS) {
        const priorityField = `${mon}Priority`
        const quantityField = `${mon}Quantity`
        const fuelField = `${mon}FuelFkId`
        const categoryField = `${mon}CategoryFkId`

        const hasCategory = hasValue(row[categoryField])
        const hasFuel = hasValue(row[fuelField])
        const hasPriority = hasValue(row[priorityField])
        const hasQty = hasValue(row[quantityField])

        // For any month: all 4 fields required or none of them
        if (hasCategory || hasFuel || hasPriority || hasQty) {
          if (!hasCategory) {
            return `Row "${row.plantName}": Select a Fuel Category for ${mon.toUpperCase()} — all fields (Category, Fuel, Priority, QTY MMBTU) are required or none`
          }
          if (!hasFuel) {
            return `Row "${row.plantName}": Select a Fuel for ${mon.toUpperCase()} — all fields (Category, Fuel, Priority, QTY MMBTU) are required or none`
          }
          if (!hasPriority) {
            return `Row "${row.plantName}": Select a Priority for ${mon.toUpperCase()} — all fields (Category, Fuel, Priority, QTY MMBTU) are required or none`
          }
          if (!hasQty) {
            return `Row "${row.plantName}": Enter QTY MMBTU for ${mon.toUpperCase()} — all fields (Category, Fuel, Priority, QTY MMBTU) are required or none`
          }
        } else {
          continue
        }

        const priority = parseInt(row[priorityField], 10)

        // Check if priority is a valid number
        if (isNaN(priority)) {
          return `Row "${row.plantName}": Priority must be a valid number`
        }

        // Check priority range 1-6
        if (priority < 1 || priority > 6) {
          return `Row "${row.plantName}": Priority must be between 1 and 6`
        }

        // Check uniqueness within plant for the same month
        const duplicates = rows.filter(
          (r) =>
            r.plantName === row.plantName &&
            r.id !== row.id &&
            r[`${mon}FuelFkId`] &&
            hasValue(r[priorityField]) &&
            parseInt(r[priorityField], 10) === priority,
        )

        if (duplicates.length > 0) {
          const existingFuel = duplicates[0].fuelName
          return `Plant "${row.plantName}": Priority ${priority} for ${mon.toUpperCase()} is already assigned to "${existingFuel}"`
        }

        // Validate QTY MMBTU is not negative
        const qty = parseFloat(row[quantityField])
        if (!isNaN(qty) && qty < 0) {
          return `Row "${row.plantName}": QTY MMBTU for ${mon.toUpperCase()} cannot be negative`
        }
      }
    }

    return null
  }

  const permissions = {
    showAction: false,
    addButton: true,
    addBtnName: 'Add Row',
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showImport: false,
    downloadExcelBtnFromUI: true,
    ExcelName: EXCEL_NAME,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: screenTitle?.title,
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

    // Validate max 3 records per plant (including existing rows)
    const plantCounts = {}
    rows.forEach((r) => {
      if (r.plantName) {
        plantCounts[r.plantName] = (plantCounts[r.plantName] || 0) + 1
      }
    })
    for (const [plant, count] of Object.entries(plantCounts)) {
      if (count > MAX_RECORDS_PER_PLANT) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: `Plant "${plant}" has ${count} records. Maximum ${MAX_RECORDS_PER_PLANT} records allowed per plant.`,
          severity: 'error',
        })
        setLoading(false)
        return
      }
    }

    // Custom validation for priority and qty
    const customValidationError = validateFuelPriorityData(data)
    if (customValidationError) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: customValidationError,
        severity: 'error',
      })
      setLoading(false)
      return
    }

    // Build list of all monthly fields to check for remarks validation
    const fieldsToCheck = MONTH_FIELDS.flatMap((mon) => [
      `${mon}CategoryFkId`,
      `${mon}FuelFkId`,
      `${mon}Priority`,
      `${mon}Quantity`,
    ])
    const validationError = validateRowDataWithRemarks(
      data,
      originalRows,
      fieldsToCheck,
      'plantName',
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

    const payload = modifiedData.map((item) => {
      const { inEdit, isNew, isEditable, plantName, ...rest } = item
      const plantFkId = [plantObject]?.find(
        (p) => p.name === item.plantName,
      )?.id
      const sanitized = Object.fromEntries(
        Object.entries(rest).map(([key, value]) => [
          key,
          value === '' || value === undefined ? null : value,
        ]),
      )
      return {
        ...sanitized,
        plantFkId: plantFkId || null,
        aopYear: AOP_YEAR,
        ...(isNew ? { id: null } : {}),
      }
    })

    try {
      const response = await InputApiService.savePlantFuelAvailabilityMonthly(
        keycloak,
        payload,
      )

      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved ${modifiedData.length} changes!`,
        severity: 'success',
      })
      await fetchFuelPriorityData()
    } catch (error) {
      console.error('Error saving fuel priority data:', error)
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
      const response = await InputApiService.importFuelPriorityExcel(
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
        await fetchFuelPriorityData()
      } else if (response?.code === 400 && response?.data) {
        const byteCharacters = atob(response.data)
        const byteNumbers = Array.from(byteCharacters, (char) =>
          char.charCodeAt(0),
        )
        const byteArray = new Uint8Array(byteNumbers)

        const blob = new Blob([byteArray], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        })

        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `Error File - Fuel Priority.xlsx`)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        await fetchFuelPriorityData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
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
      await InputApiService.exportFuelPriorityExcel(
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
      console.error('Error exporting Fuel Priority data:', error)
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
        await InputApiService.deletePlantFuelAvailabilityMonthly(
          keycloak,
          rowToDelete.id,
        )
      } catch (error) {
        setSnackbarOpen(true)
        setSnackbarData({
          message:
            'Failed to delete record: ' + (error.message || 'Unknown error'),
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

  const handleCustomItemChange = useCallback(
    (e) => {
      const { dataItem, field, value } = e
      if (!dataItem || !field) return

      if (field === 'plantName') {
        const plantCount = rows.filter(
          (r) => r.plantName === value && r.id !== dataItem.id,
        ).length
        if (plantCount >= MAX_RECORDS_PER_PLANT) {
          setSnackbarOpen(true)
          setSnackbarData({
            message: `Plant "${value}" already has ${MAX_RECORDS_PER_PLANT} records. Maximum ${MAX_RECORDS_PER_PLANT} records allowed per plant.`,
            severity: 'error',
          })
          return
        }
      }

      // If field is {mon}FuelName, value is the full option object from SelectCellEditor
      // (returnFullObject: true on column config)
      const updates = { [field]: value }

      // Fuel Category selection (per-month): {mon}CategoryName
      // value is the full option object { value, label } (returnFullObject: true)
      if (field.endsWith('CategoryName')) {
        const catFkField = field.replace('CategoryName', 'CategoryFkId')
        const monPrefix = field.slice(0, -'CategoryName'.length)
        if (value && typeof value === 'object' && value.value) {
          updates[catFkField] = value.value
          updates[field] = value.label
        } else {
          updates[catFkField] = null
          updates[field] = ''
        }
        // Reset fuel fields when category changes
        updates[`${monPrefix}FuelName`] = ''
        updates[`${monPrefix}FuelFkId`] = null
        updates[`${monPrefix}FuelCode`] = ''
        updates[`${monPrefix}Uom`] = ''
      }

      // Fuel selection (per-month): {mon}FuelName
      // value is the full option object { value, label } (returnFullObject: true)
      if (field.endsWith('FuelName')) {
        const fkField = field.replace('FuelName', 'FuelFkId')
        const monPrefix = field.slice(0, -'FuelName'.length)
        if (value && typeof value === 'object' && value.value) {
          updates[fkField] = value.value // UUID for saving
          updates[field] = value.label // name for display/filter
          // Auto-populate fuel code, UOM, and category from the selected fuel
          const fuel = fuelOptions.find((f) => f.value === value.value)
          if (fuel) {
            updates[`${monPrefix}FuelCode`] = fuel.fuelCode || ''
            updates[`${monPrefix}Uom`] = fuel.uom || ''
            if (fuel.categoryFkId) {
              updates[`${monPrefix}CategoryFkId`] = fuel.categoryFkId
              updates[`${monPrefix}CategoryName`] =
                fuel.categoryDisplayName || fuel.categoryName || ''
            }
          }
        } else {
          // Cleared or non-object value
          updates[fkField] = null
          updates[field] = ''
          updates[`${monPrefix}FuelCode`] = ''
          updates[`${monPrefix}Uom`] = ''
        }
      }

      setRows((prevRows) =>
        prevRows.map((row) =>
          row.id === dataItem.id ? { ...row, ...updates, inEdit: true } : row,
        ),
      )
      setModifiedCells((prev) => ({
        ...prev,
        [dataItem.id]: {
          ...prev[dataItem.id],
          ...dataItem,
          ...updates,
          inEdit: true,
        },
      }))
    },
    [rows, fuelOptions],
  )

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title='Plant Wise Fuel Priority'
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
        groupBy={['plantName']}
        customItemChange={handleCustomItemChange}
      />

      <DeleteDialog
        openDeleteDialogeBox={deleteDialogOpen}
        setOpenDeleteDialogeBox={setDeleteDialogOpen}
        deleteTheRecord={handleConfirmDelete}
        message='Are you sure you want to delete this Fuel Priority record?'
        confirmButtonText='Delete'
      />
    </Box>
  )
}

export default PlantFuelAvailability
