import { useState, useEffect, useCallback } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from '../../common/utilities/generateHeaders'
import { customValueFormatterPhaseTwo } from '../../common/ValueFormatterPhaseTwo'
import { SteadyStateConsumptionApiService } from '../../services/polyester/steadyStateConsumptionApiService'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import { downloadBase64Excel } from '../../common/utilities/downloadBase64Excel'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import ValidationErrorDialog from './ValidationErrorDialog'
import RowBasedKendoTable from 'components/aop-phase-two/common/RowBasedKendoTable/index'

const GradeWiseSteadyStateConsumption = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(
    dataGridStore,
    'Grade_Wise_Steady_State_Consumption',
  )
  const isFilament = verticalObject?.name?.toLowerCase() === 'filament (pfy)'

  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // Grade dropdown state
  const [grades, setGrades] = useState([])
  const [selectedGradeId, setSelectedGradeId] = useState(null)

  // calculationObject for conditional calculateDisabled
  const [calculationObject, setCalculationObject] = useState([])

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  // Detailed validation error modal states
  const [validationErrorDialogOpen, setValidationErrorDialogOpen] =
    useState(false)
  const [validationErrors, setValidationErrors] = useState([])

  const valueFormat = customValueFormatterPhaseTwo(5)
  const headerMap = generateHeaderNames(AOP_YEAR)

  const columns = [
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
      field: 'normParameterTypeDisplayName',
      title: 'Type',
      minWidth: 200,
      type: 'text',
      editable: false,
      locked: true,
      hidden: true,
    },
    {
      field: 'sapCode',
      title: 'SAP Code',
      minWidth: 120,
      type: 'text',
      editable: false,
    },
    {
      field: 'UOM',
      title: 'UOM',
      minWidth: 100,
      type: 'text',
      editable: false,
    },
    {
      field: 'april',
      title: headerMap[4],
      minWidth: 120,
      type: 'row-based',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'may',
      title: headerMap[5],
      minWidth: 120,
      type: 'row-based',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'june',
      title: headerMap[6],
      minWidth: 120,
      type: 'row-based',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'july',
      title: headerMap[7],
      minWidth: 120,
      type: 'row-based',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'august',
      title: headerMap[8],
      minWidth: 120,
      type: 'row-based',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'september',
      title: headerMap[9],
      minWidth: 120,
      type: 'row-based',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'october',
      title: headerMap[10],
      minWidth: 120,
      type: 'row-based',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'november',
      title: headerMap[11],
      minWidth: 120,
      type: 'row-based',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'december',
      title: headerMap[12],
      minWidth: 120,
      type: 'row-based',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'january',
      title: headerMap[1],
      minWidth: 120,
      type: 'row-based',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'february',
      title: headerMap[2],
      minWidth: 120,
      type: 'row-based',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'march',
      title: headerMap[3],
      minWidth: 120,
      type: 'row-based',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'remarks',
      title: 'Remark',
      minWidth: 220,
      type: 'textarea',
      editable: true,
    },
  ]

  // ===================== Fetch Grade Dropdown =====================

  const fetchGrades = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      let response = await SteadyStateConsumptionApiService.getGrades(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        const gradeList = response?.data || []
        setGrades(gradeList)
        // Auto-select first grade (index 0) on initial load
        if (gradeList.length > 0 && !selectedGradeId) {
          setSelectedGradeId(gradeList[0].gradeId)
        }
        if (!Array.isArray(gradeList) || gradeList.length === 0) {
          setLoading(false)
        }
      } else {
        setGrades([])
      }
    } catch (error) {
      setGrades([])
      console.error('Error fetching grades:', error)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak])

  // ===================== Fetch Main Data (requires gradeId) =====================

  const fetchData = useCallback(
    async (gradeId) => {
      if (!PLANT_ID || !AOP_YEAR) return
      if (!gradeId) return

      setLoading(true)
      setRows([])
      try {
        let response =
          await SteadyStateConsumptionApiService.getSteadyStateConsumptionByGrade(
            keycloak,
            gradeId,
            PLANT_ID,
            AOP_YEAR,
          )

        setCalculationObject(response?.data?.aopCalculation || [])

        const mappedData = response?.data?.mcuNormsValueDTOList || []
        const formattedData = mappedData.map((item, index) => ({
          ...item,
          idFromApi: item.id,
          id: `${index}`,
          originalRemark: item.remarks,
          Particulars: item.normParameterTypeDisplayName,
          isEditable: isFilament ? item.isEditable : true,
          type: 'number1',
        }))

        setRows(formattedData)
        setOriginalRows(formattedData)
      } catch (error) {
        console.error('Error fetching steady state consumption data:', error)
      } finally {
        setLoading(false)
      }
    },
    [PLANT_ID, AOP_YEAR, keycloak],
  )

  const validateGradeNorms = useCallback(
    async (gradeId) => {
      if (!gradeId || !PLANT_ID || !AOP_YEAR) return
      try {
        const valRes =
          await SteadyStateConsumptionApiService.validateGradeSteadyStateNorms(
            keycloak,
            PLANT_ID,
            AOP_YEAR,
            gradeId,
          )
        if (
          valRes &&
          (valRes.code === 400 || valRes.status === 400 || valRes.code === 200) &&
          Array.isArray(valRes.data) &&
          valRes.data.length > 0
        ) {
          setValidationErrors(valRes.data)
          setValidationErrorDialogOpen(true)
        }
      } catch (err) {
        console.error('Error validating grade steady state norms:', err)
      }
    },
    [PLANT_ID, AOP_YEAR, keycloak],
  )

  // ===================== Initial load & Grade change =====================

  useEffect(() => {
    if (!PLANT_ID || !AOP_YEAR) return
    setRows([])
    setOriginalRows([])
    setGrades([])
    setSelectedGradeId(null)
    Promise.all([fetchGrades()])
  }, [PLANT_ID, AOP_YEAR])

  // Re-fetch data & validate when selectedGradeId changes
  useEffect(() => {
    if (selectedGradeId) {
      fetchData(selectedGradeId)
      if (!isFilament) validateGradeNorms(selectedGradeId)
    }
  }, [selectedGradeId, isFilament, fetchData, validateGradeNorms])

  const saveChanges = useCallback(async () => {
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
      return
    }

    const fieldsToCheck = [
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
    const validationError = validateRowDataWithRemarks(
      modifiedData,
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

    setLoading(true)
    try {
      const payload = modifiedData.map((row) => ({
        april: row.april || null,
        may: row.may || null,
        june: row.june || null,
        july: row.july || null,
        august: row.august || null,
        september: row.september || null,
        october: row.october || null,
        november: row.november || null,
        december: row.december || null,
        january: row.january || null,
        february: row.february || null,
        march: row.march || null,
        remark: row.remarks,
        remarks: row.remarks,
        financialYear: AOP_YEAR,
        plantId: PLANT_ID,
        normParameterId: row.normParameterId,
        id: row.idFromApi || null,
        materialFkId: row.materialFkId || null,
        mcuVersion: row.mcuVersion || null,
        plantFkId: row.plantFkId || null,
        siteFkId: row.siteFkId || null,
        verticalFkId: row.verticalFkId || null,
        unit: row.unit || null,
        normParameterTypeId: row.normParameterTypeId || null,
        gradeId: row.gradeId || selectedGradeId || null,
      }))

      const response =
        await SteadyStateConsumptionApiService.saveGradeWiseSteadyStateConsumption(
          PLANT_ID,
          payload,
          keycloak,
          selectedGradeId,
          AOP_YEAR,
        )

      let isWeightedAverageError = false
      let errorMsg = ''
      let errorDataList = []

      if (response && (response.code === 400 || response.status === 400)) {
        isWeightedAverageError = true
        errorMsg = response.message || 'Validation failed.'
        if (response && Array.isArray(response.data)) {
          errorDataList = response.data
        }
      }

      if (isWeightedAverageError && !isFilament) {
        if (errorDataList && errorDataList.length > 0) {
          setValidationErrors(errorDataList)
          setValidationErrorDialogOpen(true)
        } else {
          setSnackbarOpen(true)
          setSnackbarData({
            message: errorMsg || 'Weighted average does not match.',
            severity: 'error',
          })
        }
      } else if (response && !(response instanceof Response)) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'Saved Successfully!', severity: 'success' })
        setModifiedCells({})
        await fetchData(selectedGradeId)
      } else {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'Norms not saved!', severity: 'error' })
      }
    } catch (error) {
      console.error('Error saving steady state consumption:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error saving data!', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [
    modifiedCells,
    PLANT_ID,
    AOP_YEAR,
    keycloak,
    selectedGradeId,
    fetchData,
    isFilament
  ])

  // ===================== Calculate (PE uses site + vertical — same as NormalOpNorms handleCalculateNormalOperationNormsPe) =====================

  const handleCalculate = async () => {
    setLoading(true)
    setSnackbarOpen(true)
    setSnackbarData({ message: 'Calculating...', severity: 'info' })
    try {
      let data;
      if (isFilament){
        data = await SteadyStateConsumptionApiService.calculateSteadyStateConsumptionPolyester(
          PLANT_ID,
          SITE_ID,
          VERTICAL_ID,
          AOP_YEAR,
          keycloak,
        )
      } else{
       data = await SteadyStateConsumptionApiService.calculateSteadyStateConsumptionPE(
          PLANT_ID,
          SITE_ID,
          VERTICAL_ID,
          AOP_YEAR,
          keycloak,
        )
      }

      if (data == 0 || data) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        await fetchGrades()
        await fetchData(selectedGradeId)
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Calculation failed. Please try again.',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error calculating steady state consumption:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: error.message || 'Calculation failed.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  // ===================== Export (PE all-grades export — same as NormalOpNorms getNormalOpsNormsExcelpe) =====================

  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({ message: 'Excel export started!', severity: 'info' })
    try {
      await SteadyStateConsumptionApiService.exportSteadyStateConsumptionPE(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_NAME,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting steady state consumption:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  // ===================== Import (with gradeId — same as NormalOpNorms saveNormalOpsNormsExcel) =====================

  const handleImport = async (file) => {
    setLoading(true)
    setSnackbarOpen(true)
    setSnackbarData({ message: 'Importing data...', severity: 'info' })
    try {
      const response =
        await SteadyStateConsumptionApiService.importSteadyStateConsumptionByGrade(
          file,
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          selectedGradeId,
        )

      let isWeightedAverageError = false
      let errorMsg = ''
      let errorDataList = []

      if (
        response &&
        response.code === 400 &&
        (response.message === 'Validation Failed' || Array.isArray(response.data))
      ) {
        isWeightedAverageError = true
        errorMsg = response.message || 'Import validation failed.'
        errorDataList = Array.isArray(response.data) ? response.data : []
      }

      if (isWeightedAverageError) {
        if (errorDataList && errorDataList.length > 0) {
          setValidationErrors(errorDataList)
          setValidationErrorDialogOpen(true)
        } else {
          setSnackbarOpen(true)
          setSnackbarData({
            message: errorMsg || 'Weighted average does not match.',
            severity: 'error',
          })
        }
      } else if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Uploaded Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        await fetchData(selectedGradeId)
      } else if (response?.code === 400 && response?.data) {
        // Partial save — download error file
        downloadBase64Excel(
          response.data,
          'Error File Grade Wise Steady State Consumption.xlsx',
        )
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        await fetchData(selectedGradeId)
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Import failed. Please try again.',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error importing steady state consumption:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Import failed. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleRemarkCellClick = (row) => {
    if (!row?.isEditable) return
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
    showExport: true,
    showImport: true,
    showCalculate: true,
    // Disable Calculate if no calculationObject from API (same as NormalOpNorms showCalculateVisibility)
    calculateDisabled:
      !calculationObject || Object.keys(calculationObject).length === 0,
    ExcelName: EXCEL_NAME,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Grade Wise Steady State Consumption (Norm/Quantity)',
    // Grade dropdown (showG → showDropdown: true for PE)
    showDropdown: true,
    remarksEditable: true,
    marginBottom: true,
  }

  // Grade dropdown config (same as NormalOpNorms dropdownLabel: 'Grade' for isPEPP)
  const dropdownConfig = {
    options: grades,
    label: 'Grade',
    placeholder: 'Select Grade',
    valueKey: 'gradeId', // grade objects use 'gradeId' not 'id'
    labelKey: 'displayName',
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <RowBasedKendoTable
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
        setCurrentRowId={() => { }}
        saveChanges={saveChanges}
        handleExport={handleExport}
        handleExcelUpload={handleImport}
        handleCalculate={handleCalculate}
        dropdownConfig={dropdownConfig}
        selectedDropdownValue={selectedGradeId || ''}
        setSelectedDropdownValue={setSelectedGradeId}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        groupBy={['normParameterTypeDisplayName']}
        customHeight={70}
        paginationConfig={{
          threshold: 100,
          buttonCount: 5,
          pageSizes: [10, 20, 50, 100],
          defaultPageSize: 100,
        }}
      />

      {/* Detailed Validation Error Modal */}
      <ValidationErrorDialog
        open={validationErrorDialogOpen}
        onClose={() => {
          setValidationErrorDialogOpen(false)
          setValidationErrors([])
        }}
        errors={validationErrors}
      />
    </Box>
  )
}

export default GradeWiseSteadyStateConsumption
