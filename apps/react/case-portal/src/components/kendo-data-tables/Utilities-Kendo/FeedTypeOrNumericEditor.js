import React from 'react'
import { DropDownList } from '@progress/kendo-react-dropdowns'
import { NoSpinnerNumericEditor } from './numbericColumns'

const FeedTypeOrNumericEditor = (props) => {
  const { dataItem, field, onChange, ...rest } = props
  const isDropdown = dataItem?.UOM === '#'

  if (!isDropdown) {
    return (
      <NoSpinnerNumericEditor
        dataItem={dataItem}
        field={field}
        onChange={onChange}
        {...rest}
      />
    )
  }

  const dropdownData =
    props.options ||
    props.column?.dropdownOptions ||
    props.column?.permissions?.feedTypeOptions ||
    []

  const selectedOption =
    dropdownData.find(
      (opt) =>
        opt.value === dataItem[field] ||
        opt.name === dataItem[field] ||
        String(opt.value) === String(dataItem[field]) ||
        String(opt.name) === String(dataItem[field])
    ) ?? dropdownData[0]

  const handleChange = (e) => {
    onChange({
      dataItem,
      field,
      syntheticEvent: e.syntheticEvent,
      value: e.target.value?.name || e.target.value,
    })
  }

  return (
    <DropDownList
      data={dropdownData}
      textField='name'
      dataItemKey='name'
      value={selectedOption}
      onChange={handleChange}
      className='dropdown-editor'
      style={{ width: '100%', height: '100%' }}
    />
  )
}

export default FeedTypeOrNumericEditor
