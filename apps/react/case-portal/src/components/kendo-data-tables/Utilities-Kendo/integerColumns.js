import React, { useState, useEffect } from 'react'
import { InputBase } from '@mui/material'

export const NoSpinnerIntegerEditor = ({
  dataItem,
  field,
  onChange,
  maxLength,
}) => {
  const initialValue = dataItem[field] ?? ''
  const [localValue, setLocalValue] = useState(initialValue)

  useEffect(() => {
    setLocalValue(dataItem[field] ?? '')
  }, [dataItem, field])

  const handleChange = (e) => {
    const val = e.target.value
    if (val === '' || /^\d+$/.test(val)) {
      setLocalValue(val)
      onChange({
        dataItem,
        field,
        value: val === '' ? null : parseInt(val, 10),
      })
    }
  }

  return (
    <InputBase
      value={localValue}
      onChange={handleChange}
      autoComplete='off'
      maxLength={maxLength}
      className='input-editor'
    />
  )
}
