import { TextField, MenuItem, InputBase } from '@mui/material'
import { useState, useEffect, useRef } from 'react'

export const ConditionalCellEditor = ({
  dataItem,
  field,
  onChange,
  format = '{0:0.00}',
}) => {
  const storedValue = dataItem[field] ?? ''
  const inputType = dataItem.inputType
  const options = dataItem.options || []

  const [localValue, setLocalValue] = useState(
    inputType === 'dropdown' ? storedValue ?? '' : storedValue,
  )
  const isFirstRender = useRef(true)
  const focusRef = useRef(null)

  // Autofocus when cell enters edit mode
  useEffect(() => {
    const timer = setTimeout(() => {
      if (focusRef.current) {
        focusRef.current.focus()
      }
    }, 50)
    return () => clearTimeout(timer)
  }, [])

  // ---- DROPDOWN HANDLERS ----
  const handleDropdownChange = (e) => {
    setLocalValue(e.target.value)
  }

  // Debounced sync for dropdown
  useEffect(() => {
    if (inputType !== 'dropdown') return

    if (isFirstRender.current) {
      isFirstRender.current = false
      return
    }

    const handler = setTimeout(() => {
      if (localValue !== storedValue) {
        onChange({ dataItem, field, value: localValue })
      }
    }, 300)

    return () => clearTimeout(handler)
  }, [localValue, dataItem, field, onChange, storedValue, inputType])

  // ---- NUMERIC HANDLERS ----
  const handleNumericChange = (e) => {
    const val = e.target.value
    if (val === '' || /^-?\d*(\.\d*)?$/.test(val)) {
      setLocalValue(val)
    }
  }

  const handleNumericBlur = () => {
    if (localValue !== storedValue) {
      onChange({ dataItem, field, value: localValue })
    }
  }

  // ---- DROPDOWN BRANCH ----
  if (inputType === 'dropdown' && options.length > 0) {
    return (
      <td>
        <TextField
          select
          inputRef={focusRef}
          value={localValue}
          onChange={handleDropdownChange}
          size='small'
          variant='outlined'
          fullWidth
          sx={{
            '& .MuiOutlinedInput-root': {
              height: '30px',
              backgroundColor: 'rgba(255, 255, 255, 0.85)',
              borderRadius: '7px',
              fontSize: '0.7rem',
              fontWeight: 600,
              color: '#1d3665',
              '& fieldset': {
                borderColor: 'rgba(0, 0, 0, 0.08)',
              },
              '&:hover fieldset': {
                borderColor: '#0100cb',
              },
              '&.Mui-focused fieldset': {
                borderColor: '#0100cb',
                borderWidth: '1.2px',
              },
            },
            '& .MuiSelect-select': {
              display: 'flex',
              alignItems: 'center',
              padding: '2px 6px !important',
            },
          }}
          SelectProps={{
            MenuProps: {
              disableScrollLock: true,
              PaperProps: {
                sx: {
                  borderRadius: '8px',
                  mt: 0.5,
                  boxShadow: '0 4px 16px rgba(0,0,0,0.08)',
                  '& .MuiMenuItem-root': {
                    fontSize: '0.7rem',
                    fontWeight: 500,
                    minHeight: '26px',
                    margin: '1px 4px',
                    borderRadius: '5px',
                    '&.Mui-selected': {
                      bgcolor: 'rgba(1, 0, 203, 0.08)',
                      color: '#0100cb',
                      fontWeight: 700,
                      '&:hover': {
                        bgcolor: 'rgba(1, 0, 203, 0.12)',
                      },
                    },
                  },
                },
              },
            },
          }}
        >
          <MenuItem value='' disabled sx={{ fontSize: '0.65rem' }}>
            <em>Select...</em>
          </MenuItem>

          {options.map((opt) => (
            <MenuItem key={opt} value={opt}>
              {opt}
            </MenuItem>
          ))}
        </TextField>
      </td>
    )
  }

  // ---- NUMERIC BRANCH ----
  return (
    <td>
      <InputBase
        inputRef={focusRef}
        value={localValue}
        onChange={handleNumericChange}
        onBlur={handleNumericBlur}
        autoComplete='off'
        className='input-editor'
      />
    </td>
  )
}
