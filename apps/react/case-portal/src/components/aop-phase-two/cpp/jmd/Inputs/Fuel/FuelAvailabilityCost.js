import { useEffect, useState, useMemo, useCallback } from 'react'
import { Box } from '@mui/material'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { useSelector } from 'react-redux'
import { FuelAvailabilityAPIService } from 'components/aop-phase-two/services/cpp/jmd/fuelAvailabilityAPIService'
import { useSession } from 'SessionStoreContext'
import { customValueFormatterPhaseTwo } from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { useDebounce } from 'hooks/useDebounce'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

// Cost grid for JMD Fuel Availability — lists the fuels already added in the
// quantity grid (CPPFuelAvailabilityTransaction rows) and lets the user edit
// only the monthly cost values + remarks. Saves go through the cost-only API
// so quantity data is never touched here.
const DATA_TYPE = 'FUEL_AVAILABILITY'

const FuelAvailabilityCost = ({ refreshTrigger = 0, exchangeRate }) => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, year, jmdSelectedPlants } = dataGridStore
  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const IS_JMD = siteObject?.name?.toLowerCase() == 'jmd'
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Fuel_Availability_Cost')

  const PLANT_ID_LIST = useMemo(
    () =>
      IS_JMD
        ? jmdSelectedPlants?.map((plant) => plant.id) ?? []
        : PLANT_ID
          ? [PLANT_ID]
          : [],
    [plantObject, jmdSelectedPlants, siteObject],
  )

  const headerMap = generateHeaderNames(AOP_YEAR)
  const valueFormat = customValueFormatterPhaseTwo(2)

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

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

  const COST_FIELD = (mon) =>
    `cost${mon.charAt(0).toUpperCase()}${mon.slice(1)}`

  const monthBaseColumnConfig = {
    editable: true,
    widthT: 100,
    minWidth: 100,
    align: 'left',
    headerAlign: 'left',
    type: 'number1',
    format: valueFormat,
  }

  const MONTH_COST_COLUMNS = MONTH_FIELDS.map((mon) => ({
    ...monthBaseColumnConfig,
    field: COST_FIELD(mon),
    title: headerMap[MONTH_TO_INDEX[mon]],
  }))

  const readonlyCol = (field, title, minWidth, extra = {}) => ({
    field,
    title,
    type: 'text',
    editable: false,
    minWidth,
    ...extra,
  })

  const columns = [
    readonlyCol('cppPlantName', 'CPP Plant', 120, { locked: true }),
    readonlyCol('fuelDisplayName', 'Fuel', 200, { locked: true }),
    // readonlyCol('uom', 'UOM', 100),
    // readonlyCol('calorificValue', 'Calorific Value', 120, {
    //   type: 'number1',
    //   format: valueFormat,
    // }),
    ...MONTH_COST_COLUMNS,
    {
      field: 'remarks',
      title: 'Remarks',
      widthT: 250,
      type: 'textarea',
      editable: true,
      minWidth: 250,
    },
  ]

  const fetchData = useCallback(async () => {
    if ((!SITE_ID && !PLANT_ID_LIST.length) || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await FuelAvailabilityAPIService.getFuelAvailabilityCost(
        keycloak,
        SITE_ID ? [] : PLANT_ID_LIST,
        AOP_YEAR,
        DATA_TYPE,
        SITE_ID,
      )
      const data = response?.data || []

      if (!data || data.length === 0) {
        setRows([])
        setOriginalRows([])
        return
      }

      const rowsWithId = data.map((row, index) => ({
        ...row,
        id: row.id || `row_${index}`,
        remarks: row.remarks || '',
      }))
      setRows(rowsWithId)
      setOriginalRows(rowsWithId)
    } catch (error) {
      console.error('Error fetching fuel availability cost data:', error)
      setRows([])
      setOriginalRows([])
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR, SITE_ID])

  useDebounce(
    () => {
      if ((SITE_ID || PLANT_ID_LIST?.length) && AOP_YEAR) {
        fetchData()
      }
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR, SITE_ID, refreshTrigger],
  )

  useEffect(() => {
    setModifiedCells({})
  }, [PLANT_ID_LIST, AOP_YEAR])

  const permissions = {
    showAction: true,
    addButton: false,
    editButton: false,
    saveBtn: true,
    allAction: true,
    showExport: true,
    ExcelName: EXCEL_NAME,
    showImport: true,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: `Fuel Cost (In INR , ${exchangeRate !== '' && exchangeRate != null ? ` 1$ = ${exchangeRate} INR` : ''})`,
  }

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

    const fieldsToCheck = MONTH_FIELDS.map(COST_FIELD)
    const validationError = validateRowDataWithRemarks(
      data,
      originalRows,
      fieldsToCheck,
      'fuelDisplayName',
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
        const { inEdit, isNew, isEditable, ...rest } = item
        const sanitized = {
          ...rest,
          type: DATA_TYPE,
          financialYear: AOP_YEAR,
        }
        Object.keys(sanitized).forEach((key) => {
          if (sanitized[key] === '' || sanitized[key] === undefined) {
            sanitized[key] = null
          }
        })
        return sanitized
      })

      await FuelAvailabilityAPIService.saveFuelAvailabilityCost(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
        payload,
      )

      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved ${modifiedData.length} changes!`,
        severity: 'success',
      })
      fetchData()
    } catch (error) {
      console.error('Error saving fuel availability cost data:', error)
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
      const response =
        await FuelAvailabilityAPIService.importFuelAvailabilityCost(
          file,
          keycloak,
          PLANT_ID_LIST,
          AOP_YEAR,
          DATA_TYPE,
        )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Excel file imported successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        await fetchData()
      } else if (response?.code === 400 && response?.data) {
        try {
          const base64Data = response.data
          const binaryString = window.atob(base64Data)
          const bytes = new Uint8Array(binaryString.length)
          for (let i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i)
          }
          const blob = new Blob([bytes], {
            type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          })
          const url = window.URL.createObjectURL(blob)
          const link = document.createElement('a')
          link.href = url
          link.download = `Fuel_Availability_Cost_Errors_${new Date().getTime()}.xlsx`
          document.body.appendChild(link)
          link.click()
          document.body.removeChild(link)
          window.URL.revokeObjectURL(url)

          setSnackbarOpen(true)
          setSnackbarData({
            message:
              response?.message ||
              'Import failed with errors. Please check the downloaded file.',
            severity: 'error',
          })
          await fetchData()
        } catch (downloadError) {
          console.error('Error downloading error file:', downloadError)
          setSnackbarOpen(true)
          setSnackbarData({
            message: 'Import failed but could not download error file.',
            severity: 'error',
          })
        }
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Failed to import Excel file.',
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
      await FuelAvailabilityAPIService.exportFuelAvailabilityCost(
        keycloak,
        SITE_ID ? [] : PLANT_ID_LIST,
        AOP_YEAR,
        DATA_TYPE,
        EXCEL_NAME,
        SITE_ID,
      )
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting Fuel Availability Cost data:', error)
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
        title={permissions.titleName}
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
        groupBy={['cppPlantName']}
        initialFieldValues={{}}
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

export default FuelAvailabilityCost
