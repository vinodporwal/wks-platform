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

  const onChangeRef = useRef(onChange)
  const dataItemRef = useRef(dataItem)
  const fieldRef = useRef(field)
  const initialValueRef = useRef(initialValue)
  const isFirstRender = useRef(true)

  useEffect(() => {
    onChangeRef.current = onChange
    dataItemRef.current = dataItem
    fieldRef.current = field
    initialValueRef.current = initialValue
  })

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

  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false
      return
    }
    const handler = setTimeout(() => {
      if (localValue !== initialValueRef.current) {
        onChangeRef.current({ dataItem: dataItemRef.current, field: fieldRef.current, value: localValue })
      }
    }, 300)
    return () => clearTimeout(handler)
  }, [localValue]) // only re-run when the user actually types

  return (
    <>
      <InputBase
        value={localValue}
        onChange={handleChange}
        autoFocus
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

