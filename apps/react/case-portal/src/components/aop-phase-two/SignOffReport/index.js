import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { Box, Typography, TextField } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'

// Phase Two Common Components & Utilities
import AdvanceKendoTable from '../common/AdvanceKendoTable/index'
import { customValueFormatterPhaseTwo } from '../common/ValueFormatterPhaseTwo'
import { generateExcelNameWithoutExt } from '../common/utilities/excelNameUtil'
import Notification from '../common/utilities/Notification'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

// Phase Two API Services
import { SignOffReportApiService } from '../services/common/signOffReportApiService'
import { HistoricPeriodBasisApiService } from '../services/common/historicPeriodBasisApiService'

/**
 * SignOffReport Component
 * Uses configuration execution details API to get StartDate and EndDate,
 * and dynamically renders a single sign-off report grid with 5 decimal places for numbers.
 */
const SIGN_OFF_COLUMNS = [
  {
    field: 'label',
    title: ' ',
    editable: false,
    minWidth: 100,
    type: 'text',
    filterable: false,
  },
  {
    field: 'prepared',
    title: 'Prepared By',
    editable: true,
    minWidth: 100,
    type: 'text',
    filterable: false,
  },
  {
    field: 'reviewed',
    title: 'Reviewed By',
    editable: true,
    minWidth: 100,
    type: 'text',
    filterable: false,
  },
  {
    field: 'approved',
    title: 'Approved By',
    editable: true,
    minWidth: 100,
    type: 'text',
    filterable: false,
  },
]

const SignOffReport = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, siteObject } = dataGridStore

  // Store variables
  const PLANT_ID = plantObject?.id || plantObject?.value || plantObject?.plantId
  const AOP_YEAR = year?.selectedYear || year

  // Local state for single grid
  const [loading, setLoading] = useState(false)
  const [columns, setColumns] = useState([])
  const [rows, setRows] = useState([])
  const [gridTitle, setGridTitle] = useState('Sign Off Report')
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [unitNo, setUnitNo] = useState(null)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const [signOffRows, setSignOffRows] = useState([
    { id: '1', label: 'Name', prepared: '', reviewed: '', approved: '' },
    { id: '2', label: 'Designation', prepared: '', reviewed: '', approved: '' },
    { id: '3', label: 'Sign', prepared: '', reviewed: '', approved: '' },
  ])


  // Notification state
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const site = siteObject?.name?.toLowerCase()?.trim()
  const plant = plantObject?.name?.toLowerCase()?.trim()

  const isFirstTwoColColor = useMemo(() => {
    if (site === 'dta' && ['dht1'].includes(plant)) return true
    return false
  }, [site, plant])

  // 5 Decimal place formatter for numeric columns
  const numberFormatFiveDecimals = useMemo(
    () => customValueFormatterPhaseTwo(5),
    [],
  )

  /**
   * Helper function to format Date object or date string to YYYY-MM-DD
   */
  const formatDateForAPI = useCallback((date) => {
    if (!date) return null
    if (typeof date === 'string') {
      const trimmed = date.trim()
      const isoMatch = trimmed.match(/^(\d{4}-\d{2}-\d{2})/)
      if (isoMatch) return isoMatch[1]
      if (/^\d{2}-\d{2}-\d{4}$/.test(trimmed)) {
        const [d, m, y] = trimmed.split('-')
        return `${y}-${m}-${d}`
      }
    }
    const d = new Date(date)
    if (isNaN(d.getTime())) return null
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${day}`
  }, [])

  /**
   * Calculate fallback dates from AOP year (1st April of prior year to 31st March)
   */
  const getDatesFromAopYear = useCallback((aopYear) => {
    if (typeof aopYear === 'string' && aopYear.includes('-')) {
      const startYear = parseInt(aopYear.split('-')[0], 10)
      if (!isNaN(startYear)) {
        return {
          startDate: `${startYear - 1}-04-01`,
          endDate: `${startYear}-03-31`,
        }
      }
    }
    return { startDate: null, endDate: null }
  }, [])

  /**
   * Enriches backend column metadata into AdvanceKendoTable-compliant columns.
   * Ensures numbers are configured to 5 decimal places.
   */
  const enrichColumns = useCallback(
    (backendCols = []) => {
      if (!Array.isArray(backendCols)) return []
      const excludeColumns = [
        'iseditable',
        'is_editable',
        'grid_type',
        'gridtype',
        'id',
        'unitno',
        'unit_no',
      ]
      const mappedCols = backendCols
        .filter((col) => {
          if (!col || !col.field) return false
          const fieldUpper = col.field.trim().toLowerCase()
          const titleUpper = col.title?.trim().toLowerCase()
          return (
            !excludeColumns.includes(fieldUpper)
            && !excludeColumns.includes(titleUpper)
          )
        })
        .map((col, index) => {
          const rawField = col.field || ''
          const rawTitle = col.title || rawField
          let titleClean = rawTitle
          let groupTag = col.group || ''

          if (rawTitle.includes('~')) {
            const parts = rawTitle.split('~')
            titleClean = parts[0]
            groupTag = parts[1] || groupTag
          } else if (rawField.includes('~')) {
            const parts = rawField.split('~')
            if (!col.title || col.title === rawField) {
              titleClean = parts[0]
            }
            groupTag = parts[1] || groupTag
          }

          if (typeof titleClean === 'string' && titleClean.includes('~')) {
            titleClean = titleClean.split('~')[0]
          }

          const formattedTitle = titleClean.replace(/_/g, ' ').trim()
          const cleanGroupTag = groupTag.replace(/_/g, ' ').trim()
          const isRemarks =
            rawField.toLowerCase().includes('remarks') ||
            titleClean.toLowerCase().includes('remark')
          const isNumber =
            col.type === 'number' ||
            (!col.type &&
              (rawField.includes('~') ||
                rawTitle.includes('~') ||
                /^(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)/i.test(titleClean)))
          const isDate = col.type === 'date'
          const isFirstCol = index === 0
          const isFrozenCol = index === 0 || index === 1

          const baseConfig = {
            field: col.field,
            title: formattedTitle,
            group: cleanGroupTag,
            editable: isRemarks ? true : Boolean(col.editable),
            hidden: col.hidden || false,
            locked: isFrozenCol && backendCols.length > 2,
          }

          if (isRemarks) {
            return {
              ...baseConfig,
              field: col.field || 'remarks',
              title: formattedTitle || 'Remarks',
              type: 'textarea',
              align: 'left',
              headerAlign: 'left',
              widthT: Math.max(180, formattedTitle.length * 11),
              minWidth: 150,
            }
          }

          if (isNumber) {
            const isDaysCol =
              formattedTitle.toLowerCase().includes('days') ||
              col.field?.toLowerCase().includes('days')
            return {
              ...baseConfig,
              type: 'number',
              format: isDaysCol ? '{0:0}' : numberFormatFiveDecimals,
              align: 'right',
              headerAlign: 'left',
              widthT: Math.max(150, Math.min(formattedTitle.length * 12, 220)),
              minWidth: 130,
            }
          }

          if (isDate) {
            return {
              ...baseConfig,
              type: 'date',
              format: '{0:dd-MM-yyyy}',
              align: 'center',
              headerAlign: 'left',
              widthT: 160,
              minWidth: 140,
            }
          }

          // Text / String columns
          return {
            ...baseConfig,
            type: 'text',
            align: 'left',
            headerAlign: 'left',
            widthT: Math.max(
              isFirstCol ? 260 : 180,
              formattedTitle.length * 11,
            ),
            minWidth: isFirstCol ? 220 : 150,
          }
        })

      const hasGroup = mappedCols.some((c) => Boolean(c.group))
      if (!hasGroup) return mappedCols

      const leadingCols = []
      const trailingCols = []
      const groupsMap = new Map()

      mappedCols.forEach((col) => {
        if (col.group) {
          if (!groupsMap.has(col.group)) {
            groupsMap.set(col.group, [])
          }
          groupsMap.get(col.group).push(col)
        } else {
          if (groupsMap.size === 0) {
            leadingCols.push(col)
          } else {
            trailingCols.push(col)
          }
        }
      })

      const isNumbered = Array.from(groupsMap.keys()).every((k) =>
        /^group\s*\d+$/i.test(k.trim()),
      )
      const sortedGroupKeys = isNumbered
        ? Array.from(groupsMap.keys()).sort((a, b) =>
            a.localeCompare(b, undefined, { numeric: true, sensitivity: 'base' }),
          )
        : Array.from(groupsMap.keys())

      const groupedCols = []
      sortedGroupKeys.forEach((key) => {
        const childCols = groupsMap.get(key) || []
        // Child columns inside a multi-column group must not be individually locked
        childCols.forEach((c) => {
          c.locked = false
        })
        groupedCols.push({
          title: key,
          headerClassName: 'center-group-header',
          children: childCols,
        })
      })

      // Lock only the first 1-2 leading columns (e.g. Particulars, UOM) if grid has more than 2 columns
      leadingCols.forEach((c, idx) => {
        c.locked = (idx === 0 || idx === 1) && mappedCols.length > 2
      })

      trailingCols.forEach((c) => {
        c.locked = false
      })

      return [...leadingCols, ...groupedCols, ...trailingCols]
    },
    [numberFormatFiveDecimals],
  )

  /**
   * Normalizes raw row data from API.
   * Ensures each row has a stable unique ID and numeric columns are parsed as numbers.
   */
  const normalizeRows = useCallback((rawRows = [], cols = []) => {
    if (!Array.isArray(rawRows)) return []

    const leafCols = []
    const traverse = (items) => {
      items.forEach((c) => {
        if (c.children && Array.isArray(c.children)) {
          traverse(c.children)
        } else {
          leafCols.push(c)
        }
      })
    }
    traverse(cols)

    const numericFields = new Set(
      leafCols.filter((c) => c.type === 'number').map((c) => c.field),
    )
    const dateFields = new Set(
      leafCols.filter((c) => c.type === 'date').map((c) => c.field),
    )

    return rawRows.map((row, idx) => {
      const normalizedRow = { ...row }

      // Guarantee unique key for Kendo and preserve database UUID
      const rowId = normalizedRow.Id ?? normalizedRow.id ?? (idx + 1)
      normalizedRow.id = rowId
      normalizedRow.Id = rowId

      // Normalize isEditable flag as boolean for row-level permissions
      const rawEditable =
        normalizedRow.isEditable ??
        normalizedRow.IsEditable ??
        normalizedRow.is_editable
      const isRowEditable =
        rawEditable !== false &&
        rawEditable !== 'false' &&
        rawEditable !== 0 &&
        rawEditable !== '0'
      normalizedRow.isEditable = isRowEditable
      normalizedRow.IsEditable = isRowEditable

      const rawUom = normalizedRow.UOM ?? normalizedRow.uom
      const isUomEmpty =
        rawUom === undefined ||
        rawUom === null ||
        String(rawUom).trim() === ''
      if (isUomEmpty) {
        normalizedRow.isNoUom = true
        normalizedRow.isEditable = false
        normalizedRow.IsEditable = false
      }

      const isDaysRow =
        !isUomEmpty &&
        String(rawUom)
          .trim()
          .toLowerCase() === 'days'

      // Convert numeric fields to JavaScript numbers so Kendo number formatting applies
      numericFields.forEach((field) => {
        const val = normalizedRow[field]
        if (val !== null && val !== undefined && val !== '') {
          const parsed =
            typeof val === 'number'
              ? val
              : parseFloat(String(val).replace(/,/g, '').trim())
          normalizedRow[field] = !isNaN(parsed)
            ? isDaysRow
              ? Math.round(parsed)
              : parsed
            : null
        } else {
          normalizedRow[field] = null
        }
      })

      // Convert date strings to Date objects if needed
      dateFields.forEach((field) => {
        const val = normalizedRow[field]
        if (val) {
          const d = new Date(val)
          if (!isNaN(d.getTime())) {
            normalizedRow[field] = d
          }
        }
      })

      // Ensure remarks field has default empty string and exists
      const remarks = normalizedRow?.Remarks ?? normalizedRow?.remarks ?? ''
      normalizedRow.remarks = remarks

      return normalizedRow
    })
  }, [])

  /**
   * Fetch configuration execution details for StartDate & EndDate,
   * then retrieve the Sign Off Report grid data.
   */
  const fetchSignOffReport = useCallback(
    async (isCalculate = false) => {
      if (!PLANT_ID || !AOP_YEAR) {
        setUnitNo(null)
        setColumns([])
        setRows([])
        return
      }

      setUnitNo(null)
      setLoading(true)
      if (isCalculate) {
        setSnackbarData({
          message: 'Calculating...',
          severity: 'info',
        })
        setSnackbarOpen(true)
      }

      try {
        // 1. Use configuration execution API to get StartDate and EndDate
        let periodFrom = null
        let periodTo = null

        try {
          const configResponse =
            await HistoricPeriodBasisApiService.getConfigurationExecutionDetails(
              keycloak,
              PLANT_ID,
              AOP_YEAR,
            )

          const configDetails = Array.isArray(configResponse?.data)
            ? configResponse.data
            : Array.isArray(configResponse?.data?.data)
              ? configResponse.data.data
              : []

          const startDateObj = configDetails.find((d) => d.Name === 'StartDate')
          const endDateObj = configDetails.find((d) => d.Name === 'EndDate')

          const rawStartDate =
            startDateObj?.AttributeValue ||
            startDateObj?.Value ||
            startDateObj?.apr
          const rawEndDate =
            endDateObj?.AttributeValue || endDateObj?.Value || endDateObj?.apr

          periodFrom = formatDateForAPI(rawStartDate)
          periodTo = formatDateForAPI(rawEndDate)
        } catch (configErr) {
          console.warn(
            'Could not fetch configuration execution details, falling back to AOP year calculation:',
            configErr,
          )
        }

        // Fallback to computed dates from AOP year if configuration execution has no records
        if (!periodFrom || !periodTo) {
          const fallbackDates = getDatesFromAopYear(AOP_YEAR)
          if (!periodFrom) periodFrom = fallbackDates.startDate
          if (!periodTo) periodTo = fallbackDates.endDate
        }

        // 2. Fetch Sign Off Report with plantId, year, periodFrom, periodTo
        const response = await SignOffReportApiService.getSignOffReport(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          periodFrom,
          periodTo,
        )

        const statusCode = response?.code || (response?.status ?? 200)
        const responseData = response?.data

        if (statusCode === 200 && responseData) {
          // Extract the single grid (whether returned directly as object or as first element of array)
          const singleGrid = Array.isArray(responseData)
            ? responseData[0]
            : Array.isArray(responseData?.data)
              ? responseData.data[0]
              : responseData

          if (!singleGrid) {
            setColumns([])
            setRows([])
            setSnackbarData({
              message:
                response?.message ||
                'No records returned for the selected criteria.',
              severity: 'info',
            })
            setSnackbarOpen(true)
            return
          }

          const rawRows = Array.isArray(singleGrid.data) ? singleGrid.data : []
          const rawCols =
            Array.isArray(singleGrid.columns) && singleGrid.columns.length > 0
              ? singleGrid.columns
              : rawRows.length > 0
              ? Object.keys(rawRows[0]).map((k) => ({ field: k, title: k }))
              : []

          const enrichedCols = enrichColumns(rawCols)
          const enrichedRows = normalizeRows(rawRows, enrichedCols)

          const foundUnitNo = rawRows.find(
            (r) => r?.UnitNo ?? r?.unitNo ?? r?.unitno,
          )
          setUnitNo(
            foundUnitNo
              ? String(
                  foundUnitNo.UnitNo ??
                    foundUnitNo.unitNo ??
                    foundUnitNo.unitno,
                )
              : null,
          )

          setColumns(enrichedCols)
          setRows(enrichedRows)
          setOriginalRows(JSON.parse(JSON.stringify(enrichedRows)))
          setModifiedCells({})

          if (isCalculate) {
            setSnackbarData({
              message: 'Data calculated successfully!',
              severity: 'success',
            })
            setSnackbarOpen(true)
          }
        } else {
          setColumns([])
          setRows([])
          setOriginalRows([])
          setModifiedCells({})
          setSnackbarData({
            message:
              response?.message ||
              (isCalculate
                ? 'Failed to calculate Sign Off Report. Please try again.'
                : 'Failed to fetch Sign Off Report. Please try again.'),
            severity: 'error',
          })
          setSnackbarOpen(true)
        }
      } catch (error) {
        console.error('Error fetching Sign Off Report:', error)
        setColumns([])
        setRows([])
        setOriginalRows([])
        setModifiedCells({})
        setSnackbarData({
          message: isCalculate
            ? 'Error calculating Sign Off Report data. Please try again.'
            : 'Error fetching Sign Off Report data. Please verify your connection.',
          severity: 'error',
        })
        setSnackbarOpen(true)
      } finally {
        setLoading(false)
      }
    },
    [
      keycloak,
      PLANT_ID,
      AOP_YEAR,
      formatDateForAPI,
      getDatesFromAopYear,
      enrichColumns,
      normalizeRows,
    ],
  )

  const handleRemarkCellClick = useCallback((row) => {
    const rawEditable = row?.isEditable ?? row?.IsEditable ?? row?.is_editable
    if (
      rawEditable === false ||
      rawEditable === 'false' ||
      rawEditable === 0 ||
      rawEditable === '0'
    ) {
      return
    }
    setCurrentRemark(row.remarks ?? '')
    setCurrentRowId(row.id ?? row.Id)
    setRemarkDialogOpen(true)
  }, [])

  /**
   * Helper to retrieve all modified rows (from modifiedCells or row comparisons)
   */
  const getModifiedRows = useCallback(() => {
    const fromModifiedCells = Object.values(modifiedCells || {}).filter(
      (r) => r && r.inEdit !== false,
    )
    if (fromModifiedCells.length > 0) {
      return fromModifiedCells
    }

    // Fallback: compare rows against originalRows
    if (
      Array.isArray(rows) &&
      Array.isArray(originalRows) &&
      originalRows.length > 0
    ) {
      return rows.filter((row) => {
        const orig = originalRows.find(
          (o) => o.id === row.id || (row.Id && o.Id === row.Id),
        )
        if (!orig) return false
        const currentRemark = String(row.Remarks ?? row.remarks ?? '').trim()
        const originalRemark = String(orig.Remarks ?? orig.remarks ?? '').trim()
        return currentRemark !== originalRemark
      })
    }

    return []
  }, [modifiedCells, rows, originalRows])

  /**
   * Save Sign Off Report Remarks with validation
   */
  const saveChanges = useCallback(async () => {
    const candidateRows = getModifiedRows()

    if (!candidateRows || candidateRows.length === 0) {
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      setSnackbarOpen(true)
      return
    }

    // Filter only those rows where remark actually changed from originalRows
    const changedRows = candidateRows.filter((row) => {
      const orig = originalRows.find(
        (o) => o.id === row.id || (row.Id && o.Id === row.Id),
      )
      const currentRemark = String(row.remarks ?? '').trim()
      const originalRemark = String(orig?.remarks ?? '').trim()
      return !orig || currentRemark !== originalRemark
    })

    if (changedRows.length === 0) {
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      setSnackbarOpen(true)
      return
    }

    // Remark validation: Remarks cannot be empty for any modified row
    const invalidRows = changedRows.filter((row) => {
      const remark = String(row.remarks ?? '').trim()
      return remark === ''
    })

    if (invalidRows.length > 0) {
      const displayNames = invalidRows
        .map(
          (row) =>
            row.Particulars ||
            row.particulars ||
            row.NormParameterName ||
            row.normParameterName ||
            row.Parameter ||
            row.parameter ||
            (columns[0]?.field ? row[columns[0].field] : '') ||
            `Row ${row.id}`,
        )
        .filter(Boolean)
        .join(', ')

      setSnackbarData({
        message: `Remarks is required for: ${displayNames}`,
        severity: 'error',
      })
      setSnackbarOpen(true)
      return
    }

    // Prepare payload with expected keys: id and remarks
    const signOffReportDTOList = changedRows.map((row) => ({
      id: String(row.Id ?? row.id ?? ''),
      remarks: String(row.remarks ?? row.Remarks ?? '').trim(),
    }))

    setLoading(true)
    try {
      const response = await SignOffReportApiService.saveSignOffReportRemarks(
        keycloak,
        AOP_YEAR,
        signOffReportDTOList,
      )

      const statusCode = response?.code || (response?.status ?? 200)

      if (statusCode === 200) {
        setSnackbarData({
          message:
            response?.message || 'Sign off report remarks saved successfully',
          severity: 'success',
        })
        setSnackbarOpen(true)
        setModifiedCells({})
        setOriginalRows(JSON.parse(JSON.stringify(rows)))
      } else {
        setSnackbarData({
          message:
            response?.message || 'Failed to save Sign Off Report remarks.',
          severity: 'error',
        })
        setSnackbarOpen(true)
      }
    } catch (error) {
      console.error('Error saving Sign Off Report remarks:', error)
      setSnackbarData({
        message:
          error?.response?.data?.message ||
          error?.message ||
          'Error saving Sign Off Report remarks. Please try again.',
        severity: 'error',
      })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [
    getModifiedRows,
    originalRows,
    columns,
    keycloak,
    AOP_YEAR,
  ])

  /**
   * Handle Calculate action:
   * 1. Silently calls the calculateSignOffReport API to trigger the backend stored procedure.
   * 2. Then fetches the updated Sign Off Report data regardless of the result.
   */
  const handleCalculate = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    setRows([])
    setOriginalRows([])
    setModifiedCells({})

    // Resolve periodFrom & periodTo
    let periodFrom = null
    let periodTo = null

    try {
      const configResponse =
        await HistoricPeriodBasisApiService.getConfigurationExecutionDetails(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )

      const configDetails = Array.isArray(configResponse?.data)
        ? configResponse.data
        : Array.isArray(configResponse?.data?.data)
          ? configResponse.data.data
          : []

      const startDateObj = configDetails.find((d) => d.Name === 'StartDate')
      const endDateObj = configDetails.find((d) => d.Name === 'EndDate')

      periodFrom = formatDateForAPI(
        startDateObj?.AttributeValue || startDateObj?.Value || startDateObj?.apr,
      )
      periodTo = formatDateForAPI(
        endDateObj?.AttributeValue || endDateObj?.Value || endDateObj?.apr,
      )
    } catch (configErr) {
      console.warn('Could not fetch config execution details for calculate, using fallback:', configErr)
    }

    if (!periodFrom || !periodTo) {
      const fallbackDates = getDatesFromAopYear(AOP_YEAR)
      if (!periodFrom) periodFrom = fallbackDates.startDate
      if (!periodTo) periodTo = fallbackDates.endDate
    }

    // Step 1: Fire calculate API silently (no notifications, ignore result)
    try {
      await SignOffReportApiService.calculateSignOffReport(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        periodFrom,
        periodTo,
      )
    } catch (calcError) {
      console.error('calculateSignOffReport error (ignored):', calcError)
    }

    // Step 2: Always fetch fresh data after the calculate call
    await fetchSignOffReport(true)
  }, [PLANT_ID, AOP_YEAR, keycloak, formatDateForAPI, getDatesFromAopYear, fetchSignOffReport])

  // Fetch report whenever plant or year changes
  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchSignOffReport()
    }
  }, [PLANT_ID, AOP_YEAR, fetchSignOffReport])

  // Excel filename generator
  const excelFileName = useMemo(() => {
    return generateExcelNameWithoutExt(dataGridStore, gridTitle)
  }, [dataGridStore, gridTitle])

  const permissions = useMemo(() => {
    return {
      showAction: true,
      allAction: true,
      saveBtn: true,
      showCalculate: true,
      calculateDisabled: !PLANT_ID || !AOP_YEAR || loading,
      showExport: false,
      downloadExcelBtnFromUI: true,
      ExcelName: excelFileName,
      showTitle: true,
      titleName: gridTitle,
      remarksEditable: true,
      showNote: Boolean(unitNo),
      note: unitNo ? `(${unitNo})` : '',
      isDaysUomInInteger: true,
      exportReportGroupWise: true,
      isSignOffReport: isFirstTwoColColor,
    }
  }, [excelFileName, gridTitle, unitNo, PLANT_ID, AOP_YEAR, loading, isFirstTwoColColor])

  return (
    <Box sx={{ minHeight: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column' }}>
      {/* Global Loader */}
      <LoaderBackdrop open={loading} />

      {/* Modern Toast Notification */}
      <Notification
        open={snackbarOpen}
        message={snackbarData.message}
        severity={snackbarData.severity}
        onClose={() => setSnackbarOpen(false)}
      />

      <AdvanceKendoTable
        title={gridTitle}
        columns={columns}
        rows={rows}
        setRows={setRows}
        loading={loading}
        pagable={false}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        saveChanges={saveChanges}
        handleCalculate={handleCalculate}
        permissions={permissions}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        extraExportGrid={{ columns: SIGN_OFF_COLUMNS, rows: signOffRows }}
      />

      {/* Second AdvanceKendoTable Component for Sign Off Details */}
      <AdvanceKendoTable
        columns={SIGN_OFF_COLUMNS}
        rows={signOffRows}
        setRows={setSignOffRows}
        pagable={false}
        permissions={{
          allAction: false,
          showAction: false,
          showTitle: false,
          filterable: false,
        }}
      />
    </Box>
  )
}

export default SignOffReport
