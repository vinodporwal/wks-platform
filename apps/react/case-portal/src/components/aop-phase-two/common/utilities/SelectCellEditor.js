import { TextField, MenuItem } from '@mui/material'
import { useState, useEffect, useRef } from 'react'

export const SelectCellEditor = ({
  dataItem,
  field,
  onChange,
  options = [],
  textField = 'text',
  valueField = 'value',
  placeholder = 'Select...',
}) => {
  const storedValue = dataItem[field] ?? ''
  const [localValue, setLocalValue] = useState(storedValue)
  const isFirstRender = useRef(true)
  const inputRef = useRef(null)

  // Autofocus when cell enters edit mode
  useEffect(() => {
    const timer = setTimeout(() => {
      if (inputRef.current) {
        inputRef.current.focus()
      }
    }, 50)
    return () => clearTimeout(timer)
  }, [])

  const handleChange = (e) => {
    setLocalValue(e.target.value)
  }

  // Debounced sync to grid, skip first render
  useEffect(() => {
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
  }, [localValue, dataItem, field, onChange, storedValue])

  return (
    <td>
      <TextField
        select
        inputRef={inputRef}
        value={localValue}
        onChange={handleChange}
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
          <em>{placeholder}</em>
        </MenuItem>

        {Array.isArray(options) &&
          options.map((option) => (
            <MenuItem
              key={option[valueField]}
              value={option[valueField]}
            >
              {option[textField]}
            </MenuItem>
          ))}
      </TextField>
    </td>
  )
}
