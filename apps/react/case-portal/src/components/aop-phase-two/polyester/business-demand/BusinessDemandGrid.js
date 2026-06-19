import React, { useState, useEffect, useCallback, useRef } from 'react'
import { Box } from '@mui/material'
import { useDispatch, useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import { setIsBlocked } from 'store/reducers/dataGridStore'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { validateFields } from 'utils/validationUtils'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { BusinessDemandApiService } from '../../services/polyester/businessDemandApiService'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import {
  convertRows,
  convertRowToTPM,
  UNIT_OPTIONS,
  DEFAULT_UNIT,
} from './utils'
import { customValueFormatterPhaseTwo } from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'

const BusinessDemandGrid = () => {
  const dispatch = useDispatch()
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    oldYear,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
    isReleased,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const PLANT_NAME = plantObject?.name
  const SITE_NAME = siteObject?.name
  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name
  const AOP_YEAR = year?.selectedYear
  const SCREEN_NAME = screenTitle?.title
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased

  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const valueFormat = ValueFormatterProduction()
  const customFormat = customValueFormatterPhaseTwo(5)
  const headerMap = generateHeaderNames(AOP_YEAR)
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Business Demand')

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const [selectedUnit, setSelectedUnit] = useState(DEFAULT_UNIT)

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const monthsConfig = [
    { field: 'april', key: 4, title: 'April' },
    { field: 'may', key: 5, title: 'May' },
    { field: 'june', key: 6, title: 'June' },
    { field: 'july', key: 7, title: 'July' },
    { field: 'aug', key: 8, title: 'August' },
    { field: 'sep', key: 9, title: 'September' },
    { field: 'oct', key: 10, title: 'October' },
    { field: 'nov', key: 11, title: 'November' },
    { field: 'dec', key: 12, title: 'December' },
    { field: 'jan', key: 1, title: 'January' },
    { field: 'feb', key: 2, title: 'February' },
    { field: 'march', key: 3, title: 'March' },
  ]

  const MONTH_FIELDS = monthsConfig.map((m) => m.field)

  const columns = [
    {
      field: 'Particulars',
      title: 'Type',
      editable: false,
      hidden: true,
      minWidth: 100,
    },
    {
      field: 'displayName',
      title: 'Particulars',
      editable: false,
      minWidth: 200,
    },
    ...monthsConfig.map((m) => ({
      field: m.field,
      title: headerMap[m.key] || m.title,
      editable: true,
      type: 'numberNonGrey',
      format: customFormat,
      minWidth: 110,
    })),
    {
      field: 'total',
      title: 'Total',
      editable: false,
      type: 'number1',
      format: valueFormat,
      minWidth: 110,
    },
    {
      field: 'remark',
      title: 'Remark',
      type: 'textarea',
      editable: true,
      minWidth: 160,
    },
  ]

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await BusinessDemandApiService.getBusinessDemand(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      const data = Array.isArray(response) ? response : response?.data || []
      if (data && Array.isArray(data)) {
        const MONTH_FIELDS = monthsConfig.map((m) => m.field)
        const formattedData = data.map((item, index) => ({
          ...item,
          idFromApi: item.id,
          id: index,
          originalRemark: item.remark,
          inEdit: false,
          Particulars: item.normParameterTypeDisplayName,
          total: MONTH_FIELDS.reduce(
            (sum, f) => sum + (Number(item[f]) || 0),
            0,
          ),
        }))

        // Vertical totals row (column-wise sum) — always built from raw TPM
        const totals = {
          id: '__totals__',
          displayName: 'Total',
          isFooter: true,
          isEditable: false,
        }
        MONTH_FIELDS.forEach((f) => {
          totals[f] = formattedData.reduce(
            (sum, row) => sum + (Number(row[f]) || 0),
            0,
          )
        })
        totals.total = MONTH_FIELDS.reduce(
          (sum, f) => sum + (totals[f] || 0),
          0,
        )

        const allRows = [...formattedData, totals]

        // Convert to the selected unit once — stored directly in rows state
        // When selectedUnit changes this function re-runs via the useEffect below
        setRows(convertRows(allRows, selectedUnit, AOP_YEAR))
        setModifiedCells({})
      } else {
        setRows([])
      }
    } catch (error) {
      console.error('Error fetching business demand data:', error)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, selectedUnit])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const saveChanges = useCallback(async () => {
    try {
      const data = Object.values(modifiedCells)
      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        return
      }

      const requiredFields = ['normParameterId', 'remark']
      const validationMessage = validateFields(data, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationMessage,
          severity: 'error',
        })
        return
      }

      const payloadData = data.map((row) => {
        // Convert displayed values back to TPM before saving
        const tpmRow = convertRowToTPM(row, selectedUnit, AOP_YEAR)
        return {
          april: tpmRow.april ?? null,
          may: tpmRow.may ?? null,
          june: tpmRow.june ?? null,
          july: tpmRow.july ?? null,
          aug: tpmRow.aug ?? null,
          sep: tpmRow.sep ?? null,
          oct: tpmRow.oct ?? null,
          nov: tpmRow.nov ?? null,
          dec: tpmRow.dec ?? null,
          jan: tpmRow.jan ?? null,
          feb: tpmRow.feb ?? null,
          march: tpmRow.march ?? null,
          remark: row.remark || null,
          avgTph: row.avgTph || null,
          year: AOP_YEAR,
          plantId: PLANT_ID,
          siteFKId: siteObject?.id,
          verticalFKId: VERTICAL_ID,
          normParameterId: row.normParameterId,
          id: row.idFromApi || null,
          inEdit: row.inEdit || false,
        }
      })

      setLoading(true)
      const res = await BusinessDemandApiService.saveBusinessDemand(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        payloadData,
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Saved Successfully!',
        severity: 'success',
      })
      setModifiedCells({})
      dispatch(setIsBlocked(false))
      setSelectedUnit(DEFAULT_UNIT)
      fetchData()
    } catch (error) {
      console.error('Error saving business demand data:', error)
    } finally {
      setLoading(false)
    }
  }, [
    modifiedCells,
    rows,
    keycloak,
    PLANT_ID,
    AOP_YEAR,
    fetchData,
    dispatch,
    siteObject,
    VERTICAL_ID,
    selectedUnit,
  ])

  const deleteRowData = useCallback(
    async (paramsForDelete) => {
      try {
        const { idFromApi, id } = paramsForDelete.row
        const deleteId = id

        if (!idFromApi) {
          setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
          return
        }

        setLoading(true)
        await BusinessDemandApiService.deleteBusinessDemand(keycloak, idFromApi)
        setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Record Deleted successfully!',
          severity: 'success',
        })
        fetchData()
      } catch (error) {
        console.error('Error deleting record:', error)
      } finally {
        setLoading(false)
      }
    },
    [keycloak, fetchData],
  )

  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })
    try {
      await BusinessDemandApiService.exportBusinessDemand(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_NAME,
        SCREEN_NAME,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel downloaded successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting business demand excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
    }
  }

  const handleExcelUpload = async (rawFile) => {
    setLoading(true)
    try {
      const response = await BusinessDemandApiService.importBusinessDemand(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        rawFile,
      )
      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Uploaded Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData()
      } else if (response?.code === 400 && response?.data) {
        const byteCharacters = atob(response.data)
        const byteNumbers = Array.from(byteCharacters, (char) =>
          char.charCodeAt(0),
        )
        const byteArray = new Uint8Array(byteNumbers)
        const blob = new Blob([byteArray], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', 'Error File - Business Demand.xlsx')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error importing business demand excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleRemarkCellClick = useCallback(
    (row) => {
      if (READ_ONLY) return
      setCurrentRemark(row.remark || '')
      setCurrentRowId(row.id)
      setRemarkDialogOpen(true)
    },
    [READ_ONLY],
  )

  // Real-time recalculation of row total and footer totals when a month cell is edited
  const customItemChange = useCallback(
    (e, setRowsFn) => {
      const { dataItem, field, value } = e
      if (!MONTH_FIELDS.includes(field)) return

      setRowsFn((prev) => {
        // 1. Recalculate the edited row's horizontal total
        const updated = prev.map((r) => {
          if (r.id === dataItem.id) {
            const newTotal = MONTH_FIELDS.reduce((sum, f) => {
              const v = f === field ? value : r[f]
              return sum + (Number(v) || 0)
            }, 0)
            return { ...r, [field]: value, total: newTotal }
          }
          return r
        })

        // 2. Recalculate the footer totals row
        const dataRows = updated.filter((r) => r.id !== '__totals__')
        const newTotals = { ...updated.find((r) => r.id === '__totals__') }
        MONTH_FIELDS.forEach((f) => {
          newTotals[f] = dataRows.reduce(
            (sum, r) => sum + (Number(r[f]) || 0),
            0,
          )
        })
        newTotals.total = MONTH_FIELDS.reduce(
          (sum, f) => sum + (newTotals[f] || 0),
          0,
        )

        return updated.map((r) => (r.id === '__totals__' ? newTotals : r))
      })
    },
    [MONTH_FIELDS],
  )

  const dropdownConfig = {
    options: UNIT_OPTIONS,
    label: 'Unit',
    placeholder: 'Select Unit',
    valueKey: 'id',
    labelKey: 'name',
  }

  const permissions = {
    showAction: false,
    addButton: false,
    deleteButton: false,
    editButton: false,
    showUnit: false,
    saveBtn: true,
    showCalculate: false,
    allAction: true,
    showDropdown: true,
    showExport: selectedUnit === 'TPM',
    showImport: selectedUnit === 'TPM',
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Business Demand (Without SD)',
    isTotalFooterActive: true,
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        title={SCREEN_NAME}
        loading={loading}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        permissions={permissions}
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        handleExport={handleExport}
        handleExcelUpload={handleExcelUpload}
        deleteRowData={deleteRowData}
        customItemChange={customItemChange}
        dropdownConfig={dropdownConfig}
        selectedDropdownValue={selectedUnit}
        setSelectedDropdownValue={setSelectedUnit}
        // groupBy='Particulars'
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        snackbarData={snackbarData}
        setSnackbarData={setSnackbarData}
      />
    </Box>
  )
}

export default BusinessDemandGrid
