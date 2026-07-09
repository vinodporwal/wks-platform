import { useGridApiRef } from '@mui/x-data-grid'
import React, { useEffect, useState, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from './index'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { CatChemRecipeChemGradeApiService } from 'services/cat-chem-recipe-chem-grade-api-service'

const CatalystChecmicalsCalculationRecipeChemGrade = ({ permissions }) => {
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
     } = dataGridStore

     const PLANT_ID = plantObject?.id
     const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
     const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
     const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()
     const AOP_YEAR = year?.selectedYear
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
               field: 'Particulars',
               title: 'Particulars',
               editable: false,
               widthT: 250,
               minWidth: 200,
          },
          {
               field: 'L1_K67',
               title: 'L1_K67',
               editable: true,
               type: 'number',
               widthT: 120,
               minWidth: 100,
          },
          {
               field: 'L2_K67',
               title: 'L2_K67',
               editable: true,
               type: 'number',
               widthT: 120,
               minWidth: 100,
          },
          {
               field: 'L2_K67F',
               title: 'L2_K67F',
               editable: true,
               type: 'number',
               widthT: 120,
               minWidth: 100,
          },
          {
               field: 'L2_K57',
               title: 'L2_K57',
               editable: true,
               type: 'number',
               widthT: 120,
               minWidth: 100,
          },
          {
               field: 'CHEM_Sodium_Bicarbonate_Food_Grade_L1_K67_Id',
               title: 'CHEM_Sodium_Bicarbonate_Food_Grade_L1_K67_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'CHEM_Sodium_Bicarbonate_Food_Grade_L2_K67_Id',
               title: 'CHEM_Sodium_Bicarbonate_Food_Grade_L2_K67_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'CHEM_Sodium_Bicarbonate_Food_Grade_L2_K67F_Id',
               title: 'CHEM_Sodium_Bicarbonate_Food_Grade_L2_K67F_Id',
               editable: false,
               hidden: true,
               isVisible: false,
          },
          {
               field: 'CHEM_Sodium_Bicarbonate_Food_Grade_L2_K57_Id',
               title: 'CHEM_Sodium_Bicarbonate_Food_Grade_L2_K57_Id',
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

               var payload = data.map((item) => ({
                    id: item.idFromApi || null,
                    Particulars: item.Particulars || '',
                    L1_K67: item.L1_K67,
                    L2_K67: item.L2_K67,
                    L2_K67F: item.L2_K67F,
                    L2_K57: item.L2_K57,
                    CHEM_Sodium_Bicarbonate_Food_Grade_L1_K67_Id: item.CHEM_Sodium_Bicarbonate_Food_Grade_L1_K67_Id || null,
                    CHEM_Sodium_Bicarbonate_Food_Grade_L2_K67_Id: item.CHEM_Sodium_Bicarbonate_Food_Grade_L2_K67_Id || null,
                    CHEM_Sodium_Bicarbonate_Food_Grade_L2_K67F_Id: item.CHEM_Sodium_Bicarbonate_Food_Grade_L2_K67F_Id || null,
                    CHEM_Sodium_Bicarbonate_Food_Grade_L2_K57_Id: item.CHEM_Sodium_Bicarbonate_Food_Grade_L2_K57_Id || null,
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
               await CatChemRecipeChemGradeApiService.exportCatChemChemGradeExcel(
                    keycloak,
                    PLANT_ID,
                    AOP_YEAR,
                    `CatChem_ChemGrade_${PLANT_NAME_NO_CASE}`,
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
               titleName: 'Catchem Chem Grade',
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
