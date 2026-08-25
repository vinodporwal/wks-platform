import React, { useState } from 'react'
import { Grid, GridColumn } from '@progress/kendo-react-grid'
import { process } from '@progress/kendo-data-query'
import { Box, CircularProgress, Typography } from '@mui/material'
import '@progress/kendo-theme-default/dist/all.css'
import '@progress/kendo-font-icons/dist/index.css'
import '../../../../kendo-data-grid.css'
import RowWorkflowStepper from './RowWorkflowStepper'

/**
 * Custom Detail Component rendered beneath an expanded row
 */
const DetailComponent = (props) => {
  return <RowWorkflowStepper row={props.dataItem} />
}

/**
 * Dedicated Kendo Grid component for AopMyApprovals
 * Supports native row expansion with live workflow stepper, sorting, and loading indicator
 */
const ApprovalsGrid = ({
  rows = [],
  columns = [],
  onExpandChange,
  loading = false,
}) => {
  const [dataState, setDataState] = useState({
    sort: [],
    filter: {
      logic: 'and',
      filters: [],
    },
  })

  const onDataStateChange = (e) => {
    setDataState(e.dataState)
  }

  const processedData = process(rows, dataState)

  return (
    <div
      className='kendo-data-grid'
      style={{
        width: '100%',
        overflow: 'hidden',
        position: 'relative',
        minHeight: '400px',
      }}
    >
      {/* Loading Overlay */}
      {loading && (
        <Box
          sx={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(255, 255, 255, 0.75)',
            zIndex: 10,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 1.5,
          }}
        >
          <CircularProgress size={36} sx={{ color: '#005eb8' }} />
          <Typography
            sx={{
              fontSize: '13px',
              fontWeight: 600,
              color: '#475569',
              letterSpacing: '0.2px',
            }}
          >
            Loading Approvals...
          </Typography>
        </Box>
      )}

      <Grid
        style={{
          width: '100%',
          maxHeight: 'calc(100vh - 220px)',
          minHeight: '400px',
        }}
        data={processedData}
        total={processedData.total}
        detail={DetailComponent}
        expandField='expanded'
        onExpandChange={onExpandChange}
        dataItemKey='id'
        resizable={true}
        filterable={false}
        sortable={true}
        sort={dataState.sort}
        onSortChange={(e) =>
          setDataState((prev) => ({ ...prev, sort: e.sort }))
        }
        pageable={false}
        onDataStateChange={onDataStateChange}
        size='small'
      >
        {columns.map((col) => {
          const colWidth = col.width ? col.width : undefined
          const colMinWidth = col.minWidth ? col.minWidth : undefined

          if (col.cell) {
            const CustomCell = col.cell
            return (
              <GridColumn
                key={col.field}
                field={col.field}
                title={col.title}
                width={colWidth}
                minWidth={colMinWidth}
                editable={false}
                filterable={false}
                sortable={col.sortable !== false}
                cells={{
                  data: CustomCell,
                }}
                cell={CustomCell}
              />
            )
          }

          return (
            <GridColumn
              key={col.field}
              field={col.field}
              title={col.title}
              width={colWidth}
              minWidth={colMinWidth}
              editable={false}
              filterable={false}
              sortable={col.sortable !== false}
            />
          )
        })}
      </Grid>
    </div>
  )
}

export default ApprovalsGrid
