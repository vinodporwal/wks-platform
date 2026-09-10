import { Box, Button, Tab, Tabs, Typography } from '@mui/material'
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
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
const REPORT_TYPE_FOR_ALL = 'NormsHistorian'
import AddIcon from '@mui/icons-material/Add'
import PlaylistAddIcon from '@mui/icons-material/PlaylistAdd'
import DownloadIcon from '@mui/icons-material/Download'
import UploadIcon from '@mui/icons-material/Upload'
import CalculateIcon from '@mui/icons-material/Calculate'
import SaveIcon from '@mui/icons-material/Save'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'

const NormsHistorianBasisPe = () => {
  const keycloak = useSession()

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
  const VERTICAL_NAME = verticalObject?.name
  const AOP_YEAR = year?.selectedYear
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const lowerSiteName = siteObject?.name?.toLowerCase()

  const timeoutIdsRef = useRef([])
  const isMountedRef = useRef(true)
  // export control (dynamic ExcelExport mount)
  const [isExporting, setIsExporting] = useState(false)
  const workbookRef = useRef(null)
  const excelExportRef = useRef(null)

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()

  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}`

  const IS_AROMATICS_HMD =
    lowerVertName == 'aromatics' && lowerSiteName == 'hmd'

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
              const decimalsToShow = IS_AROMATICS_HMD
                ? Math.min(Math.max(decimals, 0), 5)
                : Math.min(Math.max(decimals, 0), 3)

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

      const phaseTwoVertical = ['crude', 'vgoht', 'hydrotreater', 'alkylation']
      // Call the API that returns combined grids. Change REPORT_TYPE_FOR_ALL if needed.
      const apiResponse = await DataService.getProductionVolDataBasisPe(
        keycloak,
        phaseTwoVertical.includes(lowerVertName)
          ? 'PlantWiseNormsHistorian'
          : REPORT_TYPE_FOR_ALL,
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

  const fileName = `${EXCEL_EXPORT_TITLE}-Norms Historian Basis-${AOP_YEAR}.xlsx`

  const exportAllGrids = useCallback(() => {
    // Shared border style for all cells
    const borderStyle = {
      top: { size: 1, color: '#000000' },
      bottom: { size: 1, color: '#000000' },
      left: { size: 1, color: '#000000' },
      right: { size: 1, color: '#000000' },
    }

    // Header-specific style: grey background + bold
    const headerStyle = {
      background: '#D9D9D9',
      bold: true,
      borderBottom: borderStyle.bottom,
      borderTop: borderStyle.top,
      borderLeft: borderStyle.left,
      borderRight: borderStyle.right,
    }

    // Data cell style: borders only
    const dataCellStyle = {
      borderBottom: borderStyle.bottom,
      borderTop: borderStyle.top,
      borderLeft: borderStyle.left,
      borderRight: borderStyle.right,
    }

    const sheets = gridNames
      .map((gridName, idx) => {
        const d = dataMap[gridName] || { rows: [], columns: [] }
        const cols = d.columns || []
        const rows = d.rows || []
        if (!cols.length && !rows.length) return null

        // Calculate max width per column based on content length
        const colWidths = cols.map((c, colIdx) => {
          const headerText = String(c.title || c.field || '')
          let maxLen = headerText.length

          rows.forEach((r) => {
            const cellVal = normalizeCellValue(r?.[c.field])
            const cellLen = String(cellVal ?? '').length
            if (cellLen > maxLen) maxLen = cellLen
          })

          // Approximate pixel width: ~8px per character + 16px padding, min 60px
          return Math.max(maxLen * 8 + 16, 60)
        })

        const sheetColumns = cols.map((c, i) => ({
          width: colWidths[i],
          title: c.title || c.field || '',
        }))

        const headerRow = {
          cells: cols.map((c) => ({
            value: c.title || c.field || '',
            ...headerStyle,
          })),
        }

        const dataRows = rows.map((r) => ({
          cells: cols.map((c) => ({
            value: normalizeCellValue(r?.[c.field]),
            ...dataCellStyle,
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
    workbookRef.current = workbookOptions
    setIsExporting(true)
  }, [gridNames, dataMap])

  useEffect(() => {
    if (!isExporting) return

    let cancelled = false

    ;(async () => {
      try {
        if (excelExportRef.current && workbookRef.current) {
          // Prefer toDataURL (returns a Promise<string>) so we know when the file was generated.
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
            a.download = fileName
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
  }, [isExporting, fileName])

  const renderTitle = (t) => t

  const PETabs = ['Steady State Norm Basis', 'Overall Consumption Norm Basis']
  const defaultTabs = ['Steady State Norm Basis']
  let activeTabs = defaultTabs
  if (lowerVertName === 'pe') activeTabs = PETabs

  return (
    <div>
      <LoaderBackdrop open={!!loading || !!isExporting} />

      {/* transient ExcelExport: only mounted during actual export */}
      {isExporting && (
        <div style={{ display: 'none' }}>
          <ExcelExport
            data={[]}
            ref={(r) => (excelExportRef.current = r)}
            fileName={fileName}
          />
        </div>
      )}

      {/* {activeTabs?.length > 1 && (
        <Tabs
          value={tabIndex}
          onChange={(e, newIndex) => setTabIndex(newIndex)}
          variant='scrollable'
          scrollButtons='auto'
          sx={{
            borderBottom: '0px solid #ccc',
            '.MuiTabs-indicator': { display: 'none' },
            margin: '0px 0px 10px 0px',
            minHeight: '28px',
          }}
          textColor='primary'
          indicatorColor='primary'
        >
          {activeTabs.map((label, idx) => (
            <Tab
              key={idx}
              label={label}
              sx={{
                border: '1px solid #ADD8E6',
                borderBottom: '1px solid #ADD8E6',
                fontSize: '0.75rem',
                padding: '9px',
                minHeight: '12px',
              }}
            />
          ))}
        </Tabs>
      )} */}

      {tabIndex === 0 && (
        <Box display='flex' justifyContent='flex-end' mb='2px'>
          <Button
            variant='contained'
            onClick={exportAllGrids}
            className='btn-export'
            startIcon={<DownloadIcon fontSize='small' />}
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
              // Hide grids with GRID_TYPE === 'GENERAL_NOTES'
              if (d.rows?.[0]?.GRID_TYPE === 'GENERAL_NOTES') return null
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

export default NormsHistorianBasisPe
