import React, { useEffect, useRef } from 'react'
import { DropDownList } from '@progress/kendo-react-dropdowns'

const options = [
  { id: 0, value: '0' },
  { id: 1, value: '1' },
  { id: 2, value: '2' },
]

const CategoryDropdownEditor = (props) => {
  const { dataItem, field } = props
  const inputRef = useRef(null)
  const value = options.find((opt) => opt.id === dataItem[field]) || null

  const handleBlur = () => {
    const currentValue = value?.id
    const newValue = inputRef.current?.value
    if (currentValue !== newValue?.id) {
      props.onChange({
        dataItem,
        field,
        value: newValue ? newValue.id : null,
      })
    }
  }

  useEffect(() => {
    const timer = setTimeout(() => {
      if (inputRef.current?.element) {
        inputRef.current.element.focus()
      }
    }, 50)
    return () => clearTimeout(timer)
  }, [])

  return (
    <td>
      <DropDownList
        ref={inputRef}
        data={options}
        textField='value'
        dataItemKey='id'
        value={value}
        onChange={(e) => {
          props.onChange({
            dataItem,
            field,
            value: e.value ? e.value.id : null,
          })
        }}
        onBlur={handleBlur}
        className='dropdown-editor'
        style={{ width: '100%' }}
      />
    </td>
  )
}

export default CategoryDropdownEditor
