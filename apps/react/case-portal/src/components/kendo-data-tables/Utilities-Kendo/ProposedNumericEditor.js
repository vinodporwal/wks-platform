import { useState, useEffect, useRef } from 'react'
import { InputBase } from '@mui/material'

export const ProposedNumericEditor = ({
  dataItem,
  field,
  onChange,
  maxLength,
}) => {
  const initialValue = dataItem[field] ?? ''
  const [localValue, setLocalValue] = useState(initialValue)
  const isFirstRender = useRef(true)

  const handleChange = (e) => {
    const val = e.target.value
    // Allow positive numbers, negative numbers, decimals, and leading sign (+/-)
    if (val === '' || /^[-+]?\d*(\.\d*)?$/.test(val)) {
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
    <InputBase
      value={localValue}
      onChange={handleChange}
      autoComplete='off'
      maxLength={maxLength}
      className='input-editor'
    />
  )
}

export default ProposedNumericEditor
