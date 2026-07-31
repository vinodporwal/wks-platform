import React from 'react'
import { GridColumn } from '@progress/kendo-react-grid'
import LimitCellEditor from '../Utilities-Kendo/LimitCellEditor'

export const LimitColumn = ({
  col,
  setWidth,
  READ_ONLY,
  SimpleHeaderWithTooltip,
  ColumnMenuCheckboxFilter,
  isActive,
}) => {
  return (
    <GridColumn
      locked={col.locked || false}
      key='limit'
      field='limit'
      width={setWidth(col?.minWidth || 150)}
      title={col?.title}
      editable={col?.editable || true}
      cells={{
        data: (cellProps) => (
          <LimitCellEditor {...cellProps} READ_ONLY={READ_ONLY} />
        ),
        headerCell: SimpleHeaderWithTooltip,
      }}
      columnMenu={ColumnMenuCheckboxFilter}
      headerClassName={isActive ? 'active-column' : ''}
    />
  )
}

export default LimitColumn
