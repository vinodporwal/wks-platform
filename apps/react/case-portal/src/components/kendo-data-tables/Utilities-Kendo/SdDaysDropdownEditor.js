import React, { useEffect, useRef } from 'react'
import { DropDownList } from '@progress/kendo-react-dropdowns'

const SDDaysDropdownEditorWrapper = (props) => {
  const { dataItem, field, onChange, sdDaysValues = [] } = props
  const inputRef = useRef(null)

  // Use API data if available, fallback to static
  const options = sdDaysValues.length
    ? sdDaysValues.map((opt) => ({
        value: opt.value,
        text: opt.name || opt.text, // support both
      }))
    : []
  const selected = options.find((opt) => opt.value === dataItem[field])

  const handleBlur = () => {
    const currentValue = selected?.value
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
      textField='text'
      dataItemKey='value'
      value={selected}
      onChange={(e) =>
        onChange({
          dataItem,
          field,
          syntheticEvent: e.syntheticEvent,
          value: e.target.value?.value || e.target.value,
        })
      }
      onBlur={handleBlur}
      className='dropdown-editor'
      style={{ width: '100%' }}
    />
  )
}

export default SDDaysDropdownEditorWrapper
