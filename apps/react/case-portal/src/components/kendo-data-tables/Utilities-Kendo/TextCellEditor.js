import { InputBase } from '@mui/material'
import { useEffect, useRef } from 'react'

export const TextCellEditor = (props) => {
  const rawValue = props.dataItem[props.field] ?? ''
  const inputRef = useRef(null)
  const isFirstRender = useRef(true)

  const handleChange = (e) => {
    const newVal = e.target.value
    props.onChange({
      dataItem: props.dataItem,
      field: props.field,
      value: newVal,
    })
  }

  const handleBlur = () => {
    if (rawValue !== inputRef.current?.value) {
      props.onChange({
        dataItem: props.dataItem,
        field: props.field,
        value: inputRef.current?.value ?? '',
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
      <InputBase
        inputRef={inputRef}
        value={rawValue}
        onChange={handleChange}
        onBlur={handleBlur}
        className='input-editor'
      />
    </td>
  )
}
