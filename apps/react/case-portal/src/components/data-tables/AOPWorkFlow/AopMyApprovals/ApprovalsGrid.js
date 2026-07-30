import React from 'react'
import { Grid, GridColumn } from '@progress/kendo-react-grid'
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
 * Supports native row expansion with live workflow stepper
 */
const ApprovalsGrid = ({
  rows = [],
  columns = [],
  onExpandChange,
  loading = false,
}) => {
  return (
    <div className='kendo-data-grid' style={{ width: '100%', overflow: 'hidden' }}>
      <Grid
        style={{
          width: '100%',
          maxHeight: 'calc(100vh - 220px)',
          minHeight: '400px',
        }}
        data={rows}
        detail={DetailComponent}
        expandField='expanded'
        onExpandChange={onExpandChange}
        dataItemKey='id'
        resizable={true}
        sortable={true}
        pageable={
          rows.length > 50
            ? {
                buttonCount: 4,
                pageSizes: [10, 50, 100],
              }
            : false
        }
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
                filterable={col.filterable !== false}
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
              filterable={col.filterable !== false}
              sortable={col.sortable !== false}
            />
          )
        })}
      </Grid>
    </div>
  )
}

export default ApprovalsGrid
