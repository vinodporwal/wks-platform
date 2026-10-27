import React, { useState, useEffect, useCallback, useMemo } from 'react'
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
 * SignOffReportFccSezFcc2
 * Used only for Vertical = FCC, Site = SEZ_, Plant = FCC-2.
 * The API returns MANY grids: all of them are rendered, and ONE sign-off
 * table (Prepared / Reviewed / Approved) is rendered below all grids.
 */
const SIGN_OFF_COLUMNS = [
  { field: 'label', title: ' ', editable: false, minWidth: 100, type: 'text', filterable: false },
  { field: 'prepared', title: 'Prepared By', editable: true, minWidth: 100, type: 'text', filterable: false },
  { field: 'reviewed', title: 'Reviewed By', editable: true, minWidth: 100, type: 'text', filterable: false },
  { field: 'approved', title: 'Approved By', editable: true, minWidth: 100, type: 'text', filterable: false },
]

const DEFAULT_GRID_TITLE = 'Sign Off Report'
const EMPTY_GRID_KEY = '__empty__'

const isFalsyEditable = (value) =>
  value === false || value === 'false' || value === 0 || value === '0'

const cloneDeep = (value) => JSON.parse(JSON.stringify(value))

const SignOffReportFccSezFcc2 = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year } = dataGridStore

  const PLANT_ID = plantObject?.id || plantObject?.value || plantObject?.plantId
  const AOP_YEAR = year?.selectedYear || year

  const [loading, setLoading] = useState(false)

  /**
   * grids: [{ key, title, columns, rows, originalRows, modifiedCells, unitNo }]
   */
  const [grids, setGrids] = useState([])

  // Remark dialog (shared, opened only for the clicked grid)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [activeGridKey, setActiveGridKey] = useState(null)

  const [signOffRows, setSignOffRows] = useState([
    { id: '1', label: 'Name', prepared: '', reviewed: '', approved: '' },
    { id: '2', label: 'Designation', prepared: '', reviewed: '', approved: '' },
    { id: '3', label: 'Sign', prepared: '', reviewed: '', approved: '' },
  ])

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({ message: '', severity: 'info' })

  const numberFormatFiveDecimals = useMemo(() => customValueFormatterPhaseTwo(5), [])

  const updateGrid = useCallback((key, patch) => {
    setGrids((prev) =>
      prev.map((g) =>
        g.key === key
          ? { ...g, ...(typeof patch === 'function' ? patch(g) : patch) }
          : g,
      ),
    )
  }, [])

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

  const getDatesFromAopYear = useCallback((aopYear) => {
    if (typeof aopYear === 'string' && aopYear.includes('-')) {
      const startYear = parseInt(aopYear.split('-')[0], 10)
      if (!isNaN(startYear)) {
        return { startDate: `${startYear - 1}-04-01`, endDate: `${startYear}-03-31` }
      }
    }
    return { startDate: null, endDate: null }
  }, [])

  /**
   * periodFrom / periodTo from configuration execution API,
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
      console.warn('Could not fetch configuration execution details, using fallback:', configErr)
    }

    if (!periodFrom || !periodTo) {
      const fallback = getDatesFromAopYear(AOP_YEAR)
      if (!periodFrom) periodFrom = fallback.startDate
      if (!periodTo) periodTo = fallback.endDate
    }

    return { periodFrom, periodTo }
  }, [keycloak, PLANT_ID, AOP_YEAR, formatDateForAPI, getDatesFromAopYear])

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
      return backendCols
        .filter((col) => {
          if (!col || !col.field) return false
          const fieldLower = col.field.trim().toLowerCase()
          const titleLower = col.title?.trim().toLowerCase()
          return !excludeColumns.includes(fieldLower) && !excludeColumns.includes(titleLower)
        })
        .map((col, index) => {
          const isNumber = col.type === 'number'
          const isDate = col.type === 'date'
          const isFirstCol = index === 0
          const isFrozenCol = index === 0 || index === 1
          const formattedTitle = col.title || col.field.replace(/_/g, ' ')
          const isRemarks =
            col.field?.toLowerCase().includes('remarks') ||
            col.title?.toLowerCase().includes('remark')

          const baseConfig = {
            field: col.field,
            title: formattedTitle,
            editable: isRemarks ? true : Boolean(col.editable),
            hidden: col.hidden || false,
            locked: isFrozenCol && backendCols.length > 2,
          }

          if (isRemarks) {
            return {
              ...baseConfig,
              field: 'remarks',
              title: 'Remarks',
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

          return {
            ...baseConfig,
            type: 'text',
            align: 'left',
            headerAlign: 'left',
            widthT: Math.max(isFirstCol ? 260 : 180, formattedTitle.length * 11),
            minWidth: isFirstCol ? 220 : 150,
          }
        })
    },
    [numberFormatFiveDecimals],
  )

  const normalizeRows = useCallback((rawRows = [], cols = []) => {
    if (!Array.isArray(rawRows)) return []

    const numericFields = new Set(cols.filter((c) => c.type === 'number').map((c) => c.field))
    const dateFields = new Set(cols.filter((c) => c.type === 'date').map((c) => c.field))

    return rawRows.map((row, idx) => {
      const normalizedRow = { ...row }

      const rowId = normalizedRow.Id ?? normalizedRow.id ?? idx + 1
      normalizedRow.id = rowId
      normalizedRow.Id = rowId

      const rawEditable =
        normalizedRow.isEditable ?? normalizedRow.IsEditable ?? normalizedRow.is_editable
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

      const isDaysRow = !isUomEmpty && String(rawUom).trim().toLowerCase() === 'days'

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

      dateFields.forEach((field) => {
        const val = normalizedRow[field]
        if (val) {
          const d = new Date(val)
          if (!isNaN(d.getTime())) normalizedRow[field] = d
        }
      })

      normalizedRow.remarks = normalizedRow?.Remarks ?? normalizedRow?.remarks ?? ''

      return normalizedRow
    })
  }, [])

  /**
   * Converts one backend grid into the local grid shape.
   */
  const buildGrid = useCallback(
    (backendGrid, index) => {
      const rawCols = Array.isArray(backendGrid?.columns) ? backendGrid.columns : []
      const rawRows = Array.isArray(backendGrid?.data) ? backendGrid.data : []

      const cols = enrichColumns(rawCols)
      const normRows = normalizeRows(rawRows, cols)
      const foundUnitNo = rawRows.find((r) => r?.UnitNo ?? r?.unitNo ?? r?.unitno)

      // NOTE: adjust these keys to match the real API response
      // The API currently returns gridName = "Id" for every grid, so ignore
      // that value and fall back to a numbered title.
      const rawName = [
        backendGrid?.gridName,
        backendGrid?.gridTitle,
        backendGrid?.name,
        backendGrid?.title,
      ].find((n) => n && String(n).trim() && String(n).trim().toLowerCase() !== 'id')
      const title = rawName ? String(rawName).trim() : `${DEFAULT_GRID_TITLE} ${index + 1}`

      return {
        key: `grid-${index}`,
        title,
        columns: cols,
        rows: normRows,
        originalRows: cloneDeep(normRows),
        modifiedCells: {},
        unitNo: foundUnitNo
          ? String(foundUnitNo.UnitNo ?? foundUnitNo.unitNo ?? foundUnitNo.unitno)
          : null,
      }
    },
    [enrichColumns, normalizeRows],
  )

  /**
   * Fetch all Sign Off Report grids.
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

        if (statusCode === 200 && responseData) {
          const gridList = Array.isArray(responseData)
            ? responseData
            : Array.isArray(responseData?.data)
              ? responseData.data
              : [responseData]

          const built = gridList
            .filter(Boolean)
            // Hide GENERAL_NOTES grids
            .filter((g) => {
              const first = Array.isArray(g.data) ? g.data[0] : null
              const gridType = first?.GRID_TYPE ?? first?.gridType ?? first?.grid_type
              return gridType !== 'GENERAL_NOTES'
            })
            .map((g, i) => buildGrid(g, i))

          if (built.length === 0) {
            setGrids([])
            setSnackbarData({
              message: response?.message || 'No records returned for the selected criteria.',
              severity: 'info',
            })
            setSnackbarOpen(true)
            return
          }

          setGrids(built)

          if (isCalculate) {
            setSnackbarData({ message: 'Data calculated successfully!', severity: 'success' })
            setSnackbarOpen(true)
          }
        } else {
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
    [keycloak, PLANT_ID, AOP_YEAR, resolvePeriod, buildGrid],
  )

  /**
   * Refresh ONLY one grid (used by that grid's own Calculate button).
   * Other grids keep their current rows and unsaved remarks.
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

        if (statusCode === 200 && responseData) {
          const gridList = Array.isArray(responseData)
            ? responseData
            : Array.isArray(responseData?.data)
              ? responseData.data
              : [responseData]

          const visibleGrids = gridList.filter(Boolean).filter((g) => {
            const first = Array.isArray(g.data) ? g.data[0] : null
            const gridType = first?.GRID_TYPE ?? first?.gridType ?? first?.grid_type
            return gridType !== 'GENERAL_NOTES'
          })

          const backendGrid = visibleGrids[gridIndex]

          if (!backendGrid) {
            setSnackbarData({
              message: response?.message || 'No records returned for this grid.',
              severity: 'info',
            })
            setSnackbarOpen(true)
            return
          }

          // Replace only this grid
          updateGrid(gridKey, buildGrid(backendGrid, gridIndex))

          if (isCalculate) {
            setSnackbarData({ message: 'Data calculated successfully!', severity: 'success' })
            setSnackbarOpen(true)
          }
        } else {
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
    [keycloak, PLANT_ID, AOP_YEAR, resolvePeriod, buildGrid, updateGrid],
  )

  const handleRemarkCellClick = useCallback((gridKey, row) => {
    const rawEditable = row?.isEditable ?? row?.IsEditable ?? row?.is_editable
    if (isFalsyEditable(rawEditable)) return
    setActiveGridKey(gridKey)
    setCurrentRemark(row.remarks ?? '')
    setCurrentRowId(row.id ?? row.Id)
    setRemarkDialogOpen(true)
  }, [])

  /**
   * Rows of ONE grid whose remark actually changed.
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
  const saveGridChanges = useCallback(async (gridKey) => {
    const targetGrid = grids.find((g) => g.key === gridKey)
    if (!targetGrid) return

    const changedRows = []
    const firstColFieldByRow = new Map()

    getChangedRowsForGrid(targetGrid).forEach((row) => {
      changedRows.push(row)
      firstColFieldByRow.set(row, targetGrid.columns?.[0]?.field)
    })

    if (changedRows.length === 0) {
      setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
      setSnackbarOpen(true)
      return
    }

    const invalidRows = changedRows.filter((row) => String(row.remarks ?? '').trim() === '')

    if (invalidRows.length > 0) {
      const displayNames = invalidRows
        .map((row) => {
          const firstField = firstColFieldByRow.get(row)
          return (
            row.Particulars ||
            row.particulars ||
            row.NormParameterName ||
            row.normParameterName ||
            row.Parameter ||
            row.parameter ||
            (firstField ? row[firstField] : '') ||
            `Row ${row.id}`
          )
        })
        .filter(Boolean)
        .join(', ')

      setSnackbarData({ message: `Remarks is required for: ${displayNames}`, severity: 'error' })
      setSnackbarOpen(true)
      return
    }

    const signOffReportDTOList = changedRows.map((row) => ({
      id: String(row.Id ?? row.id ?? ''),
      remarks: String(row.remarks ?? '').trim(),
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
          message: response?.message || 'Sign off report remarks saved successfully',
          severity: 'success',
        })
        setSnackbarOpen(true)
        // Reset modified state only for the saved grid
        updateGrid(gridKey, (cur) => ({
          modifiedCells: {},
          originalRows: cloneDeep(cur.rows),
        }))
      } else {
        setSnackbarData({
          message: response?.message || 'Failed to save Sign Off Report remarks.',
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
  }, [grids, getChangedRowsForGrid, updateGrid, keycloak, AOP_YEAR])

  /**
   * Calculate for ONE grid: trigger backend calculation (result ignored),
   * then refresh only that grid.
   * NOTE: calculateSignOffReport has no grid parameter, so the backend
   * recalculates for the plant / year; the UI refreshes just this grid.
   */
  const handleCalculateGrid = useCallback(async (gridKey) => {
    if (!PLANT_ID || !AOP_YEAR) return

    const { periodFrom, periodTo } = await resolvePeriod()

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

    if (gridKey === EMPTY_GRID_KEY) {
      // No data loaded yet: load every grid
      await fetchSignOffReport(true)
    } else {
      await fetchSingleGrid(gridKey, true)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak, resolvePeriod, fetchSignOffReport, fetchSingleGrid])

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchSignOffReport()
    }
  }, [PLANT_ID, AOP_YEAR, fetchSignOffReport])

  /**
   * Every grid has its own Save / Calculate buttons.
   */
  const getPermissions = useCallback(
    (grid, index) => ({
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
    }),
    [PLANT_ID, AOP_YEAR, loading, dataGridStore],
  )

  // Keep one empty grid when there is no data so Calculate stays available
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
    <Box sx={{ minHeight: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column' }}>
      <LoaderBackdrop open={loading} />

      <Notification
        open={snackbarOpen}
        message={snackbarData.message}
        severity={snackbarData.severity}
        onClose={() => setSnackbarOpen(false)}
      />

      {/* All report grids */}
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
          permissions={getPermissions(g, index)}
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          extraExportGrid={
            index === 0 ? { columns: SIGN_OFF_COLUMNS, rows: signOffRows } : undefined
          }
        />
      ))}

      {/* ONE sign-off table below ALL grids */}
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

export default SignOffReportFccSezFcc2