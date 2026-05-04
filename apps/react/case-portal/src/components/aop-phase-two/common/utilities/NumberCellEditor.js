import { InputBase } from '@mui/material'
import NotificationTST from 'components/Utilities/NotificationTST'
import { useState, useEffect, useRef } from 'react'

// Utility: Get nested property value by path (supports any depth)
// Handles both nested fields (e.g., "april.shutdownHrs") and flat fields (e.g., "apr")
const getNestedValue = (obj, path) => {
  if (!path || !obj) return undefined
  const keys = path.split('.')
  return keys.reduce((acc, key) => acc?.[key], obj)
}

export const NumberCellEditor = ({
  dataItem,
  field,
  onChange,
  wholeNumberOnly = false,
  maxValue = null,
}) => {
  // Handle nested fields (e.g., "april.shutdownHrs")
  const initialValue = getNestedValue(dataItem, field) ?? ''
  const [localValue, setLocalValue] = useState(initialValue)
  const inputRef = useRef(null)

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const handleChange = (e) => {
    const val = e.target.value
    // Allow only whole numbers if wholeNumberOnly is true, otherwise allow decimals
    const pattern = wholeNumberOnly ? /^\d*$/ : /^\d*(\.\d*)?$/
    if (val === '' || pattern.test(val)) {
      // Check if value exceeds maxValue (for shutdownHrs validation)
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
        sx={{
          width: '100%',
          fontSize: '15px',
          fontWeight: 600,
          fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif !important",
          color: '#1d3665',
          px: 1,
          height: 40,
          borderRadius: '6px',
          backgroundColor: '#FFFFFF',
          border: '1px solid #E0E4EC',
          boxShadow: '0px 1px 2px rgba(0, 0, 0, 0.05)',
          transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
          '&:hover': {
            borderColor: '#B0B8C4',
            backgroundColor: '#F9FAFB',
          },
          '&.Mui-focused': {
            borderColor: '#00F5E1',
            boxShadow: '0 0 0 3px rgba(0, 245, 225, 0.12)',
            backgroundColor: '#FFFFFF',
          },
          '& input': {
            textAlign: 'right',
            padding: '0 !important',
          },
        }}
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
