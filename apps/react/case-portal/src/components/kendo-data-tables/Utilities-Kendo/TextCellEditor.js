import { Input } from '@progress/kendo-react-inputs'

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
      <Input
        value={rawValue}
        onChange={handleChange}
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
