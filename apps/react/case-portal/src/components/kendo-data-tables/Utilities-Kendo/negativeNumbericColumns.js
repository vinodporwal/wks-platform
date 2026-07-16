import { useState, useEffect, useRef } from 'react'
import { InputBase } from '../../../../node_modules/@mui/material/index'

export const NoSpinnerNumericEditorNegative = ({
  dataItem,
  field,
  onChange,
}) => {
  const initialValue = dataItem[field] ?? ''
  const [localValue, setLocalValue] = useState(initialValue)
  const isFirstRender = useRef(true)

  const handleChange = (e) => {
    const val = e.target.value
    // ✅ Allow leading + or - and decimals while typing
    if (val === '' || /^[-+]?\d*(\.\d*)?$/.test(val)) {
      setLocalValue(val)
    }
  }

  // Debounced sync to grid, but skip first render
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
    <td style={{ textAlign: 'end' }}>
      <InputBase
        value={localValue}
        onChange={handleChange}
        className='input-editor'
        style={{
          fontSize: '15px',
          padding: '2px 2px',
          height: '40px',
          lineHeight: '1rem',
        }}
      />
    </td>
  )
}
