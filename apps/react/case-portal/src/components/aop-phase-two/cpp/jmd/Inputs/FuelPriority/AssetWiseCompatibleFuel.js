import { useState, useMemo, useCallback } from 'react'
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
  const {
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
    jmdSelectedPlants,
  } = dataGridStore
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Asset_Compatible_Fuel')

  const PLANT_ID_LIST = useMemo(
    () => jmdSelectedPlants?.map((plant) => plant.id) || [],
    [jmdSelectedPlants],
  )

  const getOptions = useCallback(
    (dataItem) => {
      const plantEntry = plantFuelMap[dataItem?.plantName]
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

  const fuelSelectBaseConfig = useMemo(
    () => ({
      widthT: 120,
      minWidth: 120,
      type: 'select',
      dynamicOptions: true,
      getOptions,
      displayMode: 'label',
      editable: true,
      showClearOption: true,
    }),
    [getOptions],
  )

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
      ...FUEL_TYPES.map(({ field, title }) => ({
        ...fuelSelectBaseConfig,
        field,
        title,
      })),
      //   {
      //     field: 'remarks',
      //     title: 'Remarks',
      //     widthT: 250,
      //     type: 'textarea',
      //     editable: true,
      //     minWidth: 250,
      //     locked: true,
      //     lockPosition: 'right',
      //   },
    ],
    [fuelSelectBaseConfig],
  )

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])

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
      />
    </Box>
  )
}

export default AssetWiseCompatibleFuel
