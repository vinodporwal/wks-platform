import React, { useState, useEffect, useCallback } from 'react'
import { Box } from '@mui/material'
import { useSelector, useDispatch } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { setIsReleased } from 'store/reducers/dataGridStore'
import { getRoleName } from 'services/role-service'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { generateHeaderNames } from '../../common/utilities/generateHeaders'
import { customValueFormatterPhaseTwo } from '../../common/ValueFormatterPhaseTwo'
import { generateExcelNameWithoutExt } from '../../common/utilities/excelNameUtil'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import ReleaseDialog from '../../common/components/ReleaseDialog'
import { FixedBedAndLabCostApiService } from 'components/aop-phase-two/services/crude/fixedBedAndLabCostApiService'
import ReleaseAPIService from 'components/aop-phase-two/services/common/releaseAPIService'
import {
  formatCostCenterDropdownOptions,
  getCostCenterOptions,
  getCostCenterDescriptionById,
  getMasterIdByDescription,
} from './helpers'

const MONTH_FIELDS = [
  'apr',
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
]

const FixedBedAndLabCostScreen = () => {
  const keycloak = useSession()
  const dispatch = useDispatch()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, oldYear, isReleased } = dataGridStore

  const PLANT_ID = plantObject?.id || plantObject?.value || plantObject?.plantId
  const AOP_YEAR = year?.selectedYear || year

  const IS_OLD_YEAR = oldYear?.oldYear || oldYear?.isOldYear
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [costCenterDropdown, setCostCenterDropdown] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // Release state
  const [openReleaseDialogBox, setOpenReleaseDialogBox] = useState(false)
  const [isReleaseDisabled, setIsReleaseDisabled] = useState(true)

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const valueFormat = customValueFormatterPhaseTwo(2)
  const headerMap = generateHeaderNames(AOP_YEAR)

  // 1. Fetch Cost Center Dropdown options (Sp_GetFixedBedCostCentersDropdowns)
  const fetchCostCenterDropdown = useCallback(async () => {
    try {
      const response = await FixedBedAndLabCostApiService.getFixedBedCostCentersDropdown(keycloak)
      const data = response?.data || response?.result || response || []
      const formatted = formatCostCenterDropdownOptions(data)
      setCostCenterDropdown(formatted)
      return formatted
    } catch (error) {
      console.error('Error fetching cost center dropdown options:', error)
      return []
    }
  }, [keycloak])

  useEffect(() => {
    fetchCostCenterDropdown()
  }, [fetchCostCenterDropdown])

  // 2. Grid Columns definition: Cost Center Description (Select) + 12 Months + Remarks
  const columns = [
    {
      field: 'costCenterDescription',
      title: 'Cost Center Description',
      widthT: 320,
      minWidth: 240,
      type: 'select',
      editable: !READ_ONLY,
      locked: true,
      dynamicOptions: true,
      getOptions: (dataItem) => getCostCenterOptions(dataItem, costCenterDropdown, rows),
    },
    {
      field: 'apr',
      title: headerMap[4] || 'Apr',
      widthT: 120,
      minWidth: 110,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'may',
      title: headerMap[5] || 'May',
      widthT: 120,
      minWidth: 110,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'jun',
      title: headerMap[6] || 'Jun',
      widthT: 120,
      minWidth: 110,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'jul',
      title: headerMap[7] || 'Jul',
      widthT: 120,
      minWidth: 110,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'aug',
      title: headerMap[8] || 'Aug',
      widthT: 120,
      minWidth: 110,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'sep',
      title: headerMap[9] || 'Sep',
      widthT: 120,
      minWidth: 110,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'oct',
      title: headerMap[10] || 'Oct',
      widthT: 120,
      minWidth: 110,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'nov',
      title: headerMap[11] || 'Nov',
      widthT: 120,
      minWidth: 110,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'dec',
      title: headerMap[12] || 'Dec',
      widthT: 120,
      minWidth: 110,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'jan',
      title: headerMap[1] || 'Jan',
      widthT: 120,
      minWidth: 110,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'feb',
      title: headerMap[2] || 'Feb',
      widthT: 120,
      minWidth: 110,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'mar',
      title: headerMap[3] || 'Mar',
      widthT: 120,
      minWidth: 110,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'remarks',
      title: 'Remarks',
      widthT: 250,
      minWidth: 180,
      type: 'text',
      editable: !READ_ONLY,
    },
  ]

  const getIsReleasedStatus = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      const response = await ReleaseAPIService.getReleaseAOPStatus(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      const hasReleaseRecord = Array.isArray(response?.data)
        ? response.data.length > 0
        : response?.data &&
          typeof response.data === 'object' &&
          Object.keys(response.data).length > 0

      const isReleasedVal =
        hasReleaseRecord ||
        response?.data?.isReleased === 1 ||
        response?.isReleased === 1
          ? 1
          : 0

      if (isReleasedVal === 1) {
        setIsReleaseDisabled(true)
        dispatch(setIsReleased({ isReleased: 1 }))
      } else {
        setIsReleaseDisabled(false)
        dispatch(setIsReleased({ isReleased: 0 }))
      }
    } catch (error) {
      console.error('Error fetching release status:', error)
      setIsReleaseDisabled(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, dispatch])

  // 3. GET API - SP_GetFixedBedAndLabCostData
  const fetchData = useCallback(async () => {
    if (!AOP_YEAR) return
    setLoading(true)
    try {
      const response = await FixedBedAndLabCostApiService.getFixedBedAndLabCostData(
        keycloak,
        AOP_YEAR,
      )
      const rawList = Array.isArray(response?.data)
        ? response.data
        : response?.data?.list || response?.result || []

      const formattedData = rawList.map((item, index) => {
        const rowId = item.masterId || item.id || `row_${index}`
        const desc = item.costCenterDescription || item.costCenter || ''

        return {
          ...item,
          id: rowId,
          masterId: item.masterId || item.id,
          costCenterDescription: desc,
          apr: Number(item.apr ?? item.april ?? 0),
          may: Number(item.may ?? 0),
          jun: Number(item.jun ?? item.june ?? 0),
          jul: Number(item.jul ?? item.july ?? 0),
          aug: Number(item.aug ?? 0),
          sep: Number(item.sep ?? 0),
          oct: Number(item.oct ?? 0),
          nov: Number(item.nov ?? 0),
          dec: Number(item.dec ?? 0),
          jan: Number(item.jan ?? 0),
          feb: Number(item.feb ?? 0),
          mar: Number(item.mar ?? item.march ?? 0),
          remarks: item.remarks || '',
          isEditable: READ_ONLY
            ? false
            : item.isEditable !== undefined
            ? item.isEditable
            : true,
        }
      })

      setRows(formattedData)
      setOriginalRows(JSON.parse(JSON.stringify(formattedData)))
      setModifiedCells({})
      await getIsReleasedStatus()
    } catch (error) {
      console.error('Error fetching Fixed Bed and Lab Cost data:', error)
      setRows([])
      setOriginalRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, AOP_YEAR, READ_ONLY, getIsReleasedStatus])

  useEffect(() => {
    if (AOP_YEAR) {
      fetchData()
    }
  }, [AOP_YEAR, fetchData])

  // Custom Item Change with Duplicate Dropdown Selection Prevention
  const handleCustomItemChange = (e) => {
    if (READ_ONLY) return
    const { dataItem, field, value } = e
    if (!dataItem) return

    const rowId = dataItem.id

    const updatedRow = {
      ...dataItem,
      [field]: value,
      inEdit: true,
    }

    // 1) VALIDATION: No duplicate dropdown selection
    if (field === 'costCenterDescription') {
      const selectedId = getMasterIdByDescription(value, costCenterDropdown)
      const selectedDesc = getCostCenterDescriptionById(selectedId, costCenterDropdown)

      // Check if this cost center is already selected in another row
      const isDuplicate = rows.some(
        (r) =>
          r.id !== rowId &&
          ((r.masterId && r.masterId === selectedId) ||
            (r.costCenterDescription && r.costCenterDescription === selectedDesc))
      )

      if (isDuplicate) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: `"${selectedDesc}" is already selected in another row!`,
          severity: 'warning',
        })
        return
      }

      updatedRow.masterId = selectedId
      updatedRow.costCenterDescription = selectedDesc
    }

    setRows((prevRows) =>
      prevRows.map((r) => (r.id === rowId ? updatedRow : r)),
    )

    setModifiedCells((prev) => ({
      ...prev,
      [rowId]: {
        ...(prev[rowId] || {}),
        ...updatedRow,
      },
    }))
  }

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  // Delete row handler (calls DELETE API if persisted, otherwise removes from state)
  const deleteRowData = async (dataItem) => {
    if (!dataItem || READ_ONLY) return

    const masterId = dataItem.masterId || dataItem.id

    // If new unsaved row, remove locally
    if (dataItem.isNew || String(dataItem.id).startsWith('new_row_')) {
      setRows((prev) => prev.filter((r) => r.id !== dataItem.id))
      setModifiedCells((prev) => {
        const copy = { ...prev }
        delete copy[dataItem.id]
        return copy
      })
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Row removed successfully!',
        severity: 'success',
      })
      return
    }

    // Call DELETE API for saved record
    setLoading(true)
    try {
      await FixedBedAndLabCostApiService.deleteFixedBedAndLabCostData(
        keycloak,
        masterId,
        AOP_YEAR,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Record deleted successfully!',
        severity: 'success',
      })
      await fetchData()
    } catch (error) {
      console.error('Error deleting record:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to delete record. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  // 4. SAVE API with Duplicate and Remarks Validations
  const saveChanges = async () => {
    if (READ_ONLY) return
    setLoading(true)
    try {
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

      // -------------------------------------------------------------
      // VALIDATION 1: Check Cost Center selection & No Duplicate Rows
      // -------------------------------------------------------------
      const seenMasterIds = new Set()
      for (const row of rows) {
        const mId =
          row.masterId ||
          getMasterIdByDescription(row.costCenterDescription, costCenterDropdown)

        if (!mId || !row.costCenterDescription) {
          setSnackbarOpen(true)
          setSnackbarData({
            message: 'Please select a Cost Center for all rows before saving!',
            severity: 'warning',
          })
          setLoading(false)
          return
        }

        if (seenMasterIds.has(mId)) {
          const desc =
            getCostCenterDescriptionById(mId, costCenterDropdown) ||
            row.costCenterDescription ||
            mId
          setSnackbarOpen(true)
          setSnackbarData({
            message: `Duplicate Cost Center detected: "${desc}". Each Cost Center can only be added once!`,
            severity: 'warning',
          })
          setLoading(false)
          return
        }
        seenMasterIds.add(mId)
      }

      // -------------------------------------------------------------
      // VALIDATION 2: Remarks Validation for all modified / added rows
      // -------------------------------------------------------------
      for (const row of modifiedData) {
        const remarksVal = (row.remarks || '').trim()
        const desc =
          row.costCenterDescription ||
          getCostCenterDescriptionById(row.masterId, costCenterDropdown) ||
          'Row'

        if (!remarksVal) {
          setSnackbarOpen(true)
          setSnackbarData({
            message: `Remarks is mandatory for: "${desc}". Please provide remarks!`,
            severity: 'warning',
          })
          setLoading(false)
          return
        }

        // For existing rows, check that remarks was updated
        const origRow = originalRows.find((orig) => orig.id === row.id || orig.masterId === row.masterId)
        if (origRow) {
          const origRemarks = (origRow.remarks || '').trim()
          const dataWasUpdated = MONTH_FIELDS.some(
            (field) => Number(row[field] || 0) !== Number(origRow[field] || 0)
          )
          if (dataWasUpdated && origRemarks && remarksVal === origRemarks) {
            setSnackbarOpen(true)
            setSnackbarData({
              message: `Please update the remarks for "${desc}" to reflect the new changes!`,
              severity: 'warning',
            })
            setLoading(false)
            return
          }
        }
      }

      // -------------------------------------------------------------
      // Prepare Payload & Save
      // -------------------------------------------------------------
      const payload = modifiedData.map((row) => {
        const isNewEntry =
          row.isNew === true ||
          (row.id && String(row.id).startsWith('new_row_')) ||
          row.isTransactionExists === false

        const resolvedMasterId =
          row.masterId ||
          getMasterIdByDescription(row.costCenterDescription, costCenterDropdown)

        const resolvedCostCenterDesc =
          getCostCenterDescriptionById(resolvedMasterId, costCenterDropdown) ||
          row.costCenterDescription ||
          ''

        return {
          id: isNewEntry ? null : (row.transactionId || (!String(row.id).startsWith('new_row_') ? row.id : null)),
          masterId: resolvedMasterId,
          costCenterDescription: resolvedCostCenterDesc,
          aopYear: AOP_YEAR,
          apr: Number(row.apr || 0),
          may: Number(row.may || 0),
          jun: Number(row.jun || 0),
          jul: Number(row.jul || 0),
          aug: Number(row.aug || 0),
          sep: Number(row.sep || 0),
          oct: Number(row.oct || 0),
          nov: Number(row.nov || 0),
          dec: Number(row.dec || 0),
          jan: Number(row.jan || 0),
          feb: Number(row.feb || 0),
          mar: Number(row.mar || 0),
          remarks: row.remarks || '',
        }
      })

      await FixedBedAndLabCostApiService.saveFixedBedAndLabCostData(payload, keycloak, AOP_YEAR)

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Saved Successfully!',
        severity: 'success',
      })
      setModifiedCells({})
      fetchData()
    } catch (error) {
      console.error('Error saving data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error saving data. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleExport = async () => {
    const excelName = generateExcelNameWithoutExt('Fixed_Bed_And_Lab_Cost', AOP_YEAR)
    await FixedBedAndLabCostApiService.exportFixedBedAndLabCost(
      keycloak,
      AOP_YEAR,
      excelName,
    )
  }

  // Permissions configuration enabling Add Item, Save, and Delete
  const permissions = {
    showAction: true,
    addButton: true,
    addBtnName: 'Add Item',
    deleteButton: true,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showExport: false,
    showImport: false,
    showCalculate: false,
    ExcelName: `Fixed_Bed_And_Lab_Cost_${AOP_YEAR}`,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Fixed Bed and Lab Cost',
    showDropdown: false,
    remarksEditable: true,
  }

  return (
    <Box sx={{ p: 2 }}>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        loading={loading}
        title={permissions.showTitle ? permissions.titleName : 'Fixed Bed and Lab Cost'}
        permissions={permissions}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        customItemChange={handleCustomItemChange}
        saveChanges={saveChanges}
        deleteRowData={deleteRowData}
        handleExport={handleExport}
        fetchData={fetchData}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        snackbarData={snackbarData}
        setSnackbarData={setSnackbarData}
        openReleaseDialogBox={openReleaseDialogBox}
        setOpenReleaseDialogBox={setOpenReleaseDialogBox}
        isReleaseDisabled={isReleaseDisabled}
      />
      {openReleaseDialogBox && (
        <ReleaseDialog
          open={openReleaseDialogBox}
          handleClose={() => setOpenReleaseDialogBox(false)}
          onSuccess={() => {
            getIsReleasedStatus()
            fetchData()
          }}
        />
      )}
    </Box>
  )
}

export default FixedBedAndLabCostScreen
