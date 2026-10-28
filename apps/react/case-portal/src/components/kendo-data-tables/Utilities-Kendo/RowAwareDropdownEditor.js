import { DropDownList } from '@progress/kendo-react-dropdowns'
import React from 'react'
import { NoSpinnerNumericEditor } from './numbericColumns'

const RowAwareDropdownEditor = ({
  dataItem,
  field,
  onChange,
  options = [],
  condition,
  fallback: Fallback = NoSpinnerNumericEditor,
  ...rest
}) => {
  // Condition not met → delegate to fallback editor (e.g. normal numeric input)
  if (!condition || !condition(dataItem)) {
    return (
      <Fallback
        dataItem={dataItem}
        field={field}
        onChange={onChange}
        {...rest}
      />
    )
  }

  const selectedOption =
    options.find((opt) => opt.value === dataItem[field]) ?? options[0]

  const handleChange = (e) => {
    onChange({
      dataItem,
      field,
      syntheticEvent: e.syntheticEvent,
      value: e.target.value?.value,
    })
  }

  return (
    <DropDownList
      data={options}
      textField='name'
      dataItemKey='value'
      value={selectedOption}
      onChange={handleChange}
      style={{ width: '100%' }}
    />
  )
}

export default RowAwareDropdownEditor
