import React, { useEffect, useRef } from 'react'
import { DropDownList } from '@progress/kendo-react-dropdowns'

const monthOptions = [
  { value: 'January', text: 'January' },
  { value: 'February', text: 'February' },
  { value: 'March', text: 'March' },
  { value: 'April', text: 'April' },
  { value: 'May', text: 'May' },
  { value: 'June', text: 'June' },
  { value: 'July', text: 'July' },
  { value: 'August', text: 'August' },
  { value: 'September', text: 'September' },
  { value: 'October', text: 'October' },
  { value: 'November', text: 'November' },
  { value: 'December', text: 'December' },
]

const MonthDropdownPEPP1 = (props) => {
  const { dataItem, field, onChange, customModifiedCells, options = [] } = props
  const inputRef = useRef(null)

  const handleChange = (e) => {
    onChange({
      dataItem: dataItem,
      field: field,
      syntheticEvent: e.syntheticEvent,
      value: e.target.value?.value || e.target.value,
    })
  }

  const handleBlur = () => {
    const currentValue = selectedMonth?.value
    const newValue = inputRef.current?.value
    if (currentValue !== newValue) {
      onChange({
        dataItem: dataItem,
        field: field,
        syntheticEvent: null,
        value: newValue?.value || newValue,
      })
    }
  }

  useEffect(() => {
    const timer = setTimeout(() => {
      if (inputRef.current?.element) {
        inputRef.current.element.focus()
      }
    }, 50)
    return () => clearTimeout(timer)
  }, [])

  const selectedMonth = options.find((item) => item.value === dataItem[field])

  // Highlight only for this component when edited
  const rowId = dataItem.id
  const isEdited =
    customModifiedCells &&
    customModifiedCells[rowId] &&
    customModifiedCells[rowId][field] !== undefined

  return (
    <td
      style={{
        padding: 0,
        overflow: 'visible',
        color: isEdited ? 'orange' : undefined,
        fontWeight: isEdited ? 'bold' : undefined,
      }}
    >
      <DropDownList
        ref={inputRef}
        data={options}
        textField='text'
        dataItemKey='value'
        value={selectedMonth}
        onChange={handleChange}
        onBlur={handleBlur}
        className='dropdown-editor'
        style={{
          width: '100%',
          border: 'none',
          height: '100%',
        }}
        popupSettings={{
          appendTo: document.body,
          animate: false,
        }}
      />
    </td>
  )
}

export default MonthDropdownPEPP1
