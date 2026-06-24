import { useGridApiRef } from '@mui/x-data-grid'
import React, { useEffect, useState, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { validateFields } from 'utils/validationUtils'
import KendoDataTables from './index'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { OptimizerDataApiService } from 'services/optimizer-api-service'
// DataService / AopTabs / Box imports are no longer needed here —
// tab UI and line fetching now live only in the parent (BudgetOperatingHour)

// NEW: receives lineDetails + tabIndex from the parent instead of
// fetching/owning its own tab state
const CalculatedBusinessProposed = ({
  permissions,
  lineDetails = [],
  tabIndex = 0,
  refreshSignal = 0,
}) => {
  const screenName = 'Calculated Proposed Business Demand'
  const [modifiedCells, setModifiedCells] = React.useState({})
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { yearChanged, oldYear, plantObject, year } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
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
  const [columns, setColumns] = useState([])

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveApi =
    OptimizerDataApiService.saveCalculatedProposedBusinessOptimizerConstant

  const monthKeyMap = {
    Jan: 1,
    Feb: 2,
    Mar: 3,
    Apr: 4,
    May: 5,
    Jun: 6,
    Jul: 7,
    Aug: 8,
    Sep: 9,
    Oct: 10,
    Nov: 11,
    Dec: 12,
  }
  const HIDDEN_FIELDS = ['lineId']

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    const lineId = lineDetails[tabIndex]?.id
    if (!lineId) return // wait until parent's lineDetails has loaded

    setModifiedCells({})
    setLoading(true)
    try {
      const res =
        await OptimizerDataApiService.getCalculatedProposedBusinessOptimizer(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          lineId,
        )

      if (res?.code === 200) {
        const apiColumns = res?.data?.columns || []
        const apiRows = res?.data?.data || []

        const dynamicColumns = apiColumns
          .filter((col) => !HIDDEN_FIELDS.includes(col.field))
          .map((col) => {
            const isMonthColumn = !!monthKeyMap[col.field]
            return {
              field: col.field,
              title: col.title,
              editable: false,
              align: isMonthColumn ? 'right' : 'left',
              headerAlign: isMonthColumn ? 'right' : 'left',
              type: isMonthColumn ? 'number' : col.type,
              format: isMonthColumn ? '{0:#.###}' : undefined,
              minWidth: 100,
            }
          })

        const mappedRows = apiRows.map((item, index) => ({
          ...item,
          id: index + 1,
          isEditable: false,
        }))

        setColumns(dynamicColumns)
        setRows(mappedRows)
      } else {
        setRows([])
        setColumns([])
      }
    } catch (err) {
      console.error('Error fetching data:', err)
      setColumns([])
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [
    keycloak,
    yearChanged,
    PLANT_ID,
    AOP_YEAR,
    READ_ONLY,
    lineDetails,
    tabIndex,
  ])

  const saveChanges = React.useCallback(async () => {
    if (!saveApi) {
      console.error(
        'CalculatedBusinessProposed: saveApi is undefined — check the method name on OptimizerDataApiService',
      )
      return
    }
    try {
      setLoading(true)
      const data = Object.values(modifiedCells)

      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
        return
      }

      const requiredFields = []
      const validationMessage = validateFields(data, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({ message: validationMessage, severity: 'error' })
        setLoading(false)
        return
      }

      const lineId = lineDetails[tabIndex]?.id

      const payload = data.map(({ id, ...valueFields }) => ({
        ...valueFields,
        auditYear: AOP_YEAR,
        lineId,
      }))

      const response = await saveApi(PLANT_ID, payload, keycloak, AOP_YEAR)

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'Saved Successfully!', severity: 'success' })
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
  }, [
    modifiedCells,
    keycloak,
    PLANT_ID,
    AOP_YEAR,
    fetchData,
    lineDetails,
    tabIndex,
  ])

  // Re-fetch whenever the parent's selected tab (line) or lineDetails changes
  useEffect(() => {
    console.log('CHILD EFFECT FIRED', {
      lineDetails,
      tabIndex,
      PLANT_ID,
      AOP_YEAR,
    })
    if (lineDetails.length > 0 && lineDetails[tabIndex]) {
      fetchData()
    }
  }, [
    PLANT_ID,
    AOP_YEAR,
    oldYear,
    yearChanged,
    keycloak,
    lineDetails,
    tabIndex,
  ])

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
      saveBtn: false,
      customHeight: permissions?.customHeight,
      allAction: true,
      downloadExcelBtn: false,
      showNoteWhileDeleting: false,
      showTitleNameBusiness: true,
      titleName: screenName,
      uploadExcelBtn: false,
    },
    isOldYear,
  )

  return (
    <div>
      <LoaderBackdrop open={!!loading} />

      {/* No tabs here anymore — they live in the parent */}

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
        screenType='shutdown'
      />
    </div>
  )
}

export default CalculatedBusinessProposed
