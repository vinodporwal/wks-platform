import { InputBase } from '@mui/material'
import NotificationTST from 'components/Utilities/NotificationTST'
import { useState, useEffect, useRef } from 'react'

// Utility: Get nested property value by path (supports any depth)
const getNestedValue = (obj, path) => {
  if (!path || !obj) return undefined
  const keys = path.split('.')
  return keys.reduce((acc, key) => acc?.[key], obj)
}

export const UomNumberCellEditor = ({
  dataItem,
  field,
  onChange,
  allowNegative = false,
  maxValue = null,
}) => {
  const initialValue = getNestedValue(dataItem, field) ?? ''
  const [localValue, setLocalValue] = useState(initialValue)
  const inputRef = useRef(null)

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  // Check if UOM is '%' or 'm3/hr' (lowercase trimmed check)
  const rawUom = dataItem?.UOM ?? dataItem?.uom ?? ''
  const uom = String(rawUom).trim().toLowerCase()
  const isIntegerOnly = uom === '%' || uom === 'm3/hr'

  const handleChange = (e) => {
    const val = e.target.value
    // If UOM is '%' or 'm3/hr', only allow integers; otherwise allow decimals
    const pattern = isIntegerOnly
      ? (allowNegative ? /^-?\d*$/ : /^\d*$/)
      : (allowNegative ? /^-?\d*(\.\d*)?$/ : /^\d*(\.\d*)?$/)

    if (val === '' || pattern.test(val)) {
      if (maxValue !== null && val !== '' && parseFloat(val) > maxValue) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: `Value cannot exceed ${maxValue}!`,
          severity: 'warning',
        })
        return
      }

      if (dataItem?.productName?.trim().toLowerCase() === 'tst') {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Please enter a value between 100 and 370 !',
          severity: 'warning',
        })
      }
      setLocalValue(val)
    }
  }

  // Autofocus when cell enters edit mode
  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.focus()
    }
  }, [])

  const handleBlur = () => {
    if (localValue !== initialValue) {
      onChange({ dataItem, field, value: localValue })
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Tab' || e.key === 'Enter') {
      if (localValue !== initialValue) {
        onChange({ dataItem, field, value: localValue })
      }
    }
  }

  return (
    <td>
      <InputBase
        inputRef={inputRef}
        value={localValue}
        onChange={handleChange}
        onBlur={handleBlur}
        onKeyDown={handleKeyDown}
        autoComplete='off'
        className='input-editor'
      />
      <NotificationTST
        open={snackbarOpen}
        message={snackbarData?.message || ''}
        severity={snackbarData?.severity || 'info'}
        onClose={() => setSnackbarOpen(false)}
      />
    </td>
  )
}
