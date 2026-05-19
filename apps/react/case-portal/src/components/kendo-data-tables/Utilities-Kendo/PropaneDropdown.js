import React, { useEffect, useRef } from 'react'
import { DropDownList } from '@progress/kendo-react-dropdowns'

const PROPANE_OPTIONS = [
  { label: 'Propane Min', value: 'Propane Min' },
  { label: 'Propane 1Z', value: 'Propane 1Z' },
  { label: 'Propane 2Z', value: 'Propane 2Z' },
]

const PropaneDropdown = (props) => {
  const { dataItem, field, onChange } = props
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

  useEffect(() => {
    const timer = setTimeout(() => {
      if (inputRef.current?.element) {
        inputRef.current.element.focus()
      }
    }, 50)
    return () => clearTimeout(timer)
  }, [])

  const selectedOption = PROPANE_OPTIONS.find(
    (opt) => opt.value === dataItem[field],
  )

  return (
    <DropDownList
      ref={inputRef}
      data={PROPANE_OPTIONS}
      textField='label'
      dataItemKey='value'
      value={selectedOption}
      onChange={handleChange}
      onBlur={handleBlur}
      className='dropdown-editor'
      style={{ width: '100%' }}
    />
  )
}

export default PropaneDropdown
