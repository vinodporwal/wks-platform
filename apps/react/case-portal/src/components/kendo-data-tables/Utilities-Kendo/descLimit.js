import { InputBase } from '../../../../node_modules/@mui/material/index'

export const descLimit = (props) => {
  const rawValue = props.dataItem[props.field] ?? ''
  const type = props?.dataItem?.type ?? ''
  // const isDisabled = type === 'ramp-down' || type === 'ramp-up'
  const isDisabled = false

  const handleChange = (e) => {
    const newVal = e.target.value

    const isValid = /^[a-zA-Z0-9 ]*$/.test(newVal) && newVal.length <= 50

    if (isValid) {
      props.onChange({
        dataItem: props.dataItem,
        field: props.field,
        value: newVal,
      })
    }
  }

  return (
    <td style={{ textAlign: 'end' }}>
      <InputBase
        value={rawValue}
        onChange={handleChange}
        maxLength={250}
        disabled={isDisabled}
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
