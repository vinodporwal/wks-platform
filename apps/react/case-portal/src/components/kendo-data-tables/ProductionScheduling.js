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

const ProductionScheduling = ({ permissions }) => {
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
    const [rowsTransaction, setRowsTransaction] = useState()
    const [columnsTransaction, setColumnsTransaction] = useState([])
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
            field: 'batchPerDay',
            title: 'Batch Per Day',
            editable: true,
            type: 'integerNumberOnly',
            widthT: 300,
            minWidth: 120,
        },
        {
            field: 'productionPerBatch',
            title: 'Production Per Batch',
            editable: true,
            type: 'number',
            format: valueFormat,
            widthT: 300,
            minWidth: 120,
        },
        {
            field: 'sdWashAfterBatch',
            title: 'SD Wash After Batch',
            editable: true,
            type: 'integerNumberOnly',
            widthT: 300,
            minWidth: 120,
        },
        {
            field: 'sdFlushAfterBatch',
            title: 'SD Flush After Batch',
            editable: true,
            type: 'integerNumberOnly',
            align: 'left',
            headerAlign: 'left',
            minWidth: 120,
        },
        {
            field: 'sdWashHr',
            title: 'SD Wash Hr',
            editable: true,
            type: 'integerNumberOnly',
            minWidth: 120,
        },
        {
            field: 'sdFlushHr',
            title: 'SD Flush Hr',
            editable: true,
            type: 'integerNumberOnly',
            widthT: 300,
            minWidth: 120,
        },
        {
            field: 'quarterlySDHr',
            title: 'Quarterly SD Hr',
            editable: true,
            type: 'integerNumberOnly',
            widthT: 300,
            minWidth: 120,
        },
    ]

    const fetchData = useCallback(async () => {
        if (!PLANT_ID || !AOP_YEAR) return
        setModifiedCells({})
        setLoading(true)
        try {
            const res = await ProductionSchedulingApiService.getProductionSchedulingData(
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

    const fetchDataTransaction = useCallback(async () => {
        if (!PLANT_ID || !AOP_YEAR) return
        setLoading(true)
        try {
            const res = await ProductionSchedulingApiService.getProductionSchedulingTransactionData(
                keycloak,
                PLANT_ID,
                AOP_YEAR,
            )

            if (res?.code === 200) {
                const cols = res?.data?.column || res?.data?.columns
                if (cols && Array.isArray(cols)) {
                    const dynamicCols = cols.map((col) => ({
                        field: col.field,
                        title: col.title || col.headerName || col.field,
                        editable: col.editable ?? true,
                        widthT: col.widthT ?? 300,
                        minWidth: col.minWidth ?? 120,
                        align: col.align || (col.type === 'number' ? 'right' : 'left'),
                        type: col.type || 'string',
                    }))
                    setColumnsTransaction(dynamicCols)
                }

                const dataList = res?.data?.data || []
                setCalculationObject(res?.data?.aopCalculation)
                const mapped = dataList.map((item, index) => ({
                    ...item,
                    id: index,
                    idFromApi: item.id || null,
                    isEditable: false,
                }))
                setRowsTransaction(mapped)
            } else {
                setRowsTransaction([])
            }
        } catch (err) {
            console.error('Error fetching data:', err)
            setRowsTransaction([])
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

            const requiredFields = [
                'batchPerDay',
                'productionPerBatch',
                'sdWashAfterBatch',
                'sdFlushAfterBatch',
                'sdWashHr',
                'sdFlushHr',
                'quarterlySDHr',
            ]

            const validationMessage = validateFields(data, requiredFields)
            if (validationMessage) {
                setSnackbarOpen(true)
                setSnackbarData({
                    message: validationMessage,
                    severity: 'error',
                })
                setLoading(false)
                return
            }
            var payload = []
            payload = data.map((item) => ({
                id: item.idFromApi || null,
                batchPerDay: item.batchPerDay,
                productionPerBatch: item.productionPerBatch,
                sdWashAfterBatch: item.sdWashAfterBatch,
                sdFlushAfterBatch: item.sdFlushAfterBatch,
                sdWashHr: item.sdWashHr,
                sdFlushHr: item.sdFlushHr,
                quarterlySDHr: item.quarterlySDHr,
                aopYear: AOP_YEAR,
                plantId: PLANT_ID,
            }))

            const response = await ProductionSchedulingApiService.updateProductionSchedulingData(
                PLANT_ID,
                payload,
                keycloak,
                AOP_YEAR,
            )

            if (response) {
                setSnackbarOpen(true)
                setSnackbarData({
                    message: 'Saved Successfully!',
                    severity: 'success',
                })
                setModifiedCells({})
                fetchData()
                fetchDataTransaction()
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
        fetchDataTransaction()
    }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak])

    const handleCalculate = async () => {
        setRows([])
        setRowsTransaction([])
        setLoading(true)
        try {
            const response =
                await ProductionSchedulingApiService.handleCalculateProductionSchedulingData(
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
                await fetchDataTransaction()
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
            showAction: permissions?.showAction ?? true,
            showUnit: permissions?.showUnit ?? false,
            saveWithRemark: permissions?.saveWithRemark ?? true,
            saveBtn: permissions?.saveBtn ?? true,
            customHeight: permissions?.customHeight,
            allAction: true,
            downloadExcelBtn: false,
            showNoteWhileDeleting: false,
            showTitleNameBusiness: true,
            titleName: 'Production Scheduling',

            uploadExcelBtn: false,
        },
        isOldYear,
    )
    const adjustedPermissionsTransaction = getAdjustedPermissions(
        {
            showAction: permissions?.showAction ?? true,
            showUnit: permissions?.showUnit ?? false,
            saveWithRemark: permissions?.saveWithRemark ?? true,
            saveBtn: false,
            customHeight: permissions?.customHeight,
            allAction: true,
            downloadExcelBtn: false,
            downloadExcelBtnFromUI: true,
            ExcelName: `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_${AOP_YEAR}_Production-Scheduling`,
            showNoteWhileDeleting: false,
            showTitleNameBusiness: true,
            uploadExcelBtn: false,
            showCalculate: true,
            showCalculateVisibility: Object.keys(calculationObject || {}).length > 0
                ? true
                : false,
            makePagable: false,
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
            <KendoDataTables
                setRows={setRowsTransaction}
                columns={columnsTransaction}
                rows={rowsTransaction}
                fetchData={fetchDataTransaction}
                snackbarData={snackbarData}
                snackbarOpen={snackbarOpen}
                apiRef={apiRef}
                deleteId={deleteId}
                open1={open1}
                setDeleteId={setDeleteId}
                setOpen1={setOpen1}
                setSnackbarOpen={setSnackbarOpen}
                setSnackbarData={setSnackbarData}
                permissions={adjustedPermissionsTransaction}
                handleCalculate={handleCalculate}
            />

        </div>
    )
}
export default ProductionScheduling
