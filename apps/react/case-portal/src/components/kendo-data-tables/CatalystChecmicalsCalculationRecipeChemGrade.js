import { useGridApiRef } from '@mui/x-data-grid'
import React, { useEffect, useState, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from './index'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { CatChemRecipeChemGradeApiService } from 'services/cat-chem-recipe-chem-grade-api-service'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'

const CatalystChecmicalsCalculationRecipeChemGrade = ({ permissions, onSaveOrImport, refreshTrigger }) => {
     const [modifiedCells, setModifiedCells] = React.useState({})
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
     const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_${AOP_YEAR}_Catchem Grade`
     const IS_OLD_YEAR = oldYear?.oldYear
     const IS_RELEASED = isReleased
     const isOldYear = false
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
     const valueFormat = ValueFormatterConsumption()
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
               field: 'particulars',
               title: 'Particulars',
               editable: false,
               minWidth: 350,
               locked: true,
          },
          {
               field: 'l1K67',
               title: 'L1_K67',
               editable: true,
               type: 'number',
               widthT: 120,
               minWidth: 100,
               format: valueFormat,
          },
          {
               field: 'l2K67',
               title: 'L2_K67',
               editable: true,
               type: 'number',
               widthT: 120,
               minWidth: 100,
               format: valueFormat,
          },
          {
               field: 'l2K67F',
               title: 'L2_K67F',
               editable: true,
               type: 'number',
               widthT: 120,
               minWidth: 100,
               format: valueFormat,
          },
          {
               field: 'l2K57',
               title: 'L2_K57',
               editable: true,
               type: 'number',
               widthT: 120,
               minWidth: 100,
               format: valueFormat,
          },
          {
               field: 'l1K67Id',
               title: 'l1K67Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'l2K67Id',
               title: 'l2K67Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'l2K67FId',
               title: 'l2K67FId',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'l2K57Id',
               title: 'l2K57Id',
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
               const res = await CatChemRecipeChemGradeApiService.getCatChemChemGradeData(
                    keycloak,
                    PLANT_ID,
                    AOP_YEAR,
               )

               if (res?.code === 200) {
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

               var payload = data.map((item) => ({
                    id: item.idFromApi || null,
                    particulars: item.particulars || '',
                    l1K67: item.l1K67,
                    l2K67: item.l2K67,
                    l2K67F: item.l2K67F,
                    l2K57: item.l2K57,
                    l1K67Id: item.l1K67Id || null,
                    l2K67Id: item.l2K67Id || null,
                    l2K67FId: item.l2K67FId || null,
                    l2K57Id: item.l2K57Id || null,
               }))

               const response = await CatChemRecipeChemGradeApiService.saveCatChemChemGradeData(
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

     const downloadExcelForConfiguration = async () => {
          try {
               setSnackbarData({ message: 'Excel Export Started!', severity: 'success' })
               setSnackbarOpen(true)
               await CatChemRecipeChemGradeApiService.exportCatChemChemGradeExcel(
                    keycloak,
                    PLANT_ID,
                    AOP_YEAR,
                    `${EXCEL_EXPORT_TITLE}`,
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
               const response = await CatChemRecipeChemGradeApiService.importCatChemChemGradeExcel(
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
                    link.setAttribute('download', 'Error File CatChem ChemGrade.xlsx')
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
               downloadExcelBtn: true,
               downloadExcelBtnFromUI: false,
               ExcelName: `${EXCEL_EXPORT_TITLE}`,
               showNoteWhileDeleting: false,
               showTitleNameBusiness: true,
               titleName: 'Catchem Grade',
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

export default CatalystChecmicalsCalculationRecipeChemGrade
