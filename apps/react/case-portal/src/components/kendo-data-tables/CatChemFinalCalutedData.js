import { useGridApiRef } from '@mui/x-data-grid'
import React, { useEffect, useState, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from './index'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { CatChemFinalCalculatedDataService } from 'services/cat-chem-final-calculated-data-service'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'

const CatChemFinalCalutedData = ({ permissions, refreshTrigger }) => {
     const [refreshSignal, setRefreshSignal] = useState(0)
     const dataGridStore = useSelector((state) => state.dataGridStore)
     const {
          oldYear,
          plantObject,
          siteObject,
          verticalObject,
          year,
          isReleased,
          yearChanged,
          screenTitle,
     } = dataGridStore

     const PLANT_ID = plantObject?.id
     const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
     const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
     const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()
     const AOP_YEAR = year?.selectedYear
     const SCREEN_NAME = screenTitle?.title
     const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_${AOP_YEAR}_Calculated Norms`
     const IS_OLD_YEAR = oldYear?.oldYear
     const IS_RELEASED = isReleased
     const isOldYear = false
     const apiRef = useGridApiRef()

     const [loading, setLoading] = useState(false)
     const [snackbarData, setSnackbarData] = useState({
          message: '',
          severity: 'info',
     })
     const [snackbarOpen, setSnackbarOpen] = useState(false)
     const keycloak = useSession()
     const [rows, setRows] = useState([])
     const [columns, setColumns] = useState([])
     const [calculationObject, setCalculationObject] = useState([])
     const valueFormat = ValueFormatterConsumption()
     const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

     const fetchData = useCallback(async () => {
          if (!PLANT_ID || !AOP_YEAR) return
          setLoading(true)
          try {
               const res = await CatChemFinalCalculatedDataService.getCatChemFinalCalculatedData(
                    keycloak,
                    PLANT_ID,
                    AOP_YEAR,
               )

               if (res?.code === 200) {
                    const cols = res?.data?.column || res?.data?.columns
                    if (cols && Array.isArray(cols)) {
                         const dynamicCols = cols.map((col, index) => ({
                              field: col.field,
                              title: col.title || col.headerName || col.field,
                              editable: false,
                              widthT: col.widthT ?? 150,
                              minWidth: index === 0 ? 350 : (col.minWidth ?? 120),
                              align: col.align || (col.type === 'number' ? 'right' : 'left'),
                              type: col.type || 'string',
                              format: col.type === 'number' ? valueFormat : undefined,
                              locked: index === 0 ? true : undefined,
                         }))
                         setColumns(dynamicCols)
                    }

                    const dataList = res?.data?.data || []
                    setCalculationObject(res?.data?.aopCalculation)
                    const mapped = dataList.map((item, index) => ({
                         ...item,
                         id: index,
                         idFromApi: item.id || null,
                         isEditable: false,
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

     useEffect(() => {
          fetchData()
     }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak, refreshTrigger])

     const downloadExcelForConfiguration = async () => {
          try {
               setSnackbarData({ message: 'Excel Export Started!', severity: 'success' })
               setSnackbarOpen(true)
               await CatChemFinalCalculatedDataService.exportCatChemFinalCalculatedExcel(
                    keycloak,
                    PLANT_ID,
                    AOP_YEAR,
                    `CatChem_FinalCalculated_${PLANT_NAME_NO_CASE}`,
               )
               setSnackbarData({ message: 'Excel Export Successful!', severity: 'success' })
               setSnackbarOpen(true)
          } catch (e) {
               setSnackbarData({ message: 'Export Failed!', severity: 'error' })
               setSnackbarOpen(true)
          }
     }

     const handleCalculate = async () => {
          setRows([])
          setLoading(true)
          try {
               const response =
                    await CatChemFinalCalculatedDataService.handleCatChemFinalCalculatedData(
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

                    // load grades and pick the 0th index
                    await fetchData()
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
               showAction: false,
               showUnit: false,
               saveWithRemark: false,
               saveBtn: false,
               customHeight: permissions?.customHeight,
               allAction: true,
               downloadExcelBtn: false,
               downloadExcelBtnFromUI: true,
               ExcelName: `${EXCEL_EXPORT_TITLE}`,
               showNoteWhileDeleting: false,
               showTitleNameBusiness: true,
               titleName: 'Calculated Norms',
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
                    setRows={setRows}
                    columns={columns}
                    rows={rows}
                    fetchData={fetchData}
                    paginationOptions={[100, 200, 300]}
                    snackbarData={snackbarData}
                    snackbarOpen={snackbarOpen}
                    apiRef={apiRef}
                    setSnackbarOpen={setSnackbarOpen}
                    setSnackbarData={setSnackbarData}
                    permissions={adjustedPermissions}
                    disableRedHighlight={true}
                    downloadExcelForConfiguration={downloadExcelForConfiguration}
                    handleCalculate={handleCalculate}

               />
          </div>
     )
}

export default CatChemFinalCalutedData
