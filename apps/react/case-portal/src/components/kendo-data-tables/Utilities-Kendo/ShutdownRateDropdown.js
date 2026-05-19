import React, { useEffect, useRef } from 'react'
import { DropDownList } from '@progress/kendo-react-dropdowns'

const ShutdownRateDropdown = (props) => {
  const { dataItem, field, onChange, customModifiedCells, dropdownData } = props
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
    const currentValue = selectedValue?.value
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

  // Find the selected object based on the value in the grid data
  const selectedValue =
    (dropdownData || []).find(
      (item) => String(item.value) === String(dataItem[field]),
    ) || null

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
        data={dropdownData || []}
        textField='text'
        dataItemKey='value'
        value={selectedValue}
        onChange={handleChange}
        onBlur={handleBlur}
        className='dropdown-editor'
        style={{ width: '100%', border: 'none', height: '100%' }}
        popupSettings={{
          appendTo: document.body,
          animate: false,
        }}
      />
    </td>
  )
}

export default ShutdownRateDropdown
