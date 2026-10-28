import { InputBase } from '../../../../../node_modules/@mui/material/index'
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

  const valueRef = useRef(localValue)
  valueRef.current = localValue
  const initialValueRef = useRef(initialValue)
  initialValueRef.current = initialValue
  const onChangeRef = useRef(onChange)
  onChangeRef.current = onChange
  const dataItemRef = useRef(dataItem)
  dataItemRef.current = dataItem
  const fieldRef = useRef(field)
  fieldRef.current = field

  useEffect(() => {
    return () => {
      if (valueRef.current !== initialValueRef.current) {
        onChangeRef.current({ dataItem: dataItemRef.current, field: fieldRef.current, value: valueRef.current })
      }
    }
  }, [])

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
    const timer = setTimeout(() => {
      if (inputRef.current) {
        const el = inputRef.current.element || inputRef.current
        if (el && typeof el.focus === 'function') {
          el.focus()
        }
      }
    }, 0)
    return () => clearTimeout(timer)
  }, [])

  return (
    <td style={{ textAlign: 'start' }}>
      <InputBase
        ref={inputRef}
        value={localValue}
        onChange={handleChange}
        onBlur={handleBlur}
        autoFocus
        className='input-editor'
      />
    </td>
  )
}
