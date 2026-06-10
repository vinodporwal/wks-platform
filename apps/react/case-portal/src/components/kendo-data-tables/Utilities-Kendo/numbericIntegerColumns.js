import { useState, useEffect, useRef } from 'react'
import { InputBase, Box } from '@mui/material'
import NotificationTST from 'components/Utilities/NotificationTST'

export const NoSpinnerNumericIntegerEditor = ({
  dataItem,
  field,
  onChange,
  maxLength,
}) => {
  const initialValue = dataItem[field] ?? ''
  const [localValue, setLocalValue] = useState(initialValue)
  const isFirstRender = useRef(true)

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const handleChange = (e) => {
    const val = e.target.value
    // If integerOnly is true, allow only whole numbers (no decimal point)
    const pattern = /^\d*$/
    if (val === '' || pattern.test(val)) {
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

  return (
    <>
      <InputBase
        value={localValue}
        onChange={handleChange}
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
