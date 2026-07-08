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
import { NormalOperationNormsApiService } from 'services/normal-operation-norms-api-service'

const CatChemReceipe = ({ permissions }) => {
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
               field: 'Recipe',
               title: 'Recipe',
               editable: false,
               widthT: 200,
               minWidth: 150,
          },
          {
               field: 'Sod Bi Carb',
               title: 'SodBiCarb',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'Polystat',
               title: 'Polystat',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'Evicas',
               title: 'Evicas',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'PVA88',
               title: 'PVA88',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'PVA-55',
               title: 'PVA-55',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'B72',
               title: 'B72',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'L9P',
               title: 'L9P',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'Versene',
               title: 'Versene',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'Nonyl Phe',
               title: 'Nonyl Phe',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'IRGASTAB',
               title: 'IRGASTAB',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'ATSC',
               title: 'ATSC',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'Antiswelling',
               title: 'Antiswelling',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'Antifoam',
               title: 'Antifoam',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'K57 Catalyst',
               title: 'K57 Catalyst',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'K67 Catalyst',
               title: 'K67 Catalyst',
               editable: true,
               type: 'number',
               widthT: 110,
               minWidth: 90,
          },
          {
               field: 'DM_Water_Sodi_Bi_Carb_Id',
               title: 'DM_Water_Sodi_Bi_Carb_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'DM_Water_Polystat_Id',
               title: 'DM_Water_Polystat_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'DM_Water_Evicas_Id',
               title: 'DM_Water_Evicas_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'DM_Water_PVA88_Id',
               title: 'DM_Water_PVA88_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'DM_Water_PVA55_Id',
               title: 'DM_Water_PVA55_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'DM_Water_B72_Id',
               title: 'DM_Water_B72_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'DM_Water_L9P_Id',
               title: 'DM_Water_L9P_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'DM_Water_Versene_Id',
               title: 'DM_Water_Versene_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'DM_Water_Nonyl_Phe_Id',
               title: 'DM_Water_Nonyl_Phe_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'DM_Water_IRGASTAB_Id',
               title: 'DM_Water_IRGASTAB_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'DM_Water_ATSC_Id',
               title: 'DM_Water_ATSC_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'DM_Water_Antiswelling_Id',
               title: 'DM_Water_Antiswelling_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'DM_Water_Antifoam_Id',
               title: 'DM_Water_Antifoam_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'DM_Water_K57_Catalyst_Id',
               title: 'DM_Water_K57_Catalyst_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'DM_Water_K67_Catalyst_Id',
               title: 'DM_Water_K67_Catalyst_Id',
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
               const res = await NormalOperationNormsApiService.getCatChemCalculationData(
                    keycloak,
                    PLANT_ID,
                    AOP_YEAR,
               )

               if (res?.code === 200) {
                    const mapped = (res?.data || []).map((item, index) => ({
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

               // adjust to whichever fields are actually mandatory on this grid
               // const requiredFields = ['remarks']

               // const validationMessage = validateFields(data, requiredFields)
               // if (validationMessage) {
               //     setSnackbarOpen(true)
               //     setSnackbarData({
               //         message: validationMessage,
               //         severity: 'error',
               //     })
               //     setLoading(false)
               //     return
               // }
               var payload = []
               payload = data.map((item) => ({
                    id: item.idFromApi || null,
                    Recipe: item.Recipe || '',
                    'Sod Bi Carb': item['Sod Bi Carb'] || item.SodBiCarb || item['SodBiCarb'],
                    Polystat: item.Polystat,
                    Evicas: item.Evicas,
                    PVA88: item.PVA88,
                    'PVA-55': item['PVA-55'],
                    B72: item.B72,
                    L9P: item.L9P,
                    Versene: item.Versene,
                    'Nonyl Phe': item['Nonyl Phe'],
                    IRGASTAB: item.IRGASTAB,
                    ATSC: item.ATSC,
                    Antiswelling: item.Antiswelling,
                    Antifoam: item.Antifoam,
                    'K57 Catalyst': item['K57 Catalyst'],
                    'K67 Catalyst': item['K67 Catalyst'],
                    DM_Water_Sodi_Bi_Carb_Id: item.DM_Water_Sodi_Bi_Carb_Id || null,
                    DM_Water_Polystat_Id: item.DM_Water_Polystat_Id || null,
                    DM_Water_Evicas_Id: item.DM_Water_Evicas_Id || null,
                    DM_Water_PVA88_Id: item.DM_Water_PVA88_Id || null,
                    DM_Water_PVA55_Id: item.DM_Water_PVA55_Id || null,
                    DM_Water_B72_Id: item.DM_Water_B72_Id || null,
                    DM_Water_L9P_Id: item.DM_Water_L9P_Id || null,
                    DM_Water_Versene_Id: item.DM_Water_Versene_Id || null,
                    DM_Water_Nonyl_Phe_Id: item.DM_Water_Nonyl_Phe_Id || null,
                    DM_Water_IRGASTAB_Id: item.DM_Water_IRGASTAB_Id || null,
                    DM_Water_ATSC_Id: item.DM_Water_ATSC_Id || null,
                    DM_Water_Antiswelling_Id: item.DM_Water_Antiswelling_Id || null,
                    DM_Water_Antifoam_Id: item.DM_Water_Antifoam_Id || null,
                    DM_Water_K57_Catalyst_Id: item.DM_Water_K57_Catalyst_Id || null,
                    DM_Water_K67_Catalyst_Id: item.DM_Water_K67_Catalyst_Id || null,
               }))

               const response = await NormalOperationNormsApiService.saveCatChemCalculationData(
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
               showNoteWhileDeleting: false,
               showTitleNameBusiness: true,
               titleName: 'Catchem Receipe',

               uploadExcelBtn: false,
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

               />
          </div>
     )
}
export default CatChemReceipe
