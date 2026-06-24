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

export default function MaterialGroupedSelection() {
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

  const columns = useMemo(
    () => [
      {
        field: 'particular',
        title: 'Particular',
        editable: false,
        minWidth: 150,
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
        editable: true,
        type: 'number',
        minWidth: 100,
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
      },
    ],
    [],
  )

  const fetchData = useCallback(() => {
    setLoading(true)
    const dummyData = [
      {
        id: 1,
        idFromApi: 1,
        particular: 'ISOPentane',
        sapCode: '101',
        value: null,
        status: false,
        groupName: 'Group-1',
        isEditable: true,
      },
      {
        id: 2,
        idFromApi: 2,
        particular: 'IOS_Pantane',
        sapCode: '102',
        value: 5.6,
        status: true,
        groupName: 'Group-1',
        isEditable: true,
      },
      {
        id: 3,
        idFromApi: 3,
        particular: 'ISO-Pantane',
        sapCode: '103',
        value: null,
        status: false,
        groupName: 'Group-1',
        isEditable: true,
      },
      {
        id: 4,
        idFromApi: 4,
        particular: 'Eth',
        sapCode: '201',
        value: null,
        status: false,
        groupName: 'Group-2',
        isEditable: true,
      },
      {
        id: 5,
        idFromApi: 5,
        particular: 'Ethylene',
        sapCode: '202',
        value: 2.458,
        status: true,
        groupName: 'Group-2',
        isEditable: true,
      },
      {
        id: 6,
        idFromApi: 6,
        particular: 'Ethy-101',
        sapCode: '203',
        value: null,
        status: false,
        groupName: 'Group-2',
        isEditable: true,
      },
      {
        id: 7,
        idFromApi: 7,
        particular: 'Power',
        sapCode: '301',
        value: null,
        status: false,
        groupName: 'Group-3',
        isEditable: true,
      },
      {
        id: 8,
        idFromApi: 8,
        particular: 'Power-1',
        sapCode: '302',
        value: 1.026,
        status: true,
        groupName: 'Group-3',
        isEditable: true,
      },
      {
        id: 9,
        idFromApi: 9,
        particular: 'PowerDis',
        sapCode: '303',
        value: null,
        status: false,
        groupName: 'Group-3',
        isEditable: true,
      },
    ]
    setRows(dummyData)
    setLoading(false)
  }, [])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const customItemChange = useCallback((e, tools) => {
    const { dataItem, field, value } = e
    if (field === 'status' && value === true) {
      const currentGroup = dataItem.groupName
      const itemId = dataItem.id
      const allRows = tools?.rows || []

      // 1. Update the rows state: uncheck others in the same group
      setRows((prevRows) =>
        prevRows.map((r) => {
          if (r.groupName === currentGroup && r.id !== itemId) {
            return { ...r, status: false }
          }
          return r
        }),
      )

      // 2. Update modifiedCells for all other items in this group
      tools.setModifiedCells((prev) => {
        const next = { ...prev }
        allRows.forEach((r) => {
          if (r.groupName === currentGroup && r.id !== itemId) {
            next[r.id] = {
              ...(next[r.id] || r),
              status: false,
            }
          }
        })
        return next
      })
    }
  }, [])

  const saveChanges = useCallback(async () => {
    try {
      setLoading(true)
      const data = Object.values(modifiedCells)
      if (!data.length) {
        setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
        setSnackbarOpen(true)
        return
      }

      await new Promise((resolve) => setTimeout(resolve, 500))

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Saved Successfully!',
        severity: 'success',
      })
      setModifiedCells({})
    } catch (e) {
      console.log(e)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error while saving!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells])

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
      showTitleNameBusiness: true,
      titleName: 'Group Selection',
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
        title='Group Selection'
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
