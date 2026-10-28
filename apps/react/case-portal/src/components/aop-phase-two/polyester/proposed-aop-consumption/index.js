import React, { useCallback, useEffect, useState } from 'react'
import { Box } from '@mui/material'
import { useDispatch, useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import { setIsBlocked } from 'store/reducers/dataGridStore'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import { validateFields } from 'utils/validationUtils'
import { ProposedAopConsumptionApiService } from '../../services/polyester/proposedAopConsumptionApiService'
import AdvanceKendoTable from '../../common/AdvanceKendoTable'

const ProposedAopConsumption = () => {
  const dispatch = useDispatch()
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    oldYear,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
    isReleased,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const PLANT_NAME = plantObject?.name
  const SITE_NAME = siteObject?.name
  const VERTICAL_ID = verticalObject?.id
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const AOP_YEAR = year?.selectedYear
  const SCREEN_NAME = screenTitle?.title
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased

  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const valueFormat = ValueFormatterConsumption()
  const headerMap = generateHeaderNames(AOP_YEAR)

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [grades, setGrades] = useState([])
  const [gradeId, setGradeId] = useState(null)
  const [calculationObject, setCalculationObject] = useState([])

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const monthsConfig = [
    { name: 'April', key: 4, fieldSuffix: 'April' },
    { name: 'May', key: 5, fieldSuffix: 'May' },
    { name: 'June', key: 6, fieldSuffix: 'June' },
    { name: 'July', key: 7, fieldSuffix: 'July' },
    { name: 'August', key: 8, fieldSuffix: 'August' },
    { name: 'September', key: 9, fieldSuffix: 'September' },
    { name: 'October', key: 10, fieldSuffix: 'October' },
    { name: 'November', key: 11, fieldSuffix: 'November' },
    { name: 'December', key: 12, fieldSuffix: 'December' },
    { name: 'January', key: 1, fieldSuffix: 'January' },
    { name: 'February', key: 2, fieldSuffix: 'February' },
    { name: 'March', key: 3, fieldSuffix: 'March' },
  ]

  const columns = [
    {
      field: 'normParameterDisplayName',
      title: 'Particulars',
      editable: false,
      isDisabled: true,
      minWidth: 160,
    },
    {
      field: 'UOM',
      title: 'UOM',
      editable: false,
      isDisabled: true,
      minWidth: 80,
    },
    ...monthsConfig.map((m) => ({
      title: headerMap[m.key] || m.name,
      children: [
        {
          field: `prevYearBudget${m.fieldSuffix}`,
          title: 'LastFY',
          editable: false,
          isDisabled: true,
          type: 'number',
          format: valueFormat,
          minWidth: 110,
        },
        {
          field: `currYearBudget${m.fieldSuffix}`,
          title: 'SysGen',
          editable: false,
          isDisabled: true,
          type: 'number',
          format: valueFormat,
          minWidth: 110,
        },
        {
          field: `currYearProposed${m.fieldSuffix}`,
          title: 'Proposed',
          editable: true,
          type: 'numberNonGrey',
          format: valueFormat,
          minWidth: 110,
        },
      ],
    })),
    {
      field: 'remarks',
      title: 'Remark',
      type: 'textarea',
      editable: true,
      minWidth: 160,
    },
  ]

  const fetchGradeDropdowns = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      const response = await ProposedAopConsumptionApiService.getGrades(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.code === 200 && Array.isArray(response?.data)) {
        setGrades(response.data)
        if (response.data.length > 0) {
          const firstGrade = response.data[0]
          const firstId =
            firstGrade?.id ??
            firstGrade?.gradeId ??
            firstGrade?.gradeFkId ??
            null
          setGradeId(firstId)
        } else {
          setGradeId(null)
        }
      } else {
        setGrades([])
        setGradeId(null)
      }
    } catch (error) {
      setGrades([])
      setGradeId(null)
      console.error('Error fetching grades dropdown:', error)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  const fetchGradeDropdownsAfterCalc = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      setGrades([])
      const response = await ProposedAopConsumptionApiService.getGrades(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.code === 200 && Array.isArray(response?.data)) {
        setGrades(response.data)
        if (response.data.length === 0) {
          setGradeId(null)
          return
        }
        const firstGrade = response.data[0]
        const firstId =
          firstGrade?.id ?? firstGrade?.gradeId ?? firstGrade?.gradeFkId ?? null
        setGradeId(firstId)
      } else {
        setGrades([])
        setGradeId(null)
      }
    } catch (error) {
      setGrades([])
      setGradeId(null)
      console.error('Error fetching grades after calculation:', error)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  const fetchData = useCallback(
    async (selectedGrade) => {
      if (!PLANT_ID || !AOP_YEAR) return
      if (!selectedGrade) {
        setRows([])
        return
      }
      setLoading(true)
      try {
        const response =
          await ProposedAopConsumptionApiService.getProposedAopConsumption(
            keycloak,
            selectedGrade,
            PLANT_ID,
            AOP_YEAR,
          )
        if (response?.code === 200) {
          setCalculationObject(response?.data?.aopCalculation)
          const formattedData = response?.data?.aopProposedNormsDTOList?.map(
            (item, index) => ({
              ...item,
              idFromApi: item.id,
              originalRemark: item.remarks?.trim() || null,
              id: index,
              Particulars: item.normParameterTypeDisplayName || 'Type',
              isEditable: true,
            }),
          )
          setRows(formattedData || [])
        } else {
          setRows([])
        }
      } catch (error) {
        console.error('Error fetching proposed norms data:', error)
        setRows([])
      } finally {
        setLoading(false)
      }
    },
    [keycloak, PLANT_ID, AOP_YEAR],
  )

  // Initial load
  useEffect(() => {
    fetchGradeDropdowns()
  }, [fetchGradeDropdowns])

  // Fetch data on grade change
  useEffect(() => {
    if (gradeId) {
      fetchData(gradeId)
    } else {
      setRows([])
    }
  }, [gradeId, fetchData])

  const saveChanges = useCallback(async () => {
    try {
      const data = Object.values(modifiedCells)
      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        return
      }
      const requiredFields = ['remarks']
      const validationMessage = validateFields(data, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationMessage,
          severity: 'error',
        })
        return
      }

      const payload = data.map((row) => ({
        id: row?.idFromApi ?? null,
        prevYearBudgetApril: row?.prevYearBudgetApril ?? null,
        prevYearBudgetMay: row?.prevYearBudgetMay ?? null,
        prevYearBudgetJune: row?.prevYearBudgetJune ?? null,
        prevYearBudgetJuly: row?.prevYearBudgetJuly ?? null,
        prevYearBudgetAugust: row?.prevYearBudgetAugust ?? null,
        prevYearBudgetSeptember: row?.prevYearBudgetSeptember ?? null,
        prevYearBudgetOctober: row?.prevYearBudgetOctober ?? null,
        prevYearBudgetNovember: row?.prevYearBudgetNovember ?? null,
        prevYearBudgetDecember: row?.prevYearBudgetDecember ?? null,
        prevYearBudgetJanuary: row?.prevYearBudgetJanuary ?? null,
        prevYearBudgetFebruary: row?.prevYearBudgetFebruary ?? null,
        prevYearBudgetMarch: row?.prevYearBudgetMarch ?? null,

        currYearBudgetApril: row?.currYearBudgetApril ?? null,
        currYearBudgetMay: row?.currYearBudgetMay ?? null,
        currYearBudgetJune: row?.currYearBudgetJune ?? null,
        currYearBudgetJuly: row?.currYearBudgetJuly ?? null,
        currYearBudgetAugust: row?.currYearBudgetAugust ?? null,
        currYearBudgetSeptember: row?.currYearBudgetSeptember ?? null,
        currYearBudgetOctober: row?.currYearBudgetOctober ?? null,
        currYearBudgetNovember: row?.currYearBudgetNovember ?? null,
        currYearBudgetDecember: row?.currYearBudgetDecember ?? null,
        currYearBudgetJanuary: row?.currYearBudgetJanuary ?? null,
        currYearBudgetFebruary: row?.currYearBudgetFebruary ?? null,
        currYearBudgetMarch: row?.currYearBudgetMarch ?? null,

        currYearProposedApril: row?.currYearProposedApril ?? null,
        currYearProposedMay: row?.currYearProposedMay ?? null,
        currYearProposedJune: row?.currYearProposedJune ?? null,
        currYearProposedJuly: row?.currYearProposedJuly ?? null,
        currYearProposedAugust: row?.currYearProposedAugust ?? null,
        currYearProposedSeptember: row?.currYearProposedSeptember ?? null,
        currYearProposedOctober: row?.currYearProposedOctober ?? null,
        currYearProposedNovember: row?.currYearProposedNovember ?? null,
        currYearProposedDecember: row?.currYearProposedDecember ?? null,
        currYearProposedJanuary: row?.currYearProposedJanuary ?? null,
        currYearProposedFebruary: row?.currYearProposedFebruary ?? null,
        currYearProposedMarch: row?.currYearProposedMarch ?? null,

        remarks: row?.remarks ?? null,
      }))

      setLoading(true)
      const res =
        await ProposedAopConsumptionApiService.saveProposedAopConsumption(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          payload,
        )

      if (res?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        dispatch(setIsBlocked(false))
        fetchData(gradeId)
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Save Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error saving proposed norms data:', error)
    } finally {
      setLoading(false)
    }
  }, [
    modifiedCells,
    keycloak,
    PLANT_ID,
    AOP_YEAR,
    gradeId,
    fetchData,
    dispatch,
  ])

  const handleCalculate = async () => {
    setLoading(true)
    try {
      const data =
        await ProposedAopConsumptionApiService.calculateProposedAopConsumption(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      if (data || data === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        fetchGradeDropdownsAfterCalc()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Refresh Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: error.message || 'An error occurred',
        severity: 'error',
      })
      console.error('Error calculating proposed norms:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleExcelUpload = async (rawFile) => {
    setLoading(true)
    try {
      const response =
        await ProposedAopConsumptionApiService.importProposedAopConsumption(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          rawFile,
        )
      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Uploaded Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData(gradeId)
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
        link.setAttribute(
          'download',
          'Error File - Proposed Consumption Norms.xlsx',
        )
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchData(gradeId)
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error uploading proposed norms excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
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
      severity: 'success',
    })
    try {
      const EXCEL_EXPORT_TITLE = `${VERTICAL_ID}_${SITE_NAME}_${PLANT_NAME}`
      await ProposedAopConsumptionApiService.exportProposedAopConsumption(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_EXPORT_TITLE,
        SCREEN_NAME,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel downloaded successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting proposed norms excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
    }
  }

  const handleRemarkCellClick = useCallback(
    (row) => {
      if (READ_ONLY) return
      setCurrentRemark(row.remarks || '')
      setCurrentRowId(row.id)
      setRemarkDialogOpen(true)
    },
    [READ_ONLY],
  )

  const permissions = {
    showAction: false,
    addButton: false,
    deleteButton: false,
    editButton: false,
    showUnit: false,
    saveBtn: true,
    showCalculate: true,
    calculateDisabled: !(
      calculationObject && Object.keys(calculationObject).length > 0
    ),
    allAction: true,
    showDropdown: true,
    showExport: true,
    showImport: true,
    ExcelName: `${VERTICAL_ID}_${SITE_NAME}_${PLANT_NAME}_${SCREEN_NAME}`,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: `${SCREEN_NAME}`,
  }

  const dropdownConfig = {
    options: grades,
    label: 'Grade',
    placeholder: 'Select Grade',
    valueKey: 'gradeId',
    labelKey: 'displayName',
  }

  return (
    <Box>
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        title={SCREEN_NAME}
        loading={loading}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        permissions={permissions}
        saveChanges={saveChanges}
        handleCalculate={handleCalculate}
        handleRemarkCellClick={handleRemarkCellClick}
        handleExport={handleExport}
        handleExcelUpload={handleExcelUpload}
        groupBy='Particulars'
        dropdownConfig={dropdownConfig}
        selectedDropdownValue={gradeId || ''}
        setSelectedDropdownValue={setGradeId}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        snackbarData={snackbarData}
        setSnackbarData={setSnackbarData}
      />
    </Box>
  )
}

export default ProposedAopConsumption
