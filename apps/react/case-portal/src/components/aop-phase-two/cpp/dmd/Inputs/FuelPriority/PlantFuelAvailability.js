import { useState, useRef, useMemo, useCallback, useEffect } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo, {
  customValueFormatterPhaseTwo,
} from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { InputApiService } from 'components/aop-phase-two/services/cpp/jmd/inputApiService'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import { useDebounce } from 'hooks/useDebounce'

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
  const {
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
    jmdSelectedPlants,
  } = dataGridStore
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Fuel_Priority')
  const customFormat = customValueFormatterPhaseTwo(0)
  // const PLANT_ID_LIST = useMemo(
  //   () => jmdSelectedPlants?.map((plant) => plant.id) || [],
  //   [jmdSelectedPlants],
  // )

  const PLANT_ID = plantObject?.id

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const valueFormat = ValueFormatterPhaseTwo()

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // Track previous serialized map to avoid calling setPlantFuelMap
  // when rows re-render but the actual fuel assignments haven't changed
  const prevPlantFuelMapRef = useRef('')

  // Derive per-plant fuel map from rows and push up to parent
  // so AssetFuelPriority can filter its dropdowns per plant
  useEffect(() => {
    if (!setPlantFuelMap || !fuelOptions?.length) return
    const map = {}
    rows.forEach((row) => {
      if (!row.plantName || !row.fuelFkId) return
      const option = fuelOptions.find(
        (o) => String(o.value) === String(row.fuelFkId),
      )
      if (!option) return
      if (!map[row.plantName]) map[row.plantName] = []
      // Avoid duplicates
      if (!map[row.plantName].some((o) => o.value === option.value)) {
        map[row.plantName].push(option)
      }
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
          value: fuel.id,
          label: fuel.fuelDisplayName || fuel.fuelName || fuel.id,
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

  const priorityOptions = [
    { value: 1, label: '1' },
    { value: 2, label: '2' },
    { value: 3, label: '3' },
  ]

  const columns = [
    { field: 'id', title: 'ID', hidden: true },
    {
      field: 'plantName',
      title: 'Plant',
      widthT: 150,
      minWidth: 150,
      type: 'text',
      editable: false,
      locked: true,
    },
    {
      field: 'fuelFkId',
      title: 'Compatible Fuel',
      widthT: 180,
      minWidth: 180,
      type: 'select',
      options: fuelOptions,
      displayMode: 'label',
      editable: true,
    },
    {
      field: 'priority',
      title: 'Priority',
      widthT: 130,
      minWidth: 130,
      type: 'select',
      options: priorityOptions,
      displayMode: 'label',
      editable: true,
      format: customFormat,
    },
    {
      field: 'quantity',
      title: 'QTY MMBTU',
      widthT: 150,
      minWidth: 150,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    // {
    //   field: 'uom',
    //   title: 'UOM',
    //   widthT: 120,
    //   minWidth: 120,
    //   type: 'text',
    //   editable: false,
    // },
    {
      field: 'remarks',
      title: 'Remarks',
      widthT: 250,
      type: 'textarea',
      editable: true,
      minWidth: 250,
    },
  ]

  const fetchFuelPriorityData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await InputApiService.getFuelPriorityData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      const rawList = res?.data?.FuelPriority ?? res?.data ?? res
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
      console.error('Error fetching fuel priority data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  useDebounce(
    () => {
      if (PLANT_ID && AOP_YEAR) {
        fetchFuelPriorityData()
        setModifiedCells({})
      }
    },
    1000,
    [PLANT_ID, AOP_YEAR, fetchFuelPriorityData],
  )

  const debounceTimerRef = useRef(null)

  const validateFuelPriorityData = (data) => {
    // Validate priority range and uniqueness
    for (const row of data) {
      if (
        row.priority !== null &&
        row.priority !== undefined &&
        row.priority !== ''
      ) {
        const priority = parseInt(row.priority, 10)

        // Check if priority is a valid number
        if (isNaN(priority)) {
          return `Row "${row.fuelName}": Priority must be a valid number`
        }

        // Check priority range 1-6
        if (priority < 1 || priority > 6) {
          return `Row "${row.fuelName}": Priority must be between 1 and 6`
        }

        // Check uniqueness within plant - check against ALL rows (including unmodified)
        const duplicates = rows.filter(
          (r) =>
            r.plantName === row.plantName &&
            r.id !== row.id &&
            r.priority !== null &&
            r.priority !== undefined &&
            r.priority !== '' &&
            parseInt(r.priority, 10) === priority,
        )

        if (duplicates.length > 0) {
          const existingFuel = duplicates[0].fuelName
          return `Plant "${row.plantName}": Priority ${priority} is already assigned to "${existingFuel}"`
        }
      }

      // Validate QTY MMBTU is not negative
      if (
        row.quantity !== null &&
        row.quantity !== undefined &&
        row.quantity !== ''
      ) {
        const qty = parseFloat(row.quantity)
        if (!isNaN(qty) && qty < 0) {
          return `Row "${row.fuelName}": QTY MMBTU cannot be negative`
        }
      }
    }

    return null
  }

  const permissions = {
    showAction: true,
    addButton: false,
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

    const fieldsToCheck = ['priority', 'quantity']
    const validationError = validateRowDataWithRemarks(
      data,
      originalRows,
      fieldsToCheck,
      'fuelName',
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

    const payload = modifiedData

    try {
      console.log('payload', payload)

      const response = await InputApiService.saveFuelPriorityData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
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
        PLANT_ID,
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
        PLANT_ID,
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
      />
    </Box>
  )
}

export default PlantFuelAvailability
