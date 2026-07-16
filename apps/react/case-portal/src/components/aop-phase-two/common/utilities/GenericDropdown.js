import React from 'react'
import { TextField, MenuItem, Typography } from '@mui/material'

/**
 * Generic Dropdown Component
 *
 * @param {Object} props - Component props
 * @param {Array} props.options - Array of options to display
 * @param {string} props.value - Currently selected value
 * @param {Function} props.onChange - Callback when selection changes
 * @param {string} props.label - Dropdown label (shown as left prefix adornment)
 * @param {string} props.placeholder - Placeholder text (defaults to 'Select')
 * @param {string} props.valueKey - Key to use as the option value (defaults to 'id')
 * @param {string} props.labelKey - Key to use as the display label (defaults to 'displayName')
 * @param {string} props.className - CSS class name
 * @param {string} props.variant - TextField variant (defaults to 'outlined')
 * @param {boolean} props.disabled - Whether dropdown is disabled
 * @param {Object} props.sx - MUI sx prop for additional styling
 * @param {boolean} props.required - Whether field is required
 * @param {string} props.size - Size of the dropdown ('small', 'medium')
 * @param {Function} props.getOptionLabel - Custom function to get label from option
 * @param {Function} props.getOptionValue - Custom function to get value from option
 *
 * @example
 * // Basic usage with simple objects
 * <GenericDropdown
 *   options={grades}
 *   value={selectedGrade}
 *   onChange={(value) => setSelectedGrade(value)}
 *   label="Grade"
 *   valueKey="gradeId"
 *   labelKey="displayName"
 * />
 *
 * @example
 * // With custom label/value extraction
 * <GenericDropdown
 *   options={items}
 *   value={selectedItem}
 *   onChange={(value) => handleChange(value)}
 *   label="Item"
 *   getOptionValue={(item) => item.customId}
 *   getOptionLabel={(item) => `${item.name} (${item.code})`}
 * />
 */

const menuItemStyle = {
  fontSize: 14,
  fontWeight: 700,
  color: '#303030',
  fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
  letterSpacing: '0px',
  verticalAlign: 'middle',
}

const GenericDropdown = ({
  options = [],
  value = '',
  onChange,
  label = 'Select',
  placeholder = 'Select',
  valueKey = 'id',
  labelKey = 'displayName',
  className,
  variant = 'outlined',
  disabled = false,
  sx = {},
  required = false,
  size = 'small',
  getOptionLabel,
  getOptionValue,
}) => {
  const getValueFromOption = (option) => {
    if (getOptionValue) return getOptionValue(option)
    return option[valueKey]
  }

  const getLabelFromOption = (option) => {
    if (getOptionLabel) return getOptionLabel(option)
    return option[labelKey]
  }

  const handleChange = (e) => {
    if (onChange) onChange(e.target.value)
  }

  return (
    <TextField
      select
      value={value || ''}
      onChange={handleChange}
      variant={variant}
      size={size}
      disabled={disabled}
      required={required}
      className={className}
      InputProps={{
        startAdornment: (
          <Typography
            variant='caption'
            sx={{
              mr: 0.5,
              color: '#606060',
              fontWeight: 500,
              fontSize: '14px',
              textTransform: 'uppercase',
              letterSpacing: '0.4px',
              lineHeight: 1,
              whiteSpace: 'nowrap',
              fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
            }}
          >
            {label}:
          </Typography>
        ),
      }}
      sx={{
        minWidth: 140,
        '& .MuiOutlinedInput-root': {
          height: '30px',
          backgroundColor: 'rgba(255, 255, 255, 0.85)',
          borderRadius: '7px',
          fontSize: '14px',
          fontWeight: 700,
          color: '#252525',
          fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
          '& fieldset': { border: 'none' },
          '&:hover fieldset': { border: 'none' },
          '&.Mui-focused fieldset': { border: 'none' },
        },
        '& .MuiSelect-select': {
          display: 'flex',
          alignItems: 'center',
          padding: '2px 6px !important',
        },
        ...sx,
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
                fontSize: '14px',
                fontWeight: 700,
                minHeight: '26px',
                margin: '1px 4px',
                fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
                borderRadius: '7px',
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
      <MenuItem value='' disabled sx={menuItemStyle}>
        <em>{placeholder}</em>
      </MenuItem>

      {Array.isArray(options) &&
        options.map((option) => (
          <MenuItem
            key={getValueFromOption(option)}
            value={getValueFromOption(option)}
            sx={menuItemStyle}
          >
            {getLabelFromOption(option)}
          </MenuItem>
        ))}
    </TextField>
  )
}

export default GenericDropdown
