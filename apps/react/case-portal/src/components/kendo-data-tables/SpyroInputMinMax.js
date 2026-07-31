import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import KendoDataTablesReports from 'components/kendo-data-tables/index-reports'
import { Backdrop, Box, CircularProgress } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useSession } from 'SessionStoreContext'
import { DataService } from 'services/DataService'
import { SiteReportDataService } from 'services/SiteReportDataService'
import KendoDataTables from './index'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { useSelector } from 'react-redux'
import { add } from 'lodash'
import { validateFields } from 'utils/validationUtils'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { CrackerReportsApiDataService } from 'services/cracker-reports-api-service'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
export default function SpyroInputMinMax() {
     const keycloak = useSession()
     const dataGridStore = useSelector((state) => state.dataGridStore)
     const {
          verticalChange,
          yearChanged,
          oldYear,
          plantID,
          plantObject,
          siteObject,
          verticalObject,
          year,
          screenTitle,
     } = dataGridStore

     const PLANT_ID = plantObject?.id
     const SITE_ID = siteObject?.id
     const VERTICAL_ID = verticalObject?.id

     const SCREEN_NAME = screenTitle?.title
     const AOP_YEAR = year?.selectedYear
     const thisYear = AOP_YEAR
     const [rows, setRows] = useState([])
     const [loading, setLoading] = useState(false)

     const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
     const [currentRemark, setCurrentRemark] = useState('')
     const [currentRowId, setCurrentRowId] = useState(null)
     const [modifiedCells, setModifiedCells] = useState({})
     const [enableSaveAddBtn, setEnableSaveAddBtn] = useState(false)
     const isOldYear = false
     const IS_OLD_YEAR = oldYear?.oldYear
     const vertName = verticalChange?.selectedVertical
     const lowerVertName = vertName?.toLowerCase()
     const ValueFormate = ValueFormatterProduction()
     const headerMap = generateHeaderNames(AOP_YEAR)
     const [snackbarData, setSnackbarData] = useState({
          message: '',
          severity: 'info',
     })
     const [snackbarOpen, setSnackbarOpen] = useState(false)

     const unsavedChangesRef = useRef({ unsavedRows: {}, rowsBeforeChange: {} })

     const monthConfigs = [
          { key: 'apr', headerIdx: 4, fallback: 'April' },
          { key: 'may', headerIdx: 5, fallback: 'May' },
          { key: 'jun', headerIdx: 6, fallback: 'June' },
          { key: 'jul', headerIdx: 7, fallback: 'July' },
          { key: 'aug', headerIdx: 8, fallback: 'August' },
          { key: 'sep', headerIdx: 9, fallback: 'September' },
          { key: 'oct', headerIdx: 10, fallback: 'October' },
          { key: 'nov', headerIdx: 11, fallback: 'November' },
          { key: 'dec', headerIdx: 12, fallback: 'December' },
          { key: 'jan', headerIdx: 1, fallback: 'January' },
          { key: 'feb', headerIdx: 2, fallback: 'February' },
          { key: 'mar', headerIdx: 3, fallback: 'March' },
     ]

     const spyroMinMaxColumns = useMemo(() => {
          const monthCols = monthConfigs.map((m) => ({
               title: headerMap?.[m.headerIdx] || m.fallback,
               children: [
                    {
                         field: `${m.key}Min`,
                         title: 'Min',
                         width: 30,
                         editable: true,
                         format: ValueFormate,
                         type: 'number',
                         filter: false,
                    },
                    {
                         field: `${m.key}Max`,
                         title: 'Max',
                         width: 30,
                         editable: true,
                         format: ValueFormate,
                         type: 'number',
                         filter: false,
                    },
               ],
          }))

          return [
               {
                    field: 'idMax',
                    title: 'IDMax',
                    editable: false,
                    hidden: true,
               },
               {
                    field: 'idMin',
                    title: 'IDMin',
                    editable: false,
                    hidden: true,
               },
               {
                    field: 'displayName',
                    title: 'Particulars',
                    width: 110,
                    editable: false,
                    locked: true,
               },
               {
                    field: 'uom',
                    title: 'UOM',
                    width: 40,
                    editable: false,
                    locked: true,
               },
               ...monthCols,
               {
                    title: 'Weighted Average',
                    children: [
                         {
                              field: 'minWeightAverage',
                              title: 'min',
                              width: 30,
                              editable: false,
                              type: 'number',
                              format: ValueFormate,
                              isDisabled: false,
                              filter: false,
                         },
                         {
                              field: 'maxWeightAverage',
                              title: 'max',
                              width: 30,
                              editable: false,
                              type: 'number',
                              format: ValueFormate,
                              isDisabled: false,
                              filter: false,
                         },
                    ],
               },
          ]

     }, [headerMap])

     const transformMinMaxData = (dataList) => {
          if (!Array.isArray(dataList) || dataList.length === 0) return []

          const groups = {}

          dataList.forEach((item, index) => {
               if (item.aprMin !== undefined && item.aprMax !== undefined) {
                    const key = item.id || item.displayName || `row_${index}`
                    groups[key] = { ...item }
                    return
               }

               let baseName = (item.displayName || '')
                    .replace(/^(Max|Min)\s*/i, '')
                    .trim()

               if (!baseName) {
                    baseName = 'Furnaces'
               }

               if (!groups[baseName]) {
                    groups[baseName] = {
                         id: item.idMax || item.idMin || item.id || `row_${index + 1}`,
                         displayName: baseName,
                         uom: item.uom || '',
                    }
               }

               Object.assign(groups[baseName], item)
               groups[baseName].displayName = baseName
          })

          const monthsKeys = [
               'apr', 'may', 'jun', 'jul', 'aug', 'sep',
               'oct', 'nov', 'dec', 'jan', 'feb', 'mar'
          ]

          return Object.values(groups).map((row, idx) => {
               const cleanRow = {
                    ...row,
                    id: row.idMax || row.idMin || row.id || idx + 1,
                    isEditable: true,
               }

               monthsKeys.forEach((m) => {
                    const minKey = `${m}Min`
                    const maxKey = `${m}Max`

                    if (cleanRow[minKey] !== undefined) {
                         const val = cleanRow[minKey]
                         cleanRow[minKey] = (val === null || val === undefined || val === '' || Number(val) === 0 || isNaN(Number(val))) ? null : Number(val)
                    }
                    if (cleanRow[maxKey] !== undefined) {
                         const val = cleanRow[maxKey]
                         cleanRow[maxKey] = (val === null || val === undefined || val === '' || Number(val) === 0 || isNaN(Number(val))) ? null : Number(val)
                    }
               })

               if (cleanRow.minWeightAverage !== undefined) {
                    const val = cleanRow.minWeightAverage
                    cleanRow.minWeightAverage = (val === null || val === undefined || val === '' || Number(val) === 0 || isNaN(Number(val))) ? null : Number(val)
               }
               if (cleanRow.maxWeightAverage !== undefined) {
                    const val = cleanRow.maxWeightAverage
                    cleanRow.maxWeightAverage = (val === null || val === undefined || val === '' || Number(val) === 0 || isNaN(Number(val))) ? null : Number(val)
               }

               return cleanRow
          })
     }

     const fetchData = useCallback(async () => {
          if (!PLANT_ID || !AOP_YEAR) return
          setLoading(true)
          try {
               const res = await CrackerReportsApiDataService.getSpyroInputMinMaxData(
                    keycloak,
                    VERTICAL_ID,
                    SITE_ID,
                    PLANT_ID,
                    AOP_YEAR,
                    'Furnace'
               )

               if (res?.code === 200) {
                    const rawList = Array.isArray(res?.data.resultList)
                         ? res.data.resultList
                         : []
                    const mapped = transformMinMaxData(rawList)
                    setRows(mapped)
               } else {
                    setRows([])
               }
          } catch (err) {
               console.error('fetchData error', err)
               setRows([])
          } finally {
               setLoading(false)
          }
     }, [keycloak, yearChanged, plantID, AOP_YEAR, PLANT_ID, SITE_ID, VERTICAL_ID])

     useEffect(() => {
          fetchData()
     }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak])

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

               const payload = data.map((row) => {
                    const cleanRow = { ...row }
                    const monthsKeys = [
                         'apr', 'may', 'jun', 'jul', 'aug', 'sep',
                         'oct', 'nov', 'dec', 'jan', 'feb', 'mar'
                    ]
                    monthsKeys.forEach((m) => {
                         const minKey = `${m}Min`
                         const maxKey = `${m}Max`
                         if (cleanRow[minKey] === '' || cleanRow[minKey] === undefined) {
                              cleanRow[minKey] = null
                         }
                         if (cleanRow[maxKey] === '' || cleanRow[maxKey] === undefined) {
                              cleanRow[maxKey] = null
                         }
                    })
                    return cleanRow
               })

               const response = await CrackerReportsApiDataService.saveSpyroInputMinMaxData(
                    keycloak,
                    PLANT_ID,
                    AOP_YEAR,
                    payload,
               )

               const isSuccess =
                    response === null ||
                    response === undefined ||
                    Array.isArray(response) ||
                    response?.code === 200 ||
                    response?.status === 200 ||
                    response?.message === 'Success' ||
                    response?.status === 'success' ||
                    (response && response.ok !== false && !response.error && response.code !== 500);

               if (isSuccess) {
                    setSnackbarOpen(true)
                    setSnackbarData({
                         message: 'Saved Successfully!',
                         severity: 'success',
                    })
                    setModifiedCells({})
                    fetchData()
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
     }, [modifiedCells, keycloak, PLANT_ID, AOP_YEAR, fetchData])

     const handleRemarkCellClick = useCallback((row) => {
          setCurrentRemark(row.remarks || '')
          setCurrentRowId(row.id)
          setRemarkDialogOpen(true)
     }, [])

     const getAdjustedPermissions = (permissions, isOldYear) => {
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

     const adjustedPermissions = getAdjustedPermissions(
          {
               allAction: true,
               showTitleNameBusiness: true,
               adjustedPermissions: true,
               ExcelName: `${lowerVertName}_Spyro_Input_Min_Max_${AOP_YEAR}`,
               saveBtn: true,
          },
          isOldYear,
     )

     return (
          <Box>
               <LoaderBackdrop open={!!loading} />

               <KendoDataTables
                    columns={spyroMinMaxColumns}
                    rows={rows}
                    setRows={setRows}
                    modifiedCells={modifiedCells}
                    setModifiedCells={setModifiedCells}
                    remarkDialogOpen={remarkDialogOpen}
                    setRemarkDialogOpen={setRemarkDialogOpen}
                    currentRemark={currentRemark}
                    setCurrentRemark={setCurrentRemark}
                    currentRowId={currentRowId}
                    setCurrentRowId={setCurrentRowId}
                    enableSaveAddBtn={enableSaveAddBtn}
                    saveChanges={saveChanges}
                    handleRemarkCellClick={handleRemarkCellClick}
                    permissions={adjustedPermissions}
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
