import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Box } from '@mui/material'
import moment from '../../../node_modules/moment/moment.js'
import Notification from 'components/Utilities/Notification'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from './index'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { useSelector } from 'react-redux'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { PlantAopReportApiService } from 'services/plant-aop-report-api-service'
import { validateFields } from 'utils/validationUtils'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'

export default function DecokingPlanningNotification() {
     const keycloak = useSession()
     const dataGridStore = useSelector((state) => state.dataGridStore)
     const {
          year,
          verticalChange,
          yearChanged,
          oldYear,
          plantObject,
          siteObject,
     } = dataGridStore
     const AOP_YEAR = year?.selectedYear
     const PLANT_ID = plantObject?.id
     const SITE_NAME_NO_CASE = siteObject?.name?.toLowerCase()
     const PLANT_NAME_NO_CASE = plantObject?.name?.toLowerCase()
     const thisYear = AOP_YEAR

     const [rows, setRows] = useState([])
     const [loading, setLoading] = useState(false)

     const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
     const [currentRemark, setCurrentRemark] = useState('')
     const [currentRowId, setCurrentRowId] = useState(null)
     const [modifiedCells, setModifiedCells] = useState({})
     const [enableSaveAddBtn, setEnableSaveAddBtn] = useState(false)
     const [columns, setColumns] = useState([]);
     const isOldYear = false
     const IS_OLD_YEAR = oldYear?.oldYear
     const vertName = verticalChange?.selectedVertical
     const lowerVertName = vertName?.toLowerCase()

     const headerMap = generateHeaderNames(AOP_YEAR)

     const [snackbarData, setSnackbarData] = useState({
          message: '',
          severity: 'info',
     })
     const [snackbarOpen, setSnackbarOpen] = useState(false)

     const unsavedChangesRef = useRef({ unsavedRows: {}, rowsBeforeChange: {} })

     const oldYearLabel = useMemo(() => {
          if (!thisYear || !thisYear.includes('-')) return ''
          const [start, end] = thisYear.split('-').map(Number)
          return `${start - 1}-${(end - 1).toString().slice(-2)}`
     }, [thisYear])

     const FORMATE_DECIMAL = ValueFormatterProduction()

     const fetchData = useCallback(async () => {
          if (!PLANT_ID || !AOP_YEAR) return;

          setLoading(true);
          setModifiedCells({});

          try {
               const res = await PlantAopReportApiService.GetPlanningNotification(
                    keycloak,
                    PLANT_ID,
                    AOP_YEAR
               );

               if (res?.code === 200) {
                    const apiColumns = res?.data?.columns || [];
                    const apiRows = res?.data?.data || [];

                    const dynamicColumns = apiColumns.map((col) => {
                         let fieldName = col.field;

                         return {
                              field: fieldName,
                              title: col.title,
                              type: col.type,
                              editable: false,
                              minWidth: 100,
                         };
                    });

                    const mappedRows = apiRows.map((row, index) => {
                         const newRow = {
                              id: row.id ?? index,
                              ...row,
                              isEditable: false,
                         };

                         const rawDateVal = row.Date ?? row.aDate;
                         if (rawDateVal) {
                              const parsedDate = moment(rawDateVal, ['MMM D, YYYY', 'MMMM D, YYYY', 'YYYY-MM-DD', 'MM/DD/YYYY']);
                              if (parsedDate.isValid()) {
                                   const formatted = parsedDate.format('YYYY-MM-DD');
                                   newRow.Date = formatted;
                                   newRow.aDate = formatted;
                              } else {
                                   newRow.Date = rawDateVal;
                                   newRow.aDate = rawDateVal;
                              }
                         }

                         return newRow;
                    });

                    setColumns(dynamicColumns);
                    setRows(mappedRows);
               } else {
                    setColumns([]);
                    setRows([]);
               }
          } catch (err) {
               console.log(err);
               setColumns([]);
               setRows([]);
          } finally {
               setLoading(false);
          }
     }, [keycloak, PLANT_ID, AOP_YEAR]);

     useEffect(() => {
          fetchData()
     }, [fetchData])

     const handleRemarkCellClick = useCallback((row) => {
          setCurrentRemark(row.remark || '')
          setCurrentRowId(row.id)
          setRemarkDialogOpen(true)
     }, [])

     const getAdjustedPermissionsC = (permissions, isOldYear) => {
          if (isOldYear != 1) return permissions
          return {
               ...permissions,
               showAction: false,
               addButton: false,
               deleteButton: false,
               editButton: false,
               showUnit: false,
               saveWithRemark: false,
               saveBtn: false,
               isOldYear: isOldYear,
          }
     }

     const adjustedPermissionsC = getAdjustedPermissionsC(
          {
               allAction: true,
               saveBtn: false,
               alwaysEnableSave: true,
               showTitleNameBusiness: true,
               titleName: 'SAD Overlapping',
               adjustedPermissions: true,
               downloadExcelBtn: false,
               uploadExcelBtn: false,
               makePagable: false,
               disablePagination: true,
               ExcelName: `${lowerVertName}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_${AOP_YEAR}_Group_Selection`,
          },
          isOldYear,
     )

     return (
          <Box>
               <LoaderBackdrop open={!!loading} />

               <KendoDataTables
                    rows={rows}
                    setRows={setRows}
                    columns={columns}
                    title='Material Grouped Selection'
                    modifiedCells={modifiedCells}
                    setModifiedCells={setModifiedCells}
                    remarkDialogOpen={remarkDialogOpen}
                    setRemarkDialogOpen={setRemarkDialogOpen}
                    currentRemark={currentRemark}
                    setCurrentRemark={setCurrentRemark}
                    currentRowId={currentRowId}
                    setCurrentRowId={setCurrentRowId}
                    enableSaveAddBtn={enableSaveAddBtn}
                    handleRemarkCellClick={handleRemarkCellClick}
                    permissions={adjustedPermissionsC}
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
