import { Box, Button, Typography } from '@mui/material'
import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import {
  ExcelExport,
  ExcelExportColumn,
} from '@progress/kendo-react-excel-export'
import KendoDataGrid from 'components/Kendo-Report-DataGrid/index'
// import KendoDataTablesReports from 'components/kendo-data-tables/index-reports'  //use this 
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useSelector } from 'react-redux'
import { DataService } from 'services/DataService'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import {
  CustomAccordion,
  CustomAccordionDetails,
  CustomAccordionSummary,
} from 'utils/CustomAccrodian'

import AddIcon from '@mui/icons-material/Add'
import PlaylistAddIcon from '@mui/icons-material/PlaylistAdd'
import DownloadIcon from '@mui/icons-material/Download'
import UploadIcon from '@mui/icons-material/Upload'
import CalculateIcon from '@mui/icons-material/Calculate'
import SaveIcon from '@mui/icons-material/Save'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'

const EtheleneStock = () => {
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
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear

  const verticalName = verticalObject?.name || ''
  const siteName = siteObject?.name || ''
  const plantName = plantObject?.name || ''

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()

  const timeoutIdsRef = useRef([])
  const isMountedRef = useRef(true)
  const exportRefs = useRef({})

  useEffect(() => {
    return () => {
      isMountedRef.current = false
      timeoutIdsRef.current.forEach((t) => clearTimeout(t))
      timeoutIdsRef.current = []
    }
  }, [])

  const enrichColumns = useCallback(
    (backendCols = []) => {
      const filteredCols = backendCols.filter(
        (col) => col.field !== 'GRID_TYPE',
      )
      const applyFixedWidth = filteredCols.length < 7
      const fixedWidth = applyFixedWidth ? 250 : 220
      return backendCols
        .filter((col) => col.field !== 'GRID_TYPE')
        .map((col) => {
          const isTextCol = col.type === 'string'
          const isNumberCol = col.type === 'number'
          return {
            ...col,
            title: col.title || col.field,
            filterable: true,
            filter: isTextCol ? 'text' : isNumberCol ? 'numeric' : undefined,
            align: isTextCol ? 'left' : isNumberCol ? 'right' : undefined,
            ...(isNumberCol ? { format: '{0:0.0000}' } : {}),
            editable: false,
            isRightAlligned: isNumberCol ? 'numeric' : undefined,
            ...(fixedWidth ? { widthT: fixedWidth } : {}),
          }
        })
    },
    [AOP_YEAR, PLANT_ID, keycloak],
  )

  // ---------------------------------------------------------------------------
  // Infer columns from row objects (returns [{ field, title, type }])
  // ---------------------------------------------------------------------------
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
        // detect date-like strings
        const d = new Date(v)
        if (!isNaN(d.getTime())) {
          detectedType = 'date'
          break
        }
        // numeric string (allow commas)
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

  // ---------------------------------------------------------------------------
  // Normalize row values according to detected column types
  // ---------------------------------------------------------------------------
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
      // strings and objects left as-is (objects will be stringified during export)
    })
    return parsed
  }

  // ---------------------------------------------------------------------------
  // Fetch all grids in one call and build dataMap + gridNames
  // The backend is expected to return: apiResponse.data = [ { gridName, data: [...] }, ... ]
  // ---------------------------------------------------------------------------

  const fetchAllGrids = useCallback(async () => {
    // clear previous timers if any
    timeoutIdsRef.current.forEach((t) => clearTimeout(t))
    timeoutIdsRef.current = []

    setDataMap({})

    try {
      setLoading(true)

      let apiResponse
      try {
        apiResponse = await DataService.getEtheleneStock(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      } catch (err) {
        console.error('API call failed, falling back to dummy data', err)
      }

      if (!apiResponse || apiResponse.code !== 200 || !apiResponse.data || (Array.isArray(apiResponse.data) && apiResponse.data.length === 0)) {
        const dummyRows = []
        const startDate = new Date('2026-04-01')
        for (let i = 0; i < 80; i++) {
          const currentDate = new Date(startDate)
          currentDate.setDate(startDate.getDate() + i)
          const yyyy = currentDate.getFullYear()
          const mm = String(currentDate.getMonth() + 1).padStart(2, '0')
          const dd = String(currentDate.getDate()).padStart(2, '0')
          const dateString = `${dd}-${mm}-${yyyy}`

          dummyRows.push({
            Date: dateString,
            'Production Ethylene': Number((4000 + Math.random() * 1000).toFixed(4)),
            'Production Propylene': Number((2000 + Math.random() * 1000).toFixed(4)),
            MEG: Number((1000 + Math.random() * 500).toFixed(4)),
            LDPE: Number((800 + Math.random() * 400).toFixed(4)),
            LLDPE: Number((900 + Math.random() * 400).toFixed(4)),
            PP: Number((1800 + Math.random() * 400).toFixed(4)),
            'Ethylene Stock in Cyro-Tank': Number((15000 + Math.random() * 10000).toFixed(4)),
          })
        }

        apiResponse = {
          code: 200,
          data: [
            {
              gridName: 'Ethylene Stock Report',
              data: dummyRows,
              columns: [
                { field: 'Date', title: 'Date', type: 'string' },
                { field: 'Production Ethylene', title: 'Production Ethylene', type: 'number' },
                { field: 'Production Propylene', title: 'Production Propylene', type: 'number' },
                { field: 'MEG', title: 'MEG', type: 'number' },
                { field: 'LDPE', title: 'LDPE', type: 'number' },
                { field: 'LLDPE', title: 'LLDPE', type: 'number' },
                { field: 'PP', title: 'PP', type: 'number' },
                { field: 'Ethylene Stock in Cyro-Tank', title: 'Ethylene Stock in Cyro-Tank', type: 'number' },
              ],
            },
          ],
        }
      }

      const gridsArray = Array.isArray(apiResponse.data)
        ? apiResponse.data
        : Array.isArray(apiResponse.data?.data)
          ? apiResponse.data.data
          : []

      if (!Array.isArray(gridsArray) || gridsArray.length === 0) {
        setGridNames([])
        setDataMap({})
        return
      }

      const newMap = {}
      const normalizedNames = []

      gridsArray.forEach((g) => {
        // Display Name
        let displayName = g.gridName

        if (
          g.gridName === 'GRID_TYPE' ||
          g.gridName?.startsWith('UNKNOWN_GRID')
        ) {
          displayName =
            Array.isArray(g.data) && g.data.length > 0 && g.data[0].GRID_TYPE
              ? g.data[0].GRID_TYPE
              : g.gridName
        }

        normalizedNames.push(displayName)

        // Rows
        const rawRows = Array.isArray(g.data) ? g.data : []

        // Columns
        const inferredCols =
          Array.isArray(g.columns) && g.columns.length
            ? g.columns
            : inferColumnsFromRows(rawRows)

        // Detect groupBy column
        const hasGroupBy = inferredCols.some((col) => col.field === 'groupBy')
        const gridGroupBy = hasGroupBy ? 'groupBy' : null

        // Hidden keys (including groupBy so it doesn't render as a visible column)
        const hiddenKeys = ['GRID_TYPE', 'groupBy']

        let enrichedCols = enrichColumns(inferredCols).map((col) => ({
          ...col,
          hidden: hiddenKeys.includes(col.field) ? true : col.hidden,
        }))

        // Normalize Data
        const rowsWithId = rawRows.map((r, i) => {
          const parsed = normalizeRowValues(r, inferredCols)

          return {
            ...parsed,
            id: i,
            isEditable: false,
          }
        })

        if (g.gridName === 'Raw Data - Max Achieved Capacity') {
          enrichedCols = enrichedCols.map((col) => ({
            ...col,
            widthT: 120,
          }))
        }

        // IMPORTANT:
        // Use displayName as key instead of g.gridName
        newMap[displayName] = {
          rows: rowsWithId,
          columns: enrichedCols,
          groupBy: gridGroupBy,
        }
      })

      if (isMountedRef.current) {
        setGridNames(normalizedNames)
        setDataMap(newMap)
      }
    } catch (err) {
      console.error('Error fetching all grids:', err)
    } finally {
      if (isMountedRef.current) {
        setLoading(false)
      }
    }
  }, [AOP_YEAR, PLANT_ID, keycloak, enrichColumns])

  useEffect(() => {
    setTabIndex(0)
    fetchAllGrids()
    return () => {
      timeoutIdsRef.current.forEach((t) => clearTimeout(t))
      timeoutIdsRef.current = []
    }
  }, [fetchAllGrids, AOP_YEAR, PLANT_ID, keycloak, oldYear, yearChanged])

  // ---------------------------------------------------------------------------
  // Excel export helpers (keeps your existing implementation compatible)
  // ---------------------------------------------------------------------------

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

  const exportAllGrids = useCallback(() => {
    const keys = Object.keys(exportRefs.current || {})
    const firstKey = keys.find((k) => exportRefs.current[k])
    if (!firstKey) return
    const baseRef = exportRefs.current[firstKey]
    if (!baseRef || typeof baseRef.save !== 'function') return

    const sheets = gridNames
      .map((gridName, idx) => {
        const d = dataMap[gridName] || { rows: [], columns: [] }
        const cols = d.columns || []
        const rows = d.rows || []
        if (!cols.length && !rows.length) return null

        const sheetColumns = cols.map((c) => ({
          autoWidth: true,
          title: c.title || c.field || '',
        }))

        const headerRow = {
          cells: cols.map((c) => ({ value: c.title || c.field || '' })),
        }

        const dataRows = rows.map((r) => ({
          cells: cols.map((c) => ({ value: normalizeCellValue(r?.[c.field]) })),
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

    try {
      baseRef.save(workbookOptions)
    } catch (err) {
      console.error('Export save failed:', err)
    }
  }, [gridNames])

  const currentDateTime = new Date()
    .toISOString()
    .replace(/T/, ' ')
    .replace(/:/g, '-')
    .split('.')[0]
  const fileName = `${verticalName}_${siteName}_${plantName}_${AOP_YEAR}_Ethylene Stock in Cryo Tank.xlsx`

  const renderTitle = (t) => t

  const PETabs = ['Steady State Norm Basis', 'Overall Consumption Norm Basis']
  const defaultTabs = ['Steady State Norm Basis']
  let activeTabs = defaultTabs
  if (lowerVertName === 'pe') activeTabs = PETabs

  return (
    <div>
      <LoaderBackdrop open={!!loading} />

      {/* Hidden ExcelExport instances for each grid */}
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
                />
              ))}
            </ExcelExport>
          )
        })}
      </div>

      <Box display='flex' justifyContent='flex-end' mb='2px'>
        <Button
          variant='contained'
          onClick={exportAllGrids}
          className='btn-export'
          // disabled={READ_ONLY}
          startIcon={<DownloadIcon fontSize='small' />}
        >
          Export
        </Button>
      </Box>

      <Box display='flex' flexDirection='column' gap={2}>
        {tabIndex === 0 && (
          <>
            {gridNames.map((name, idx) => {
              // if (idx === 0) return null

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
                      <Box sx={{ width: '100%', margin: 0 }}>
                        <KendoDataGrid
                          rows={d.rows}
                          columns={d.columns}
                          permissions={{
                            isHeight: d?.rows?.length > 15,
                            customHeight: 'calc(100vh - 275px)',
                            disablePagination: true,
                          }}
                          groupBy={d.groupBy || null}
                        />
                      </Box>
                    </CustomAccordionDetails>
                  </CustomAccordion>
                </div>
              )
            })}
          </>
        )}
      </Box>
    </div>
  )
}

export default EtheleneStock
