import { Box, Button, Typography, useTheme } from '@mui/material'
import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import { DataGrid } from '@mui/x-data-grid'
import {
  ExcelExport,
  ExcelExportColumn,
} from '@progress/kendo-react-excel-export'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useSelector } from 'react-redux'
import { DataService } from 'services/DataService'
import { useSession } from 'SessionStoreContext'
import {
  CustomAccordion,
  CustomAccordionDetails,
  CustomAccordionSummary,
} from 'utils/CustomAccrodian'
import ConsumptionNormsHistorianBasis from './ConsumptionNormsHistorianBasis'
import { DataSetaApiService } from 'services/data-set-api-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { FileExportIcon } from 'assets/images/icons/index'
const REPORT_TYPE_FOR_ALL = 'CausticSodaLye'

const CausticSodaLyeBasis = () => {
  const keycloak = useSession()
  const theme = useTheme()
  const isDark = theme.palette.mode === 'dark'
  const [dataMap, setDataMap] = useState({})
  const [gridNames, setGridNames] = useState([])
  const [loading, setLoading] = useState(false)
  const [tabIndex, setTabIndex] = useState(0)

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    yearChanged,
    oldYear,
    plantID,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
  } = dataGridStore
  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id

  const PLANT_NAME_U = plantObject?.name?.toUpperCase()
  const SITE_NAME_U = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_U = verticalObject?.name?.toUpperCase()

  const AOP_YEAR = year?.selectedYear
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const EXCEL_NAME = `${VERTICAL_NAME_U}_${SITE_NAME_U}_${PLANT_NAME_U}_Caustic_Soda_Lye_Norms_${AOP_YEAR}`

  const timeoutIdsRef = useRef([])
  const isMountedRef = useRef(true)
  const exportRefs = useRef({})

  // ---------- ADDED refs for workbook / transient excel export (from reference)
  const workbookRef = useRef(null)
  const excelExportRef = useRef(null)
  // -----------------------------------------------------------------------

  useEffect(() => {
    isMountedRef.current = true

    return () => {
      isMountedRef.current = false
      timeoutIdsRef.current.forEach((t) => clearTimeout(t))
      timeoutIdsRef.current = []
    }
  }, [])

  const enrichColumns = useCallback(
    (backendCols = []) => {
      function countDecimals(value) {
        if (value == null) return 0
        const s = String(value).replace(/,/g, '').trim()
        if (!s.includes('.')) return 0
        const frac = s.split('.')[1] || ''
        // remove trailing zeros from the fractional part (so 2024.0 -> 0 decimals)
        const fracNoTrailing = frac.replace(/0+$/, '')
        return fracNoTrailing.length
      }

      const isManyColumns = backendCols.length > 15

      return backendCols
        .filter((col) => col.field !== 'GRID_TYPE')
        .map((col) => {
          const isTextCol = col.type === 'string'
          const isNumberCol = col.type === 'number'

          const base = {
            ...col,
            title: col.title || col.field,
            filterable: true,

            flex: isManyColumns ? undefined : 1,
            width: isManyColumns ? 150 : undefined,
            filter: isTextCol ? 'text' : isNumberCol ? 'numeric' : undefined,
            editable: false,
            headerAlign: 'left',
            align: isNumberCol ? 'right' : 'left',
          }

          if (!isNumberCol) return base

          return {
            ...base,
            renderCell: (params) => {
              const original = params?.row?.[col.field] ?? params?.value
              const decimals = countDecimals(original)
              const decimalsToShow = Math.min(Math.max(decimals, 0), 3)

              const text =
                params?.value == null || params?.value === ''
                  ? ''
                  : decimalsToShow === 0
                    ? String(Number(params.value))
                    : Number(params.value).toFixed(decimalsToShow)

              return (
                <div
                  title={String(params.value)}
                  style={{
                    width: '100%',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                    textAlign: 'right',
                  }}
                >
                  {text}
                </div>
              )
            },
          }
        })
    },
    [keycloak, PLANT_ID, AOP_YEAR],
  )

  function isValidDateString(str) {
    if (typeof str !== 'string') return false

    // Common date patterns
    const datePatterns = [
      /^\d{1,2}[-/]\d{1,2}[-/]\d{4}$/, // DD-MM-YYYY or DD/MM/YYYY
      /^\d{4}[-/]\d{1,2}[-/]\d{1,2}$/, // YYYY-MM-DD or YYYY/MM/DD
      /^[A-Za-z]{3}\s+\d{1,2},\s+\d{4}/, // "Apr 1, 2025" format
      /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/, // ISO format
    ]

    const matchesPattern = datePatterns.some((pattern) =>
      pattern.test(str.trim()),
    )

    if (!matchesPattern && !/[-/,\s:]/.test(str)) {
      return false
    }

    return matchesPattern
  }

  function inferColumnsFromRows(rows = []) {
    const fieldSet = new Set()
    rows.forEach((r) => {
      if (!r || typeof r !== 'object') return
      Object.keys(r).forEach((k) => fieldSet.add(k))
    })

    const fields = Array.from(fieldSet)

    const cols = fields.map((f) => {
      let detectedType = 'string'
      for (const r of rows) {
        if (!r) continue
        const v = r?.[f]
        if (v === undefined || v === null || v === '') continue
        if (typeof v === 'number') {
          detectedType = 'number'
          break
        }

        const d = new Date(v)
        if (!isNaN(d.getTime()) && isValidDateString(v)) {
          detectedType = 'date'
          break
        }
        const numericCandidate = String(v).replace(/[,]/g, '')
        if (!isNaN(Number(numericCandidate))) {
          detectedType = 'number'
          break
        }
      }
      return { field: f, title: f, type: detectedType }
    })

    return cols
  }

  function normalizeRowValues(row = {}, columns = []) {
    const parsed = { ...row }
    columns.forEach((c) => {
      const raw = row[c.field]
      if (raw === undefined || raw === null || raw === '') {
        parsed[c.field] = raw === 0 ? 0 : null
        return
      }
      if (c.type === 'number') {
        parsed[c.field] =
          typeof raw === 'number'
            ? raw
            : Number(String(raw).replace(/[,]/g, ''))
        if (Number.isNaN(parsed[c.field])) parsed[c.field] = null
        return
      }
      if (c.type === 'date') {
        const d = new Date(raw)
        parsed[c.field] = !isNaN(d.getTime()) ? d : null
        return
      }
    })
    return parsed
  }

  const fetchAllGrids = useCallback(async () => {
    setGridNames([])
    setDataMap({})
    if (!PLANT_ID || !AOP_YEAR) return
    // clear previous timers if any
    timeoutIdsRef.current.forEach((t) => clearTimeout(t))
    timeoutIdsRef.current = []

    try {
      setLoading(true)

      const configData = await DataService.getConfigurationExecutionDetails(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (configData?.code !== 200) {
        setLoading(false)
        return
      }
      const StartDate = configData.data.find(
        (d) => d.Name === 'StartDate',
      )?.AttributeValue
      const EndDate = configData.data.find(
        (d) => d.Name === 'EndDate',
      )?.AttributeValue
      if (!StartDate || !EndDate) {
        setGridNames([])
        setDataMap({})
        setLoading(false)
        return
      }

      // Call the API that returns combined grids. Change REPORT_TYPE_FOR_ALL if needed.
      const apiResponse = await DataService.getProductionVolDataBasisPe(
        keycloak,
        REPORT_TYPE_FOR_ALL,
        StartDate,
        EndDate,
        null,
        PLANT_ID,
        AOP_YEAR,
      )

      if (apiResponse?.code !== 200) {
        setGridNames([])
        setDataMap({})
        setLoading(false)
        return
      }

      // Support two possible shapes for convenience:
      // 1) apiResponse.data is the array of grids
      // 2) apiResponse.data.data is the array (older wrappers)
      const gridsArray = Array.isArray(apiResponse.data)
        ? apiResponse.data
        : Array.isArray(apiResponse.data?.data)
          ? apiResponse.data.data
          : []

      if (!Array.isArray(gridsArray) || gridsArray.length === 0) {
        setGridNames([])
        setDataMap({})
        setLoading(false)
        return
      }

      const normalizedNames = gridsArray.map((g) => g.gridName)
      setGridNames(normalizedNames)

      const newMap = {}
      gridsArray.forEach((g) => {
        const rawRows = Array.isArray(g.data) ? g.data : []
        // BEFORE:
        // const inferredCols = inferColumnsFromRows(rawRows)

        // AFTER:
        const inferredCols =
          Array.isArray(g.columns) && g.columns.length
            ? g.columns
            : inferColumnsFromRows(rawRows)

        const enrichedCols = enrichColumns(inferredCols)

        const rowsWithId = rawRows.map((r, i) => {
          const parsed = normalizeRowValues(r, inferredCols)
          return { ...parsed, id: i, isEditable: false }
        })

        newMap[g.gridName] = { rows: rowsWithId, columns: enrichedCols }
      })

      if (isMountedRef.current) setDataMap(newMap)
    } catch (err) {
      console.error('Error fetching all grids (new shape):', err)
    } finally {
      if (isMountedRef.current) setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, enrichColumns])

  useEffect(() => {
    if (tabIndex == 0) {
      fetchAllGrids()
      return () => {
        timeoutIdsRef.current.forEach((t) => clearTimeout(t))
        timeoutIdsRef.current = []
      }
    }
  }, [
    fetchAllGrids,
    keycloak,
    PLANT_ID,
    AOP_YEAR,
    oldYear,
    yearChanged,
    tabIndex,
  ])

  // eslint-disable-next-line
  const INVALID_SHEET_CHARS_RE = /[\\\/\?\*\[\]\:]/g
  function sanitizeSheetName(name = '', fallback = 'Sheet') {
    let s = String(name || '')
      .replace(INVALID_SHEET_CHARS_RE, ' ')
      .trim()
    if (s.length === 0) s = fallback
    if (s.length > 31) s = s.slice(0, 31)
    return s
  }

  function normalizeCellValue(v) {
    if (v === undefined || v === null) return ''
    if (v instanceof Date) return v
    if (typeof v === 'object') {
      try {
        return JSON.stringify(v)
      } catch {
        return String(v)
      }
    }
    return v
  }

  const [isExporting, setIsExporting] = useState(false)

  // ---------------------------------------------------------------------------
  // Modified export flow: build workbookOptions and set workbookRef + isExporting
  // then a useEffect will call excelExportRef.current.toDataURL/save exactly like
  // your BestAchievedNorms reference. No other logic touched.
  // ---------------------------------------------------------------------------
  const exportAllGrids = useCallback(() => {
    try {
      const sheets = gridNames
        .map((gridName, idx) => {
          const d = dataMap[gridName] || { rows: [], columns: [] }
          const cols = d.columns || []
          const rows = d.rows || []
          if (!cols.length && !rows.length) return null

          // filter out hidden columns (including Material_FK_Id / materialFkId)
          let filteredCols = cols.filter(
            (c) =>
              !(
                c &&
                (c.field === 'Material_FK_Id' || c.field === 'materialFkId')
              ) && !c.hidden,
          )

          const sheetColumns = filteredCols.map((c) => ({
            autoWidth: true,
            title: c.title || c.field || '',
          }))

          const headerRow = {
            cells: filteredCols.map((c) => ({
              value: c.title || c.field || '',
            })),
          }

          // helper to find match for coloring (use lookup map for O(1))
          // NOTE: redLookupRef isn't defined here; if you need coloring logic similar to other file,
          // you can add a lookup ref. For now, keep as-is (no coloring) unless you want it.
          const dataRows = rows.map((r) => ({
            cells: filteredCols.map((c) => ({
              value: normalizeCellValue(r?.[c.field]),
            })),
          }))

          const sheetRows = [headerRow, ...dataRows]

          return {
            title: sanitizeSheetName(gridName, `Sheet${idx + 1}`),
            columns: sheetColumns,
            rows: sheetRows,
          }
        })
        .filter(Boolean)

      if (!sheets.length) return

      const workbookOptions = { sheets }
      // set workbook options and trigger transient ExcelExport to generate file
      workbookRef.current = workbookOptions
      setIsExporting(true)
    } catch (err) {
      console.error('Export prepare failed:', err)
    }
  }, [gridNames, dataMap])
  // ---------------------------------------------------------------------------

  // This effect runs when isExporting is set and actually generates & downloads file
  useEffect(() => {
    if (!isExporting) return

    let cancelled = false

    ;(async () => {
      try {
        if (excelExportRef.current && workbookRef.current) {
          if (typeof excelExportRef.current.toDataURL === 'function') {
            const dataUrl = await excelExportRef.current.toDataURL(
              workbookRef.current,
            )
            if (cancelled) return

            // Convert data URL to blob then trigger download programmatically.
            const base64 = dataUrl.split(',')[1]
            const byteString = atob(base64)
            const mimeString = dataUrl.split(',')[0].split(':')[1].split(';')[0]
            const ab = new ArrayBuffer(byteString.length)
            const ia = new Uint8Array(ab)
            for (let i = 0; i < byteString.length; i++)
              ia[i] = byteString.charCodeAt(i)
            const blob = new Blob([ab], { type: mimeString })
            const url = URL.createObjectURL(blob)
            const a = document.createElement('a')
            a.href = url
            a.download = `${EXCEL_NAME}.xlsx`
            document.body.appendChild(a)
            a.click()
            a.remove()
            URL.revokeObjectURL(url)
          } else if (typeof excelExportRef.current.save === 'function') {
            // Fallback to save() if toDataURL is not available in this kendo version
            excelExportRef.current.save(workbookRef.current)
          } else {
            console.error(
              'ExcelExport ref method missing: toDataURL or save not found',
            )
          }
        } else {
          console.error('ExcelExport ref or workbookOptions missing')
        }
      } catch (err) {
        console.error('Export save failed:', err)
      } finally {
        workbookRef.current = null
        if (!cancelled) setIsExporting(false)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [isExporting, EXCEL_NAME])

  const fileName = `${EXCEL_NAME}.xlsx`

  const renderTitle = (t) => t

  const defaultTabs = ['Steady State Norm Basis']
  let activeTabs = defaultTabs

  return (
    <div>
      <LoaderBackdrop open={!!loading || !!isExporting} />

      {/* Hidden ExcelExport instances for each grid (unchanged) */}
      <div style={{ display: 'none' }}>
        {gridNames.map((name) => {
          const data = dataMap[name] || { rows: [], columns: [] }
          const setRef = (ref) => {
            if (ref) exportRefs.current[name] = ref
          }
          return (
            <ExcelExport
              key={`excel-${name}`}
              data={data.rows}
              ref={setRef}
              fileName={fileName}
            >
              {(data.columns || []).map((col) => (
                <ExcelExportColumn
                  key={col.field}
                  field={col.field}
                  title={col.title || col.field}
                  headerCellOptions={{
                    background: '#d9e1f2',
                    color: '#000',
                    bold: true,
                  }}
                />
              ))}
            </ExcelExport>
          )
        })}
      </div>

      {/* TRANSIENT ExcelExport used for toDataURL/save when isExporting is true */}
      {isExporting && (
        <div style={{ display: 'none' }}>
          <ExcelExport
            data={[]}
            ref={(r) => (excelExportRef.current = r)}
            fileName={fileName}
          />
        </div>
      )}

      {tabIndex === 0 && (
        <Box display='flex' justifyContent='flex-end' mb='2px'>
          <Button
            variant='contained'
            onClick={exportAllGrids}
            className={isDark ? 'btn-dark-no' : 'btn-export'}
            startIcon={
              <Box component='img' src={FileExportIcon} className='w16-icon' />
            }
          >
            Export
          </Button>
        </Box>
      )}

      <Box display='flex' flexDirection='column' gap={2}>
        {tabIndex === 0 && (
          <>
            {gridNames.map((name) => {
              const d = dataMap[name] || { rows: [], columns: [] }
              return (
                <div key={name}>
                  <CustomAccordion defaultExpanded disableGutters>
                    <CustomAccordionSummary
                      aria-controls={`${name}-content`}
                      id={`${name}-header`}
                    >
                      <Typography component='span' className='grid-title'>
                        {renderTitle(name)}
                      </Typography>
                    </CustomAccordionSummary>
                    <CustomAccordionDetails>
                      <Box
                        sx={{
                          width: '100%',
                          margin: 0,
                          height: d?.rows?.length > 50 ? 500 : 'auto',
                        }}
                      >
                        <DataGrid
                          rows={d.rows}
                          className='custom-data-grid'
                          columns={d.columns}
                          disableSelectionOnClick
                          disableColumnSelector
                          disableDensitySelector
                          density='standard'
                          rowHeight={30}
                          pagination={d?.rows?.length > 99}
                          hideFooterPagination={d?.rows?.length <= 99}
                          hideFooter={d?.rows?.length < 30}
                          pageSize={100}
                          rowsPerPageOptions={[100]}
                          hideFooterSelectedRowCount={false}
                          experimentalFeatures={{ newEditingApi: true }}
                        />
                      </Box>
                    </CustomAccordionDetails>
                  </CustomAccordion>
                </div>
              )
            })}
          </>
        )}
        {/* THIS TAB IS NOT LONGER REQUIRED */}
        {tabIndex === 1 && <ConsumptionNormsHistorianBasis />}
      </Box>
    </div>
  )
}

export default CausticSodaLyeBasis
