import React, { useEffect, useRef } from 'react'

const FullValueEditor = ({ dataItem, field, onChange }) => {
  const inputRef = useRef(null)
  const initialValue = dataItem[field] ?? ''

  const handleChange = (e) => {
    const value = e.target.value
    // Keep value as string, but convert to number if you want numeric only
    onChange({
      dataItem,
      field,
      value: value === '' ? null : Number(value),
    })
  }

  const handleBlur = () => {
    const currentValue = initialValue
    const newValue = inputRef.current?.value
    if (currentValue !== newValue) {
      onChange({
        dataItem,
        field,
        value: newValue === '' ? null : Number(newValue),
      })
    }
  }

  // Auto-focus when cell enters edit mode
  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.focus()
    }
  }, [])

  return (
    <td>
      <input
        ref={inputRef}
        type='number'
        step='any'
        value={dataItem[field] ?? ''}
        onChange={handleChange}
        onBlur={handleBlur}
        className='input-editor'
        style={{ width: '100%' }}
      />
    </td>
  )
}

export default FullValueEditor
