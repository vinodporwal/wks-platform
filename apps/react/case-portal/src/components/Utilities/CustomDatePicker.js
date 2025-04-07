import React from 'react'
import DatePicker from 'react-datepicker'
import 'react-datepicker/dist/react-datepicker.css'

export default function CustomDatePicker({ id, value, field, api, colDef }) {
  const handleChange = (date) => {
    api.setEditCellValue({ id, field, value: date }, event)
    api.commitCellChange({ id, field }) // Commit changes
    api.setCellMode(id, field, 'view') // Exit edit mode
  }

  return (
    <DatePicker
      selected={value ? new Date(value) : null}
      onChange={handleChange}
      showTimeSelect
      dateFormat='dd/MM/yyyy h:mm:ss aa'
      className='MuiInputBase-input MuiInput-input'
      wrapperClassName='date-picker-wrapper'
      autoFocus
    />
  )
}
