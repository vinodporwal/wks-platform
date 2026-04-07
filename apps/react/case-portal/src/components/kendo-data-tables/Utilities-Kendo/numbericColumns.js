import { useState, useEffect, useRef } from 'react'
import { InputBase, Box } from '@mui/material'
import NotificationTST from 'components/Utilities/NotificationTST'

export const NoSpinnerNumericEditor = ({ dataItem, field, onChange }) => {
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
        sx={{
          fontSize: '0.8125rem',
          fontWeight: 600,
          color: '#1d3665', // Using your deep navy color for the text
          px: 1,
          height: 28,
          borderRadius: '6px',
          backgroundColor: '#FFFFFF', // Solid white
          border: '1px solid #E0E4EC', // Very soft grey border
          boxShadow: '0px 1px 2px rgba(0, 0, 0, 0.05)', // Tiny "lift" from the page
          transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
          '&:hover': {
            borderColor: '#B0B8C4',
            backgroundColor: '#F9FAFB', // Extremely subtle shift on hover
          },
          '&.Mui-focused': {
            borderColor: '#00F5E1', // Your signature Cyan
            boxShadow: '0 0 0 3px rgba(0, 245, 225, 0.12)', // Modern "Halo" glow
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
    </>
  )
}
