import React, { useEffect, useRef } from 'react'
import { DropDownList } from '@progress/kendo-react-dropdowns'

const YearDropdownEditor = (props) => {
  const { dataItem, field, onChange, AOP_YEAR } = props
  const inputRef = useRef(null)

  const getYearOptions = (AOP_YEAR) => {
    let baseYear = parseInt(String(AOP_YEAR).slice(0, 4), 10)
    if (isNaN(baseYear)) baseYear = new Date().getFullYear()
    return Array.from({ length: 6 }, (_, i) => {
      const yearStr = (baseYear - i).toString()
      return {
        value: yearStr, // value as string
        text: yearStr,
      }
    })
  }

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

  const options = getYearOptions(AOP_YEAR)
  const selected = options.find((opt) => opt.value === String(dataItem[field]))

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

export default YearDropdownEditor
