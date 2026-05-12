import { DropDownList } from '@progress/kendo-react-dropdowns'
import React, { useEffect, useRef } from 'react'
import { NoSpinnerNumericEditor } from './numbericColumns'

const RowAwareDropdownEditor = ({
  dataItem,
  field,
  onChange,
  options = [],
  condition,
  fallback: Fallback = NoSpinnerNumericEditor,
  ...rest
}) => {
  const inputRef = useRef(null)

  // Condition not met → delegate to fallback editor (e.g. normal numeric input)
  if (!condition || !condition(dataItem)) {
    return (
      <Fallback
        dataItem={dataItem}
        field={field}
        onChange={onChange}
        {...rest}
      />
    )
  }

  const selectedOption =
    options.find((opt) => opt.value === dataItem[field]) ?? options[0]

  const handleChange = (e) => {
    onChange({
      dataItem,
      field,
      syntheticEvent: e.syntheticEvent,
      value: e.target.value?.value,
    })
  }

  const handleBlur = () => {
    const currentValue = selectedOption?.value
    const newValue = inputRef.current?.value
    if (currentValue !== newValue) {
      onChange({
        dataItem,
        field,
        syntheticEvent: null,
        value: newValue?.value || newValue,
      })
    }
  }

  // Auto-focus when cell enters edit mode
  useEffect(() => {
    const timer = setTimeout(() => {
      if (inputRef.current?.element) {
        inputRef.current.element.focus()
      }
    }, 50)
    return () => clearTimeout(timer)
  }, [])

  return (
    <DropDownList
      ref={inputRef}
      data={options}
      textField='name'
      dataItemKey='value'
      value={selectedOption}
      onChange={handleChange}
      onBlur={handleBlur}
      style={{ width: '100%' }}
    />
  )
}

export default RowAwareDropdownEditor
