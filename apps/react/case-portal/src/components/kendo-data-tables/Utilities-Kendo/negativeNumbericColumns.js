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
  const latestValueRef = useRef(localValue)
  const initialValueRef = useRef(initialValue)
  latestValueRef.current = localValue

  const handleChange = (e) => {
    const val = e.target.value
    // ✅ Allow leading + or - and decimals while typing
    if (val === '' || /^[-+]?\d*(\.\d*)?$/.test(val)) {
      setLocalValue(val)
    }
  }

  const handleBlur = () => {
    if (latestValueRef.current !== initialValueRef.current) {
      onChange({ dataItem, field, value: latestValueRef.current })
      initialValueRef.current = latestValueRef.current
    }
  }

  // Debounced sync to grid, but skip first render
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false
      return
    }

    const handler = setTimeout(() => {
      if (localValue !== initialValueRef.current) {
        onChange({ dataItem, field, value: localValue })
        initialValueRef.current = localValue
      }
    }, 300)

    return () => clearTimeout(handler)
  }, [localValue, dataItem, field, onChange])

  // Flush on unmount if user navigated away quickly via Tab before debounce timer fired
  useEffect(() => {
    return () => {
      if (latestValueRef.current !== initialValueRef.current) {
        onChange({ dataItem, field, value: latestValueRef.current })
      }
    }
  }, [dataItem, field, onChange])

  return (
    <td style={{ textAlign: 'end' }}>
      <InputBase
        autoFocus
        value={localValue}
        onChange={handleChange}
        onBlur={handleBlur}
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
