import { Input } from '@progress/kendo-react-inputs'
import NotificationTST from 'components/Utilities/NotificationTST'
import { useState, useEffect, useRef } from 'react'

export const NoSpinnerNumericEditor = ({ dataItem, field, onChange }) => {
  const initialValue = dataItem?.[field] ?? ''
  const [localValue, setLocalValue] = useState(initialValue)
  const isFirstRender = useRef(true)

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const isTST = dataItem?.productName?.trim().toLowerCase() === 'tst'

  const handleChange = (e) => {
    const val = e.target.value

    // Allow empty value
    if (val === '') {
      setLocalValue(val)
      return
    }

    // Allow only numbers + decimals
    if (!/^\d*(\.\d*)?$/.test(val)) return

    const numVal = Number(val)

    // TST validation
    if (isTST && (numVal < 100 || numVal > 370)) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Please enter a value between 100 and 370!',
        severity: 'warning',
      })
    } else {
      setSnackbarOpen(false)
    }

    setLocalValue(val)
  }

  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false
      return
    }

    const timer = setTimeout(() => {
      if (localValue !== initialValue) {
        onChange({
          dataItem,
          field,
          value: localValue === '' ? null : Number(localValue),
        })
      }
    }, 300)

    return () => clearTimeout(timer)
  }, [localValue, initialValue, dataItem, field, onChange])

  return (
    <>
      <Input
        value={localValue}
        onChange={handleChange}
        style={{
          fontSize: '0.8rem',
          padding: '2px 4px',
          height: '25px',
          lineHeight: '1rem',
        }}
      />

      <NotificationTST
        open={snackbarOpen}
        message={snackbarData.message}
        severity={snackbarData.severity}
        onClose={() => setSnackbarOpen(false)}
      />
    </>
  )
}
