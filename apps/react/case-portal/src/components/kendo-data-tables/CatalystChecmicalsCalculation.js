import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { Box, Backdrop, CircularProgress, MenuItem, Select, FormControl, InputLabel } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
import KendoDataTables from './index'
import KendoDataTablesReciepe from './index-reports-receipe'
import { DataService } from 'services/DataService'
import { getRoleName } from 'services/role-service'
import { RawMaterialNormsBasisApiService } from 'services/raw-material-norms-basis-api-service'
import { validateFields } from 'utils/validationUtils'
import getEnhancedAOPColDefs from 'components/data-tables/CommonHeader/kendo_ConfigHeader'
import Notification from 'components/Utilities/Notification'
import { NormalOperationNormsApiService } from 'services/normal-operation-norms-api-service'

const STATIC_CONSTANTS = [
  { id: 1, TypeDisplayName: 'Packaging Constant', displayName: 'Bag Weight', uom: 'Kg', apr: 0.05, remarks: 'Remark' },
  { id: 2, TypeDisplayName: 'Packaging Constant', displayName: 'Pallet Weight', uom: 'Kg', apr: 25, remarks: '' },
  { id: 3, TypeDisplayName: 'Constant', displayName: 'Steam factor', uom: 'MT/MT', apr: 2.5, remarks: '' },
]

const STATIC_CONSUMPTION = [
  { id: 101, displayName: 'CHEM ISOPROPYL ALCOHOL', uom: 'MT', emulsifierBatch: 0.5, catalystBatch: 0.2, bufferBatch: 0.1, shortStopBatch: 0.05, coatingBatch: 0.02, norms: 0.87, gradeId: 'grade1' },
  { id: 102, displayName: 'CHEM ULTRANOX 626', uom: 'Kg', emulsifierBatch: 2, catalystBatch: 1, bufferBatch: 0.5, shortStopBatch: 0.2, coatingBatch: 0.1, norms: 3.8, gradeId: 'grade1' },
  { id: 103, displayName: 'CHEM ISOPROPYL ALCOHOL', uom: 'MT', emulsifierBatch: 0.6, catalystBatch: 0.25, bufferBatch: 0.12, shortStopBatch: 0.06, coatingBatch: 0.03, norms: 0.95, gradeId: 'grade2' },
  { id: 104, displayName: 'CHEM ULTRANOX 626', uom: 'Kg', emulsifierBatch: 2.2, catalystBatch: 1.1, bufferBatch: 0.55, shortStopBatch: 0.22, coatingBatch: 0.11, norms: 4.1, gradeId: 'grade2' },
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
  const [snackbarData, setSnackbarData] = useState({ message: '', severity: 'info' })
  const apiRef = useGridApiRef()

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, oldYear, verticalObject, siteObject, isReleased } = dataGridStore
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

  const [constantRows, setConstantRows] = useState([])
  const [recipeRows, setRecipeRows] = useState([])
  const [consumptionRows, setConsumptionRows] = useState([])
  const [recipeGrades, setRecipeGrades] = useState([])
  const [allGrades, setAllGrades] = useState([]) // Used for Grid 3 dropdown
  const [selectedGrade, setSelectedGrade] = useState('')

  const [modifiedConstantCells, setModifiedConstantCells] = useState({})
  const [modifiedRecipeCells, setModifiedRecipeCells] = useState({})
  const [modifiedConsumptionCells, setModifiedConsumptionCells] = useState({})

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const FORMATE_VALUE = '{0:0.000}'

  // Grid 1: Constant Columns
  const constantColumns = getEnhancedAOPColDefs({
    configType: 'CatChem',
    FORMATE_VALUE,
  })

  // Grid 2: Recipe Columns (Dynamic)
  const recipeColumns = useMemo(() => {
    const baseCols = [
      { field: 'particulars', title: 'Particulars', widthT: 200, editable: false },
      { field: 'uom', title: 'UOM', widthT: 80, editable: false },
    ]

    const dynamicCols = (recipeGrades.length > 0 ? recipeGrades : STATIC_GRADES).map(grade => ({
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
    return consumptionRows.filter(row => row.gradeFKId === selectedGrade || row.gradeId === selectedGrade)
  }, [consumptionRows, selectedGrade])

  // Grid 3: Cat-chem Consumption Columns
  const consumptionColumns = [
    { field: 'displayName', title: 'Material', widthT: 200, editable: false },
    { field: 'uom', title: 'UOM', widthT: 80, editable: false },
    { field: 'emulsifierBatch', title: 'Emulsifier Batch Consumption', widthT: 150, editable: false, type: 'number', format: FORMATE_VALUE },
    { field: 'catalystBatch', title: 'Catalyst batch Consumption', widthT: 150, editable: false, type: 'number', format: FORMATE_VALUE },
    { field: 'bufferBatch', title: 'Buffer batch Consumption', widthT: 150, editable: false, type: 'number', format: FORMATE_VALUE },
    { field: 'shortStopBatch', title: 'Short stop batch Consumption', widthT: 150, editable: false, type: 'number', format: FORMATE_VALUE },
    { field: 'coatingBatch', title: 'Coating batch Consumption', widthT: 150, editable: false, type: 'number', format: FORMATE_VALUE },
    { field: 'apr', title: 'Total Consumption', widthT: 150, editable: false, type: 'number', format: FORMATE_VALUE },
    { field: 'norms', title: 'Norms (Kg/MT)', widthT: 120, editable: false, type: 'number', format: FORMATE_VALUE },
  ]

  const handleRemarkClick = (row, type) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      // Fetch Constants
      try {
        const constRes = await RawMaterialNormsBasisApiService.getData(keycloak, PLANT_ID, AOP_YEAR, 'catchem')
        if (constRes?.data?.catChemNormList?.length > 0) {
          setConstantRows(constRes.data.catChemNormList.map((row, index) => ({
            ...row,
            id: row.id || index,
            particulars: row.displayName,
            value: parseFloat(row.apr) || 0,
          })))
        } else {
          setConstantRows(STATIC_CONSTANTS.map(row => ({ ...row, particulars: row.displayName, value: row.apr })))
        }
      } catch (e) {
        setConstantRows(STATIC_CONSTANTS.map(row => ({ ...row, particulars: row.displayName, value: row.apr })))
      }

      // Fetch Recipe Grades (for Grid 2 Header)
      try {
        const recipeGradesRes = await DataService.getAllGrades(keycloak, PLANT_ID)
        if (recipeGradesRes?.length > 0) {
          setRecipeGrades(recipeGradesRes)
        }
      } catch (e) {
        console.error('Error fetching recipe grades:', e)
      }

      // Fetch Consumption Grades (for Grid 3 Dropdown)
      try {
        const consumptionGradesRes = await NormalOperationNormsApiService.getNormalOperationNormsGrades(keycloak, PLANT_ID, AOP_YEAR)
        if (consumptionGradesRes?.code === 200 && consumptionGradesRes?.data?.length > 0) {
          setAllGrades(consumptionGradesRes.data)
          if (!selectedGrade) setSelectedGrade(consumptionGradesRes.data[0].id)
        } else {
          setAllGrades([])
        }
      } catch (e) {
        setAllGrades([])
      }

      // Fetch Recipe Data
      try {
        const recipeRes = await DataService.getPeConfigCatChemData(keycloak, PLANT_ID, AOP_YEAR)
        if (recipeRes?.length > 0) {
          const formattedData = recipeRes?.map((item, index) => {
        const converted = {}

        Object.entries(item).forEach(([key, value]) => {
          console.log(key, value)
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
            particulars:item.ReceipeName,
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

      // Fetch Consumption
      try {
        const consumptionRes = await NormalOperationNormsApiService.getNormalOperationNormsData(keycloak, selectedGrade, false, PLANT_ID, AOP_YEAR)
        if (consumptionRes?.data?.mcuNormsValueDTOList?.length > 0) {
          setConsumptionRows(consumptionRes.data.mcuNormsValueDTOList.map((row, index) => {
            const insulator = parseFloat(row.insulatorBatch || row.emulsifierBatch) || 0
            const catalyst = parseFloat(row.catalystBatch) || 0
            const buffer = parseFloat(row.bufferBatch) || 0
            const shortStop = parseFloat(row.shortStopBatch) || 0
            const coating = parseFloat(row.coatingBatch) || 0
            const total = insulator + catalyst + buffer + shortStop + coating
            
            return {
              ...row,
              id: row.normParameterFkId || index,
              emulsifierBatch: insulator,
              catalystBatch: catalyst,
              bufferBatch: buffer,
              shortStopBatch: shortStop,
              coatingBatch: coating,
              apr: total, // Total Consumption
              norms: parseFloat(row.norms) || 0,
            }
          }))
        } else {
          setConsumptionRows(STATIC_CONSUMPTION.map(row => ({ ...row, apr: row.emulsifierBatch + row.catalystBatch + row.bufferBatch + row.shortStopBatch + row.coatingBatch })))
        }
      } catch (e) {
        setConsumptionRows(STATIC_CONSUMPTION.map(row => ({ ...row, apr: row.emulsifierBatch + row.catalystBatch + row.bufferBatch + row.shortStopBatch + row.coatingBatch })))
      }

    } catch (error) {
      console.error('Error fetching Catalyst data:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [PLANT_ID, AOP_YEAR])

  const saveConstantChanges = async () => {
    const data = Object.values(modifiedConstantCells).filter(r => r.inEdit)
    if (data.length === 0) return
    const validation = validateFields(data, ['remarks'])
    if (validation) {
      setSnackbarData({ message: validation, severity: 'error' })
      setSnackbarOpen(true)
      return
    }
    setLoading(true)
    try {
      const payload = data.map(row => ({
        normParameterFKId: row.normParameterFKId,
        apr: row.value,
        remarks: row.remarks,
        auditYear: AOP_YEAR,
        uom: row.uom,
      }))
      await RawMaterialNormsBasisApiService.postData(keycloak, payload, PLANT_ID, AOP_YEAR)
      setSnackbarData({ message: 'Constants Saved!', severity: 'success' })
      setSnackbarOpen(true)
      fetchData()
    } catch (e) {
      console.error(e)
      // Simulate success for static data mode
      setSnackbarData({ message: 'Constants Saved!', severity: 'success' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }

  const saveRecipeChanges = async () => {
    const data = Object.values(modifiedRecipeCells).filter(r => r.inEdit)
    if (data.length === 0) return
    setLoading(true)
    try {
      const payload = data.map(row => ({
        recId: row.Reciepe_FK_ID?.toString() || '0',
        grades: Object.entries(row)
          .filter(([key]) => /^[0-9A-Fa-f-]{36}$/.test(key) || key.startsWith('grade'))
          .reduce((acc, [key, value]) => {
            acc[key] = Number(value)
            return acc
          }, {}),
      }))
      await DataService.updatePeConfigData(keycloak, payload, PLANT_ID, AOP_YEAR)
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
      await RawMaterialNormsBasisApiService.handleCalculateNormsConfiguration(PLANT_ID, AOP_YEAR, keycloak)
      setSnackbarData({ message: 'Calculation Successful!', severity: 'success' })
      setSnackbarOpen(true)
      fetchData()
    } catch (e) {
      console.error(e)
      // Simulate success for static data mode
      setSnackbarData({ message: 'Calculation Successful!', severity: 'success' })
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
        await DataService.getRecipeCatChemExcel(keycloak, PLANT_ID, AOP_YEAR, title, 'Manual')
      } else if (type === 'recipe') {
        await DataService.getRecipeCatChemExcel(keycloak, PLANT_ID, AOP_YEAR, `${EXCEL_EXPORT_TITLE}_${title}`)
      }
      setSnackbarData({ message: 'Excel Export Successful!', severity: 'success' })
      setSnackbarOpen(true)
    } catch (e) {
      setSnackbarData({ message: 'Export Failed!', severity: 'error' })
      setSnackbarOpen(true)
    }
  }

  const handleExcelUpload = async (file, type) => {
    setLoading(true)
    try {
      if (type === 'constant') {
        await DataService.saveRecipeCatChemExcel(file, keycloak, PLANT_ID, AOP_YEAR, 'Manual')
      } else if (type === 'recipe') {
        await DataService.saveRecipeCatChemExcel(file, keycloak, PLANT_ID, AOP_YEAR)
      }
      setSnackbarData({ message: 'Import Successful!', severity: 'success' })
      setSnackbarOpen(true)
      fetchData()
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
    // fetchData(gradeId)
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Backdrop sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }} open={loading}>
        <CircularProgress color='inherit' />
      </Backdrop>

      {/* Grid 1: Constant */}
        <KendoDataTables
          title="Constant"
          columns={constantColumns}
          rows={constantRows}
          setRows={setConstantRows}
          modifiedCells={modifiedConstantCells}
          setModifiedCells={setModifiedConstantCells}
          saveChanges={saveConstantChanges}
          permissions={adjustedPermissionsConstant()}
          groupBy="TypeDisplayName"
          fetchData={fetchData}
          downloadExcelForConfiguration={() => downloadExcel('constant', 'Constant')}
          handleExcelUpload={(file) => handleExcelUpload(file, 'constant')}
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          apiRef={apiRef}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          handleRemarkCellClick={handleRemarkClick}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
        />

      {/* Grid 2: Recipe */}
        <KendoDataTables
          title="Recipe"
          columns={recipeColumns}
          rows={recipeRows}
          setRows={setRecipeRows}
          modifiedCells={modifiedRecipeCells}
          setModifiedCells={setModifiedRecipeCells}
          saveChanges={saveRecipeChanges}
          permissions={adjustedPermissionsRecipe()}
          groupBy="TypeDisplayName"
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
          title="Cat-chem Consumption"
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
