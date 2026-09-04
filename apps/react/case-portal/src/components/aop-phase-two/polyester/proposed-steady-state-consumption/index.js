import React, { useEffect, useState } from 'react'
import { useSession } from 'SessionStoreContext'
import { Box } from '@mui/material'
import { ConsumptionNormsApiService } from 'services/consumption-norms-api-service'
import { getRoleName } from 'services/role-service'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import { downloadBase64Excel } from 'components/aop-phase-two/common/utilities/downloadBase64Excel'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import { useSelector } from 'react-redux'
import { ProposedAopApiService } from 'components/aop-phase-two/services/polyester/proposed-aop-api-service'
import { customValueFormatterPhaseTwo } from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'

const ProposedSteadyStateConsumption = () => {
  const [modifiedCells, setModifiedCells] = React.useState({})
  const [calculationObject, setCalculationObject] = useState([])
  const keycloak = useSession()

  const [open1, setOpen1] = useState(false)
  const defaultCustomHeight = { mainBox: '55vh', otherBox: '112%' }

  const dataGridStore = useSelector((state) => state.dataGridStore)

  const {
    verticalChange,
    yearChanged,
    oldYear,
    plantID,
    plantObject,
    siteObject,
    verticalObject,
    year,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const SCREEN_NAME = 'Proposed Steady State Consumption'

  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [gradeId, setGradeId] = useState(null)
  const [gradeName, setGradeName] = useState(null)
  const [grades, setGrades] = useState([])
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const valueFormat = customValueFormatterPhaseTwo(5)

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveEditedData = async (newRows) => {
    setLoading(true)
    try {
      const payload = newRows.map((row) => ({
        id: row?.idFromApi ?? null,
        normParameterId: row?.normParameterId ?? null,
        normParameterTypeId: row?.normParameterTypeId ?? null,
        normParameterTypeDisplayName: row?.normParameterTypeDisplayName ?? null,
        productName: row?.productName ?? null,
        uom: row?.uom ?? null,
        lastFY: row?.lastFY ?? null,
        sysGrn: row?.sysGrn ?? null,
        proposed: row?.proposed === '' ? null : (row?.proposed ?? null),
        remarks: row?.remarks ?? null,
        plantId: row?.plantId ?? null,
        aopYear: row?.aopYear ?? null,
        gradeId: row?.gradeId ?? null,
      }))

      const response = await ProposedAopApiService.saveProposedAOP(
        keycloak,
        payload,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Save Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error saving data!', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error saving data!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const saveChanges = React.useCallback(async () => {
    const editedData = Object.values(modifiedCells)
    if (editedData.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No changes to save!',
        severity: 'info',
      })
      return
    }

    const fieldsToCheck = ['proposed']
    const validationMessage = validateRowDataWithRemarks(
      editedData,
      originalRows,
      fieldsToCheck,
      'productName'
    )
    if (validationMessage) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: validationMessage,
        severity: 'error',
      })
      return
    }

    saveEditedData(editedData)
  }, [modifiedCells, originalRows])

  const fetchGradeDropdowns = async () => {
    try {
      const response =
        await ConsumptionNormsApiService.getProposedAOPNormsGrades(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )

      if (response?.code == 200) {
        const normalized = (response?.data || []).map((grade) => ({
          ...grade,
          displayName: grade.displayName || grade.DisplayName || grade.name || grade.Name || '',
          name: grade.name || grade.Name || '',
        }))
        setGrades(normalized)
        if (response?.data?.length > 0) {
          setGradeId(response?.data[0]?.gradeId)
        }
      }

      fetchData(response?.data[0]?.gradeId)
    } catch (error) {
      setGrades([])
      console.error('Error fetching data:', error)
    }
  }


  const fetchGradeDropdownsAfterCalc = async () => {
    try {
      setGrades([])
      const response =
        await ConsumptionNormsApiService.getProposedAOPNormsGrades(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )

      if (response?.code == 200) {
        const normalized = (response?.data || []).map((grade) => ({
          ...grade,
          displayName: grade.displayName || grade.DisplayName || grade.name || grade.Name || '',
          name: grade.name || grade.Name || '',
        }))
        setGrades(normalized)
      }

      if (response?.data?.length === 0) {
        await fetchData()
        return
      }

      const firstGrade = response?.data[0]
      const firstId =
        firstGrade?.id ?? firstGrade?.gradeId ?? firstGrade?.gradeFkId ?? null

      setGradeId(firstId)

      fetchData(firstId)
    } catch (error) {
      setGrades([])
      console.error('Error fetching Business Demand data:', error)
    }
  }


  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      setRows([])
      const response = await ProposedAopApiService.getProposedAOP(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code !== 200) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Error fetching data.',
          severity: 'error',
        })
        return
      }

      setCalculationObject(response?.data?.aopCalculation || [])

      const formattedData = (response?.data?.proposedAOP || []).map(
        (item, index) => {
          return {
            ...item,
            idFromApi: item.id,
            originalRemark: item.remarks?.trim() || null,
            id: index,
            Particulars: item.normParameterTypeDisplayName || 'Type',
            UOM: item.uom,
            isEditable: item.isEditable,
          }
        },
      )

      setRows(formattedData)
      setOriginalRows(formattedData)
    } catch (error) {
      console.error('Error fetching data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error fetching data.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak])

  const productionColumns = [
    {
      field: 'productName',
      title: 'Particulars',
      editable: false,
      fixedWidth: 250,
      locked: true,
    },
    {
      field: 'SAPCode',
      title: 'SAP Code',
      editable: false,
      fixedWidth: 100,
    },
    {
      field: 'UOM',
      title: 'UOM',
      editable: false,
      fixedWidth: 100,
    },
    {
      field: 'lastFY',
      title: 'Last FY',
      editable: false,
      type: 'number',
      fixedWidth: 150,
      format: valueFormat
    },
    {
      field: 'sysGrn',
      title: 'Sys Gen',
      editable: false,
      type: 'number',
      fixedWidth: 150,
      format: valueFormat
    },
    {
      field: 'proposed',
      title: 'Proposed',
      editable: true,
      type: 'numberNonGrey',
      fixedWidth: 150,
      format: valueFormat
    },
    {
      field: 'remarks',
      title: 'Remarks',
      editable: true,
      fixedWidth: 200,
    },
  ]

  const handleCalculate = async () => {
    setLoading(true)
    try {
      const response = await ProposedAopApiService.calculateProposedAOP(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Data Refresh Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: error.message || 'An error occurred during calculation',
        severity: 'error',
      })
      console.error('Calculation Error!', error)
    } finally {
      setLoading(false)
    }
  }

  const EXCEL_EXPORT_TITLE = generateExcelName(
    dataGridStore,
    'Proposed_Steady_State_Consumption'
  )

  const handleExport = async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })

    try {
      await ProposedAopApiService.exportProposedAOP(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_EXPORT_TITLE,
      )
    } catch (error) {
      console.error('Error downloading Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
    } finally {
      setSnackbarOpen(true)
    }
  }

  const handleExcelUpload = async (rawFile) => {
    if (!rawFile) return
    setLoading(true)
    try {
      const response = await ProposedAopApiService.importProposedAOP(
        rawFile,
        keycloak,
      )
      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Upload Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData()
      } else if (response?.code === 400 && response?.data) {
        downloadBase64Excel(response.data, 'Error File - Proposed AOP.xlsx')

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Upload Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error uploading excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
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
    ExcelName: EXCEL_EXPORT_TITLE,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: `${SCREEN_NAME}`,
    showDropdown: false,
    remarksEditable: true,
    marginBottom: true,
  }

  return (
    <div>
      <LoaderBackdrop open={!!loading} />
      <Box>
        <AdvanceKendoTable
          title={SCREEN_NAME}
          loading={loading}
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          columns={productionColumns}
          rows={rows}
          setRows={setRows}
          saveChanges={saveChanges}
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          handleCalculate={handleCalculate}
          handleRemarkCellClick={handleRemarkCellClick}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
          setCurrentRowId={setCurrentRowId}
          permissions={permissions}
          groupBy='Particulars'
          handleExport={handleExport}
          handleExcelUpload={handleExcelUpload}
          paginationConfig={{
            threshold: 100,
            buttonCount: 5,
            pageSizes: [100, 200, 300],
            defaultPageSize: 100,
          }}
        />
      </Box>
    </div>
  )
}

export default ProposedSteadyStateConsumption
