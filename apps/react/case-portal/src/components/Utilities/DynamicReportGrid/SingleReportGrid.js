import React, { memo, useCallback, useMemo, useState } from 'react'
import { Box, Chip, Tooltip, Typography } from '@mui/material'
import { DataGrid } from '@mui/x-data-grid'
import {
  CustomAccordion,
  CustomAccordionDetails,
  CustomAccordionSummary,
} from 'utils/CustomAccrodian'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import CustomColumnHeader from './CustomColumnHeader'
import { MONTH_MAP } from './reportGridHelpers'

export const SingleReportGrid = memo(
  ({
    name,
    rawData,
    rawColumns,
    isAromaticsHmd,
    aopYear,
    defaultExpanded = true,
  }) => {
    const [expanded, setExpanded] = useState(defaultExpanded)
    const [activeFilters, setActiveFilters] = useState({}) // { field: Set of selected values }

    const headerMap = useMemo(() => generateHeaderNames(aopYear), [aopYear])

    // Lazy unique distinct values generator (computed ONLY when column filter opens)
    const getUniqueValues = useCallback(
      (field) => {
        const isNumberCol =
          rawColumns.find((c) => c.field === field)?.type === 'number'
        const decimals = isAromaticsHmd ? 5 : 3
        const set = new Set()
        const rows = rawData || []
        for (let i = 0; i < rows.length; i++) {
          let val = rows[i]?.[field]
          if (isNumberCol && val != null && val !== '') {
            const num = Number(val)
            val = isNaN(num) ? val : num.toFixed(decimals)
          }
          set.add(val ?? '')
        }
        return Array.from(set).sort((a, b) => {
          if (typeof a === 'number' && typeof b === 'number') return a - b
          return String(a).localeCompare(String(b), undefined, {
            numeric: true,
          })
        })
      },
      [rawColumns, rawData, isAromaticsHmd],
    )

    const handleApplyFilter = useCallback((field, selectedSet) => {
      setActiveFilters((prev) => ({
        ...prev,
        [field]: selectedSet,
      }))
    }, [])

    const handleClearFilter = useCallback((field) => {
      setActiveFilters((prev) => {
        const next = { ...prev }
        delete next[field]
        return next
      })
    }, [])

    // 1. Enrich columns with Custom Header Filter Menu & formatters
    const columns = useMemo(() => {
      return (rawColumns || [])
        .filter((col) => col.field !== 'GRID_TYPE')
        .map((col) => {
          const isNumberCol = col.type === 'number'
          const decimalsToShow = isAromaticsHmd ? 5 : 3

          let displayTitle = col.title || col.headerName || col.field
          const titleKey = String(displayTitle || '')
            .trim()
            .toLowerCase()
          const fieldKey = String(col.field || '')
            .trim()
            .toLowerCase()
          const monthNum = MONTH_MAP[titleKey] || MONTH_MAP[fieldKey]

          if (monthNum && headerMap && headerMap[monthNum]) {
            displayTitle = headerMap[monthNum]
          }

          const selectedVals = activeFilters[col.field]

          return {
            field: col.field,
            headerName: displayTitle,
            flex: rawColumns.length > 15 ? undefined : 1,
            width: rawColumns.length > 15 ? 125 : undefined,
            minWidth: 85,
            headerAlign: isNumberCol ? 'right' : 'left',
            align: isNumberCol ? 'right' : 'left',
            sortable: false,
            filterable: false,
            renderHeader: () => (
              <CustomColumnHeader
                field={col.field}
                title={displayTitle}
                getUniqueValues={getUniqueValues}
                selectedValues={selectedVals}
                onApplyFilter={handleApplyFilter}
                onClearFilter={handleClearFilter}
                isNumeric={isNumberCol}
              />
            ),
            renderCell: (params) => {
              if (params.value == null || params.value === '') return ''
              let displayVal = params.value
              if (isNumberCol) {
                const num = Number(params.value)
                displayVal = isNaN(num)
                  ? params.value
                  : num.toFixed(decimalsToShow)
              }
              const fullVal = String(params.value ?? '')

              return (
                <Tooltip title={fullVal} arrow placement='top' enterDelay={300}>
                  <span
                    style={{
                      width: '100%',
                      textAlign: isNumberCol ? 'right' : 'left',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                      display: 'inline-block',
                      cursor: 'pointer',
                    }}
                  >
                    {displayVal}
                  </span>
                </Tooltip>
              )
            },
          }
        })
    }, [
      rawColumns,
      headerMap,
      isAromaticsHmd,
      getUniqueValues,
      activeFilters,
      handleApplyFilter,
      handleClearFilter,
    ])

    // 2. Filter rows based on active column checkbox selections
    const filteredRows = useMemo(() => {
      let result = rawData || []
      const activeEntries = Object.entries(activeFilters)

      if (activeEntries.length === 0) return result

      return result.filter((row) => {
        return activeEntries.every(([field, selectedSet]) => {
          if (!selectedSet || selectedSet.size === 0) return true
          const colDef = rawColumns.find((c) => c.field === field)
          let val = row[field]
          if (colDef?.type === 'number' && val != null && val !== '') {
            const num = Number(val)
            val = isNaN(num) ? val : num.toFixed(isAromaticsHmd ? 5 : 3)
          }
          return selectedSet.has(val ?? '')
        })
      })
    }, [rawData, activeFilters, rawColumns, isAromaticsHmd])

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
              label={`${filteredRows.length} Rows`}
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
                rows={filteredRows}
                columns={columns}
                getRowId={(row) => row.id}
                rowHeight={48}
                headerHeight={38}
                disableColumnSorting
                pageSizeOptions={[50, 100]}
                initialState={{
                  pagination: { paginationModel: { pageSize: 50, page: 0 } },
                }}
                disableRowSelectionOnClick
                disableColumnMenu
                sx={{
                  border: 'none',
                  fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
                  fontSize: '0.82rem',
                  '& .MuiDataGrid-sortIcon': {
                    display: 'none !important',
                  },
                  '& .MuiDataGrid-iconButtonContainer': {
                    display: 'none !important',
                  },
                  '& .MuiDataGrid-columnHeaders': {
                    backgroundColor: '#F8FAFC',
                    color: '#1E293B',
                    fontWeight: 600,
                    borderBottom: '1px solid #CBD5E1',
                    minHeight: '38px !important',
                    maxHeight: '38px !important',
                    lineHeight: '38px !important',
                  },
                  '& .MuiDataGrid-columnHeader': {
                    padding: '0 !important',
                    borderRight: '1px solid #E2E8F0',
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
                    borderRight: '1px solid #F1F5F9',
                    borderBottom: '1px solid #F1F5F9',
                    padding: '0 8px',
                    color: '#1E293B',
                    '&:focus, &:focus-within': { outline: 'none' },
                  },
                  '& .MuiDataGrid-row:nth-of-type(even)': {
                    backgroundColor: '#FAFAFA',
                  },
                  '& .MuiDataGrid-row:hover': {
                    backgroundColor: '#F0F7FF',
                  },
                  '& .MuiDataGrid-footerContainer': {
                    minHeight: '48px !important',
                    maxHeight: '48px !important',
                    borderTop: '1px solid #E2E8F0',
                    backgroundColor: '#F8FAFC',
                    display: 'flex',
                    alignItems: 'center',
                  },
                  '& .MuiTablePagination-root': {
                    color: '#475569',
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
                      color: '#475569',
                      margin: 0,
                      lineHeight: '48px',
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
                        color: '#CBD5E1',
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

export default SingleReportGrid
