import { DropDownList } from '@progress/kendo-react-dropdowns'
import { useState, useEffect, useRef } from 'react'

export const SelectCellEditor = ({
  dataItem,
  field,
  onChange,
  options = [],
  textField = 'label',
  valueField = 'value',
  placeholder = 'Select...',
}) => {
  const storedValue = dataItem[field] ?? ''
  // Find the matching option object based on the stored value
  // Handle both string and numeric values
  const normalizeValue = (val) => {
    // Check if it's a numeric string or number (e.g., '4', '4.0', 4, 4.0)
    const numVal = parseFloat(val)
    if (!isNaN(numVal)) {
      // It's a numeric value, normalize to string
      return String(numVal)
    }
    // It's a non-numeric string (e.g., 'Price', 'Amount'), return as-is
    return String(val)
  }
  const selectedOption =
    options.find(
      (opt) => normalizeValue(opt[valueField]) === normalizeValue(storedValue),
    ) || null
  const [localValue, setLocalValue] = useState(selectedOption)
  const inputRef = useRef(null)

  // Autofocus when cell enters edit mode
  useEffect(() => {
    const timer = setTimeout(() => {
      if (inputRef.current?.element) {
        inputRef.current.element.focus()
      }
    }, 50)
    return () => clearTimeout(timer)
  }, [])

  const handleChange = (e) => {
    const selectedOpt = e.value
    const newValue = selectedOpt ? selectedOpt[valueField] : ''
    setLocalValue(selectedOpt)
    onChange({ dataItem, field, value: newValue })
  }

  return (
    <DropDownList
      ref={inputRef}
      data={options}
      textField={textField}
      dataItemKey={valueField}
      value={localValue}
      onChange={handleChange}
      className='dropdown-editor'
      style={{ width: '100%' }}
    />
  )
}
