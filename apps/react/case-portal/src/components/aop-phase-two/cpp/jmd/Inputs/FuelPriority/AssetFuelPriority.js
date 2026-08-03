import { useState, useMemo, useCallback, Children } from 'react'
import { Box } from '@mui/material'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { AssetPriorityApiService } from 'components/aop-phase-two/services/cpp/jmd/assetPriorityApiService'
import { InputApiService } from 'components/aop-phase-two/services/cpp/jmd/inputApiService'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { useDebounce } from 'hooks/useDebounce'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

// Static constants — defined at module level so they are never re-created
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

const FUEL_PRIORITY_FIELDS = [
  { field: 'primary', title: 'Primary' },
  { field: 'secondary', title: 'Secondary' },
  { field: 'tertiary', title: 'Tertiary' },
]

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

const MONTH_BASE_COLUMN_CONFIG = {
  editable: true,
  widthT: 150,
  minWidth: 150,
}

const AssetFuelPriority = ({ fuelOptions = [], plantFuelMap = {} }) => {
  const keycloak = useSession()
  // State management

  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
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
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Asset_Priority')

  const PLANT_ID_LIST = useMemo(
    () => jmdSelectedPlants?.map((plant) => plant.id) || [],
    [jmdSelectedPlants],
  )
  const headerMap = generateHeaderNames(AOP_YEAR)
  const valueFormat = ValueFormatterPhaseTwo()

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // Create a per-month getOptions function that returns only fuels available
  // for that plant in that specific month
  const getOptionsForMonth = useCallback(
    (mon) => (dataItem) => {
      const plantMonthFuels = plantFuelMap[dataItem?.plantName]?.[mon]
      return plantMonthFuels?.length ? plantMonthFuels : fuelOptions
    },
    [plantFuelMap, fuelOptions],
  )

  // Generate month columns dynamically — memoized so grid doesn't remount on every render
  const MONTH_COLUMNS = useMemo(
    () =>
      MONTH_FIELDS.map((mon) => ({
        ...MONTH_BASE_COLUMN_CONFIG,
        field: mon,
        title: headerMap[MONTH_TO_INDEX[mon]],
        children: FUEL_PRIORITY_FIELDS.map(({ field, title }) => ({
          widthT: 120,
          minWidth: 120,
          type: 'select',
          dynamicOptions: true,
          getOptions: getOptionsForMonth(mon),
          displayMode: 'label',
          editable: true,
          returnFullObject: true,
          field: `${mon}${field.charAt(0).toUpperCase() + field.slice(1)}FuelName`,
          title,
        })),
      })),
    [getOptionsForMonth, headerMap],
  )

  // Column definitions — memoized so AdvanceKendoTable doesn't rebuild on unrelated renders
  const columns = useMemo(
    () => [
      {
        field: 'assetName',
        title: 'Asset Name',
        widthT: 150,
        type: 'text',
        editable: false,
        locked: true,
        minWidth: 150,
      },
      // Monthly columns — Apr → Mar (generated, no duplication)
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
    ],
    [MONTH_COLUMNS],
  )

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])

  const fetchAssetPriorityData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await InputApiService.getAssetFuelPriority(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )

      const nonFuelAssetType = ['PRDS', 'STG']
      const rawList =
        res?.data?.filter(
          (item) => !nonFuelAssetType.includes(item.assetType),
        ) || []

      if (rawList.length === 0) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        setLoading(false)
        return
      }

      // API returns UUIDs in fuel fields (aprPrimary, aprSecondary, etc.)
      // Add FuelName fields with labels for display, keep UUID fields as-is for saving
      const rowsWithEditableFlag = rawList.map((row, index) => {
        const transformed = {
          ...row,
          id: row.id || index + 1,
          remarks: row.remarks || '',
        }
        MONTH_FIELDS.forEach((mon) => {
          FUEL_PRIORITY_FIELDS.forEach(({ field }) => {
            const fieldName = `${mon}${field.charAt(0).toUpperCase() + field.slice(1)}`
            const fuelNameField = `${fieldName}FuelName`
            const uuid = row[fieldName]
            if (uuid) {
              const option = fuelOptions.find(
                (o) => String(o.value) === String(uuid),
              )
              transformed[fuelNameField] = option ? option.label : ''
            } else {
              transformed[fuelNameField] = ''
            }
          })
        })
        return transformed
      })

      setRows(rowsWithEditableFlag)
      setOriginalRows(rowsWithEditableFlag)
    } catch (error) {
      console.error('Error fetching asset fuel priority data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR, fuelOptions])

  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && AOP_YEAR && fuelOptions?.length > 0) {
        fetchAssetPriorityData()
        setModifiedCells({})
      }
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR, fuelOptions?.length, fetchAssetPriorityData],
  )

  // Permissions (adjust as needed)
  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showTitleNameBusiness: true,
    titleName: screenTitle?.title,
    showImport: false,
    ExcelName: EXCEL_NAME,
    showTitle: true,
  }

  // Save handler with API call
  const saveChanges = async () => {
    setLoading(true)
    console.log('modifiedCells', modifiedCells)
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length == 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      setLoading(false)
      return
    }

    var rawData = Object.values(modifiedCells)
    const data = rawData.filter((row) => row.inEdit)
    if (data.length == 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      setLoading(false)
      return
    }

    // Validate no duplicate fuels within the same month for any modified row
    for (const row of data) {
      for (const mon of MONTH_FIELDS) {
        const primary = row[`${mon}PrimaryFuelName`]
        const secondary = row[`${mon}SecondaryFuelName`]
        const tertiary = row[`${mon}TertiaryFuelName`]

        const selectedFuels = [primary, secondary, tertiary].filter(
          (val) => val && String(val).trim() !== '',
        )
        const uniqueFuels = new Set(selectedFuels)

        if (selectedFuels.length !== uniqueFuels.size) {
          const monthName = mon.charAt(0).toUpperCase() + mon.slice(1)
          setSnackbarOpen(true)
          setSnackbarData({
            message: `Duplicate fuels selected in ${monthName} for asset ${row.assetName || ''}.`,
            severity: 'error',
          })
          setLoading(false)
          return
        }
      }
    }

    // Custom validation: If any row data is updated, remarks must be filled and different from original
    const fieldsToCheck = MONTH_FIELDS.map((mon) =>
      FUEL_PRIORITY_FIELDS.map(
        ({ field }) =>
          `${mon}${field.charAt(0).toUpperCase() + field.slice(1)}FuelName`,
      ),
    ).flat()
    const validationError = validateRowDataWithRemarks(
      data,
      originalRows,
      fieldsToCheck,
      'assetName',
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

    try {
      const payload = modifiedData.map((item) => {
        const { inEdit, ...rest } = item
        // Remove FuelName display fields, keep UUID fields for backend
        const cleanedItem = { ...rest }
        MONTH_FIELDS.forEach((mon) => {
          FUEL_PRIORITY_FIELDS.forEach(({ field }) => {
            const fuelNameField = `${mon}${field.charAt(0).toUpperCase() + field.slice(1)}FuelName`
            delete cleanedItem[fuelNameField]
          })
        })
        return cleanedItem
      })

      // Call PUT /task/asset-fuel-priority to save all modified rows
      await InputApiService.saveAssetFuelPriority(keycloak, payload)

      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved ${modifiedData.length} changes!`,
        severity: 'success',
      })
      fetchAssetPriorityData()
    } catch (error) {
      console.error('Error saving asset fuel priority data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to save changes. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  // Custom item change handler — follows PlantFuelAvailability pattern
  // field is the FuelName field (e.g. aprPrimaryFuelName)
  // Store label in FuelName field for display, UUID in the original field for saving
  const handleCustomItemChange = useCallback(({ dataItem, field, value }) => {
    const updates = { [field]: value }
    // If field ends with FuelName, also update the UUID field
    if (field.endsWith('FuelName')) {
      const uuidField = field.replace('FuelName', '')
      if (value && typeof value === 'object') {
        updates[uuidField] = value.value // UUID for saving
        updates[field] = value.label // name for display
      } else {
        updates[uuidField] = null
        updates[field] = value || ''
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
  }, [])

  // Handle remark cell click
  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }
  console.log('rows', rows)
  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title='Asset Wise Fuel Priority'
        permissions={permissions}
        customItemChange={handleCustomItemChange}
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
        groupBy={['plantName', 'assetType']}
      />
    </Box>
  )
}

export default AssetFuelPriority
