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
  const selectedOption =
    options.find((opt) => opt[valueField] === storedValue) || null
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
    <td>
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
    </td>
  )
}
