import { Input } from '@progress/kendo-react-inputs'
import NotificationTST from 'components/Utilities/NotificationTST'
import { useState, useEffect, useRef } from 'react'
import { InputBase } from '../../../../../node_modules/@mui/material/index'

export const NoSpinnerNumericEditor = ({
  dataItem,
  field,
  onChange,
  allowNegative = false,
  tdProps,
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
    const pattern = allowNegative ? /^-?\d*(\.\d*)?$/ : /^\d*(\.\d*)?$/
    if (val === '' || pattern.test(val)) {
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

  const valueRef = useRef(localValue)
  valueRef.current = localValue
  const initialValueRef = useRef(initialValue)
  initialValueRef.current = initialValue
  const onChangeRef = useRef(onChange)
  onChangeRef.current = onChange
  const dataItemRef = useRef(dataItem)
  dataItemRef.current = dataItem
  const fieldRef = useRef(field)
  fieldRef.current = field

  useEffect(() => {
    return () => {
      if (valueRef.current !== initialValueRef.current) {
        onChangeRef.current({ dataItem: dataItemRef.current, field: fieldRef.current, value: valueRef.current })
      }
    }
  }, [])

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
    <td {...tdProps}>
      <InputBase
        inputRef={inputRef}
        value={localValue}
        onChange={handleChange}
        onBlur={handleBlur}
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
