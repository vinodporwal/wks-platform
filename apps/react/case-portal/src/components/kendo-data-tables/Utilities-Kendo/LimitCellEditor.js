import { DropDownList } from '@progress/kendo-react-dropdowns'
import { useMemo } from 'react'

const LimitCellEditor = (props) => {
  const { READ_ONLY, dataItem, field, onChange, ...tdProps } = props

  // Hardcoded dropdown options
  const allOptions = useMemo(
    () => [
      { value: '<', label: '<' },
      { value: '>', label: '>' },
      { value: '+-', label: '+-' },
    ],
    [],
  )

  const currentValueObj = useMemo(
    () => allOptions.find((opt) => opt.value === dataItem[field]) || null,
    [allOptions, dataItem, field],
  )

  const isDisabled = dataItem?.uom != '%' || READ_ONLY

  if (typeof onChange === 'function') {
    const handleChange = (e) => {
      onChange({
        dataItem,
        field,
        value: e.value?.value, // store only '<', '>', '+-'
      })
    }

    return (
      <DropDownList
        data={allOptions}
        textField='label'
        dataItemKey='value'
        value={currentValueObj}
        onChange={handleChange}
        className={isDisabled ? 'dropdown-editor dropdown-editor-disabled' : 'dropdown-editor'}
        style={{
          width: '100%',
        }}
        disabled={isDisabled}
      />
    )
  }

  // Display selected value in read-only mode
  const displayLabel =
    allOptions.find((opt) => opt.value === dataItem[field])?.label || ''

  return (
    <td
      {...tdProps}
      style={{
        padding: '0.5rem 1rem',
        whiteSpace: 'nowrap',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        ...tdProps.style,
        backgroundColor: isDisabled ? '#f1f5f9' : tdProps.style?.backgroundColor,
      }}
    >
      {displayLabel || '—'}
    </td>
  )
}

export default LimitCellEditor
