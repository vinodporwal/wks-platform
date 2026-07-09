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
import { CatChemRecipeApiService } from 'services/cat-chem-recipe-api-service'


const CatalystChecmicalsCalculationRecipe = ({ permissions }) => {
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

     const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}`

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
               widthT: 200,
               minWidth: 150,
          },
          {
               field: 'sodBiCarb',
               title: 'SodBiCarb',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'polystat',
               title: 'Polystat',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'evicas',
               title: 'Evicas',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'pva88',
               title: 'PVA88',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'pva55',
               title: 'PVA-55',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'b72',
               title: 'B72',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'l9p',
               title: 'L9P',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'versene',
               title: 'Versene',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'nonylPhe',
               title: 'Nonyl Phe',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'irgastab',
               title: 'IRGASTAB',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'atsc',
               title: 'ATSC',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'antiswelling',
               title: 'Antiswelling',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'antifoam',
               title: 'Antifoam',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'k57Catalyst',
               title: 'K57 Catalyst',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'k67Catalyst',
               title: 'K67 Catalyst',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'dmWaterSodiBiCarbId',
               title: 'dmWaterSodiBiCarbId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'dmWaterPolystatId',
               title: 'dmWaterPolystatId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'dmWaterEvicasId',
               title: 'dmWaterEvicasId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'dmWaterPva88Id',
               title: 'dmWaterPva88Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'dmWaterPva55Id',
               title: 'dmWaterPva55Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'dmWaterB72Id',
               title: 'dmWaterB72Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'dmWaterL9pId',
               title: 'dmWaterL9pId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'dmWaterVerseneId',
               title: 'dmWaterVerseneId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'dmWaterNonylPheId',
               title: 'dmWaterNonylPheId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'dmWaterIrgastabId',
               title: 'dmWaterIrgastabId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'dmWaterAtscId',
               title: 'dmWaterAtscId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'dmWaterAntiswellingId',
               title: 'dmWaterAntiswellingId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'dmWaterAntifoamId',
               title: 'dmWaterAntifoamId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'dmWaterK57CatalystId',
               title: 'dmWaterK57CatalystId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'dmWaterK67CatalystId',
               title: 'dmWaterK67CatalystId',
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
               const res = await CatChemRecipeApiService.getCatChemRecipeData(
                    keycloak,
                    PLANT_ID,
                    AOP_YEAR,
               )

               if (res?.code === 200) {
                    const mapped = (res?.data?.Data || []).map((item, index) => ({
                         ...item,
                         id: index,
                         idFromApi: item.id || null,
                         isEditable: true,
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
     }, [keycloak, yearChanged, PLANT_ID, AOP_YEAR])

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
                    dmWaterSodiBiCarbId: item.dmWaterSodiBiCarbId || null,
                    dmWaterPolystatId: item.dmWaterPolystatId || null,
                    dmWaterEvicasId: item.dmWaterEvicasId || null,
                    dmWaterPva88Id: item.dmWaterPva88Id || null,
                    dmWaterPva55Id: item.dmWaterPva55Id || null,
                    dmWaterB72Id: item.dmWaterB72Id || null,
                    dmWaterL9pId: item.dmWaterL9pId || null,
                    dmWaterVerseneId: item.dmWaterVerseneId || null,
                    dmWaterNonylPheId: item.dmWaterNonylPheId || null,
                    dmWaterIrgastabId: item.dmWaterIrgastabId || null,
                    dmWaterAtscId: item.dmWaterAtscId || null,
                    dmWaterAntiswellingId: item.dmWaterAntiswellingId || null,
                    dmWaterAntifoamId: item.dmWaterAntifoamId || null,
                    dmWaterK57CatalystId: item.dmWaterK57CatalystId || null,
                    dmWaterK67CatalystId: item.dmWaterK67CatalystId || null,
               }))

               const response = await CatChemRecipeApiService.saveCatChemRecipeData(
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
                    fetchData()
                    setRefreshSignal((prev) => prev + 1)
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
     }, [modifiedCells, keycloak, PLANT_ID, AOP_YEAR, fetchData, setRefreshSignal])

     useEffect(() => {
          fetchData()
     }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak])

     const downloadExcelForConfiguration = async () => {
          try {
               setSnackbarData({ message: 'Excel Export Started!', severity: 'success' })
               setSnackbarOpen(true)
               await CatChemRecipeApiService.exportCatChemRecipeExcel(
                    keycloak,
                    PLANT_ID,
                    AOP_YEAR,
                    `CatChem_Recipe_${PLANT_NAME_NO_CASE}`,
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
               const response = await CatChemRecipeApiService.importCatChemRecipeExcel(
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
                    link.setAttribute('download', 'Error File CatChem Recipe.xlsx')
                    document.body.appendChild(link)
                    link.click()
                    link.remove()
                    window.URL.revokeObjectURL(url)
                    setSnackbarOpen(true)
                    setSnackbarData({ message: 'Partial data saved. Error file downloaded.', severity: 'warning' })
                    fetchData()
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
               downloadExcelBtn: true,
               showNoteWhileDeleting: false,
               showTitleNameBusiness: true,
               titleName: 'Catchem Receipe',
               uploadExcelBtn: true,
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
               />
          </div>
     )
}
export default CatalystChecmicalsCalculationRecipe
