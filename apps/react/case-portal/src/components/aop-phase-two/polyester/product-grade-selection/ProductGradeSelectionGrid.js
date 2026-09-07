import React, { useCallback, useEffect, useMemo, useState, useRef } from 'react'
import { Box } from '@mui/material'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { useSelector } from 'react-redux'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { getRoleName } from 'services/role-service'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import { ProductGradeSelectionApiService } from '../../services/polyester/productGradeSelectionApiService'

export default function ProductGradeSelectionGrid({ onSaveSuccess }) {
    const keycloak = useSession()
    const dataGridStore = useSelector((state) => state.dataGridStore)
    const { year, plantObject, oldYear, isReleased, screenTitle } = dataGridStore
    const AOP_YEAR = year?.selectedYear
    const PLANT_ID = plantObject?.id
    const IS_OLD_YEAR = oldYear?.oldYear
    const IS_RELEASED = isReleased

    const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
    const EXCEL_NAME = generateExcelName(dataGridStore, 'Product_Grade_Selection')

    const [rows, setRows] = useState([])
    const [loading, setLoading] = useState(false)
    const originalRemarkRef = useRef('')

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


    const fetchData = useCallback(async () => {
        setModifiedCells({})
        setLoading(true)
        try {
            const resp = await ProductGradeSelectionApiService.getGradeSelection(keycloak, PLANT_ID, AOP_YEAR)
            const mappedData = (resp?.data || []).map((item, index) => ({
                id: item.gradeId || index,
                gradeId: item.gradeId,
                normParameterId: item.normParameterId,
                particular: item.materialName,
                status: item.isSelected,
                remark: item.remarks,
                inEdit: false
            }))
            
            originalRemarkRef.current = mappedData[0]?.remark || ''
            setRows(mappedData)
        } catch (e) {
            console.error('Error fetching data:', e)
            setRows([])
        } finally {
            setLoading(false)
        }
    }, [keycloak, PLANT_ID, AOP_YEAR])

    useEffect(() => {
        fetchData()
    }, [fetchData])

    const customItemChange = useCallback((e, setRowsFn, setModifiedCellsFn) => {
        // Multi-selection is standard behavior, no override needed
    }, [])

    const saveChanges = useCallback(async () => {
        try {
            setLoading(true)
            
            const hasDataChanges = Object.keys(modifiedCells).some(
                (rowId) => Object.keys(modifiedCells[rowId]).some((key) => key !== 'remark')
            )

            // Extract the unified remark from the first row (or default empty)
            const currentRemark = rows[0]?.remark || ''

            if (hasDataChanges) {
                if (!currentRemark.trim()) {
                    setSnackbarOpen(true)
                    setSnackbarData({
                        message: 'Please fill the Remarks when data is modified.',
                        severity: 'error',
                    })
                    setLoading(false)
                    return
                }

                if (currentRemark === originalRemarkRef.current) {
                    setSnackbarOpen(true)
                    setSnackbarData({
                        message: 'Remarks must be different from the original when data is modified.',
                        severity: 'error',
                    })
                    setLoading(false)
                    return
                }
            }
            
            const payload = rows.map((row) => ({
                normParameterId: row.normParameterId,
                gradeId: row.gradeId,
                materialName: row.particular,
                isSelected: row.status,
                remarks: currentRemark,
            }))
            
            const response = await ProductGradeSelectionApiService.saveGradeSelection(keycloak, payload, AOP_YEAR)
            
            if (response?.code === 200) {
                setSnackbarOpen(true)
                setSnackbarData({
                    message: 'Saved Successfully!',
                    severity: 'success',
                })
                setModifiedCells({})
                originalRemarkRef.current = currentRemark
                fetchData()
                if (onSaveSuccess) {
                    await onSaveSuccess()
                }
            } else {
                throw new Error(response?.message || 'Failed to save')
            }
        } catch (e) {
            console.error('Error saving data:', e)
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

    const columns = useMemo(
        () => [
            {
                field: 'particular',
                title: 'Product Grades',
                editable: false,
                minWidth: 200,
                locked: true,
            },
            {
                field: 'status',
                title: 'Selection',
                editable: true,
                type: 'checkbox',
                minWidth: 100,
            },
            {
                field: 'remark',
                title: 'Remarks',
                editable: true,
                type: 'mergedCells',
                minWidth: 150,
                cell: (props) => {
                    const { dataItem, field, rowType, ...kendoTdProps } = props
                    
                    const rowIndex = rows.findIndex(r => r.id === dataItem.id)

                    if (rowIndex === 0) {
                        const rawValue = dataItem.remark
                        const isEdited = modifiedCells?.[dataItem.id]?.remark !== undefined
                        return (
                            <td
                                {...kendoTdProps}
                                rowSpan={rows.length}
                                className={`${kendoTdProps.className || ''} ${isEdited ? 'edited-cell' : 'non-edited-cell'}`}
                                style={{
                                    ...kendoTdProps.style,
                                    verticalAlign: 'middle',
                                    textAlign: 'center',
                                    cursor: 'pointer',
                                    borderLeft: '1px solid rgba(0, 0, 0, 0.08)',
                                    backgroundColor: '#fff',
                                    color: rawValue ? 'inherit' : 'gray',
                                }}
                                onClick={(e) => {
                                    e.preventDefault()
                                    e.stopPropagation()
                                }}
                                onDoubleClick={(e) => {
                                    e.preventDefault()
                                    e.stopPropagation()
                                    handleRemarkCellClick(dataItem)
                                }}
                            >
                                {rawValue || 'Add'}
                            </td>
                        )
                    }
                    return null
                }
            },
        ],
        [rows, handleRemarkCellClick, modifiedCells],
    )

    const permissions = useMemo(
        () => ({
            allAction: true,
            saveBtn: !READ_ONLY,
            alwaysEnableSave: true,
            showTitleNameBusiness: true,
            showTitle: true,
            titleName: 'Product Grade Selection',
            downloadExcelBtnFromUI: false,
            showCalculate: false,
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
                // groupBy='groupName'
                customItemChange={customItemChange}
                snackbarOpen={snackbarOpen}
                setSnackbarOpen={setSnackbarOpen}
                snackbarData={snackbarData}
                setSnackbarData={setSnackbarData}
            />
        </Box>
    )
}
