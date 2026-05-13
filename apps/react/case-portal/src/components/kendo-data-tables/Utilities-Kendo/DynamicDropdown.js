import React, { useEffect, useRef } from 'react'
import { DropDownList, MultiSelect } from '@progress/kendo-react-dropdowns'

const DynamicDropdown = (props) => {
  const { dataItem, field, onChange, options = [], multiSelect = false } = props
  const inputRef = useRef(null)

  const handleChange = (e) => {
    if (multiSelect) {
      onChange({
        dataItem: dataItem,
        field: field,
        syntheticEvent: e.syntheticEvent,
        value: e.target.value,
      })
    } else {
      onChange({
        dataItem: dataItem,
        field: field,
        syntheticEvent: e.syntheticEvent,
        value: e.target.value?.value || e.target.value,
      })
    }
  }

  const handleBlur = () => {
    if (multiSelect) {
      const currentValues = Array.isArray(dataItem[field]) ? dataItem[field] : []
      const newValues = inputRef.current?.value || []
      if (JSON.stringify(currentValues) !== JSON.stringify(newValues)) {
        onChange({
          dataItem: dataItem,
          field: field,
          syntheticEvent: null,
          value: newValues,
        })
      }
    } else {
      const currentValue = selectedOption?.value
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
  }

  useEffect(() => {
    const timer = setTimeout(() => {
      if (inputRef.current?.element) {
        inputRef.current.element.focus()
      }
    }, 50)
    return () => clearTimeout(timer)
  }, [])

  if (multiSelect) {
    const selectedValues = Array.isArray(dataItem[field])
      ? dataItem[field]
      : dataItem[field]
        ? [dataItem[field]]
        : []

    const selectedOptions = options.filter((opt) =>
      selectedValues.includes(opt.value),
    )

    return (
      <MultiSelect
        ref={inputRef}
        data={options}
        textField='name'
        dataItemKey='value'
        value={selectedOptions}
        onChange={handleChange}
        onBlur={handleBlur}
        style={{ width: '100%' }}
      />
    )
  }

  const selectedOption = options.find((opt) => opt.value === dataItem[field])

  return (
    <DropDownList
      ref={inputRef}
      data={options}
      textField='name'
      dataItemKey='value'
      value={selectedOption}
      onChange={handleChange}
      onBlur={handleBlur}
      className='dropdown-editor'
      style={{ width: '100%' }}
    />
  )
}

export default DynamicDropdown
