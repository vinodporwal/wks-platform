import { InputBase } from '@mui/material'
import { useState, useEffect, useRef } from 'react'

export const NumericEditorWithMinMax = ({
  dataItem,
  field,
  onChange,
  min,
  max,
}) => {
  // Resolve nested field paths (e.g., 'april.min' -> dataItem.april.min)
  const getNestedValue = (obj, fieldPath) => {
    const parts = fieldPath.split('.')
    let value = obj
    for (let part of parts) {
      value = value?.[part]
    }
    return value ?? ''
  }

  const initialValue = getNestedValue(dataItem, field)
  const [localValue, setLocalValue] = useState(initialValue)
  const [error, setError] = useState('')
  const inputRef = useRef(null)

  const handleChange = (e) => {
    let val = e.target.value

    // Allow only numeric (including decimal and negative)
    if (val === '' || /^-?\d*(\.\d*)?$/.test(val)) {
      const num = parseFloat(val)

      // Validate against min/max if provided
      if (val !== '') {
        let errorMsg = ''

        if (min !== undefined && num < min) {
          errorMsg = `Please enter a number between ${min} to ${max}`
        } else if (max !== undefined && num > max) {
          errorMsg = `Please enter a number between ${min} to ${max}`
        }

        setError(errorMsg)
      } else {
        setError('')
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
    if (localValue !== initialValue && !error) {
      onChange({ dataItem, field, value: localValue })
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Tab' || e.key === 'Enter') {
      if (localValue !== initialValue && !error) {
        onChange({ dataItem, field, value: localValue })
      }
    }
  }

  return (
    <td style={{ position: 'relative' }}>
      <InputBase
        inputRef={inputRef}
        value={localValue}
        onChange={handleChange}
        onBlur={handleBlur}
        onKeyDown={handleKeyDown}
        autoComplete='off'
        className='input-editor'
        sx={{
          width: '100%',
          fontSize: '0.8125rem',
          fontWeight: 600,
          fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif !important",
          color: '#1d3665',
          px: 1,
          height: 40,
          borderRadius: '6px',
          backgroundColor: error ? '#fff5f5' : '#FFFFFF',
          border: `1px solid ${error ? '#d32f2f' : '#E0E4EC'}`,
          boxShadow: error
            ? '0 0 0 3px rgba(211, 47, 47, 0.10)'
            : '0px 1px 2px rgba(0, 0, 0, 0.05)',
          transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
          '&:hover': {
            borderColor: error ? '#d32f2f' : '#B0B8C4',
            backgroundColor: error ? '#fff5f5' : '#F9FAFB',
          },
          '&.Mui-focused': {
            borderColor: error ? '#d32f2f' : '#00F5E1',
            boxShadow: error
              ? '0 0 0 3px rgba(211, 47, 47, 0.12)'
              : '0 0 0 3px rgba(0, 245, 225, 0.12)',
            backgroundColor: '#FFFFFF',
          },
          '& input': {
            textAlign: 'right',
            padding: '0 !important',
          },
        }}
        title={error || ''}
      />
      {error && (
        <div
          style={{
            position: 'absolute',
            top: '100%',
            left: 0,
            backgroundColor: '#ffebee',
            color: '#d32f2f',
            padding: '4px 8px',
            fontSize: '0.75rem',
            borderRadius: '4px',
            whiteSpace: 'nowrap',
            zIndex: 1000,
            marginTop: '2px',
            border: '1px solid #d32f2f',
          }}
        >
          {error}
        </div>
      )}
    </td>
  )
}
