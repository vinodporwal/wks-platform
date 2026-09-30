import React, { useMemo } from 'react'
import AdvanceKendoTable from '../AdvanceKendoTable/index'
import {
  DynamicRowCellEditor,
  DynamicRowDisplayCell,
} from '../utilities/DynamicRowCellEditor'
import { useSelector } from 'react-redux'

const RowBasedKendoTable = (props) => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { verticalObject } = dataGridStore
  
  const isFilamentOrStaple = useMemo(() => 
    ['filament (pfy)', 'staple (psf)'].includes(verticalObject?.name?.toLowerCase()), 
    [verticalObject]
  )

  const { columns, rows, ...restProps } = props

  const getDecimalPlacesFromFormat = (format) => {
    if (!format) return 2
    const match = format.match(/\{0:0\.(0+)\}/)
    return match ? match[1].length : 2
  }

  const enhancedColumns = useMemo(() => {
    return columns.map((col) => {
      if (col.type === 'row-based' || col.type === 'conditional') {
              const renderDataCell = (cellProps, inEditPhase = false) => {
                const { dataItem, field } = cellProps
                const rowId = dataItem.id
                const customModifiedCells =
                  props.externalCustomModifiedCells || {}

                const isEdited = Object.prototype.hasOwnProperty.call(
                  customModifiedCells?.[rowId] || {},
                  field,
                )

                const value = dataItem?.[field]
                const inputType = dataItem?.type

                let displayValue = value

                if (inputType === 'boolean' || inputType === 'yesno') {
                  displayValue =
                    typeof value === 'boolean' ? (value ? 'Yes' : 'No') : value
                } else if (inputType === 'date' && value instanceof Date) {
                  const year = value.getFullYear()
                  const month = String(value.getMonth() + 1).padStart(2, '0')
                  const day = String(value.getDate()).padStart(2, '0')
                  displayValue = `${year}-${month}-${day}`
                } else if (inputType === 'datetime' && value instanceof Date) {
                  const year = value.getFullYear()
                  const month = String(value.getMonth() + 1).padStart(2, '0')
                  const day = String(value.getDate()).padStart(2, '0')
                  const hours = String(value.getHours()).padStart(2, '0')
                  const minutes = String(value.getMinutes()).padStart(2, '0')
                  displayValue = `${year}-${month}-${day} ${hours}:${minutes}`
                } else if (!isNaN(value) && value !== null && value !== '') {
                  const decimals = isFilamentOrStaple 
                    ? getDecimalPlacesFromFormat(col.format)
                    : dataItem.isEditable ? 2 : 2
                  displayValue = Number(value).toFixed(decimals)
                }

                return (
                  <td
                    {...cellProps.tdProps}
                    title={String(displayValue ?? '')}
                    style={{
                      color: isEdited && !inEditPhase ? 'orange' : inEditPhase ? '#999' : undefined,
                      fontWeight: isEdited ? 'bold' : undefined,
                      ...(inEditPhase ? {
                        backgroundColor: '#f5f5f5',
                        cursor: 'not-allowed',
                      } : {}),
                    }}
                  >
                    {displayValue ?? ''}
                  </td>
                )
              }

              return {
                ...col,
                type: 'row-based',
                cells: {
                  edit: {
                    text: (cellProps) => {
                      const { dataItem, field } = cellProps;
                      const isColEditable = col.editable !== false;
                      const isRowEditable = dataItem.isEditable !== false;
                      
                      let isCellEditable = true;
                      if (dataItem?.nonEditableFields?.includes(field)) isCellEditable = false;
                      if (dataItem?.editableFields && !dataItem?.editableFields?.includes(field)) isCellEditable = false;
                      
                      if (!isColEditable || !isRowEditable || !isCellEditable) {
                        return renderDataCell(cellProps, true);
                      }
                      
                      return <DynamicRowCellEditor {...cellProps} />
                    }
                  },
                  data: (cellProps) => renderDataCell(cellProps, false),
                },
              }
      }
      return col
    })
  }, [columns, props.externalCustomModifiedCells])

  return (
    <AdvanceKendoTable {...restProps} columns={enhancedColumns} rows={rows} />
  )
}

export default RowBasedKendoTable
