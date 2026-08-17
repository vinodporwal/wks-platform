import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Box } from '@mui/material'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { useSelector } from 'react-redux'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { PlantAopReportApiService } from 'services/plant-aop-report-api-service'
import { getRoleName } from 'services/role-service'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

export default function MaterialGroupedSelectionGrid({ onSaveSuccess }) {
    const keycloak = useSession()
    const dataGridStore = useSelector((state) => state.dataGridStore)
    const { year, plantObject, oldYear, isReleased, screenTitle } = dataGridStore
    const AOP_YEAR = year?.selectedYear
    const PLANT_ID = plantObject?.id
    const IS_OLD_YEAR = oldYear?.oldYear
    const IS_RELEASED = isReleased

    const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
    const EXCEL_NAME = generateExcelName(dataGridStore, 'Group_Selection')

    const [rows, setRows] = useState([])
    const [loading, setLoading] = useState(false)

    const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
    const [currentRemark, setCurrentRemark] = useState('')
    const [currentRowId, setCurrentRowId] = useState(null)
    const [modifiedCells, setModifiedCells] = useState({})

    const [snackbarData, setSnackbarData] = useState({
        message: '',
        severity: 'info',
    })
    const [snackbarOpen, setSnackbarOpen] = useState(false)

    const FORMATE_DECIMAL = ValueFormatterProduction()

    const columns = useMemo(
        () => [
            {
                field: 'particular',
                title: 'Particular',
                editable: false,
                minWidth: 200,
                locked: true,
            },
            {
                field: 'sapCode',
                title: 'SAP Code',
                editable: false,
                minWidth: 100,
            },
            {
                field: 'value',
                title: 'Value',
                editable: false,
                minWidth: 100,
                isEditable: false,
                isDisabled: false,
                type: 'groupedColumn',
                format: FORMATE_DECIMAL,
            },
            {
                field: 'status',
                title: 'Status',
                editable: true,
                type: 'checkbox',
                minWidth: 100,
            },
            {
                field: 'groupName',
                title: 'Group',
                hidden: true,
                isVisible: false,
            },
        ],
        [FORMATE_DECIMAL],
    )

    const fetchData = useCallback(async () => {
        if (!PLANT_ID || !AOP_YEAR) return
        setModifiedCells({})
        setLoading(true)
        try {
            const res = await PlantAopReportApiService.getGroupedSelection(
                keycloak,
                PLANT_ID,
                AOP_YEAR,
            )

            if (res?.code === 200) {
                const rawData = Array.isArray(res?.data)
                    ? res.data
                    : res?.data?.Data || []
                const mapped = rawData.map((item, index) => ({
                    ...item,
                    id: item.id !== undefined && item.id !== null ? item.id : index,
                    idFromApi: item.id,
                    particular: item.displayName || item.name,
                    sapCode: item.sapMaterialCode,
                    value:
                        item.value !== null && item.value !== undefined && item.value !== ''
                            ? parseFloat(item.value)
                            : null,
                    status: !!item.status,
                    groupName: item.normParameterType,
                    isEditable: item.isEditable,
                    originalValueStr: item.value,
                    inEdit: false,
                }))
                setRows(mapped)
            } else {
                setRows([])
            }
        } catch (e) {
            console.error('Error fetching material grouped selection:', e)
            setRows([])
        } finally {
            setLoading(false)
        }
    }, [keycloak, PLANT_ID, AOP_YEAR])

    useEffect(() => {
        fetchData()
    }, [fetchData])

    const customItemChange = useCallback((e, setRowsFn, setModifiedCellsFn) => {
        const { dataItem, field, value } = e
        if (field === 'status' && value === true) {
            const currentGroup = dataItem.groupName
            const itemId = dataItem.id

            setRowsFn((prevRows) => {
                const updatedRows = prevRows.map((r) => {
                    if (r.groupName === currentGroup && r.id !== itemId) {
                        return { ...r, status: false }
                    }
                    return r
                })

                setModifiedCellsFn((prevModified) => {
                    const nextModified = { ...prevModified }
                    prevRows.forEach((r) => {
                        if (r.groupName === currentGroup && r.id !== itemId) {
                            nextModified[r.id] = {
                                ...(nextModified[r.id] || r),
                                status: false,
                            }
                        }
                    })
                    return nextModified
                })

                return updatedRows
            })
        }
    }, [])

    const saveChanges = useCallback(async () => {
        try {
            setLoading(true)
            const data =
                Object.keys(modifiedCells).length > 0
                    ? Object.values(modifiedCells)
                    : rows

            if (!data.length) {
                setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
                setSnackbarOpen(true)
                return
            }

            const payload = data.map((item) => ({
                id: item.idFromApi || item.id,
                name: item.name,
                displayName: item.displayName,
                uom: item.uom,
                value:
                    item.value !== null && item.value !== undefined
                        ? parseFloat(item.value)
                        : null,
                status: !!item.status,
                dependantAttributeId: item.dependantAttributeId || null,
                normParameterTypeFkId: item.normParameterTypeFkId || null,
                plantFkId: item.plantFkId || PLANT_ID,
                isEditable: item.isEditable,
                sapMaterialCode: item.sapCode || item.sapMaterialCode,
                normParameterType: item.groupName || item.normParameterType,
                aopYear: item.aopYear || AOP_YEAR,
            }))

            const response = await PlantAopReportApiService.saveGroupedSelection(
                keycloak,
                payload,
            )

            if (response?.code === 200) {
                setSnackbarOpen(true)
                setSnackbarData({
                    message: 'Saved Successfully!',
                    severity: 'success',
                })
                setModifiedCells({})
                fetchData()
                if (onSaveSuccess) {
                    await onSaveSuccess()
                }
            } else {
                setSnackbarOpen(true)
                setSnackbarData({
                    message: response?.message || 'Save failed!',
                    severity: 'error',
                })
            }
        } catch (e) {
            console.error('Error saving material grouped selection:', e)
            setSnackbarOpen(true)
            setSnackbarData({
                message: 'Error while saving!',
                severity: 'error',
            })
        } finally {
            setLoading(false)
        }
    }, [
        modifiedCells,
        rows,
        keycloak,
        PLANT_ID,
        fetchData,
        AOP_YEAR,
        onSaveSuccess,
    ])

    const handleRemarkCellClick = useCallback(
        (row) => {
            if (READ_ONLY) return
            setCurrentRemark(row.remark || '')
            setCurrentRowId(row.id)
            setRemarkDialogOpen(true)
        },
        [READ_ONLY],
    )

    const permissions = useMemo(
        () => ({
            allAction: true,
            saveBtn: !READ_ONLY,
            alwaysEnableSave: true,
            showTitleNameBusiness: true,
            showTitle: true,
            titleName: 'Material Grouped Selection',
            ExcelName: EXCEL_NAME,
            showAction: false,
            addButton: false,
            deleteButton: false,
            editButton: false,
            downloadExcelBtn: false,
            uploadExcelBtn: false,
        }),
        [READ_ONLY, screenTitle, EXCEL_NAME],
    )

    return (
        <Box>
            <LoaderBackdrop open={!!loading} />

            <AdvanceKendoTable
                rows={rows}
                setRows={setRows}
                columns={columns}
                title={permissions.titleName}
                loading={loading}
                modifiedCells={modifiedCells}
                setModifiedCells={setModifiedCells}
                remarkDialogOpen={remarkDialogOpen}
                setRemarkDialogOpen={setRemarkDialogOpen}
                currentRemark={currentRemark}
                setCurrentRemark={setCurrentRemark}
                currentRowId={currentRowId}
                setCurrentRowId={setCurrentRowId}
                saveChanges={saveChanges}
                handleRemarkCellClick={handleRemarkCellClick}
                permissions={permissions}
                groupBy='groupName'
                customItemChange={customItemChange}
                snackbarOpen={snackbarOpen}
                setSnackbarOpen={setSnackbarOpen}
                snackbarData={snackbarData}
                setSnackbarData={setSnackbarData}
            />
        </Box>
    )
}
