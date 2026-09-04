import { useState, useEffect, useRef } from 'react'
import { InputBase, Box } from '@mui/material'
import NotificationTST from 'components/Utilities/NotificationTST'

export const NoSpinnerNumericEditor = ({
  dataItem,
  field,
  onChange,
  maxLength,
}) => {
  const initialValue = dataItem[field] ?? ''
  const [localValue, setLocalValue] = useState(initialValue)
  const isFirstRender = useRef(true)
  const latestValueRef = useRef(localValue)
  const initialValueRef = useRef(initialValue)
  latestValueRef.current = localValue

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
    if (latestValueRef.current !== initialValueRef.current) {
      onChange({ dataItem, field, value: latestValueRef.current })
      initialValueRef.current = latestValueRef.current
    }
  }

  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false
      return
    }
    const handler = setTimeout(() => {
      if (localValue !== initialValueRef.current) {
        onChange({ dataItem, field, value: localValue })
        initialValueRef.current = localValue
      }
    }, 300)
    return () => clearTimeout(handler)
  }, [localValue, dataItem, field, onChange])

  // Flush on unmount if user navigated away quickly via Tab before debounce timer fired
  useEffect(() => {
    return () => {
      if (latestValueRef.current !== initialValueRef.current) {
        onChange({ dataItem, field, value: latestValueRef.current })
      }
    }
  }, [dataItem, field, onChange])

  return (
    <>
      <InputBase
        autoFocus
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
