import { useState, useEffect, useCallback } from 'react'
import {
  Box,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  IconButton,
} from '@mui/material'
import { styled } from '@mui/material/styles'
import CloseIcon from '@mui/icons-material/Close'
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { generateHeaderNames } from '../../common/utilities/generateHeaders'
import { customValueFormatterPhaseTwo } from '../../common/ValueFormatterPhaseTwo'
import { SteadyStateConsumptionApiService } from '../../services/polyester/steadyStateConsumptionApiService'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import { downloadBase64Excel } from '../../common/utilities/downloadBase64Excel'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'

const PremiumErrorDialog = styled(Dialog)(() => ({
  '& .MuiPaper-root': {
    borderRadius: '16px',
    width: '500px',
    boxShadow: '0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)',
    borderTop: '5px solid #ef4444',
    padding: '8px',
  },
}))


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

  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // Grade dropdown state (for PE/PP — same as NormalOpNorms.js isPEPP logic)
  const [grades, setGrades] = useState([])
  const [selectedGradeId, setSelectedGradeId] = useState(null)

  // calculationObject for conditional calculateDisabled
  const [calculationObject, setCalculationObject] = useState([])

  // allRedCell for norm transaction highlights
  const [allRedCell, setAllRedCell] = useState([])

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  // Error modal states
  const [errorModalOpen, setErrorModalOpen] = useState(false)
  const [errorModalTitle, setErrorModalTitle] = useState('')
  const [errorModalMessage, setErrorModalMessage] = useState('')

  const showErrorModal = (title, message) => {
    setErrorModalTitle(title)
    setErrorModalMessage(message)
    setErrorModalOpen(true)
  }


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
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'may',
      title: headerMap[5],
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'june',
      title: headerMap[6],
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'july',
      title: headerMap[7],
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'august',
      title: headerMap[8],
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'september',
      title: headerMap[9],
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'october',
      title: headerMap[10],
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'november',
      title: headerMap[11],
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'december',
      title: headerMap[12],
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'january',
      title: headerMap[1],
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'february',
      title: headerMap[2],
      minWidth: 120,
      type: 'number1',
      editable: true,
      format: valueFormat,
    },
    {
      field: 'march',
      title: headerMap[3],
      minWidth: 120,
      type: 'number1',
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

  // ===================== Fetch Grade Dropdown (for PE — same as NormalOpNorms fetchGradeDropdowns) =====================

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

  // ===================== Fetch Norm Transactions (allRedCell for red highlight) =====================

  const fetchNormTransactions = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      let res = await SteadyStateConsumptionApiService.getNormTransactions(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (res?.code === 200) {
        const normalized = (res?.data || []).map((obj) => ({
          ...obj,
          normParameterFKId: obj.normParameterFKId?.toUpperCase(),
        }))
        setAllRedCell(normalized)
      }
    } catch (error) {
      console.error('Error fetching norm transactions:', error)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak])

  // ===================== Fetch Main Data (requires gradeId for PE — same as NormalOpNorms fetchData) =====================

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

  // ===================== Initial load & Grade change (same as NormalOpNorms fetchAllData) =====================

  useEffect(() => {
    if (!PLANT_ID || !AOP_YEAR) return
    setRows([])
    setOriginalRows([])
    setAllRedCell([])
    setGrades([])
    setSelectedGradeId(null)
    Promise.all([fetchGrades(), fetchNormTransactions()])
  }, [PLANT_ID, AOP_YEAR])

  // Re-fetch data when selectedGradeId changes
  useEffect(() => {
    if (selectedGradeId) {
      fetchData(selectedGradeId)
      fetchNormTransactions()
    }
  }, [selectedGradeId, fetchData, fetchNormTransactions])

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
        await SteadyStateConsumptionApiService.saveSteadyStateConsumptionByGrade(
          PLANT_ID,
          payload,
          keycloak,
          selectedGradeId,
          AOP_YEAR,
        )

      let isWeightedAverageError = false
      let errorMsg = ''

      if (response instanceof Response) {
        // Specific error status codes check (e.g. response.status === 400 or other specific code provided later)
        isWeightedAverageError = true
        try {
          const errData = await response.json()
          errorMsg = errData.message || errData.error || errorMsg
        } catch (e) {
          errorMsg = `HTTP ${response.status}: ${response.statusText}`
        }
      } else if (response && (response.code === 400 || response.status === 400)) {
        isWeightedAverageError = true
        errorMsg = response.message || 'Validation failed.'
      } else if (Array.isArray(response) && response.length > 0) {
        const failedRecs = response.filter((r) => r.saveStatus === 'Failed')
        if (failedRecs.length > 0) {
          isWeightedAverageError = true
          errorMsg = failedRecs
            .map((r) => `${r.productName || 'Record'}: ${r.errDescription}`)
            .join('\n')
        }
      }

      if (isWeightedAverageError) {
        showErrorModal(
          'Weighted Average Error',
          errorMsg || 'Weighted average does not match.',
        )
      } else if (response && !(response instanceof Response)) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'Saved Successfully!', severity: 'success' })
        setModifiedCells({})
        await fetchData(selectedGradeId)
        await fetchNormTransactions()
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
    fetchNormTransactions,
  ])


  // ===================== Calculate (PE uses site + vertical — same as NormalOpNorms handleCalculateNormalOperationNormsPe) =====================

  const handleCalculate = async () => {
    setLoading(true)
    setSnackbarOpen(true)
    setSnackbarData({ message: 'Calculating...', severity: 'info' })
    try {
      // Same as NormalOperationNormsApiService.handleCalculateNormalOperationNormsPe
      const data =
        await SteadyStateConsumptionApiService.calculateSteadyStateConsumptionPE(
          PLANT_ID,
          SITE_ID,
          VERTICAL_ID,
          AOP_YEAR,
          keycloak,
        )

      if (data == 0 || data) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        await fetchGrades()
        await fetchData(selectedGradeId)
        await fetchNormTransactions()
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

      if (response instanceof Response) {
        // Specific error status codes check
        isWeightedAverageError = true
        try {
          const errData = await response.json()
          errorMsg = errData.message || errData.error || errorMsg
        } catch (e) {
          errorMsg = `HTTP ${response.status}: ${response.statusText}`
        }
      } else if (response && response.code === 400 && !response.data) {
        isWeightedAverageError = true
        errorMsg = response.message || 'Import validation failed.'
      }

      if (isWeightedAverageError) {
        showErrorModal(
          'Weighted Average Error',
          errorMsg || 'Weighted average does not match.',
        )
      } else if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Uploaded Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        await fetchData(selectedGradeId)
        await fetchNormTransactions()
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
        await fetchNormTransactions()
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
    showCalculate: false,
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
        allRedCell={allRedCell}
        groupBy={['normParameterTypeDisplayName']}
        customHeight={70}
        paginationConfig={{
          threshold: 100,
          buttonCount: 5,
          pageSizes: [10, 20, 50, 100],
          defaultPageSize: 100,
        }}
      />

      {/* Premium Centered Error Modal */}
      <PremiumErrorDialog
        open={errorModalOpen}
        onClose={() => setErrorModalOpen(false)}
        disableScrollLock
      >
        <DialogTitle
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            pb: 1,
            borderBottom: '1px solid #f3f4f6',
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <ErrorOutlineIcon sx={{ color: '#ef4444', fontSize: '1.8rem' }} />
            <Typography
              variant="h6"
              sx={{
                fontWeight: 700,
                color: '#1f2937',
                fontSize: '1.1rem',
              }}
            >
              {errorModalTitle}
            </Typography>
          </Box>
          <IconButton
            size="small"
            onClick={() => setErrorModalOpen(false)}
            sx={{ color: '#9ca3af' }}
          >
            <CloseIcon fontSize="small" />
          </IconButton>
        </DialogTitle>

        <DialogContent sx={{ mt: 2, pb: 1 }}>
          <Typography
            sx={{
              fontSize: '0.875rem',
              color: '#4b5563',
              lineHeight: 1.6,
              mb: 2.5,
            }}
          >
            The system encountered a validation issue with the steady-state consumption values. Please ensure your weighted average matches the required plant-wise targets.
          </Typography>

          <Box
            sx={{
              p: 2,
              borderRadius: '10px',
              bgcolor: '#fef2f2',
              border: '1px solid #fee2e2',
            }}
          >
            <Typography
              sx={{
                fontSize: '0.75rem',
                color: '#991b1b',
                fontWeight: 600,
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
                mb: 0.5,
              }}
            >
              Error Details
            </Typography>
            <Typography
              sx={{
                fontSize: '0.825rem',
                color: '#7f1d1d',
                fontFamily: 'monospace',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                fontWeight: 500,
              }}
            >
              {errorModalMessage}
            </Typography>
          </Box>
        </DialogContent>

        <DialogActions sx={{ p: 2, pt: 1 }}>
          <Button
            onClick={() => setErrorModalOpen(false)}
            variant="contained"
            size="medium"
            sx={{
              bgcolor: '#ef4444',
              color: '#ffffff',
              borderRadius: '8px',
              textTransform: 'none',
              fontWeight: 600,
              px: 3,
              '&:hover': {
                bgcolor: '#dc2626',
              },
            }}
          >
            Close
          </Button>
        </DialogActions>
      </PremiumErrorDialog>
    </Box>
  )
}

export default GradeWiseSteadyStateConsumption