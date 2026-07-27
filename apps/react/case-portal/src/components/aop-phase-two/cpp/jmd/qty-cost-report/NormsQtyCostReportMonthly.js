import { useMemo, useState, useCallback } from 'react'
import { Box } from '@mui/material'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { useDispatch, useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo, {
  customValueFormatterPhaseTwo,
} from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { validateNestedRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import { UtilityPlantApiServiceV2 } from 'components/aop-phase-two/services/cpp/jmd/utilityPlantApiServiceV2'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { downloadBase64Excel } from 'components/aop-phase-two/common/utilities/downloadBase64Excel'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import { useDebounce } from 'hooks/useDebounce'

const NormsQtyCostReport = () => {
  const keycloak = useSession()
  // State management
  const dispatch = useDispatch()
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
  const PLANT_ID = plantObject?.id

  const AOP_YEAR = year?.selectedYear

  const PLANT_ID_LIST = useMemo(
    () => jmdSelectedPlants?.map((plant) => plant.id) || [],
    [jmdSelectedPlants],
  )

  const EXCEL_NAME = generateExcelName(
    dataGridStore,
    'NORMS_QTY_COST_REPORT_MONTHLY',
  )

  const headerMap = generateHeaderNames(AOP_YEAR)
  const valueFormat = ValueFormatterPhaseTwo()
  const valueFormatTwo = customValueFormatterPhaseTwo(2)

  // Fiscal-year month order: Apr → Mar
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
  const MONTH_COLUMNS = [
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
  ].map((mon) => ({
    title: headerMap[MONTH_TO_INDEX[mon]],
    children: [
      {
        field: `${mon}.qty`,
        title: 'Gen. Quantity',
        widthT: 130,
        minWidth: 130,
        type: 'number',
        format: valueFormatTwo,
      },
      {
        field: `${mon}.norms`,
        title: 'Norms',
        widthT: 130,
        minWidth: 130,
        editable: false,
        type: 'number1',
        format: valueFormat,
      },
      {
        field: `${mon}.quantity`,
        title: 'Quantity',
        widthT: 130,
        minWidth: 130,
        type: 'number',
        format: valueFormatTwo,
      },
      {
        field: `${mon}.price`,
        title: 'Price',
        widthT: 130,
        minWidth: 130,
        editable: false,
        type: 'number1',
        format: valueFormatTwo,
        hidden: false,
      },
      {
        field: `${mon}.amount`,
        title: 'Amount',
        widthT: 130,
        minWidth: 130,
        type: 'number',
        format: valueFormatTwo,
        hidden: false,
      },
    ],
  }))

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // Base columns (common to all views)
  const baseColumns = [
    //Generating Plant
    {
      field: 'cppPlantName',
      title: 'CPP Plant',
      widthT: 180,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 180,
    },
    //Generating Plant
    {
      field: 'generatingPlantName',
      title: 'Generating Plant',
      widthT: 180,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 180,
    },
    //Utility
    {
      field: 'utilityName',
      title: 'Utility',
      widthT: 120,
      type: 'text',
      editable: false,
      locked: false,
      minWidth: 120,
    },
    // Utility ID
    {
      field: 'utilityId',
      title: 'Utility ID',
      widthT: 120,
      type: 'text',
      editable: false,
      locked: false,
      minWidth: 120,
    },
    //UOM
    {
      field: 'uom',
      title: 'Generation UOM',
      widthT: 180,
      type: 'text',
      editable: false,
      minWidth: 180,
    },
    // Account
    {
      field: 'accountName',
      title: 'Account',
      widthT: 120,
      type: 'text',
      editable: false,
      minWidth: 120,
    },
    // Material
    {
      field: 'materialName',
      title: 'Material',
      widthT: 120,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 120,
    },
    // SAP Code
    {
      field: 'materialId',
      title: 'SAP Code',
      widthT: 120,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 120,
    },
    // Issuing Plant
    {
      field: 'issuingPlantName',
      title: 'Issuing Plant',
      widthT: 150,
      type: 'text',
      editable: false,
      minWidth: 150,
    },
    {
      field: 'issuingUom',
      title: 'Issuing UOM',
      widthT: 150,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 150,
    },
  ]

  // Column definitions for monthly view
  const nestedColumns = [
    ...baseColumns,
    // Monthly columns (Norms / Quantity / Amount / Price) ─ Apr → Mar
    ...MONTH_COLUMNS,
  ]

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [calculationLoading, setCaculationLoading] = useState(false)

  const fetchNormsData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await UtilityPlantApiServiceV2.getNormBasedUtilityBudget(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )

      if (res?.data?.length === 0) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        setLoading(false)
        return
      }
      let tempRes = res?.data?.list?.map((item, index) => {
        return {
          ...item,
          id: item.id || index + 1,
          remarks: item.remarks || '',
        }
      })

      setRows(tempRes)
      setOriginalRows(tempRes)
    } catch (error) {
      console.error('Error fetching fixed consumption data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR])

  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && AOP_YEAR) {
        fetchNormsData()
        setModifiedCells({})
      }
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR, fetchNormsData],
  )

  // Permissions for monthly view (editable)
  const monthlyPermissions = useMemo(() => {
    return {
      showAction: true,
      addButton: false,
      deleteButton: false,
      editButton: true,
      saveBtn: false,
      allAction: true,
      showTitleNameBusiness: true,
      titleName: screenTitle?.title,
      showImport: false,
      showTitle: true,
      showExport: true,
      ExcelName: EXCEL_NAME,
    }
  }, [])

  // Calculate Norms data via API
  const handleCalculate = async () => {
    setCaculationLoading(true)
    try {
      const res = await UtilityPlantApiServiceV2.calculateNormsData(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )

      if (res) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Calculation completed successfully!',
          severity: 'success',
        })
        // Refresh the data after calculation
        await fetchNormsData()
      }
    } catch (error) {
      console.error('Error calculating norms data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error during calculation. Please try again.',
        severity: 'error',
      })
    } finally {
      setCaculationLoading(false)
    }
  }

  // Save handler with API call
  const saveChanges = async () => {
    setLoading(true)
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

    // Custom validation: If any row data is updated, remarks must be filled and different from original
    const fieldsToCheck = [
      'apr.norms',
      'apr.price',
      'may.norms',
      'may.price',
      'jun.norms',
      'jun.price',
      'jul.norms',
      'jul.price',
      'aug.norms',
      'aug.price',
      'sep.norms',
      'sep.price',
      'oct.norms',
      'oct.price',
      'nov.norms',
      'nov.price',
      'dec.norms',
      'dec.price',
      'jan.norms',
      'jan.price',
      'feb.norms',
      'feb.price',
      'mar.norms',
      'mar.price',
    ]
    const validationError = validateNestedRowDataWithRemarks(
      data,
      originalRows,
      fieldsToCheck,
      'generatingPlantName',
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
    const tempPayload = payload?.map((item) => {
      const { normHeaderId, id, inEdit, ...rest } = item
      return {
        ...rest,
        normsHeaderFkId: normHeaderId,
      }
    })

    try {
      // Transform modifiedCells into the format expected by the API

      console.log('payload', tempPayload)

      // Update the local state with the saved data
      // setRows(updatedRows)
      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved ${modifiedData.length} changes!`,
        severity: 'success',
      })
    } catch (error) {
      console.error('Error saving plant requirement data:', error)
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
      const response = await UtilityPlantApiServiceV2.saveNormsExcel(
        file,
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Excel file imported successfully!',
          severity: 'success',
        })
        // Refresh data after import
        await fetchNormsData()
      } else if (response?.code === 400 && response?.data) {
        // Handle error response with Excel file download
        try {
          downloadBase64Excel(
            response.data,
            `Norms_Errors_${new Date().getTime()}.xlsx`,
          )

          setSnackbarOpen(true)
          setSnackbarData({
            message:
              response?.message ||
              'Import failed with errors. Please check the downloaded file.',
            severity: 'error',
          })
          // Refresh data after import
          await fetchNormsData()
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
      await UtilityPlantApiServiceV2.exportNormBasedUtilityBudgetDetailed(
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
      console.error('Error exporting Norms data:', error)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  // Handle remark cell click
  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        {/* Monthly Grid */}
        <AdvanceKendoTable
          columns={nestedColumns}
          rows={rows}
          setRows={setRows}
          handleCalculate={handleCalculate}
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          title='Norms Qty. Cost Report - Monthly'
          permissions={monthlyPermissions}
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
          customHeight={80}
          groupBy={['cppPlantName', 'generatingPlantName']}
        />
      </Box>
    </Box>
  )
}

export default NormsQtyCostReport
