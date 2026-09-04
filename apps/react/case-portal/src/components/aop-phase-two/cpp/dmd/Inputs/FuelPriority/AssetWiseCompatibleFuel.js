import { useState, useMemo, useCallback, useEffect } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { InputApiService } from 'components/aop-phase-two/services/cpp/jmd/inputApiService'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { useDebounce } from 'hooks/useDebounce'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

const FUEL_TYPES = [
  { field: 'primary', title: 'Primary' },
  { field: 'secondary', title: 'Secondary' },
  { field: 'tertiary', title: 'Tertiary' },
]

const AssetWiseCompatibleFuel = ({ fuelOptions = [], plantFuelMap = {} }) => {
  const keycloak = useSession()

  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year, screenTitle } =
    dataGridStore
  const AOP_YEAR = year?.selectedYear
  const PLANT_ID = plantObject?.id
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Asset_Compatible_Fuel')

  const PLANT_ID_LIST = useMemo(() => [PLANT_ID], [PLANT_ID])

  const getPlantFuels = useCallback(
    (plantName) => {
      const plantEntry = plantFuelMap[plantName]
      if (plantEntry) {
        // plantFuelMap is now month-wise: { apr: [...], may: [...], ... }
        // Flatten all months into a unique list for annual view
        if (!Array.isArray(plantEntry)) {
          const allFuels = []
          Object.values(plantEntry).forEach((monthFuels) => {
            if (Array.isArray(monthFuels)) {
              monthFuels.forEach((fuel) => {
                if (!allFuels.some((f) => f.value === fuel.value)) {
                  allFuels.push(fuel)
                }
              })
            }
          })
          return allFuels.length ? allFuels : fuelOptions
        }
        // Backward compat: if it's still a flat array
        return plantEntry.length ? plantEntry : fuelOptions
      }
      return fuelOptions
    },
    [plantFuelMap, fuelOptions],
  )

  const getCategoryOptions = useCallback(
    (dataItem) => {
      const availableFuels = getPlantFuels(dataItem?.plantName)
      const map = new Map()
      availableFuels.forEach((f) => {
        if (f.categoryFkId && !map.has(f.categoryFkId)) {
          map.set(f.categoryFkId, {
            value: f.categoryFkId,
            label: f.categoryDisplayName || f.categoryName || f.categoryFkId,
          })
        }
      })
      return Array.from(map.values())
    },
    [getPlantFuels],
  )

  const getFuelOptionsForField = useCallback(
    (categoryFkField) => {
      return (dataItem) => {
        const availableFuels = getPlantFuels(dataItem?.plantName)
        const catId = dataItem?.[categoryFkField]
        if (!catId) return availableFuels
        return availableFuels.filter((f) => f.categoryFkId === catId)
      }
    },
    [getPlantFuels],
  )

  const columns = useMemo(() => {
    const cols = [
      {
        field: 'assetName',
        title: 'Asset Name',
        widthT: 150,
        type: 'text',
        editable: false,
        locked: true,
        minWidth: 150,
      },
    ]
    FUEL_TYPES.forEach(({ field, title }) => {
      const categoryField = `${field}Category`
      const categoryFkField = `${field}CategoryFkId`
      const fuelCodeField = `${field}FuelCode`

      cols.push({
        title,
        children: [
          // Category dropdown
          {
            field: categoryField,
            title: 'Category',
            widthT: 150,
            minWidth: 150,
            type: 'select',
            dynamicOptions: true,
            getOptions: getCategoryOptions,
            displayMode: 'label',
            editable: true,
            showClearOption: true,
            returnFullObject: true,
          },
          // Fuel dropdown (filtered by category)
          {
            field,
            title: 'Fuel',
            widthT: 120,
            minWidth: 120,
            type: 'select',
            dynamicOptions: true,
            getOptions: getFuelOptionsForField(categoryFkField),
            displayMode: 'label',
            editable: true,
            showClearOption: true,
          },
          // Fuel Code (read-only, auto-populated)
          {
            field: fuelCodeField,
            title: 'Code',
            widthT: 100,
            minWidth: 100,
            type: 'text',
            editable: false,
          },
        ],
      })
    })
    return cols
  }, [getCategoryOptions, getFuelOptionsForField])

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])

  useEffect(() => {
    if (!fuelOptions?.length) return
    setRows((prev) => {
      if (!prev?.length) return prev
      let changed = false
      const next = prev.map((row) => {
        const enriched = { ...row }
        FUEL_TYPES.forEach(({ field }) => {
          const fuelId = row[field]
          if (!fuelId) return
          const fuel = fuelOptions.find(
            (f) => String(f.value) === String(fuelId),
          )
          if (!fuel) return
          const categoryFkField = `${field}CategoryFkId`
          const categoryField = `${field}Category`
          const fuelCodeField = `${field}FuelCode`
          if (!enriched[categoryFkField] && fuel.categoryFkId) {
            enriched[categoryFkField] = fuel.categoryFkId
            changed = true
          }
          const categoryDisplay =
            fuel.categoryDisplayName || fuel.categoryName || ''
          if (!enriched[categoryField] && categoryDisplay) {
            enriched[categoryField] = categoryDisplay
            changed = true
          }
          if (!enriched[fuelCodeField] && fuel.fuelCode) {
            enriched[fuelCodeField] = fuel.fuelCode
            changed = true
          }
        })
        return enriched
      })
      return changed ? next : prev
    })
  }, [fuelOptions, rows])

  const fetchAssetCompatibleFuelData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await InputApiService.getCompatibleFuelAssets(
        keycloak,
        PLANT_ID_LIST,
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

      const rowsWithEditableFlag = rawList.map((row, index) => {
        // Parse compatibleFuel string to array
        let compatibleFuelList = row.compatibleFuelList || []

        // If compatibleFuelList is empty, try to parse from compatibleFuel string
        if (compatibleFuelList.length === 0 && row.compatibleFuel) {
          try {
            // Try to parse as JSON array first: ["id1","id2"] or ['id1','id2']
            if (
              row.compatibleFuel.trim().startsWith('[') &&
              row.compatibleFuel.trim().endsWith(']')
            ) {
              compatibleFuelList = JSON.parse(row.compatibleFuel)
            }
          } catch (e) {
            console.error('Error parsing compatibleFuel:', e)
            compatibleFuelList = []
          }
        }

        return {
          ...row,
          id: row.id || index + 1,
          remarks: row.remarks || '',
          primary: compatibleFuelList.length > 0 ? compatibleFuelList[0] : '',
          secondary: compatibleFuelList.length > 1 ? compatibleFuelList[1] : '',
          tertiary: compatibleFuelList.length > 2 ? compatibleFuelList[2] : '',
          // isEditable: false,
        }
      })

      setRows(rowsWithEditableFlag)
      setOriginalRows(rowsWithEditableFlag)
    } catch (error) {
      console.error('Error fetching asset compatible fuel data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST])

  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && fuelOptions?.length > 0) {
        fetchAssetCompatibleFuelData()
        setModifiedCells({})
      }
    },
    1000,
    [PLANT_ID_LIST, fuelOptions?.length, fetchAssetCompatibleFuelData],
  )

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

  console.log('rows', rows)

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

    for (const row of data) {
      const primary = row.primary
      const secondary = row.secondary
      const tertiary = row.tertiary

      const selectedFuels = [primary, secondary, tertiary].filter(
        (val) => val && val.trim() !== '',
      )
      const uniqueFuels = new Set(selectedFuels)

      if (selectedFuels.length !== uniqueFuels.size) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: `Duplicate fuels selected for asset ${row.assetName || ''}.`,
          severity: 'error',
        })
        setLoading(false)
        return
      }
    }

    // const fieldsToCheck = ['primary', 'secondary', 'tertiary']
    // const validationError = validateRowDataWithRemarks(
    //   data,
    //   originalRows,
    //   fieldsToCheck,
    //   'assetName',
    // )

    // if (validationError) {
    //   setSnackbarOpen(true)
    //   setSnackbarData({
    //     message: validationError,
    //     severity: 'error',
    //   })
    //   setLoading(false)
    //   return
    // }

    try {
      const payload = modifiedData.map((item) => {
        const { inEdit, ...rest } = item
        // Reconstruct compatibleFuel from primary, secondary, tertiary
        const fuels = [rest.primary, rest.secondary, rest.tertiary].filter(
          (f) => f && f.trim() !== '',
        )
        return {
          assetId: rest.assetId,
          assetName: rest.assetName,
          assetType: rest.assetType,
          assetCategory: rest.assetCategory,
          cppPlantFkId: rest.cppPlantFkId,
          plantName: rest.plantName,
          compatibleFuel: JSON.stringify(fuels),
        }
      })

      const response = await InputApiService.saveAssetCompatibleFuel(
        keycloak,
        payload,
      )

      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message:
          response?.message ||
          `Successfully saved ${modifiedData.length} changes!`,
        severity: 'success',
      })
      fetchAssetCompatibleFuelData()
    } catch (error) {
      console.error('Error saving asset compatible fuel data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: error?.message || 'Failed to save changes. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleRemarkCellClick = (row) => {
    // Remark dialog functionality can be added if needed
  }

  const handleCustomItemChange = useCallback(
    (e) => {
      const { dataItem, field, value } = e
      if (!dataItem || !field) return

      const updates = { [field]: value }

      // Category selection: {field}Category
      // value is the full option object { value, label } (returnFullObject: true)
      if (field.endsWith('Category')) {
        const fuelTypeField = field.replace('Category', '')
        const catFkField = `${fuelTypeField}CategoryFkId`
        if (value && typeof value === 'object' && value.value) {
          updates[catFkField] = value.value
          updates[field] = value.label
        } else {
          updates[catFkField] = null
          updates[field] = ''
        }
        // Reset fuel + code when category changes
        updates[fuelTypeField] = ''
        updates[`${fuelTypeField}FuelCode`] = ''
      }

      // Fuel selection: primary, secondary, tertiary
      // value is the fuel UUID string (no returnFullObject on fuel column)
      if (FUEL_TYPES.some((ft) => ft.field === field)) {
        if (value) {
          const fuel = fuelOptions.find(
            (f) => String(f.value) === String(value),
          )
          if (fuel) {
            updates[`${field}FuelCode`] = fuel.fuelCode || ''
            if (fuel.categoryFkId) {
              updates[`${field}CategoryFkId`] = fuel.categoryFkId
              updates[`${field}Category`] =
                fuel.categoryDisplayName || fuel.categoryName || ''
            }
          }
        } else {
          updates[`${field}FuelCode`] = ''
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
    [fuelOptions],
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
        title='Asset Wise Compatible Fuel'
        permissions={permissions}
        handleRemarkCellClick={handleRemarkCellClick}
        saveChanges={saveChanges}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        groupBy={['plantName', 'assetType']}
        defaultGridExpanded={false}
        customItemChange={handleCustomItemChange}
      />
    </Box>
  )
}

export default AssetWiseCompatibleFuel
