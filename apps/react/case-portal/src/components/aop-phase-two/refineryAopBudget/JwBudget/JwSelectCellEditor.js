import React, { useState, useEffect, useRef } from 'react'
import { DropDownList } from '@progress/kendo-react-dropdowns'
import { filterBy } from '@progress/kendo-data-query'

/**
 * Dedicated Custom Select Cell Editor for Unit dropdown in JobWork screens.
 * Accurately matches values and display labels without any numeric coercion issues.
 */
export const JwSelectCellEditor = ({
  dataItem,
  field,
  onChange,
  options = [],
  textField = 'label',
  valueField = 'value',
  placeholder = 'Select Unit...',
  showClearOption = false,
  returnFullObject = false,
}) => {
  const storedValue = String(dataItem?.[field] || '').trim()

  const optionsWithClear = showClearOption
    ? [{ [valueField]: '', [textField]: 'Clear' }, ...options]
    : options

  const findSelectedOption = (opts, val) => {
    if (!val) return null
    const target = String(val).trim().toLowerCase()
    return (
      opts.find((opt) => {
        const v = String(opt[valueField] || opt.value || opt.label || '').trim().toLowerCase()
        const t = String(opt[textField] || opt.label || opt.displayName || '').trim().toLowerCase()
        const id = String(opt.id || opt.unitId || '').trim().toLowerCase()
        return v === target || t === target || (id && id === target)
      }) || null
    )
  }

  const [localValue, setLocalValue] = useState(() =>
    findSelectedOption(optionsWithClear, storedValue),
  )
  const [filteredData, setFilteredData] = useState(optionsWithClear)
  const inputRef = useRef(null)

  useEffect(() => {
    const matched = findSelectedOption(optionsWithClear, storedValue)
    setLocalValue(matched)
    setFilteredData(optionsWithClear)
  }, [storedValue, options])

  const filterData = (filter) => {
    if (!filter) return optionsWithClear
    return filterBy(optionsWithClear, filter)
  }

  const handleFilterChange = (event) => {
    setFilteredData(filterData(event.filter))
  }

  // Autofocus when entering edit mode
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
    const newValue = returnFullObject
      ? selectedOpt
      : selectedOpt
      ? selectedOpt[valueField] || selectedOpt[textField] || ''
      : ''
    setLocalValue(selectedOpt)
    onChange({ dataItem, field, value: newValue })
  }

  return (
    <DropDownList
      ref={inputRef}
      data={filteredData}
      textField={textField}
      dataItemKey={valueField}
      value={localValue}
      onChange={handleChange}
      filterable
      onFilterChange={handleFilterChange}
      className="dropdown-editor"
      style={{ width: '100%' }}
      placeholder={placeholder}
    />
  )
}
