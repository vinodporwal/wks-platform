import { useState, useEffect, useRef } from 'react'
import { InputBase, Box } from '@mui/material'
import NotificationTST from 'components/Utilities/NotificationTST'

export const NoSpinnerNumericEditor = ({
  dataItem,
  field,
  onChange,
  maxLength,
}) => {
  // Handle nested field paths (e.g., "apr.shutdownHrs")
  const getNestedValue = (obj, path) => {
    if (!path || !obj) return undefined
    const keys = path.split('.')
    return keys.reduce((acc, key) => acc?.[key], obj)
  }

  const initialValue = getNestedValue(dataItem, field) ?? ''
  const [localValue, setLocalValue] = useState(initialValue)
  const isFirstRender = useRef(true)
  const inputRef = useRef(null)

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const handleChange = (e) => {
    const val = e.target.value
    if (val === '' || /^\d*(\.\d*)?$/.test(val)) {
      if (dataItem?.productName?.trim().toLowerCase() === 'tst') {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Please enter a value between 100 and 370!',
          severity: 'warning',
        })
      }
      setLocalValue(val)
    }
  }

  const handleBlur = () => {
    if (localValue !== initialValue) {
      onChange({ dataItem, field, value: localValue })
    }
  }

  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false
      return
    }
    const handler = setTimeout(() => {
      if (localValue !== initialValue) {
        onChange({ dataItem, field, value: localValue })
      }
    }, 300)
    return () => clearTimeout(handler)
  }, [localValue, dataItem, field, onChange, initialValue])

  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.focus()
    }
  }, [])

  return (
    <>
      <InputBase
        inputRef={inputRef}
        value={localValue}
        onChange={handleChange}
        onBlur={handleBlur}
        autoComplete='off'
        maxLength={maxLength}
        className='input-editor'
      />
      <NotificationTST
        open={snackbarOpen}
        message={snackbarData?.message || ''}
        severity={snackbarData?.severity || 'info'}
        onClose={() => setSnackbarOpen(false)}
      />
    </>
  )
}
