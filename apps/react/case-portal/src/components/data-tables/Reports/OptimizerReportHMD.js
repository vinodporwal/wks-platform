

import { Box, Button, Typography } from '@mui/material'
import { DataGrid } from '@mui/x-data-grid'
import { ExcelExport } from '@progress/kendo-react-excel-export'
import { FileExportIcon } from 'assets/images/icons'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
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
import { ReportDataService } from 'services/ReportDataService'

const REPORT_TYPE_FOR_ALL = 'AnnualAOPCost'

const OptimizerReportHMD = () => {
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

  const timeoutIdsRef = useRef([])
  const isMountedRef = useRef(true)
  // export control (dynamic ExcelExport mount)
  const [isExporting, setIsExporting] = useState(false)
  const workbookRef = useRef(null)
  const excelExportRef = useRef(null)

  const [collapsedGroups, setCollapsedGroups] = useState({})

  const toggleGroupCollapse = useCallback((gridName, groupName) => {
    setCollapsedGroups((prev) => ({
      ...prev,
      [`${gridName}-${groupName}`]: !prev[`${gridName}-${groupName}`],
    }))
  }, [])

  const getGroupedRowsAndColumns = useCallback(
    (rows = [], columns = [], gridName = '') => {
      // Check if 'ROW_GROUP' is one of the columns
      const hasNormCol = columns.some((c) => c.field === 'ROW_GROUP')
      if (!hasNormCol || rows.length === 0) {
        return { rows, columns }
      }

      // Group the rows by the 'ROW_GROUP' value
      const groups = {}
      rows.forEach((row) => {
        const groupVal = row.ROW_GROUP || 'Other'
        if (!groups[groupVal]) {
          groups[groupVal] = []
        }
        groups[groupVal].push(row)
      })

      const groupedRows = []
      let idCounter = 100000

      Object.keys(groups).forEach((groupName) => {
        const isCollapsed = !!collapsedGroups[`${gridName}-${groupName}`]

        // 1. Insert a Group Header row
        groupedRows.push({
          id: `group-header-${idCounter++}`,
          isGroupHeader: true,
          groupName: groupName,
          particulars: groupName,
        })

        // 2. Insert all the rows in this group only if not collapsed
        if (!isCollapsed) {
          groups[groupName].forEach((row) => {
            groupedRows.push({
              ...row,
              isGroupChild: true,
            })
          })
        }
      })

      // Hide the 'ROW_GROUP' column
      const filteredColumns = columns.filter((c) => c.field !== 'ROW_GROUP')

      // Enhance columns
      const finalColumns = filteredColumns.map((col, index) => {
        if (index === 0) {
          return {
            ...col,
            colSpan: (value, row) => (row.isGroupHeader ? 100 : 1),
            renderCell: (params) => {
              if (params.row.isGroupHeader) {
                const isCollapsed =
                  !!collapsedGroups[`${gridName}-${params.row.groupName}`]
                return (
                  <Box
                    display='flex'
                    alignItems='center'
                    onClick={() =>
                      toggleGroupCollapse(gridName, params.row.groupName)
                    }
                    style={{
                      cursor: 'pointer',
                      userSelect: 'none',
                      width: '100%',
                      height: '100%',
                      paddingLeft: '4px',
                    }}
                  >
                    <span
                      style={{
                        marginRight: '8px',
                        display: 'flex',
                        alignItems: 'center',
                        fontSize: '0.75rem',
                        color: '#475569',
                      }}
                    >
                      {isCollapsed ? '▶' : '▼'}
                    </span>
                    <span
                      style={{
                        fontWeight: '700',
                        color: '#1e293b',
                        textTransform: 'uppercase',
                        fontSize: '0.8rem',
                        letterSpacing: '0.02em',
                      }}
                    >
                      {params.row.groupName}
                    </span>
                  </Box>
                )
              }
              return <span style={{ paddingLeft: '24px' }}>{params.value}</span>
            },
          }
        }

        // For other columns
        const originalRenderCell = col.renderCell
        return {
          ...col,
          renderCell: (params) => {
            if (params.row.isGroupHeader) {
              return null
            }
            if (originalRenderCell) {
              return originalRenderCell(params)
            }
            return params.value
          },
        }
      })

      return { rows: groupedRows, columns: finalColumns }
    },
    [collapsedGroups, toggleGroupCollapse],
  )

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
      const apiResponse = await ReportDataService.getOptimizerInputOutput(
        keycloak,
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




  const renderTitle = (t) => t

  const PETabs = ['Steady State Norm Basis', 'Overall Consumption Norm Basis']
  const defaultTabs = ['Steady State Norm Basis']
  let activeTabs = defaultTabs
  if (lowerVertName === 'pe') activeTabs = PETabs

  return (
    <div>
      <LoaderBackdrop open={!!loading} />










      <Box display='flex' flexDirection='column' gap={2}>
        {tabIndex === 0 && (
          <>
            {gridNames.map((name) => {
              const d = dataMap[name] || { rows: [], columns: [] }
              const { rows: groupedRows, columns: groupedCols } =
                getGroupedRowsAndColumns(d.rows, d.columns, name)
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
                          height: groupedRows?.length > 50 ? 500 : 'auto',
                        }}
                      >
                        <DataGrid
                          rows={groupedRows}
                          className='custom-data-grid'
                          columns={groupedCols}
                          disableSelectionOnClick
                          disableColumnSelector
                          disableDensitySelector
                          density='standard'
                          rowHeight={36}
                          pagination={groupedRows?.length > 99}
                          hideFooterPagination={groupedRows?.length <= 99}
                          hideFooter={groupedRows?.length < 30}
                          pageSize={100}
                          rowsPerPageOptions={[100]}
                          hideFooterSelectedRowCount={false}
                          experimentalFeatures={{ newEditingApi: true }}
                          getRowClassName={(params) =>
                            params.row.isGroupHeader ? 'group-header-row' : ''
                          }
                          sx={{
                            border: 0,
                            '& .group-header-row': {
                              backgroundColor: '#ffffff',
                              borderBottom: '1px solid #e2e8f0',
                              borderTop: '1px solid #e2e8f0',
                              '&:hover': {
                                backgroundColor: '#f8fafc',
                              },
                            },
                          }}
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

export default OptimizerReportHMD
