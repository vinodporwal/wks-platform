import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Box } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from './index'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { useSelector } from 'react-redux'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { PlantAopReportApiService } from 'services/plant-aop-report-api-service'
import { validateFields } from 'utils/validationUtils'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'

export default function GroupMaterial({ onSaveSuccess }) {
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

    const [rows, setRows] = useState()
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

    const columns = useMemo(
        () => [
            {
                field: 'name',
                title: 'Name',
                editable: false,
                minWidth: 200,
                locked: true
            },
            {
                field: 'uom',
                title: 'UOM',
                editable: false,
                minWidth: 170,
                locked: true
            },
            {
                field: 'apl',
                title: headerMap[4] || 'Apr',
                editable: true,
                type: 'checkbox',
                minWidth: 100,
            },
            {
                field: 'may',
                title: headerMap[5] || 'May',
                editable: true,
                type: 'checkbox',
                minWidth: 100,
            },
            {
                field: 'jun',
                title: headerMap[6] || 'Jun',
                editable: true,
                type: 'checkbox',
                minWidth: 100,
            },
            {
                field: 'jul',
                title: headerMap[7] || 'Jul',
                editable: true,
                type: 'checkbox',
                minWidth: 100,
            },
            {
                field: 'aug',
                title: headerMap[8] || 'Aug',
                editable: true,
                type: 'checkbox',
                minWidth: 100,
            },
            {
                field: 'sep',
                title: headerMap[9] || 'Sep',
                editable: true,
                type: 'checkbox',
                minWidth: 100,
            },
            {
                field: 'oct',
                title: headerMap[10] || 'Oct',
                editable: true,
                type: 'checkbox',
                minWidth: 100,
            },
            {
                field: 'nov',
                title: headerMap[11] || 'Nov',
                editable: true,
                type: 'checkbox',
                minWidth: 100,
            },
            {
                field: 'dec',
                title: headerMap[12] || 'Dec',
                editable: true,
                type: 'checkbox',
                minWidth: 100,
            },
            {
                field: 'jan',
                title: headerMap[1] || 'Jan',
                editable: true,
                type: 'checkbox',
                minWidth: 100,
            },
            {
                field: 'feb',
                title: headerMap[2] || 'Feb',
                editable: true,
                type: 'checkbox',
                minWidth: 100,
            },
            {
                field: 'mar',
                title: headerMap[3] || 'Mar',
                editable: true,
                type: 'checkbox',
                minWidth: 100,
            },
            {
                field: 'groupName',
                title: 'Group Name',
                hidden: true,
                isVisible: false
            },
        ],
        [headerMap, FORMATE_DECIMAL],
    )

    const parseBoolean = (val) => {
        if (val === true || val === 1 || val === '1' || val === 'true' || val === 'TRUE' || val === 'True') {
            return true
        }
        return false
    }

    const fetchData = useCallback(async () => {
        if (!PLANT_ID || !AOP_YEAR) {
            setRows()
            return
        }
        setModifiedCells({})
        setLoading(true)
        try {
            const res = await PlantAopReportApiService.getGroupMaterialDetails(
                keycloak,
                PLANT_ID,
                AOP_YEAR,
            )

            if (res?.code === 200) {
                const rawData = Array.isArray(res?.data)
                    ? res.data
                    : res?.data?.Data || []
                const mapped = rawData.map((item, idx) => {
                    const aprVal = item.apr !== undefined ? item.apr : (item.apl !== undefined ? item.apl : (item.Apr !== undefined ? item.Apr : item.April))
                    return {
                        ...item,
                        id: item.id || item.Id || idx + 1,
                        idFromApi: item.id || item.Id || idx + 1,
                        name: item.name || item.Name || item.displayName || item.DisplayName,
                        particular: item.displayName || item.name || item.DisplayName || item.Name,
                        uom: item.uom || item.uom || item.uom || item.uom,
                        status: parseBoolean(item.status !== undefined ? item.status : item.Status),
                        groupName: item.groupName || item.normParameterType || item.GroupName || item.NormParameterType,
                        apl: parseBoolean(aprVal),
                        may: parseBoolean(item.may !== undefined ? item.may : item.May),
                        jun: parseBoolean(item.jun !== undefined ? item.jun : (item.Jun !== undefined ? item.Jun : item.June)),
                        jul: parseBoolean(item.jul !== undefined ? item.jul : (item.Jul !== undefined ? item.Jul : item.July)),
                        aug: parseBoolean(item.aug !== undefined ? item.aug : (item.Aug !== undefined ? item.Aug : item.August)),
                        sep: parseBoolean(item.sep !== undefined ? item.sep : (item.Sep !== undefined ? item.Sep : item.September)),
                        oct: parseBoolean(item.oct !== undefined ? item.oct : (item.Oct !== undefined ? item.Oct : item.October)),
                        nov: parseBoolean(item.nov !== undefined ? item.nov : (item.Nov !== undefined ? item.Nov : item.November)),
                        dec: parseBoolean(item.dec !== undefined ? item.dec : (item.Dec !== undefined ? item.Dec : item.December)),
                        jan: parseBoolean(item.jan !== undefined ? item.jan : (item.Jan !== undefined ? item.Jan : item.January)),
                        feb: parseBoolean(item.feb !== undefined ? item.feb : (item.Feb !== undefined ? item.Feb : item.February)),
                        mar: parseBoolean(item.mar !== undefined ? item.mar : (item.Mar !== undefined ? item.Mar : item.March)),
                        isEditable: item.isEditable !== undefined ? parseBoolean(item.isEditable) : true,
                    }
                })
                setRows(mapped || [])
            } else {
                setRows()
            }
        } catch (e) {
            console.log(e)
            setRows()
        } finally {
            setLoading(false)
        }
    }, [keycloak, PLANT_ID, AOP_YEAR])

    useEffect(() => {
        fetchData()
    }, [fetchData])

    const monthFields = useMemo(
        () => [
            'apl',
            'may',
            'jun',
            'jul',
            'aug',
            'sep',
            'oct',
            'nov',
            'dec',
            'jan',
            'feb',
            'mar',
            'status',
        ],
        [],
    )

    const customItemChange = useCallback(
        (e, tools) => {
            const { dataItem, field, value } = e

            if (monthFields.includes(field)) {
                const currentGroup = dataItem.groupName || dataItem.normParameterType
                const itemId = dataItem.id

                if (value === true) {
                    // 1. Update the rows state: uncheck this specific month field for all other rows in the same group
                    setRows((prevRows) =>
                        prevRows.map((r) => {
                            const rGroup = r.groupName || r.normParameterType
                            if (rGroup === currentGroup && r.id !== itemId) {
                                return { ...r, [field]: false }
                            }
                            return r
                        }),
                    )

                    // 2. Update modifiedCells for all affected items in this group
                    tools.setModifiedCells((prev) => {
                        const next = { ...prev }
                        const allRows = tools?.rows || []
                        allRows.forEach((r) => {
                            const rGroup = r.groupName || r.normParameterType
                            if (rGroup === currentGroup) {
                                if (r.id === itemId) {
                                    next[r.id] = {
                                        ...(next[r.id] || r),
                                        ...dataItem,
                                        [field]: true,
                                    }
                                } else {
                                    next[r.id] = {
                                        ...(next[r.id] || r),
                                        [field]: false,
                                    }
                                }
                            }
                        })
                        return next
                    })
                } else {
                    // When unchecked, record this row's false value in modifiedCells
                    tools.setModifiedCells((prev) => ({
                        ...prev,
                        [itemId]: {
                            ...(prev[itemId] || dataItem),
                            [field]: false,
                        },
                    }))
                }
            }
        },
        [monthFields],
    )

    const saveChanges = useCallback(async () => {
        try {
            setLoading(true)
            const data = Object.keys(modifiedCells).length > 0
                ? Object.values(modifiedCells)
                : rows

            if (!data.length) {
                setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
                setSnackbarOpen(true)
                return
            }

            const payload = data.map((item) => {
                const aprValue = item.apl !== undefined ? item.apl : (item.apr !== undefined ? item.apr : item.Apr)
                return {
                    ...item,
                    id: item.idFromApi || item.id,
                    name: item.name,
                    displayName: item.displayName || item.name,
                    uom: item.uom,
                    status: !!item.status,
                    apl: !!item.apl,
                    apr: !!aprValue,
                    may: !!item.may,
                    jun: !!item.jun,
                    jul: !!item.jul,
                    aug: !!item.aug,
                    sep: !!item.sep,
                    oct: !!item.oct,
                    nov: !!item.nov,
                    dec: !!item.dec,
                    jan: !!item.jan,
                    feb: !!item.feb,
                    mar: !!item.mar,
                    dependantAttributeId: item.dependantAttributeId || null,
                    normParameterTypeFkId: item.normParameterTypeFkId || item.normParameterFKId || null,
                    normParameterFKId: item.normParameterFKId || item.normParameterTypeFkId || item.idFromApi || item.id || null,
                    plantFkId: item.plantFkId || item.plantFKId || PLANT_ID,
                    plantFKId: item.plantFKId || item.plantFkId || PLANT_ID,
                    isEditable: item.isEditable !== undefined ? item.isEditable : true,
                    uom: item.uom || item.uom,
                    normParameterType: item.groupName || item.normParameterType,
                    groupName: item.groupName || item.normParameterType,
                    aopYear: item.aopYear || AOP_YEAR,
                }
            })

            let response
            if (typeof PlantAopReportApiService.saveGroupMaterialDetails === 'function') {
                response = await PlantAopReportApiService.saveGroupMaterialDetails(keycloak, AOP_YEAR, payload)
            } else {
                response = await PlantAopReportApiService.saveGroupedSelection(keycloak, payload)
            }

            if (response?.code === 200) {
                setSnackbarOpen(true)
                setSnackbarData({
                    message: response?.message || 'Saved Successfully!',
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
            console.error('Error saving group material details:', e)
            setSnackbarOpen(true)
            setSnackbarData({
                message: 'Error while saving!',
                severity: 'error',
            })
        } finally {
            setLoading(false)
        }
    }, [modifiedCells, rows, keycloak, PLANT_ID, fetchData, AOP_YEAR])

    const handleCalculate = () => { }

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
            saveBtn: true,
            alwaysEnableSave: true,
            showTitleNameBusiness: true,
            titleName: 'Group Material',
            adjustedPermissions: true,
            downloadExcelBtn: false,
            uploadExcelBtn: false,
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
                title='Group Material'
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
                handleCalculate={handleCalculate}
                handleRemarkCellClick={handleRemarkCellClick}
                permissions={adjustedPermissionsC}
                groupBy='groupName'
                customItemChange={customItemChange}
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
