import { useGridApiRef } from '@mui/x-data-grid'
import React, { useEffect, useState, useMemo, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { PlantAopReportApiService } from 'services/plant-aop-report-api-service'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { validateFields } from 'utils/validationUtils'
import KendoDataTables from './index'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import CalculatedBusinessProposed from './ProprosedBusinessGradeOptimizer'
import BudgetOperatingHour from './BudgetOperatingHourGradeMix'
import { DataService } from 'services/DataService'
import VmcTrade from './VmcTrade'
import VcmStockbalance from './VcmStockBalance'
import { ProductionSchedulingApiService } from 'services/production-scheduling-api-service'
import { CatChemRecipeCalcApiService } from 'services/cat-chem-recipe-calc-api-service'


const CatalystChecmicalsCalculationRecipeCalc = ({ permissions, onSaveOrImport, refreshTrigger }) => {
     const [modifiedCells, setModifiedCells] = React.useState({})
     const [refreshSignal, setRefreshSignal] = useState(0)
     const dataGridStore = useSelector((state) => state.dataGridStore)
     const {
          verticalChange,
          yearChanged,
          oldYear,
          plantObject,
          siteObject,
          verticalObject,
          year,
          screenTitle,
     } = dataGridStore

     const PLANT_ID = plantObject?.id
     const PLANT_NAME = plantObject?.name

     const SITE_ID = siteObject?.id
     const SITE_NAME = siteObject?.name

     const VERTICAL_ID = verticalObject?.id
     const VERTICAL_NAME = verticalObject?.name

     const AOP_YEAR = year?.selectedYear
     const vertName = verticalChange?.selectedVertical
     const SCREEN_NAME = screenTitle?.title

     const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
     const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
     const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()

     const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_${AOP_YEAR}_Catchem Calc`

     const lowerVertName = vertName?.toLowerCase()
     const lowerSiteName = SITE_NAME?.toLowerCase()
     const lowerPlantName = PLANT_NAME?.toLowerCase()
     const plantName = plantObject?.name
     const siteName = siteObject?.name
     const isOldYear = false
     const IS_OLD_YEAR = oldYear?.oldYear
     const [open1, setOpen1] = useState(false)
     const [deleteId, setDeleteId] = useState(null)
     const apiRef = useGridApiRef()

     const [loading, setLoading] = useState(false)
     const [snackbarData, setSnackbarData] = useState({
          message: '',
          severity: 'info',
     })
     const [snackbarOpen, setSnackbarOpen] = useState(false)
     const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
     const [currentRemark, setCurrentRemark] = useState('')
     const [currentRowId, setCurrentRowId] = useState(null)
     const keycloak = useSession()
     const [rows, setRows] = useState()
     const [calculationObject, setCalculationObject] = useState([])
     const valueFormat = ValueFormatterConsumption()
     const { isReleased } = dataGridStore
     const IS_RELEASED = isReleased
     const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
     const handleRemarkCellClick = (row) => {
          if (READ_ONLY) return
          setCurrentRemark(row.remarks || '')
          setCurrentRowId(row.id)
          setRemarkDialogOpen(true)
     }
     const columns = [
          {
               field: 'id',
               title: 'Id',
               editable: false,
               widthT: 180,
               minWidth: 150,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'recipe',
               title: 'Recipe',
               editable: false,
               minWidth: 350,
               locked: true,
          },
          {
               field: 'sodBiCarb',
               title: 'SodBiCarb',
               editable: true,
               type: 'number',
               widthT: 200,
               minWidth: 150,
               format: valueFormat,
          },
          {
               field: 'polystat',
               title: 'Polystat',
               editable: true,
               type: 'number',
               widthT: 200,
               minWidth: 150,
               format: valueFormat,
          },
          {
               field: 'evicas',
               title: 'Evicas',
               editable: true,
               type: 'number',
               widthT: 200,
               minWidth: 150,
               format: valueFormat,
          },
          {
               field: 'pva88',
               title: 'PVA88',
               editable: true,
               type: 'number',
               widthT: 200,
               minWidth: 150,
               format: valueFormat,
          },
          {
               field: 'pva55',
               title: 'PVA-55',
               editable: true,
               type: 'number',
               widthT: 200,
               minWidth: 150,
               format: valueFormat,
          },
          {
               field: 'b72',
               title: 'B72',
               editable: true,
               type: 'number',
               widthT: 200,
               minWidth: 150,
               format: valueFormat,
          },
          {
               field: 'l9p',
               title: 'L9P',
               editable: true,
               type: 'number',
               widthT: 200,
               minWidth: 150,
               format: valueFormat,
          },
          {
               field: 'versene',
               title: 'Versene',
               editable: true,
               type: 'number',
               widthT: 200,
               minWidth: 150,
               format: valueFormat,
          },
          {
               field: 'nonylPhe',
               title: 'Nonyl Phe',
               editable: true,
               type: 'number',
               widthT: 200,
               minWidth: 150,
               format: valueFormat,
          },
          {
               field: 'irgastab',
               title: 'IRGASTAB',
               editable: true,
               type: 'number',
               widthT: 200,
               minWidth: 150,
               format: valueFormat,
          },
          {
               field: 'atsc',
               title: 'ATSC',
               editable: true,
               type: 'number',
               widthT: 200,
               minWidth: 150,
               format: valueFormat,
          },
          {
               field: 'antiswelling',
               title: 'Antiswelling',
               editable: true,
               type: 'number',
               widthT: 200,
               minWidth: 150,
               format: valueFormat,
          },
          {
               field: 'antifoam',
               title: 'Antifoam',
               editable: true,
               type: 'number',
               widthT: 200,
               minWidth: 150,
               format: valueFormat,
          },
          {
               field: 'k57Catalyst',
               title: 'K57 Catalyst',
               editable: true,
               type: 'number',
               widthT: 200,
               minWidth: 150,
               format: valueFormat,
          },
          {
               field: 'k67Catalyst',
               title: 'K67 Catalyst',
               editable: true,
               type: 'number',
               widthT: 200,
               minWidth: 150,
               format: valueFormat,
          },
          {
               field: 'sodiBiCarbId',
               title: 'sodiBiCarbId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'polystatId',
               title: 'polystatId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'evicasId',
               title: 'evicasId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'pva88Id',
               title: 'pva88Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'pva55Id',
               title: 'pva55Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'b72Id',
               title: 'b72Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'l9pId',
               title: 'l9pId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'verseneId',
               title: 'verseneId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'nonylPheId',
               title: 'nonylPheId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'irgastabId',
               title: 'irgastabId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'atscId',
               title: 'atscId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'antiswellingId',
               title: 'antiswellingId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'antifoamId',
               title: 'antifoamId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'k57CatalystId',
               title: 'k57CatalystId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'k67CatalystId',
               title: 'k67CatalystId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
     ];

     const fetchData = useCallback(async () => {
          if (!PLANT_ID || !AOP_YEAR) return
          setModifiedCells({})
          setLoading(true)
          try {
               const res = await CatChemRecipeCalcApiService.getCatChemCalcData(
                    keycloak,
                    PLANT_ID,
                    AOP_YEAR,
               )

               if (res?.code === 200) {
                    setCalculationObject(res?.data?.aopCalculation)
                    const mapped = (res?.data?.Data || []).map((item, index) => ({
                         ...item,
                         id: index,
                         idFromApi: item.id || null,
                         isEditable: item.isEditable ?? true,
                    }))
                    setRows(mapped)
               } else {
                    setRows([])
               }
          } catch (err) {
               console.error('Error fetching data:', err)
               setRows([])
          } finally {
               setLoading(false)
          }
     }, [keycloak, yearChanged, PLANT_ID, AOP_YEAR, refreshTrigger])

     const saveChanges = React.useCallback(async () => {
          try {
               setLoading(true)
               const data = Object.values(modifiedCells)

               if (data.length === 0) {
                    setSnackbarOpen(true)
                    setSnackbarData({
                         message: 'No Records to Save!',
                         severity: 'info',
                    })
                    return
               }

               var payload = []
               payload = data.map((item) => ({
                    id: item.idFromApi || null,
                    recipe: item.recipe || '',
                    sodBiCarb: item.sodBiCarb,
                    polystat: item.polystat,
                    evicas: item.evicas,
                    pva88: item.pva88,
                    pva55: item.pva55,
                    b72: item.b72,
                    l9p: item.l9p,
                    versene: item.versene,
                    nonylPhe: item.nonylPhe,
                    irgastab: item.irgastab,
                    atsc: item.atsc,
                    antiswelling: item.antiswelling,
                    antifoam: item.antifoam,
                    k57Catalyst: item.k57Catalyst,
                    k67Catalyst: item.k67Catalyst,
                    sodiBiCarbId: item.sodiBiCarbId || null,
                    polystatId: item.polystatId || null,
                    evicasId: item.evicasId || null,
                    pva88Id: item.pva88Id || null,
                    pva55Id: item.pva55Id || null,
                    b72Id: item.b72Id || null,
                    l9pId: item.l9pId || null,
                    verseneId: item.verseneId || null,
                    nonylPheId: item.nonylPheId || null,
                    irgastabId: item.irgastabId || null,
                    atscId: item.atscId || null,
                    antiswellingId: item.antiswellingId || null,
                    antifoamId: item.antifoamId || null,
                    k57CatalystId: item.k57CatalystId || null,
                    k67CatalystId: item.k67CatalystId || null,
               }))

               const response = await CatChemRecipeCalcApiService.saveCatChemCalcData(
                    keycloak,
                    PLANT_ID,
                    AOP_YEAR,
                    payload,
               )

               if (response) {
                    setSnackbarOpen(true)
                    setSnackbarData({
                         message: 'Saved Successfully!',
                         severity: 'success',
                    })
                    setModifiedCells({})
                    if (onSaveOrImport) onSaveOrImport()
               } else {
                    setSnackbarOpen(true)
                    setSnackbarData({
                         message: response?.message || 'Save failed!',
                         severity: 'error',
                    })
               }
          } catch (error) {
               setSnackbarOpen(true)
               setSnackbarData({
                    message: 'Unexpected error occurred!',
                    severity: 'error',
               })
          } finally {
               setLoading(false)
          }
     }, [modifiedCells, keycloak, PLANT_ID, AOP_YEAR, fetchData, setRefreshSignal, onSaveOrImport])

     useEffect(() => {
          fetchData()
     }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak, refreshTrigger])

     const handleCalculate = async () => {
          setRows([])
          setLoading(true)
          try {
               const response = await CatChemRecipeCalcApiService.calculateMakeupBatchRecipeCalc(
                    keycloak,
                    PLANT_ID,
                    AOP_YEAR,
               )

               if (response?.code == 200) {
                    setSnackbarOpen(true)
                    setSnackbarData({
                         message: 'Data refreshed successfully!',
                         severity: 'success',
                    })
                    await fetchData()
                    if (onSaveOrImport) onSaveOrImport()
               } else {
                    setSnackbarOpen(true)
                    setSnackbarData({
                         message: 'Data Refresh Failed!',
                         severity: 'error',
                    })
               }
          } catch (error) {
               console.error('Error saving refresh data:', error)
          } finally {
               setLoading(false)
          }
     }

     const downloadExcelForConfiguration = async () => {
          try {
               setSnackbarData({ message: 'Excel Export Started!', severity: 'success' })
               setSnackbarOpen(true)
               await CatChemRecipeCalcApiService.exportCatChemCalcExcel(
                    keycloak,
                    PLANT_ID,
                    AOP_YEAR,
                    `CatChem_Calc_${PLANT_NAME_NO_CASE}`,
               )
               setSnackbarData({ message: 'Excel Export Successful!', severity: 'success' })
               setSnackbarOpen(true)
          } catch (e) {
               setSnackbarData({ message: 'Export Failed!', severity: 'error' })
               setSnackbarOpen(true)
          }
     }

     const handleExcelUpload = async (file) => {
          setLoading(true)
          try {
               const response = await CatChemRecipeCalcApiService.importCatChemCalcExcel(
                    file,
                    keycloak,
                    PLANT_ID,
                    AOP_YEAR,
               )
               if (response?.code == 200) {
                    setSnackbarOpen(true)
                    setSnackbarData({ message: 'Data Uploaded Successfully!', severity: 'success' })
                    setModifiedCells({})
                    fetchData()
                    if (onSaveOrImport) onSaveOrImport()
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
                    link.setAttribute('download', 'Error File CatChem Calc.xlsx')
                    document.body.appendChild(link)
                    link.click()
                    link.remove()
                    window.URL.revokeObjectURL(url)
                    setSnackbarOpen(true)
                    setSnackbarData({ message: 'Partial data saved. Error file downloaded.', severity: 'warning' })
                    fetchData()
                    if (onSaveOrImport) onSaveOrImport()
               } else {
                    setSnackbarOpen(true)
                    setSnackbarData({ message: 'Upload Failed!', severity: 'error' })
               }
          } catch (e) {
               setSnackbarData({ message: 'Import Failed!', severity: 'error' })
               setSnackbarOpen(true)
          } finally {
               setLoading(false)
          }
     }

     const getAdjustedPermissions = (permissions, isOldYear) => {
          if (isOldYear != 1) return permissions
          return {
               ...permissions,
               showAction: false,
               addButton: false,
               deleteButton: false,
               downloadExcelBtn: false,
               uploadExcelBtn: false,
               editButton: false,
               showUnit: false,
               saveWithRemark: false,
               saveBtn: false,
               isOldYear: isOldYear,
               allAction: false,
          }
     }

     const adjustedPermissions = getAdjustedPermissions(
          {
               showAction: permissions?.showAction ?? true,
               showUnit: permissions?.showUnit ?? false,
               saveWithRemark: permissions?.saveWithRemark ?? true,
               saveBtn: permissions?.saveBtn ?? true,
               customHeight: permissions?.customHeight,
               allAction: true,
               downloadExcelBtn: false,
               downloadExcelBtnFromUI: true,
               ExcelName: `${EXCEL_EXPORT_TITLE}`,
               showNoteWhileDeleting: false,
               showTitleNameBusiness: true,
               titleName: 'Catchem Calc',
               uploadExcelBtn: false,
               showCalculate: true,
               showCalculateVisibility: Object.keys(calculationObject || {}).length > 0,
          },
          isOldYear,
     )

     return (
          <div>
               <LoaderBackdrop open={!!loading} />



               <KendoDataTables
                    modifiedCells={modifiedCells}
                    setModifiedCells={setModifiedCells}
                    setRows={setRows}
                    columns={columns}
                    rows={rows}
                    fetchData={fetchData}
                    saveChanges={saveChanges}
                    paginationOptions={[100, 200, 300]}
                    snackbarData={snackbarData}
                    snackbarOpen={snackbarOpen}
                    apiRef={apiRef}
                    deleteId={deleteId}
                    open1={open1}
                    setDeleteId={setDeleteId}
                    setOpen1={setOpen1}
                    setSnackbarOpen={setSnackbarOpen}
                    setSnackbarData={setSnackbarData}
                    handleRemarkCellClick={handleRemarkCellClick}
                    remarkDialogOpen={remarkDialogOpen}
                    setRemarkDialogOpen={setRemarkDialogOpen}
                    currentRemark={currentRemark}
                    setCurrentRemark={setCurrentRemark}
                    currentRowId={currentRowId}
                    permissions={adjustedPermissions}
                    disableRedHighlight={true}
                    downloadExcelForConfiguration={downloadExcelForConfiguration}
                    handleExcelUpload={handleExcelUpload}
                    handleCalculate={handleCalculate}
               />
          </div>
     )
}
export default CatalystChecmicalsCalculationRecipeCalc
