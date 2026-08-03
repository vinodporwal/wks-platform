import React, { useState, useEffect, useRef } from 'react'
import { Box, Backdrop, CircularProgress } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { generateHeaderNames } from '../../common/utilities/generateHeaders'
import ValueFormatterPhaseTwo, {
     customValueFormatterPhaseTwo,
} from '../../common/ValueFormatterPhaseTwo'
import { validateRowDataWithRemarks } from '../../common/commonUtilityFunctions'
import { SteadyStateConsumptionApiService } from '../../services/crude/steadyStateConsumptionApiService'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { JswBudgetSourceAPIService } from 'components/aop-phase-two/services/crude/jwBudgetSourceAPIService'

const JwBudgetScreen = () => {
     const keycloak = useSession()
     const dataGridStore = useSelector((state) => state.dataGridStore)
     const { plantObject, siteObject, year } = dataGridStore

     const PLANT_ID = plantObject?.id
     const SITE_ID = siteObject?.id || siteObject?.value || plantObject?.siteId || plantObject?.siteFKId
     const AOP_YEAR = year?.selectedYear || year

     const isFetchedRef = useRef(false)

     const [loading, setLoading] = useState(false)
     const [rows, setRows] = useState([])
     const [originalRows, setOriginalRows] = useState([])
     const [modifiedCells, setModifiedCells] = useState({})
     const [unitDropdown, setUnitDropdown] = useState([])
     const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
     const [currentRemark, setCurrentRemark] = useState('')
     const [currentRowId, setCurrentRowId] = useState(null)
     const [snackbarData, setSnackbarData] = useState({
          message: '',
          severity: 'info',
     })
     const [snackbarOpen, setSnackbarOpen] = useState(false)

     const valueFormat = customValueFormatterPhaseTwo(5)
     const headerMap = generateHeaderNames(AOP_YEAR)

     const columns = [
          {
               field: 'id',
               title: 'Id',
               widthT: 250,
               minWidth: 200,
               type: 'text',
               editable: false,
               locked: true,
               hidden: true,
          },
          {
               field: 'unit',
               title: 'Unit',
               widthT: 250,
               minWidth: 200,
               type: 'select',
               options: unitDropdown,
               editable: true,
               locked: true,
          },
          {
               field: 'normParameterTypeDisplayName',
               title: 'Type',
               widthT: 250,
               minWidth: 200,
               type: 'text',
               editable: false,
               locked: true,
               hidden: true,
          },
          {
               field: 'uom',
               title: 'UOM',
               widthT: 120,
               minWidth: 120,
               type: 'text',
               editable: false,
          },
          {
               field: 'apr',
               title: headerMap[4],
               widthT: 120,
               minWidth: 120,
               type: 'number1',
               editable: true,
               format: valueFormat,
          },
          {
               field: 'may',
               title: headerMap[5],
               widthT: 120,
               minWidth: 120,
               type: 'number1',
               editable: true,
               format: valueFormat,
          },
          {
               field: 'jun',
               title: headerMap[6],
               widthT: 120,
               minWidth: 120,
               type: 'number1',
               editable: true,
               format: valueFormat,
          },
          {
               field: 'jul',
               title: headerMap[7],
               widthT: 120,
               minWidth: 120,
               type: 'number1',
               editable: true,
               format: valueFormat,
          },
          {
               field: 'aug',
               title: headerMap[8],
               widthT: 120,
               minWidth: 120,
               type: 'number1',
               editable: true,
               format: valueFormat,
          },
          {
               field: 'sep',
               title: headerMap[9],
               widthT: 120,
               minWidth: 120,
               type: 'number1',
               editable: true,
               format: valueFormat,
          },
          {
               field: 'oct',
               title: headerMap[10],
               widthT: 120,
               minWidth: 120,
               type: 'number1',
               editable: true,
               format: valueFormat,
          },
          {
               field: 'nov',
               title: headerMap[11],
               widthT: 120,
               minWidth: 120,
               type: 'number1',
               editable: true,
               format: valueFormat,
          },
          {
               field: 'dec',
               title: headerMap[12],
               widthT: 120,
               minWidth: 120,
               type: 'number1',
               editable: true,
               format: valueFormat,
          },
          {
               field: 'jan',
               title: headerMap[1],
               widthT: 120,
               minWidth: 120,
               type: 'number1',
               editable: true,
               format: valueFormat,
          },
          {
               field: 'feb',
               title: headerMap[2],
               widthT: 120,
               minWidth: 120,
               type: 'number1',
               editable: true,
               format: valueFormat,
          },
          {
               field: 'mar',
               title: headerMap[3],
               widthT: 120,
               minWidth: 120,
               type: 'number1',
               editable: true,
               format: valueFormat,
          },
          
     ]

     const dummyRows = []

     useEffect(() => {
          if (SITE_ID) {
               fetchUnitDropdown()
          }
     }, [SITE_ID])

     const fetchUnitDropdown = async () => {
          try {
               const response = await JswBudgetSourceAPIService.getDropdownUnit(
                    keycloak,
                    SITE_ID,
               )
               const data = response?.data || response?.result || response || []
               const formattedOptions = Array.isArray(data)
                    ? data.map((item) => ({
                         label: item?.Unit || item?.unit || item?.name || item?.displayName || '',
                         value: item?.Unit || item?.unit || item?.id || item?.Id || '',
                         id: item?.Id || item?.id,
                         unitId: item?.Id || item?.id,
                         uom: item?.UOM || item?.uom || '',
                    }))
                    : []
               setUnitDropdown(formattedOptions)
          } catch (error) {
               console.error('Error fetching unit dropdown options:', error)
          }
     }

     const handleCustomItemChange = (e, setRowsState, setModifiedCellsState) => {
          const { dataItem, field, value } = e
          if (field === 'unit') {
               const valStr = typeof value === 'object' ? (value?.value || value?.label || '') : String(value || '')
               const selectedObj = unitDropdown.find(
                    (opt) =>
                         String(opt.value || '').toLowerCase() === valStr.toLowerCase() ||
                         String(opt.label || '').toLowerCase() === valStr.toLowerCase() ||
                         String(opt.id || '').toLowerCase() === valStr.toLowerCase() ||
                         String(opt.unitId || '').toLowerCase() === valStr.toLowerCase()
               )
               if (selectedObj) {
                    const uomVal = selectedObj.uom || ''
                    const unitIdVal = selectedObj.unitId || selectedObj.id || ''
                    const unitLabel = selectedObj.label || selectedObj.value || valStr

                    dataItem.unit = unitLabel
                    dataItem.uom = uomVal
                    dataItem.UOM = uomVal
                    dataItem.unitId = unitIdVal
                    dataItem.unitFKId = unitIdVal
                    dataItem.normParameterFKId = unitIdVal

                    setRowsState((prev) =>
                         prev.map((r) =>
                              String(r.id) === String(dataItem.id)
                                   ? {
                                        ...r,
                                        unit: unitLabel,
                                        uom: uomVal,
                                        UOM: uomVal,
                                        unitId: unitIdVal,
                                        unitFKId: unitIdVal,
                                        normParameterFKId: unitIdVal,
                                   }
                                   : r
                         )
                    )

                    setModifiedCellsState((prev) => {
                         const rowId = dataItem.id
                         const previousModified = prev[rowId] || {}
                         const updatedRow = {
                              ...dataItem,
                              ...previousModified,
                              unit: unitLabel,
                              uom: uomVal,
                              UOM: uomVal,
                              unitId: unitIdVal,
                              unitFKId: unitIdVal,
                              normParameterFKId: unitIdVal,
                         }
                         return { ...prev, [rowId]: updatedRow }
                    })
               }
          }
     }

     useEffect(() => {
          if (SITE_ID && AOP_YEAR && !isFetchedRef.current) {
               isFetchedRef.current = true
               fetchData()
          }
     }, [SITE_ID, AOP_YEAR])

     const fetchData = async () => {
          setLoading(true)
          try {
               const response =
                    await JswBudgetSourceAPIService.getJswBudgetSourceData(
                         keycloak,
                         SITE_ID,
                         AOP_YEAR,
                    )
               const data = response?.data || []
               const formattedData = data?.map((item, index) => ({
                    ...item,
                    unit: item.unit || item.Unit || '',
                    uom: item.uom || item.UOM || '',
                    UOM: item.UOM || item.uom || '',
                    remarks: item.remarks || '',
                    id: item?.id || index + 1,
                    isEditable: true,
               }))
               setRows(formattedData)
               setOriginalRows(formattedData)
          } catch (error) {
               console.error('Error fetching jw budget data:', error)
               setSnackbarOpen(true)
               setSnackbarData({
                    message: 'Error fetching data',
                    severity: 'error',
               })
               setRows([])
               setOriginalRows([])
          } finally {
               setLoading(false)
          }
     }

     const saveChanges = async () => {
          setLoading(true)

          const modifiedData = Object.values(modifiedCells)
          if (modifiedData.length === 0) {
               setSnackbarOpen(true)
               setSnackbarData({
                    message: 'No Records to Save!',
                    severity: 'info',
               })
               setLoading(false)
               return
          }

          const data = modifiedData.filter((row) => row.inEdit)
          if (data.length === 0) {
               setSnackbarOpen(true)
               setSnackbarData({
                    message: 'No Records to Save!',
                    severity: 'info',
                })
               setLoading(false)
               return
          }

          const payloadData = data.map((row) => {
               const matched = unitDropdown.find(
                    (opt) => opt.value === row.unit || opt.label === row.unit || opt.id === row.unit || opt.unitId === row.unitId
               )
               return {
                    ...row,
                    id: row.id || matched?.unitId || matched?.id,
                    uom: row.uom || row.UOM || matched?.uom || '',
                    jan:row.jan || 0,
                    feb:row.feb || 0,
                    mar:row.mar || 0,
                    apr:row.apr || 0,
                    may:row.may || 0,
                    jun:row.jun || 0,
                    jul:row.jul || 0,
                    aug:row.aug || 0,
                    sep:row.sep || 0,
                    oct:row.oct || 0,
                    nov:row.nov || 0,
                    dec:row.dec || 0,   
               }
          })

          try {
               await JswBudgetSourceAPIService.saveJswBudgetSourceData(
                    payloadData,
                    keycloak,
                    AOP_YEAR,
               )

               setSnackbarOpen(true)
               setSnackbarData({
                    message: 'Data saved successfully!',
                    severity: 'success',
               })
               setModifiedCells({})
               setOriginalRows([])
               setRows([])
               isFetchedRef.current = false
               await fetchData()
          } catch (error) {
               
               console.error('Error saving jw budget data:', error)
               setSnackbarOpen(true)
               setSnackbarData({
                    message: 'Error saving data!',
                    severity: 'error',
               })
          } finally {
               setLoading(false)
          }
     }

     const handleRemarkCellClick = (row) => {
          setCurrentRemark(row.remarks || '')
          setCurrentRowId(row.id)
          setRemarkDialogOpen(true)
     }

     const permissions = {
          showAction: true,
          addButton: true,
          deleteButton: true,
          editButton: true,
          saveBtn: true,
          allAction: true,
          showExport: false,
          showImport: false,
          showCalculate: false,
          ExcelName: `JwBudget_${AOP_YEAR}`,
          showTitleNameBusiness: true,
          showTitle: true,
          titleName: 'Jw Budget',
          showDropdown: false,
          remarksEditable: true,
     }

     return (
          <Box>
               <LoaderBackdrop open={!!loading} />

               <AdvanceKendoTable
                    columns={columns}
                    rows={rows}
                    setRows={setRows}
                    modifiedCells={modifiedCells}
                    setModifiedCells={setModifiedCells}
                    customItemChange={handleCustomItemChange}
                    title={permissions.showTitle ? permissions.titleName : ''}
                    permissions={permissions}
                    handleRemarkCellClick={handleRemarkCellClick}
                    remarkDialogOpen={remarkDialogOpen}
                    setRemarkDialogOpen={setRemarkDialogOpen}
                    currentRemark={currentRemark}
                    setCurrentRemark={setCurrentRemark}
                    currentRowId={currentRowId}
                    setCurrentRowId={() => { }}
                    saveChanges={saveChanges}
                    snackbarData={snackbarData}
                    snackbarOpen={snackbarOpen}
                    setSnackbarOpen={setSnackbarOpen}
                    setSnackbarData={setSnackbarData}
                    //customHeight={70}
               />
          </Box>
     )
}

export default JwBudgetScreen
