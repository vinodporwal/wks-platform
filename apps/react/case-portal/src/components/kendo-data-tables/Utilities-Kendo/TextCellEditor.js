import { InputBase } from '@mui/material'

export const TextCellEditor = (props) => {
  const rawValue = props.dataItem[props.field] ?? ''

  const handleChange = (e) => {
    const newVal = e.target.value
    props.onChange({
      dataItem: props.dataItem,
      field: props.field,
      value: newVal,
    })
  }

  return (
    <td>
      <InputBase
        value={rawValue}
        onChange={handleChange}
        className='input-editor'
      />
    </td>
  )
}
