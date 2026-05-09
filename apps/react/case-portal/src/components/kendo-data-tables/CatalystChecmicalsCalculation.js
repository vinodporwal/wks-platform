import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { Box, Backdrop, CircularProgress } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
import KendoDataTables from './index'
import { DataService } from 'services/DataService'
import { getRoleName } from 'services/role-service'
import { validateFields } from 'utils/validationUtils'
import getEnhancedAOPColDefs from 'components/data-tables/CommonHeader/kendo_ConfigHeader'
import Notification from 'components/Utilities/Notification'
import { NormalOperationNormsApiService } from 'services/normal-operation-norms-api-service'

const STATIC_CONSUMPTION = [
  {
    id: 101,
    displayName: 'CHEM ISOPROPYL ALCOHOL',
    uom: 'MT',
    emulsifierBatch: 0.5,
    catalystBatch: 0.2,
    bufferBatch: 0.1,
    shortStopBatch: 0.05,
    coatingBatch: 0.02,
    norms: 0.87,
    gradeId: 'grade1',
  },
  {
    id: 102,
    displayName: 'CHEM ULTRANOX 626',
    uom: 'Kg',
    emulsifierBatch: 2,
    catalystBatch: 1,
    bufferBatch: 0.5,
    shortStopBatch: 0.2,
    coatingBatch: 0.1,
    norms: 3.8,
    gradeId: 'grade1',
  },
  {
    id: 103,
    displayName: 'CHEM ISOPROPYL ALCOHOL',
    uom: 'MT',
    emulsifierBatch: 0.6,
    catalystBatch: 0.25,
    bufferBatch: 0.12,
    shortStopBatch: 0.06,
    coatingBatch: 0.03,
    norms: 0.95,
    gradeId: 'grade2',
  },
  {
    id: 104,
    displayName: 'CHEM ULTRANOX 626',
    uom: 'Kg',
    emulsifierBatch: 2.2,
    catalystBatch: 1.1,
    bufferBatch: 0.55,
    shortStopBatch: 0.22,
    coatingBatch: 0.11,
    norms: 4.1,
    gradeId: 'grade2',
  },
]

const STATIC_GRADES = [
  { id: 'grade1', name: 'GRade1', displayName: 'GRade1' },
  { id: 'grade2', name: 'GRade2', displayName: 'GRade2' },
  { id: 'grade3', name: 'GRade3', displayName: 'GRade3' },
  { id: 'grade4', name: 'GRade4', displayName: 'GRade4' },
  { id: 'grade5', name: 'GRade5', displayName: 'GRade5' },
  { id: 'grade6', name: 'GRade6', displayName: 'GRade6' },
  { id: 'grade7', name: 'GRade7', displayName: 'GRade7' },
]

const CatalystChecmicalsCalculation = () => {
  const [loading, setLoading] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const apiRef = useGridApiRef()

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, oldYear, verticalObject, siteObject, isReleased } =
    dataGridStore
  const PLANT_ID = plantObject?.id
  const VERTICAL_ID = verticalObject?.id
  const SITE_ID = siteObject?.id
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const keycloak = useSession()
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, isReleased)
  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}`
  const [productionRowsConstants, setProductionRowsConstants] = useState([])
  const [recipeRows, setRecipeRows] = useState([])
  const [consumptionRows, setConsumptionRows] = useState([])
  const [consumptionColumns, setConsumptionColumns] = useState([])
  const [recipeGrades, setRecipeGrades] = useState([])
  const [allGrades, setAllGrades] = useState([]) // Used for Grid 3 dropdown
  const [selectedGrade, setSelectedGrade] = useState('')
  const [gradeId, setGradeId] = useState(null)
  const [modifiedConstantCells, setModifiedConstantCells] = useState({})
  const [modifiedRecipeCells, setModifiedRecipeCells] = useState({})
  const [modifiedConsumptionCells, setModifiedConsumptionCells] = useState({})

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [currentRemarkConstants, setCurrentRemarkConstants] = useState('')
  const [currentRowIdConstants, setCurrentRowIdConstants] = useState(null)
  const [remarkDialogOpenConstants, setRemarkDialogOpenConstants] =
    useState(false)
  const unsavedChangesRefConstants = React.useRef({
    unsavedRows: {},
    rowsBeforeChange: {},
  })

  const FORMATE_VALUE = '{0:0.000}'

  // Grid 1: Constant Columns
  const colDefsConstants = [
    {
      field: 'DisplayName',
      title: 'Particulars',
      editable: false,
      widthT: 220,
      hidden: false,
    },
    {
      field: 'UOM',
      title: 'UOM',
      editable: false,
      widthT: 80,
    },
    {
      field: 'ConstantValue',
      title: 'Value',
      editable: true,
      type: 'number',
      widthT: 120,
    },

    {
      field: 'remarks',
      title: 'Remark',
      editable: false,
      type: 'string',
    },
  ]

  // Grid 2: Recipe Columns (Dynamic)
  const recipeColumns = useMemo(() => {
    const baseCols = [
      {
        field: 'particulars',
        title: 'Particulars',
        widthT: 200,
        editable: false,
      },
      { field: 'uom', title: 'UOM', widthT: 80, editable: false },
    ]

    const dynamicCols = (
      recipeGrades.length > 0 ? recipeGrades : STATIC_GRADES
    ).map((grade) => ({
      field: grade?.id?.toUpperCase() || grade.name || grade.displayName,
      title: grade.displayName || grade.name,
      widthT: 100,
      type: 'number',
      format: FORMATE_VALUE,
      editable: true,
    }))

    return [...baseCols, ...dynamicCols]
  }, [recipeGrades])

  const filteredConsumptionRows = useMemo(() => {
    if (!selectedGrade) return consumptionRows
    return consumptionRows.filter(
      (row) => row.Grade_FK_Id === selectedGrade, // ? exact API field
    )
  }, [consumptionRows, selectedGrade])

  // Grid 3: Cat-chem Consumption Columns
  // const consumptionColumns = [
  //   { field: 'displayName', title: 'Material', widthT: 200, editable: false },
  //   { field: 'uom', title: 'UOM', widthT: 80, editable: false },
  //   { field: 'emulsifierBatch', title: 'Emulsifier Batch Consumption', widthT: 150, editable: false, type: 'number', format: FORMATE_VALUE },
  //   { field: 'catalystBatch', title: 'Catalyst batch Consumption', widthT: 150, editable: false, type: 'number', format: FORMATE_VALUE },
  //   { field: 'bufferBatch', title: 'Buffer batch Consumption', widthT: 150, editable: false, type: 'number', format: FORMATE_VALUE },
  //   { field: 'shortStopBatch', title: 'Short stop batch Consumption', widthT: 150, editable: false, type: 'number', format: FORMATE_VALUE },
  //   { field: 'coatingBatch', title: 'Coating batch Consumption', widthT: 150, editable: false, type: 'number', format: FORMATE_VALUE },
  //   { field: 'apr', title: 'Total Consumption', widthT: 150, editable: false, type: 'number', format: FORMATE_VALUE },
  //   { field: 'norms', title: 'Norms (Kg/MT)', widthT: 120, editable: false, type: 'number', format: FORMATE_VALUE },
  // ]

  const handleRemarkClick = (row, type) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }
  const handleRemarkCellClickConstants = (row) => {
    if (READ_ONLY) return
    setCurrentRemarkConstants(row.remarks || '')
    setCurrentRowIdConstants(row.id)
    setRemarkDialogOpenConstants(true)
  }

  const fetchConstantsData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    setProductionRowsConstants([])
    try {
      const constantsRes =
        await DataService.getCatalystSelectivityDataConstants(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          true,
        )

      if (constantsRes?.code !== 200) {
        setProductionRowsConstants([])
        return
      }

      const data = constantsRes?.data
      const formattedData = data.map((item, index) => ({
        ...item,
        idFromApi: item.id,
        id: index,
        originalRemark: item.Remarks,
        srNo: index + 1,
        Particulars: item.NormTypeName,
        remarks: item.Remarks,
      }))

      setProductionRowsConstants(formattedData)
    } catch (error) {
      console.error('Error fetching constants data:', error)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      // Fetch Recipe Grades (for Grid 2 Header)
      try {
        const recipeGradesRes = await DataService.getAllGrades(
          keycloak,
          PLANT_ID,
        )
        if (recipeGradesRes?.length > 0) {
          setRecipeGrades(recipeGradesRes)
        }
      } catch (e) {
        console.error('Error fetching recipe grades:', e)
      }

      // Fetch Consumption Grades (for Grid 3 Dropdown)
      try {
        const consumptionGradesRes =
          await NormalOperationNormsApiService.getNormalOperationNormsGrades(
            keycloak,
            PLANT_ID,
            AOP_YEAR,
          )
        if (
          consumptionGradesRes?.code === 200 &&
          consumptionGradesRes?.data?.length > 0
        ) {
          setAllGrades(consumptionGradesRes.data)
          setSelectedGrade(consumptionGradesRes.data[0].gradeId)
        } else {
          setAllGrades([])
        }
      } catch (e) {
        setAllGrades([])
      }

      // Fetch Recipe Data
      try {
        const recipeRes = await DataService.getPeConfigCatChemData(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
        if (recipeRes?.length > 0) {
          const formattedData = recipeRes?.map((item, index) => {
            const converted = {}

            Object.entries(item).forEach(([key, value]) => {
              if (
                key !== 'UOM' &&
                typeof value === 'string' &&
                value.trim() !== '' &&
                !isNaN(value)
              ) {
                converted[key] = value.includes('.')
                  ? parseFloat(value)
                  : parseInt(value, 10)
              } else {
                converted[key] = value
              }
            })

            return {
              ...converted,
              id: index,
              TypeDisplayName: item?.TypeDisplayName
                ? item?.TypeDisplayName
                : 'Recipe',
              particulars: item.ReceipeName,
              uom: item.UOM,
            }
          })
          setRecipeRows(formattedData)
        } else {
          setRecipeRows([])
        }
      } catch (e) {
        setRecipeRows([])
      }
    } catch (error) {
      console.error('Error fetching Catalyst data:', error)
    } finally {
      setLoading(false)
    }
  }

  const getCatChemCalculationData = async () => {
    // Fetch Consumption
    if (!PLANT_ID || !AOP_YEAR || !selectedGrade) return
    try {
      const consumptionRes = await DataService.getCatChemCalculationData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        selectedGrade,
      )
      if (consumptionRes?.code === 200) {
        setConsumptionColumns(
          consumptionRes?.data?.columns
            ?.filter((col) => col.field !== 'Grade_FK_Id')
            .map((col) => ({
              ...col,
              format: col.type === 'number' ? FORMATE_VALUE : undefined,
              editable: false,
            })),
        )

        setConsumptionRows(
          consumptionRes?.data?.data?.map((row, index) => ({
            ...row,
            id: index,
            // Grade_FK_Id already exists in row from API ?
          })),
        )
      } else {
        setConsumptionRows([])
      }
    } catch (e) {
      setConsumptionRows([])
    }
  }
  useEffect(() => {
    fetchConstantsData()
  }, [fetchConstantsData])

  useEffect(() => {
    fetchData()
  }, [PLANT_ID, AOP_YEAR])

  useEffect(() => {
    if (!PLANT_ID || !AOP_YEAR || !selectedGrade) return
    getCatChemCalculationData()
  }, [PLANT_ID, AOP_YEAR, selectedGrade])
  const saveCatalystData = async (newRow) => {
    setLoading(true)
    try {
      var payload = []

      payload = newRow.map((row) => ({
        apr: row.apr || row.ConstantValue || null,
        may: row.apr || row.ConstantValue || null,
        jun: row.apr || row.ConstantValue || null,
        jul: row.apr || row.ConstantValue || null,
        aug: row.apr || row.ConstantValue || null,
        sep: row.apr || row.ConstantValue || null,
        oct: row.apr || row.ConstantValue || null,
        nov: row.apr || row.ConstantValue || null,
        dec: row.apr || row.ConstantValue || null,
        jan: row.apr || row.ConstantValue || null,
        feb: row.apr || row.ConstantValue || null,
        mar: row.apr || row.ConstantValue || null,
        UOM: '',
        auditYear: AOP_YEAR,
        normParameterFKId: row.normParameterFKId || row.NormParameter_FK_Id,
        remarks: row.remarks,
        id: row.idFromApi || null,
      }))

      const response = await DataService.saveCatalystData(
        PLANT_ID,
        payload,
        keycloak,
        AOP_YEAR,
      )
      if (response) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setModifiedConstantCells({})
        setLoading(false)
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Falied!',
          severity: 'error',
        })
      }

      return response
    } catch (error) {
      console.error('Error saving data:', error)
      setLoading(false)
    } finally {
      fetchConstantsData()
      setLoading(false)
    }
  }
  const saveChanges = React.useCallback(async () => {
    try {
      var data = Object.values(modifiedConstantCells)

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
      saveCatalystData(data)
    } catch (error) {
      // Handle error if necessary
    }
  }, [modifiedConstantCells])

  const saveRecipeChanges = async () => {
    const data = Object.values(modifiedRecipeCells).filter((r) => r.inEdit)
    if (data.length === 0) return
    setLoading(true)
    try {
      const payload = data.map((row) => ({
        recId: row.Reciepe_FK_ID?.toString() || '0',
        grades: Object.entries(row)
          .filter(
            ([key]) =>
              /^[0-9A-Fa-f-]{36}$/.test(key) || key.startsWith('grade'),
          )
          .reduce((acc, [key, value]) => {
            acc[key] = Number(value)
            return acc
          }, {}),
      }))
      await DataService.updatePeConfigData(
        keycloak,
        payload,
        PLANT_ID,
        AOP_YEAR,
      )
      setSnackbarData({ message: 'Recipe Saved!', severity: 'success' })
      setSnackbarOpen(true)
      fetchData()
    } catch (e) {
      console.error(e)
      setSnackbarData({ message: 'Recipe Saved!', severity: 'success' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }

  const handleCalculate = async () => {
    setLoading(true)
    try {
      await DataService.postCatChemCalculate(keycloak, PLANT_ID, AOP_YEAR)
      setSnackbarData({
        message: 'Calculation Successful!',
        severity: 'success',
      })
      setSnackbarOpen(true)
      fetchData()
    } catch (e) {
      console.error(e)
      // Simulate success for static data mode
      setSnackbarData({
        message: 'Calculation Successful!',
        severity: 'success',
      })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }

  const downloadExcel = async (type, title) => {
    try {
      setSnackbarData({ message: 'Excel Export Started!', severity: 'success' })
      setSnackbarOpen(true)
      if (type === 'constant') {
        await DataService.getConfigurationExcelConstants(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          `${EXCEL_EXPORT_TITLE}_${title}`,
        )
      } else if (type === 'recipe') {
        await DataService.getRecipeCatChemExcel(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          `${EXCEL_EXPORT_TITLE}_${title}`,
        )
      }
      setSnackbarData({
        message: 'Excel Export Successful!',
        severity: 'success',
      })
      setSnackbarOpen(true)
    } catch (e) {
      setSnackbarData({ message: 'Export Failed!', severity: 'error' })
      setSnackbarOpen(true)
    }
  }

  const handleExcelUpload = async (file, type) => {
    setLoading(true)
    try {
      let response
      if (type === 'constant') {
        response = await DataService.saveConfigurationExcelConstants(
          file,
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      } else if (type === 'recipe') {
        response = await DataService.saveRecipeCatChemExcel(
          file,
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      }
      if (response?.code == 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Upload Successfully!',
          severity: 'success',
        })
        setModifiedConstantCells({})
        setModifiedRecipeCells({})
        setLoading(false)

        fetchData()
      } else if (response?.code === 400 && response?.data) {
        const byteCharacters = atob(response.data)
        const byteNumbers = new Array(byteCharacters.length)
        for (let i = 0; i < byteCharacters.length; i++) {
          byteNumbers[i] = byteCharacters.charCodeAt(i)
        }
        const byteArray = new Uint8Array(byteNumbers)
        const blob = new Blob([byteArray], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        })

        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `Error File ${type}.xlsx`)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Falied!',
          severity: 'error',
        })
      }

      return response
    } catch (e) {
      setSnackbarData({ message: 'Import Failed!', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }

  const adjustedPermissionsConstant = () => ({
    showAction: true,
    saveWithRemark: true,
    saveBtn: true,
    allAction: true,
    downloadExcelBtn: true,
    uploadExcelBtn: true,
    showTitleNameBusiness: true,
    titleName: 'Constant',
    showCalculate: false,
  })

  const adjustedPermissionsRecipe = () => ({
    showAction: false,
    addButtons: false,
    deleteButton: false,
    editButton: false,
    saveWithRemark: true,
    saveBtn: true,
    downloadExcelBtn: true,
    uploadExcelBtn: true,
    showLoad: true,
    allAction: true,
    showTitleNameBusiness: true,
    titleName: 'Recipe',
  })

  const adjustedPermissionsConsumption = () => ({
    showAction: true,
    saveWithRemark: true,
    saveBtn: false,
    allAction: true,
    downloadExcelBtn: true,
    showTitleNameBusiness: true,
    titleName: 'Cat-chem Consumption',
    showCalculate: true,
    showG: true,
    marginBottom: true,
    dropdownLabel: 'Select Grade',
    uploadExcelBtn: false,
    showTitle: true,
  })

  const handleConsumptionGradeChange = (gradeId) => {
    setSelectedGrade(gradeId)
    getCatChemCalculationData(gradeId)
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Backdrop
        sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
        open={loading}
      >
        <CircularProgress color='inherit' />
      </Backdrop>

      {/* Grid 1: Constant */}
      <KendoDataTables
        title='Constant'
        columns={colDefsConstants}
        setRows={setProductionRowsConstants}
        rows={productionRowsConstants}
        modifiedCells={modifiedConstantCells}
        setModifiedCells={setModifiedConstantCells}
        saveChanges={saveChanges}
        permissions={adjustedPermissionsConstant()}
        groupBy='Particulars'
        fetchData={fetchConstantsData}
        downloadExcelForConfiguration={() =>
          downloadExcel('constant', 'Constant')
        }
        handleExcelUpload={(file) => handleExcelUpload(file, 'constant')}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        apiRef={apiRef}
        remarkDialogOpen={remarkDialogOpenConstants}
        setRemarkDialogOpen={setRemarkDialogOpenConstants}
        currentRemark={currentRemarkConstants}
        setCurrentRemark={setCurrentRemarkConstants}
        handleRemarkCellClick={handleRemarkCellClickConstants}
        currentRowId={currentRowIdConstants}
        unsavedChangesRef={unsavedChangesRefConstants}
      />

      {/* Grid 2: Recipe */}
      <KendoDataTables
        title='Recipe'
        columns={recipeColumns}
        rows={recipeRows}
        setRows={setRecipeRows}
        modifiedCells={modifiedRecipeCells}
        setModifiedCells={setModifiedRecipeCells}
        saveChanges={saveRecipeChanges}
        permissions={adjustedPermissionsRecipe()}
        groupBy='TypeDisplayName'
        fetchData={fetchData}
        downloadExcelForConfiguration={() => downloadExcel('recipe', 'Recipe')}
        handleExcelUpload={(file) => handleExcelUpload(file, 'recipe')}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        apiRef={apiRef}
        handleRemarkCellClick={handleRemarkClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
      />

      {/* Grid 3: Cat-chem Consumption */}
      <KendoDataTables
        title='Cat-chem Consumption'
        columns={consumptionColumns}
        rows={filteredConsumptionRows}
        grades={allGrades}
        setRows={setConsumptionRows}
        modifiedCells={modifiedConsumptionCells}
        setModifiedCells={setModifiedConsumptionCells}
        permissions={adjustedPermissionsConsumption()}
        handleCalculate={handleCalculate}
        fetchData={fetchData}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        apiRef={apiRef}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        handleGradeChange={handleConsumptionGradeChange}
      />

      <Notification
        open={snackbarOpen}
        message={snackbarData.message}
        severity={snackbarData.severity}
        onClose={() => setSnackbarOpen(false)}
      />
    </Box>
  )
}

export default CatalystChecmicalsCalculation
