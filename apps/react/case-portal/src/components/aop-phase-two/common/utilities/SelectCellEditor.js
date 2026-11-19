import { DropDownList } from '@progress/kendo-react-dropdowns'
import {
  filterBy
} from '@progress/kendo-data-query'
import React, { useState, useEffect, useRef } from 'react'

export const SelectCellEditor = ({
  dataItem,
  field,
  onChange,
  options = [],
  textField = 'label',
  valueField = 'value',
  placeholder = 'Select...',
  showClearOption = false,
  returnFullObject = false,
  keyboardNavigation = false,
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

  // Add clear option if enabled
  const optionsWithClear = showClearOption
    ? [{ [valueField]: '', [textField]: 'Clear' }, ...options]
    : options

  const selectedOption =
    optionsWithClear.find(
      (opt) =>
        normalizeValue(opt[valueField]) === normalizeValue(storedValue) ||
        normalizeValue(opt[textField]) === normalizeValue(storedValue),
    ) || null
  const [localValue, setLocalValue] = useState(selectedOption)
  const [filteredData, setFilteredData] = useState(optionsWithClear.slice())
  const [popupOpen, setPopupOpen] = useState(false)
  const [focusedIndex, setFocusedIndex] = useState(-1)
  const inputRef = useRef(null)
  const pendingFocusIndex = useRef(null)

  const filterData = (filter) => {
    const data = optionsWithClear.slice()
    return filterBy(data, filter)
  }

  const handleFilterChange = (event) => {
    setFilteredData(filterData(event.filter))
    setFocusedIndex(-1)
  }

  const pickValues = (val) => {
    if (val === null || val === undefined || val === '') return []
    if (typeof val !== 'object') return [val]
    return [val[valueField], val.value, val[textField], val.label].filter(
      (v) => v !== null && v !== undefined && v !== '',
    )
  }

  const isSameOption = (opt, val) => {
    const vals = pickValues(val)
    if (!vals.length || !opt) return false
    const optVals = pickValues(opt)
    return vals.some((v) =>
      optVals.some((ov) => normalizeValue(ov) === normalizeValue(v)),
    )
  }

  const getSelectedIndex = () =>
    filteredData.findIndex((opt) => isSameOption(opt, localValue))

  const firstEnabledIndex = () =>
    filteredData.findIndex((opt) => !opt?.disabled)

  const lastEnabledIndex = () => {
    for (let i = filteredData.length - 1; i >= 0; i--) {
      if (!filteredData[i]?.disabled) return i
    }
    return -1
  }

  // Keep the focused index inside the filtered list bounds
  useEffect(() => {
    if (focusedIndex >= filteredData.length) {
      setFocusedIndex(filteredData.length - 1)
    }
  }, [filteredData, focusedIndex])

  const handleOpen = () => {
    setPopupOpen(true)
    const pending = pendingFocusIndex.current
    pendingFocusIndex.current = null
    if (pending !== null) {
      setFocusedIndex(pending)
    } else {
      const selectedIdx = getSelectedIndex()
      setFocusedIndex(selectedIdx >= 0 ? selectedIdx : firstEnabledIndex())
    }
  }

  const handleClose = () => {
    setPopupOpen(false)
    setFocusedIndex(-1)
    pendingFocusIndex.current = null
  }

  const closePopup = () => {
    inputRef.current?.togglePopup()
  }

  const moveFocus = (direction) => {
    if (!filteredData.length) return
    setFocusedIndex((prev) => {
      let next = prev
      for (let i = 0; i < filteredData.length; i++) {
        next = (next + direction + filteredData.length) % filteredData.length
        if (!filteredData[next]?.disabled) break
      }
      return next
    })
  }

  const commitFocusedOption = () => {
    const item = filteredData[focusedIndex]
    if (item === undefined) {
      closePopup()
      return
    }
    const newValue = returnFullObject
      ? item
      : item
        ? item[valueField]
        : ''
    setLocalValue(item)
    onChange({ dataItem, field, value: newValue })
    closePopup()
  }

  // Keyboard navigation: arrows move the highlighted option inside the open
  // popup, Enter commits it, Escape closes — instead of Kendo's default
  // behavior where arrows immediately change the value and close the popup.
  const handleKeyDownCapture = (e) => {
    const { key } = e
    if (key === 'ArrowDown' || key === 'ArrowUp') {
      e.preventDefault()
      e.stopPropagation()
      if (!popupOpen) {
        const selectedIdx = getSelectedIndex()
        pendingFocusIndex.current =
          key === 'ArrowDown'
            ? selectedIdx >= 0
              ? selectedIdx
              : firstEnabledIndex()
            : selectedIdx >= 0
              ? selectedIdx
              : lastEnabledIndex()
        inputRef.current?.togglePopup()
      } else {
        moveFocus(key === 'ArrowDown' ? 1 : -1)
      }
      return
    }
    if (key === 'Home' || key === 'End') {
      e.preventDefault()
      e.stopPropagation()
      if (popupOpen) {
        setFocusedIndex(key === 'Home' ? firstEnabledIndex() : lastEnabledIndex())
      } else {
        pendingFocusIndex.current =
          key === 'Home' ? firstEnabledIndex() : lastEnabledIndex()
        inputRef.current?.togglePopup()
      }
      return
    }
    if (key === 'Enter' && popupOpen) {
      e.preventDefault()
      e.stopPropagation()
      commitFocusedOption()
      return
    }
    if (key === 'Escape' && popupOpen) {
      e.preventDefault()
      e.stopPropagation()
      closePopup()
    }
  }

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
    const newValue = returnFullObject
      ? selectedOpt
      : selectedOpt
        ? selectedOpt[valueField]
        : ''
    setLocalValue(selectedOpt)
    onChange({ dataItem, field, value: newValue })
  }

  // Highlight the keyboard-focused option inside the popup. When the focused
  // option is also the selected one, Kendo's own selected styling already
  // marks it — skip the override so it doesn't look grayed out.
  const renderItem = (li, itemProps) => {
    const isFocused =
      popupOpen && itemProps?.index === focusedIndex && !itemProps?.selected
    if (!isFocused) return li
    return React.cloneElement(li, {
      ...li.props,
      style: {
        ...li.props.style,
        backgroundColor: '#dbeafe',
      },
    })
  }

  return (
    <div
      onKeyDownCapture={
        keyboardNavigation ? handleKeyDownCapture : undefined
      }
      style={{ width: '100%' }}
    >
      <DropDownList
        ref={inputRef}
        data={filteredData}
        textField={textField}
        dataItemKey={valueField}
        value={localValue}
        onChange={handleChange}
        onOpen={keyboardNavigation ? handleOpen : undefined}
        onClose={keyboardNavigation ? handleClose : undefined}
        filterable
        onFilterChange={handleFilterChange}
        itemRender={keyboardNavigation ? renderItem : undefined}
        className='dropdown-editor'
        style={{ width: '100%' }}
      />
    </div>
  )
}
