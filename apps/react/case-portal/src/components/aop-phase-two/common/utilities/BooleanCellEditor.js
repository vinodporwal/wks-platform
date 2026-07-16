import { useState, useEffect, useRef } from 'react'

// Size presets for checkbox and toggle button
const SIZE_MAP = {
  small: {
    checkbox: { width: '14px', height: '14px' },
    button: { padding: '2px 8px', fontSize: '0.65rem', minWidth: '52px' },
  },
  medium: {
    checkbox: { width: '18px', height: '18px' },
    button: { padding: '4px 12px', fontSize: '0.75rem', minWidth: '70px' },
  },
  large: {
    checkbox: { width: '22px', height: '22px' },
    button: { padding: '6px 16px', fontSize: '0.875rem', minWidth: '90px' },
  },
}

export const BooleanCellEditor = ({
  dataItem,
  field,
  onChange,
  trueLabel = 'Yes',
  falseLabel = 'No',
  useCheckbox = true,
  size = 'medium',
}) => {
  const storedValue = dataItem[field] ?? false
  const [localValue, setLocalValue] = useState(storedValue)
  const isFirstRender = useRef(true)

  const sizeStyles = SIZE_MAP[size] ?? SIZE_MAP.medium

  const handleChange = (e) => {
    const newValue = e.target.checked
    setLocalValue(newValue)
  }

  const handleToggleClick = () => {
    setLocalValue(!localValue)
  }

  // Debounced sync to grid, but skip first render
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false
      return
    }

    const handler = setTimeout(() => {
      // Only send if the value actually changed
      if (localValue !== storedValue) {
        onChange({ dataItem, field, value: localValue })
      }
    }, 300)

    return () => clearTimeout(handler)
  }, [localValue, dataItem, field, onChange, storedValue])

  if (useCheckbox) {
    return (
      <td style={{ textAlign: 'center', padding: '6px 2px' }}>
        <input
          type='checkbox'
          checked={localValue}
          onChange={handleChange}
          style={{
            cursor: 'pointer',
            accentColor: '#1976d2',
            ...sizeStyles.checkbox,
          }}
        />
      </td>
    )
  }

  // Alternative: Toggle button style
  return (
    <td style={{ textAlign: 'center', padding: '2px' }}>
      <button
        onClick={handleToggleClick}
        style={{
          fontWeight: '500',
          backgroundColor: localValue ? '#4CAF50' : '#f44336',
          color: 'white',
          border: 'none',
          borderRadius: '3px',
          cursor: 'pointer',
          transition: 'background-color 0.2s',
          ...sizeStyles.button,
        }}
      >
        {localValue ? trueLabel : falseLabel}
      </button>
    </td>
  )
}
