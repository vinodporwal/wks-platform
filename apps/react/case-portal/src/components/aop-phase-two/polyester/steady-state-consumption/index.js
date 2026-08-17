import { useState, useEffect, useCallback } from 'react'
import { Box } from '@mui/material'
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

const SteadyStateConsumption = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(
    dataGridStore,
    'Steady_State_Consumption',
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

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    setLoading(true)
    setRows([])
    try {
      let response =
        await SteadyStateConsumptionApiService.getSteadyStateConsumptionByGrade(
          keycloak,
          null,
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
  }, [PLANT_ID, AOP_YEAR, keycloak])

  useEffect(() => {
    if (!PLANT_ID || !AOP_YEAR) return
    setRows([])
    setOriginalRows([])
    setAllRedCell([])
    setGrades([])
    setSelectedGradeId(null)
  }, [PLANT_ID, AOP_YEAR])

  useEffect(() => {
    fetchData()
  }, [fetchData])

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
        gradeId: null,
      }))

      const response =
        await SteadyStateConsumptionApiService.saveSteadyStateConsumptionByGrade(
          PLANT_ID,
          payload,
          keycloak,
          null,
          AOP_YEAR,
        )

      if (response) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'Saved Successfully!', severity: 'success' })
        setModifiedCells({})
        await fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'Norms not saved!', severity: 'error' })
      }
    } catch (error) {
      console.error('Error saving steady state consumption:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Save failed, please try again!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, PLANT_ID, AOP_YEAR, keycloak, fetchData])

  // ===================== Calculate (PE uses site + vertical — same as NormalOpNorms handleCalculateNormalOperationNormsPe) =====================

  const handleCalculate = async () => {
    setLoading(true)
    setSnackbarOpen(true)
    setSnackbarData({ message: 'Calculating...', severity: 'info' })
    try {
      const data =
        await SteadyStateConsumptionApiService.calculateSteadyStateConsumption(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )

      if (data == 0 || data) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        await fetchData()
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
      await SteadyStateConsumptionApiService.exportSteadyStateConsumption(
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
          null,
        )
      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Uploaded Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        await fetchData()
      } else if (response?.code === 400 && response?.data) {
        // Partial save — download error file
        downloadBase64Excel(response.data, 'Error File Steady state Norms.xlsx')
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        await fetchData()
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
    titleName: 'Steady State Consumption (Norm/Quantity)',
    // Grade dropdown (showG → showDropdown: true for PE)
    showDropdown: false,
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
    </Box>
  )
}

export default SteadyStateConsumption
