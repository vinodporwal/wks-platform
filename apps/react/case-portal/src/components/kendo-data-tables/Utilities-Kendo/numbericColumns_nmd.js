import { InputBase } from '../../../../node_modules/@mui/material/index'
import { useState, useEffect, useRef } from 'react'

export const PostCrDaysEditorNMD = ({ dataItem, field, onChange }) => {
  // Check if isCr was true in the original data (before any edits)
  // We'll need to pass this as a prop or check modifiedCells
  // const wasOriginallyIsCr = dataItem.originalIsCr !== undefined
  //   ? dataItem.originalIsCr
  //   : dataItem.isCr;

  // const isEditable = wasOriginallyIsCr === true;
  const isEditable = dataItem.isCr === true
  const initialValue = dataItem[field] ?? ''
  const [localValue, setLocalValue] = useState(initialValue)
  const isFirstRender = useRef(true)
  const latestValueRef = useRef(localValue)
  const initialValueRef = useRef(initialValue)
  latestValueRef.current = localValue

  const handleChange = (e) => {
    const val = e.target.value
    if (val === '' || /^\d*(\.\d*)?$/.test(val)) {
      setLocalValue(val)
    }
  }

  const handleBlur = () => {
    if (isEditable && latestValueRef.current !== initialValueRef.current) {
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

    // Only sync if editable
    if (!isEditable) return

    const handler = setTimeout(() => {
      if (localValue !== initialValueRef.current) {
        onChange({ dataItem, field, value: localValue })
        initialValueRef.current = localValue
      }
    }, 300)

    return () => clearTimeout(handler)
  }, [localValue, dataItem, field, onChange, isEditable])

  // Flush on unmount if user navigated away quickly via Tab before debounce timer fired
  useEffect(() => {
    return () => {
      if (isEditable && latestValueRef.current !== initialValueRef.current) {
        onChange({ dataItem, field, value: latestValueRef.current })
      }
    }
  }, [dataItem, field, onChange, isEditable])

  // Render decision AFTER all hooks
  if (!isEditable) {
    return <td style={{ textAlign: 'end' }}>{dataItem[field] ?? ''}</td>
  }

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
