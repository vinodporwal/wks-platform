import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react'
import { Box } from '@mui/material'
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
 * and dynamically renders ONE grid for EVERY grid returned by the API
 * (grid 1, grid 2, ... added automatically one below the other),
 * with 5 decimal places for numbers.
 *
 * - Every grid has its own Save and Calculate buttons.
 * - ONE sign-off table (Prepared / Reviewed / Approved) is shown below all grids.
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

const DEFAULT_GRID_TITLE = 'Sign Off Report'
const EMPTY_GRID_KEY = '__empty__'

const isFalsyEditable = (value) =>
  value === false || value === 'false' || value === 0 || value === '0'

const cloneDeep = (value) => JSON.parse(JSON.stringify(value))

// First column that really has a field (columns can be wrapped in groups)
const getFirstLeafField = (cols = []) => {
  for (const c of cols) {
    if (c?.children && Array.isArray(c.children)) {
      const f = getFirstLeafField(c.children)
      if (f) return f
    } else if (c?.field) {
      return c.field
    }
  }
  return undefined
}

// Grids with GRID_TYPE === 'GENERAL_NOTES' are not shown
const isGeneralNotesGrid = (g) => {
  const first = Array.isArray(g?.data) ? g.data[0] : null
  const gridType = first?.GRID_TYPE ?? first?.gridType ?? first?.grid_type
  return gridType === 'GENERAL_NOTES'
}

const SignOffReport = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, siteObject } = dataGridStore

  // Store variables
  const PLANT_ID = plantObject?.id || plantObject?.value || plantObject?.plantId
  const AOP_YEAR = year?.selectedYear || year

  const [loading, setLoading] = useState(false)

  /**
   * grids: [{ key, title, columns, rows, originalRows, modifiedCells, unitNo }]
   * One entry per grid returned by the API.
   */
  const [grids, setGridsState] = useState([])

  // Always holds the latest grids. AdvanceKendoTable calls setModifiedCells()
  // from INSIDE a setRows() updater, so both updates must be applied
  // synchronously on the latest value, otherwise the modifiedCells update is
  // lost and the Save button stays disabled.
  const gridsRef = useRef([])
  const setGrids = useCallback((valueOrFn) => {
    const next =
      typeof valueOrFn === 'function' ? valueOrFn(gridsRef.current) : valueOrFn
    gridsRef.current = next
    setGridsState(next)
  }, [])

  // Remark dialog (shared state, opened only for the clicked grid)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [activeGridKey, setActiveGridKey] = useState(null)

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
   * Patch ONE grid by key. `patch` can be an object or (currentGrid) => object.
   * The patch is resolved first (it may trigger nested updates such as
   * setModifiedCells inside setRows) and then merged into the latest grids.
   */
  const updateGrid = useCallback(
    (key, patch) => {
      const current = gridsRef.current.find((g) => g.key === key)
      if (!current) return
      const resolved = typeof patch === 'function' ? patch(current) : patch
      setGrids((prev) =>
        prev.map((g) => (g.key === key ? { ...g, ...resolved } : g)),
      )
    },
    [setGrids],
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
   * periodFrom / periodTo from the configuration execution API,
   * falling back to AOP year based dates.
   */
  const resolvePeriod = useCallback(async () => {
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
      console.warn(
        'Could not fetch configuration execution details, falling back to AOP year calculation:',
        configErr,
      )
    }

    if (!periodFrom || !periodTo) {
      const fallbackDates = getDatesFromAopYear(AOP_YEAR)
      if (!periodFrom) periodFrom = fallbackDates.startDate
      if (!periodTo) periodTo = fallbackDates.endDate
    }

    return { periodFrom, periodTo }
  }, [keycloak, PLANT_ID, AOP_YEAR, formatDateForAPI, getDatesFromAopYear])

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
            !excludeColumns.includes(fieldUpper) &&
            !excludeColumns.includes(titleUpper)
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
                /^(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)/i.test(
                  titleClean,
                )))
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
      const rowId = normalizedRow.Id ?? normalizedRow.id ?? idx + 1
      normalizedRow.id = rowId
      normalizedRow.Id = rowId

      // Normalize isEditable flag as boolean for row-level permissions
      const rawEditable =
        normalizedRow.isEditable ??
        normalizedRow.IsEditable ??
        normalizedRow.is_editable
      const isRowEditable = !isFalsyEditable(rawEditable)
      normalizedRow.isEditable = isRowEditable
      normalizedRow.IsEditable = isRowEditable

      const rawUom = normalizedRow.UOM ?? normalizedRow.uom
      const isUomEmpty =
        rawUom === undefined || rawUom === null || String(rawUom).trim() === ''
      if (isUomEmpty) {
        normalizedRow.isNoUom = true
        normalizedRow.isEditable = false
        normalizedRow.IsEditable = false
      }

      const isDaysRow =
        !isUomEmpty && String(rawUom).trim().toLowerCase() === 'days'

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
      normalizedRow.remarks = normalizedRow?.Remarks ?? normalizedRow?.remarks ?? ''

      return normalizedRow
    })
  }, [])

  /**
   * Converts ONE backend grid into the local grid shape.
   * `total` = number of grids in the response (used for the title fallback).
   */
  const buildGrid = useCallback(
    (backendGrid, index, total = 1) => {
      const rawRows = Array.isArray(backendGrid?.data) ? backendGrid.data : []
      const rawCols =
        Array.isArray(backendGrid?.columns) && backendGrid.columns.length > 0
          ? backendGrid.columns
          : rawRows.length > 0
            ? Object.keys(rawRows[0]).map((k) => ({ field: k, title: k }))
            : []

      const cols = enrichColumns(rawCols)
      const normRows = normalizeRows(rawRows, cols)
      const foundUnitNo = rawRows.find(
        (r) => r?.UnitNo ?? r?.unitNo ?? r?.unitno,
      )

      // The API currently returns gridName = "Id" for every grid, so that
      // value is ignored. Adjust the keys below if the API gets a real name.
      const rawName = [
        backendGrid?.gridName,
        backendGrid?.gridTitle,
        backendGrid?.name,
        backendGrid?.title,
      ].find(
        (n) => n && String(n).trim() && String(n).trim().toLowerCase() !== 'id',
      )
      const title = rawName
        ? String(rawName).trim()
        : total > 1
          ? `${DEFAULT_GRID_TITLE} ${index + 1}`
          : DEFAULT_GRID_TITLE

      return {
        key: `grid-${index}`,
        title,
        columns: cols,
        rows: normRows,
        originalRows: cloneDeep(normRows),
        modifiedCells: {},
        unitNo: foundUnitNo
          ? String(
              foundUnitNo.UnitNo ?? foundUnitNo.unitNo ?? foundUnitNo.unitno,
            )
          : null,
      }
    },
    [enrichColumns, normalizeRows],
  )

  /**
   * Calls the Sign Off Report API and returns the list of visible grids.
   */
  const requestVisibleGrids = useCallback(async () => {
    const { periodFrom, periodTo } = await resolvePeriod()

    const response = await SignOffReportApiService.getSignOffReport(
      keycloak,
      PLANT_ID,
      AOP_YEAR,
      periodFrom,
      periodTo,
    )

    const statusCode = response?.code || (response?.status ?? 200)
    const responseData = response?.data

    if (statusCode !== 200 || !responseData) {
      return { ok: false, response, backendGrids: [] }
    }

    // Response can be: array of grids | { data: [grids] } | single grid object
    const gridList = Array.isArray(responseData)
      ? responseData
      : Array.isArray(responseData?.data)
        ? responseData.data
        : [responseData]

    const backendGrids = gridList.filter(Boolean).filter((g) => !isGeneralNotesGrid(g))

    return { ok: true, response, backendGrids }
  }, [keycloak, PLANT_ID, AOP_YEAR, resolvePeriod])

  /**
   * Fetch ALL grids returned by the API.
   */
  const fetchSignOffReport = useCallback(
    async (isCalculate = false) => {
      if (!PLANT_ID || !AOP_YEAR) {
        setGrids([])
        return
      }

      setLoading(true)
      if (isCalculate) {
        setSnackbarData({ message: 'Calculating...', severity: 'info' })
        setSnackbarOpen(true)
      }

      try {
        const { ok, response, backendGrids } = await requestVisibleGrids()

        if (!ok) {
          setGrids([])
          setSnackbarData({
            message:
              response?.message ||
              (isCalculate
                ? 'Failed to calculate Sign Off Report. Please try again.'
                : 'Failed to fetch Sign Off Report. Please try again.'),
            severity: 'error',
          })
          setSnackbarOpen(true)
          return
        }

        if (backendGrids.length === 0) {
          setGrids([])
          setSnackbarData({
            message:
              response?.message ||
              'No records returned for the selected criteria.',
            severity: 'info',
          })
          setSnackbarOpen(true)
          return
        }

        setGrids(
          backendGrids.map((g, i) => buildGrid(g, i, backendGrids.length)),
        )

        if (isCalculate) {
          setSnackbarData({
            message: 'Data calculated successfully!',
            severity: 'success',
          })
          setSnackbarOpen(true)
        }
      } catch (error) {
        console.error('Error fetching Sign Off Report:', error)
        setGrids([])
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
    [PLANT_ID, AOP_YEAR, setGrids, requestVisibleGrids, buildGrid],
  )

  /**
   * Refresh ONLY one grid (used by that grid's own Calculate button).
   * Other grids keep their rows and unsaved remarks.
   * Grids are matched by position in the response ("grid-0", "grid-1", ...).
   */
  const fetchSingleGrid = useCallback(
    async (gridKey, isCalculate = false) => {
      const gridIndex = Number(String(gridKey).replace('grid-', ''))
      if (!PLANT_ID || !AOP_YEAR || Number.isNaN(gridIndex)) return

      setLoading(true)
      if (isCalculate) {
        setSnackbarData({ message: 'Calculating...', severity: 'info' })
        setSnackbarOpen(true)
      }

      try {
        const { ok, response, backendGrids } = await requestVisibleGrids()

        if (!ok) {
          setSnackbarData({
            message:
              response?.message ||
              (isCalculate
                ? 'Failed to calculate Sign Off Report. Please try again.'
                : 'Failed to fetch Sign Off Report. Please try again.'),
            severity: 'error',
          })
          setSnackbarOpen(true)
          return
        }

        const backendGrid = backendGrids[gridIndex]
        if (!backendGrid) {
          setSnackbarData({
            message: response?.message || 'No records returned for this grid.',
            severity: 'info',
          })
          setSnackbarOpen(true)
          return
        }

        // Replace only this grid
        updateGrid(gridKey, buildGrid(backendGrid, gridIndex, backendGrids.length))

        if (isCalculate) {
          setSnackbarData({
            message: 'Data calculated successfully!',
            severity: 'success',
          })
          setSnackbarOpen(true)
        }
      } catch (error) {
        console.error('Error refreshing Sign Off Report grid:', error)
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
    [PLANT_ID, AOP_YEAR, requestVisibleGrids, buildGrid, updateGrid],
  )

  /**
   * Remark cell click. Remembers which grid was clicked so that only that
   * grid's dialog opens.
   */
  const handleRemarkCellClick = useCallback((gridKey, row) => {
    const rawEditable = row?.isEditable ?? row?.IsEditable ?? row?.is_editable
    if (isFalsyEditable(rawEditable)) return
    setActiveGridKey(gridKey)
    setCurrentRemark(row.remarks ?? '')
    setCurrentRowId(row.id ?? row.Id)
    setRemarkDialogOpen(true)
  }, [])

  /**
   * Rows of ONE grid whose remark actually changed
   * (from modifiedCells, or by comparing rows with originalRows).
   */
  const getChangedRowsForGrid = useCallback((grid) => {
    const fromModifiedCells = Object.values(grid.modifiedCells || {}).filter(
      (r) => r && r.inEdit !== false,
    )

    const candidates =
      fromModifiedCells.length > 0
        ? fromModifiedCells
        : (grid.rows || []).filter((row) => {
            const orig = (grid.originalRows || []).find(
              (o) => o.id === row.id || (row.Id && o.Id === row.Id),
            )
            if (!orig) return false
            const cur = String(row.Remarks ?? row.remarks ?? '').trim()
            const old = String(orig.Remarks ?? orig.remarks ?? '').trim()
            return cur !== old
          })

    return candidates.filter((row) => {
      const orig = (grid.originalRows || []).find(
        (o) => o.id === row.id || (row.Id && o.Id === row.Id),
      )
      const cur = String(row.remarks ?? '').trim()
      const old = String(orig?.remarks ?? '').trim()
      return !orig || cur !== old
    })
  }, [])

  /**
   * Save remarks of ONE grid (the grid whose Save button was clicked).
   */
  const saveGridChanges = useCallback(
    async (gridKey) => {
      const targetGrid = gridsRef.current.find((g) => g.key === gridKey)
      if (!targetGrid) return

      const changedRows = getChangedRowsForGrid(targetGrid)

      if (changedRows.length === 0) {
        setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
        setSnackbarOpen(true)
        return
      }

      // Remark validation: Remarks cannot be empty for any modified row
      const invalidRows = changedRows.filter(
        (row) => String(row.remarks ?? '').trim() === '',
      )

      if (invalidRows.length > 0) {
        const firstField = getFirstLeafField(targetGrid.columns)
        const displayNames = invalidRows
          .map(
            (row) =>
              row.Particulars ||
              row.particulars ||
              row.NormParameterName ||
              row.normParameterName ||
              row.Parameter ||
              row.parameter ||
              (firstField ? row[firstField] : '') ||
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

      // Payload with expected keys: id and remarks
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
          // Reset modified state only for the saved grid
          updateGrid(gridKey, (cur) => ({
            modifiedCells: {},
            originalRows: cloneDeep(cur.rows),
          }))
          await fetchSignOffReport()
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
    },
    [getChangedRowsForGrid, updateGrid, keycloak, AOP_YEAR, fetchSignOffReport],
  )

  /**
   * Calculate for ONE grid:
   * 1. Silently calls calculateSignOffReport (backend stored procedure).
   * 2. Then refreshes only that grid.
   * NOTE: calculateSignOffReport has no grid parameter, so the backend
   * recalculates for the plant / year; the UI refreshes just this grid.
   */
  const handleCalculateGrid = useCallback(
    async (gridKey) => {
      if (!PLANT_ID || !AOP_YEAR) return

      const { periodFrom, periodTo } = await resolvePeriod()

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
      if (gridKey === EMPTY_GRID_KEY) {
        // No data loaded yet: load every grid
        await fetchSignOffReport(true)
      } else {
        await fetchSingleGrid(gridKey, true)
      }
    },
    [PLANT_ID, AOP_YEAR, keycloak, resolvePeriod, fetchSignOffReport, fetchSingleGrid],
  )

  // Fetch report whenever plant or year changes
  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchSignOffReport()
    }
  }, [PLANT_ID, AOP_YEAR, fetchSignOffReport])

  // Permissions are built per grid (own Save / Calculate, title, note, Excel name)
  const getPermissions = useCallback(
    (grid) => ({
      showAction: true,
      allAction: true,
      saveBtn: true,
      showCalculate: true,
      calculateDisabled: !PLANT_ID || !AOP_YEAR || loading,
      showExport: false,
      downloadExcelBtnFromUI: true,
      ExcelName: generateExcelNameWithoutExt(dataGridStore, grid.title),
      showTitle: true,
      titleName: grid.title,
      remarksEditable: true,
      showNote: Boolean(grid.unitNo),
      note: grid.unitNo ? `(${grid.unitNo})` : '',
      isDaysUomInInteger: true,
      exportReportGroupWise: true,
      isSignOffReport: isFirstTwoColColor,
    }),
    [PLANT_ID, AOP_YEAR, loading, dataGridStore, isFirstTwoColColor],
  )

  // When there is no data yet, still render one empty grid so the toolbar
  // (Calculate / Save) stays available, same as the original behaviour.
  const displayGrids = useMemo(() => {
    if (grids.length > 0) return grids
    return [
      {
        key: EMPTY_GRID_KEY,
        title: DEFAULT_GRID_TITLE,
        columns: [],
        rows: [],
        originalRows: [],
        modifiedCells: {},
        unitNo: null,
      },
    ]
  }, [grids])

  return (
    <Box
      sx={{
        minHeight: 'calc(100vh - 120px)',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* Global Loader */}
      <LoaderBackdrop open={loading} />

      {/* Modern Toast Notification */}
      <Notification
        open={snackbarOpen}
        message={snackbarData.message}
        severity={snackbarData.severity}
        onClose={() => setSnackbarOpen(false)}
      />

      {/* One grid for every grid in the API response, added automatically below each other */}
      {displayGrids.map((g, index) => (
        <AdvanceKendoTable
          key={g.key}
          title={g.title}
          columns={g.columns}
          rows={g.rows}
          setRows={(value) =>
            updateGrid(g.key, (cur) => ({
              rows: typeof value === 'function' ? value(cur.rows) : value,
            }))
          }
          loading={loading}
          pagable={false}
          handleRemarkCellClick={(row) => handleRemarkCellClick(g.key, row)}
          // Open the dialog only for the grid that was clicked
          remarkDialogOpen={remarkDialogOpen && activeGridKey === g.key}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={activeGridKey === g.key ? currentRowId : null}
          setCurrentRowId={setCurrentRowId}
          modifiedCells={g.modifiedCells}
          setModifiedCells={(value) =>
            updateGrid(g.key, (cur) => ({
              modifiedCells:
                typeof value === 'function' ? value(cur.modifiedCells) : value,
            }))
          }
          saveChanges={() => saveGridChanges(g.key)}
          handleCalculate={() => handleCalculateGrid(g.key)}
          permissions={getPermissions(g)}
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          extraExportGrid={
            index === 0
              ? { columns: SIGN_OFF_COLUMNS, rows: signOffRows }
              : undefined
          }
        />
      ))}

      {/* ONE Sign Off Details table, always below ALL grids */}
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