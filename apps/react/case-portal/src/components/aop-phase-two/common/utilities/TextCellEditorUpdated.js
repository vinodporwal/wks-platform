import { Input } from '@progress/kendo-react-inputs'
import { useState, useEffect, useRef } from 'react'

export const TextCellEditorUpdated = ({ dataItem, field, onChange }) => {
  const initialValue = dataItem[field] ?? ''
  const [localValue, setLocalValue] = useState(initialValue)
  const isFirstRender = useRef(true)
  const inputRef = useRef(null)

  const handleChange = (e) => {
    const val = e.target.value
    setLocalValue(val) // accept any text input
  }

  const handleBlur = () => {
    if (localValue !== initialValue) {
      onChange({ dataItem, field, value: localValue })
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

  useEffect(() => {
    if (inputRef.current) {
      const el = inputRef.current.element || inputRef.current
      if (el && typeof el.focus === 'function') el.focus()
    }
  }, [])

  return (
    <td style={{ textAlign: 'start' }}>
      <Input
        ref={inputRef}
        value={localValue}
        onChange={handleChange}
        onBlur={handleBlur}
        style={{
          fontSize: '0.8125rem',
          fontWeight: 600,
          color: '#1d3665',
          padding: '0 8px',
          height: '28px',
          lineHeight: '28px',
          width: '100%',
          borderRadius: '6px',
          backgroundColor: '#FFFFFF',
          border: '1px solid #E0E4EC',
          boxShadow: '0px 1px 2px rgba(0, 0, 0, 0.05)',
          transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
          textAlign: 'left',
        }}
      />
    </td>
  )
}
