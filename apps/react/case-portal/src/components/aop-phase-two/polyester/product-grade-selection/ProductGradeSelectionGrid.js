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
                editable: false,
                minWidth: 150,
            },
        ],
        [],
    )

    const fetchData = useCallback(async () => {
        setModifiedCells({})
        setLoading(true)
        try {
            const dummyData = [
                { id: 1, particular: 'Grade 1', status: true, remark: 'FY 2027-28' },
                { id: 2, particular: 'Grade 10', status: true, remark: 'FY 2027-28' },
                { id: 3, particular: 'Grade 2', status: true, remark: 'FY 2027-28' },
                { id: 4, particular: 'Grade 3', status: true, remark: 'FY 2027-28' },
                { id: 5, particular: 'Grade 4', status: true, remark: 'FY 2027-28' },
                { id: 6, particular: 'Grade 5', status: false, remark: 'FY 2026-27' },
                { id: 7, particular: 'Grade 6', status: true, remark: 'FY 2027-28' },
                { id: 8, particular: 'Grade 7', status: true, remark: 'FY 2027-28' },
                { id: 9, particular: 'Grade 8', status: false, remark: 'FY 2026-27' },
                { id: 10, particular: 'Grade 9', status: true, remark: 'FY 2027-28' },
                { id: 11, particular: 'Grade 11', status: true, remark: 'FY 2027-28' },
            ].map((item) => ({ ...item, inEdit: false }))
            
            setRows(dummyData)
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
            // Mock API save for dummy data
            setTimeout(async () => {
                setSnackbarOpen(true)
                setSnackbarData({
                    message: 'Saved Successfully!',
                    severity: 'success',
                })
                setModifiedCells({})
                if (onSaveSuccess) {
                    await onSaveSuccess()
                }
                setLoading(false)
            }, 500)
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

    const permissions = useMemo(
        () => ({
            allAction: true,
            saveBtn: !READ_ONLY,
            alwaysEnableSave: true,
            showTitleNameBusiness: true,
            showTitle: true,
            titleName: 'Product Grade Selection',
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
