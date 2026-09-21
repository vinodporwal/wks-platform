import React, { memo, useCallback, useMemo, useState } from 'react'
import { Box, Chip, Tooltip, Typography } from '@mui/material'
import { DataGrid } from '@mui/x-data-grid'
import {
  CustomAccordion,
  CustomAccordionDetails,
  CustomAccordionSummary,
} from 'utils/CustomAccrodian'
import CustomColumnHeader from './CustomColumnHeader'
import { isDateColumn, parseDateSafe, formatDateLocal } from './reportGridHelpers'

function countDecimals(value) {
  if (value == null) return 0
  const s = String(value).replace(/,/g, '').trim()
  if (!s.includes('.')) return 0
  const frac = s.split('.')[1] || ''
  const fracNoTrailing = frac.replace(/0+$/, '')
  return fracNoTrailing.length
}

function getDefaultColumnWidth(col, isDate, isNum) {
  if (col?.width) return col.width
  if (isDate) return 200
  if (isNum) return 200
  return 300
}

function rowMatchesFilter(row, field, filter, rawColumns, isAromaticsHmd) {
  if (!filter) return true
  const rawCell = row[field]
  const isNum =
    typeof rawCell === 'number' ||
    !isNaN(Number(String(rawCell).replace(/,/g, '')))
  const colDef = rawColumns.find((c) => c.field === field)
  const isDate = isDateColumn(colDef, [row])

  const condition = filter.condition
  const selectedSet = filter.selected

  // 1. Condition check
  if (condition) {
    const { operator, value } = condition
    const op = operator

    if (op === 'isEmpty') {
      if (
        rawCell !== null &&
        rawCell !== undefined &&
        String(rawCell).trim() !== ''
      ) {
        return false
      }
    } else if (op === 'isNotEmpty') {
      if (
        rawCell === null ||
        rawCell === undefined ||
        String(rawCell).trim() === ''
      ) {
        return false
      }
    } else if (value !== undefined && value !== '') {
      const strCell = String(rawCell ?? '').toLowerCase()
      const condVal = String(value).toLowerCase().trim()

      if (isDate) {
        const rowDate = parseDateSafe(rawCell)
        const filterDate = parseDateSafe(value)

        if (rowDate && filterDate) {
          const rowTime = rowDate.getTime()
          const filterTime = filterDate.getTime()

          switch (op) {
            case 'equals':
              if (rowTime !== filterTime) return false
              break
            case 'doesNotEqual':
              if (rowTime === filterTime) return false
              break
            case 'isAfter':
              if (rowTime <= filterTime) return false
              break
            case 'isAfterOrEqual':
              if (rowTime < filterTime) return false
              break
            case 'isBefore':
              if (rowTime >= filterTime) return false
              break
            case 'isBeforeOrEqual':
              if (rowTime > filterTime) return false
              break
            default:
              break
          }
        } else {
          if (op === 'equals' && strCell !== condVal) return false
          if (op === 'doesNotEqual' && strCell === condVal) return false
          if (op === 'contains' && !strCell.includes(condVal)) return false
          if (op === 'doesNotContain' && strCell.includes(condVal))
            return false
        }
      } else if (
        isNum &&
        !isNaN(Number(rawCell)) &&
        !isNaN(Number(value))
      ) {
        const cellNum = Number(rawCell)
        const filterNum = Number(value)

        switch (op) {
          case 'equals':
            if (cellNum !== filterNum) return false
            break
          case 'doesNotEqual':
            if (cellNum === filterNum) return false
            break
          case 'greaterThan':
            if (cellNum <= filterNum) return false
            break
          case 'greaterThanOrEqual':
            if (cellNum < filterNum) return false
            break
          case 'lessThan':
            if (cellNum >= filterNum) return false
            break
          case 'lessThanOrEqual':
            if (cellNum > filterNum) return false
            break
          case 'contains':
            if (!strCell.includes(condVal)) return false
            break
          case 'doesNotContain':
            if (strCell.includes(condVal)) return false
            break
          case 'startsWith':
            if (!strCell.startsWith(condVal)) return false
            break
          case 'endsWith':
            if (!strCell.endsWith(condVal)) return false
            break
          default:
            break
        }
      } else {
        switch (op) {
          case 'contains':
            if (!strCell.includes(condVal)) return false
            break
          case 'doesNotContain':
            if (strCell.includes(condVal)) return false
            break
          case 'startsWith':
            if (!strCell.startsWith(condVal)) return false
            break
          case 'endsWith':
            if (!strCell.endsWith(condVal)) return false
            break
          case 'equals':
            if (strCell !== condVal) return false
            break
          case 'doesNotEqual':
            if (strCell === condVal) return false
            break
          default:
            break
        }
      }
    }
  }

  // 2. Checkbox selection check
  if (selectedSet && selectedSet.size > 0) {
    let val = rawCell
    if (isNum && val != null && val !== '') {
      const num = Number(val)
      val = isNaN(num) ? val : num.toFixed(isAromaticsHmd ? 5 : 3)
    } else if (isDate && val != null && val !== '') {
      val = formatDateLocal(val)
    }
    if (!selectedSet.has(val ?? '')) return false
  }

  return true
}

const SingleReportGrid = memo(
  ({
    name,
    rawData = [],
    rawColumns = [],
    isAromaticsHmd = false,
    aopYear = '',
    defaultExpanded = true,
  }) => {
    const [expanded, setExpanded] = useState(defaultExpanded)
    const [activeFilters, setActiveFilters] = useState({})
    const [sortConfig, setSortConfig] = useState(null) // { field: string, direction: 'asc' | 'desc' }
    const [columnWidths, setColumnWidths] = useState({})

    const handleColumnResize = useCallback((params) => {
      if (params?.colDef?.field && params?.width) {
        setColumnWidths((prev) => ({
          ...prev,
          [params.colDef.field]: params.width,
        }))
      }
    }, [])

    const handleFilterChange = useCallback((field, filterData) => {
      setActiveFilters((prev) => {
        const next = { ...prev }
        if (!filterData) {
          delete next[field]
        } else {
          next[field] = filterData
        }
        return next
      })
    }, [])

    const handleSortChange = useCallback((field, explicitDirection) => {
      if (explicitDirection !== undefined) {
        setSortConfig(
          explicitDirection ? { field, direction: explicitDirection } : null,
        )
        return
      }
      setSortConfig((prev) => {
        if (!prev || prev.field !== field) {
          return { field, direction: 'asc' }
        }
        if (prev.direction === 'asc') {
          return { field, direction: 'desc' }
        }
        return null // reset sort
      })
    }, [])

    const columns = useMemo(() => {
      const validCols = rawColumns.filter((c) => c.field !== 'GRID_TYPE')

      return validCols.map((col) => {
        const isNum = col.type === 'number'
        const isDate = isDateColumn(col, rawData)
        const customWidth = columnWidths[col.field]
        const resolvedWidth = customWidth || getDefaultColumnWidth(col, isDate, isNum)

        // Cascading filter: compute available rows for this column's header filter dropdown
        const otherFilterKeys = Object.keys(activeFilters).filter((k) => k !== col.field)
        const rowsForCol =
          otherFilterKeys.length === 0
            ? rawData
            : rawData.filter((row) =>
                otherFilterKeys.every((f) =>
                  rowMatchesFilter(row, f, activeFilters[f], rawColumns, isAromaticsHmd),
                ),
              )

        return {
          field: col.field,
          headerName: col.title || col.field,
          type: isNum ? 'number' : 'string',
          sortable: false,
          filterable: false,
          disableColumnMenu: true,
          width: resolvedWidth,
          minWidth: 100,
          headerAlign: isNum ? 'right' : 'left',
          align: isNum ? 'right' : 'left',
          renderHeader: () => (
            <CustomColumnHeader
              colDef={col}
              rows={rowsForCol}
              activeFilter={activeFilters[col.field]}
              onFilterChange={handleFilterChange}
              sortConfig={sortConfig}
              onSortChange={handleSortChange}
              isAromaticsHmd={isAromaticsHmd}
            />
          ),
          renderCell: (params) => {
            const rawVal = params?.row?.[col.field] ?? params?.value

            if (rawVal == null || rawVal === '') {
              return <span style={{ color: '#94A3B8' }}>-</span>
            }

            if (isDate) {
              const formattedDate = formatDateLocal(rawVal)
              return (
                <Tooltip title={String(rawVal)} placement='top' arrow enterDelay={400}>
                  <Box
                    component='span'
                    sx={{
                      display: 'block',
                      width: '100%',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                      textAlign: 'left',
                      fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
                    }}
                  >
                    {formattedDate}
                  </Box>
                </Tooltip>
              )
            }

            if (isNum) {
              const decimals = countDecimals(rawVal)
              const decimalsToShow = isAromaticsHmd
                ? Math.min(Math.max(decimals, 0), 5)
                : Math.min(Math.max(decimals, 0), 3)

              const text =
                decimalsToShow === 0
                  ? String(Number(rawVal))
                  : Number(rawVal).toFixed(decimalsToShow)

              return (
                <Tooltip title={String(rawVal)} placement='top' arrow enterDelay={400}>
                  <Box
                    component='span'
                    sx={{
                      display: 'block',
                      width: '100%',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                      textAlign: 'right',
                      fontVariantNumeric: 'tabular-nums',
                      fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
                    }}
                  >
                    {text}
                  </Box>
                </Tooltip>
              )
            }

            return (
              <Tooltip title={String(rawVal)} placement='top' arrow enterDelay={400}>
                <Box
                  component='span'
                  sx={{
                    display: 'block',
                    width: '100%',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                    textAlign: 'left',
                    fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
                  }}
                >
                  {String(rawVal)}
                </Box>
              </Tooltip>
            )
          },
        }
      })
    }, [
      rawColumns,
      rawData,
      columnWidths,
      activeFilters,
      handleFilterChange,
      sortConfig,
      handleSortChange,
      isAromaticsHmd,
    ])

    const filteredRows = useMemo(() => {
      const activeFilterKeys = Object.keys(activeFilters)
      if (activeFilterKeys.length === 0) return rawData

      return rawData.filter((row) =>
        activeFilterKeys.every((field) =>
          rowMatchesFilter(row, field, activeFilters[field], rawColumns, isAromaticsHmd),
        ),
      )
    }, [rawData, activeFilters, rawColumns, isAromaticsHmd])

    // ===================== SORTING APPLICATION =====================
    const sortedAndFilteredRows = useMemo(() => {
      if (!sortConfig || !sortConfig.field || !sortConfig.direction) {
        return filteredRows
      }

      const { field, direction } = sortConfig
      const colDef = rawColumns.find((c) => c.field === field)
      const isNum = colDef?.type === 'number'
      const isDate = isDateColumn(colDef, rawData)
      const multiplier = direction === 'asc' ? 1 : -1

      return [...filteredRows].sort((rowA, rowB) => {
        const valA = rowA[field]
        const valB = rowB[field]

        // Keep nulls / blanks at the bottom
        const isBlankA = valA == null || valA === ''
        const isBlankB = valB == null || valB === ''
        if (isBlankA && isBlankB) return 0
        if (isBlankA) return 1
        if (isBlankB) return -1

        if (isDate) {
          const dateA = parseDateSafe(valA)
          const dateB = parseDateSafe(valB)
          if (dateA && dateB) {
            return (dateA.getTime() - dateB.getTime()) * multiplier
          }
        }

        if (isNum) {
          const numA = Number(String(valA).replace(/,/g, ''))
          const numB = Number(String(valB).replace(/,/g, ''))
          if (!isNaN(numA) && !isNaN(numB)) {
            return (numA - numB) * multiplier
          }
        }

        return (
          String(valA).localeCompare(String(valB), undefined, {
            numeric: true,
            sensitivity: 'base',
          }) * multiplier
        )
      })
    }, [filteredRows, sortConfig, rawColumns, rawData])

    return (
      <CustomAccordion
        expanded={expanded}
        onChange={() => setExpanded((prev) => !prev)}
        disableGutters
        sx={{
          border: '1px solid #D1D5DB',
          borderRadius: '6px !important',
          mb: 2,
          overflow: 'hidden',
          boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
        }}
      >
        <CustomAccordionSummary
          aria-controls={`${name}-content`}
          id={`${name}-header`}
          sx={{
            backgroundColor: '#F8FAFC',
            borderBottom: expanded ? '1px solid #D1D5DB' : 'none',
            minHeight: '44px',
            px: 2,
          }}
        >
          <Box display='flex' alignItems='center' gap={1.5} width='100%'>
            <Typography
              sx={{
                fontWeight: 600,
                fontSize: '0.92rem',
                color: '#1E293B',
                fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
              }}
            >
              {name}
            </Typography>
            <Chip
              label={`${sortedAndFilteredRows.length} Rows`}
              size='small'
              sx={{
                height: '22px',
                fontSize: '0.72rem',
                backgroundColor: '#EFF6FF',
                color: '#1E40AF',
                border: '1px solid #BFDBFE',
                fontWeight: 600,
              }}
            />
          </Box>
        </CustomAccordionSummary>

        <CustomAccordionDetails sx={{ p: 0, backgroundColor: '#FFFFFF' }}>
          {expanded && (
            <Box sx={{ height: 540, width: '100%', overflow: 'hidden' }}>
              <DataGrid
                rows={sortedAndFilteredRows}
                columns={columns}
                getRowId={(row) => row.id}
                rowHeight={48}
                headerHeight={38}
                disableColumnSorting
                onColumnWidthChange={handleColumnResize}
                onColumnResize={handleColumnResize}
                pageSizeOptions={[50, 100]}
                initialState={{
                  pagination: { paginationModel: { pageSize: 50, page: 0 } },
                }}
                disableRowSelectionOnClick
                disableColumnMenu
                sx={{
                  border: '1px solid #CBD5E1',
                  borderRadius: '4px',
                  fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
                  fontSize: '0.82rem',
                  '& .MuiDataGrid-sortIcon': {
                    display: 'none !important',
                  },
                  '& .MuiDataGrid-iconButtonContainer': {
                    display: 'none !important',
                  },
                  '& .MuiDataGrid-columnHeaders': {
                    backgroundColor: '#E2E8F0',
                    color: '#0F172A',
                    fontWeight: 700,
                    borderBottom: '2px solid #94A3B8',
                    minHeight: '38px !important',
                    maxHeight: '38px !important',
                    lineHeight: '38px !important',
                  },
                  '& .MuiDataGrid-columnHeader': {
                    padding: '0 !important',
                    borderRight: '1px solid #CBD5E1',
                    height: '38px !important',
                    '&:focus, &:focus-within': { outline: 'none' },
                  },
                  '& .MuiDataGrid-columnHeaderTitleContainer': {
                    padding: '0 !important',
                    width: '100% !important',
                    height: '100% !important',
                    display: 'flex !important',
                    alignItems: 'stretch !important',
                    justifyContent: 'stretch !important',
                  },
                  '& .MuiDataGrid-columnHeaderTitleContainerContent': {
                    width: '100% !important',
                    height: '100% !important',
                    display: 'flex !important',
                    alignItems: 'stretch !important',
                  },
                  '& .MuiDataGrid-cell': {
                    borderRight: '1px solid #CBD5E1',
                    borderBottom: '1px solid #CBD5E1',
                    padding: '0 8px',
                    color: '#0F172A',
                    '&:focus, &:focus-within': { outline: 'none' },
                  },
                  '& .MuiDataGrid-row': {
                    backgroundColor: '#FFFFFF',
                  },
                  '& .MuiDataGrid-row:nth-of-type(even)': {
                    backgroundColor: '#F1F5F9',
                  },
                  '& .MuiDataGrid-row:hover': {
                    backgroundColor: '#DBEAFE !important',
                  },
                  '& .MuiDataGrid-footerContainer': {
                    minHeight: '48px !important',
                    maxHeight: '48px !important',
                    borderTop: '2px solid #CBD5E1',
                    backgroundColor: '#F1F5F9',
                    display: 'flex',
                    alignItems: 'center',
                  },
                  '& .MuiTablePagination-root': {
                    color: '#334155',
                    fontSize: '0.82rem',
                    overflow: 'visible',
                  },
                  '& .MuiTablePagination-toolbar': {
                    minHeight: '48px !important',
                    height: '48px !important',
                    paddingLeft: '16px',
                    paddingRight: '16px',
                  },
                  '& .MuiTablePagination-selectLabel, & .MuiTablePagination-displayedRows':
                  {
                    fontSize: '0.82rem',
                    color: '#334155',
                    margin: 0,
                    lineHeight: '48px',
                    fontWeight: 500,
                  },
                  '& .MuiTablePagination-select': {
                    fontSize: '0.82rem',
                    paddingTop: '6px',
                    paddingBottom: '6px',
                  },
                  '& .MuiTablePagination-actions': {
                    marginLeft: '12px',
                    '& .MuiIconButton-root': {
                      padding: '6px',
                      color: '#0284C7',
                      '&.Mui-disabled': {
                        color: '#94A3B8',
                      },
                    },
                  },
                }}
              />
            </Box>
          )}
        </CustomAccordionDetails>
      </CustomAccordion>
    )
  },
)

SingleReportGrid.displayName = 'SingleReportGrid'

export default SingleReportGrid
