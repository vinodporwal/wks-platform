import React from 'react'
import { DropDownList, MultiSelect } from '@progress/kendo-react-dropdowns'

const DynamicDropdown = (props) => {
  const {
    dataItem,
    field,
    onChange,
    options = [],
    multiSelect = false,
    getDropdownOptions,
  } = props

  // Determine which options to use
  let dropdownOptions = options
  if (getDropdownOptions) {
    dropdownOptions = getDropdownOptions(dataItem)
  }

  const handleChange = (e) => {
    if (multiSelect) {
      onChange({
        dataItem: dataItem,
        field: field,
        syntheticEvent: e.syntheticEvent,
        value: e.target.value,
      })
    } else {
      onChange({
        dataItem: dataItem,
        field: field,
        syntheticEvent: e.syntheticEvent,
        value: e.target.value?.value || e.target.value,
      })
    }
  }

  if (multiSelect) {
    const selectedValues = Array.isArray(dataItem[field])
      ? dataItem[field]
      : dataItem[field]
        ? [dataItem[field]]
        : []

    const selectedOptions = dropdownOptions.filter((opt) =>
      selectedValues.includes(opt.value),
    )

    return (
      <MultiSelect
        data={dropdownOptions}
        textField='name'
        dataItemKey='value'
        value={selectedOptions}
        onChange={handleChange}
        style={{ width: '100%' }}
        className='dropdown-editor'
      />
    )
  }

  const selectedOption = dropdownOptions.find(
    (opt) => opt.value === dataItem[field],
  )

  return (
    <DropDownList
      data={dropdownOptions}
      textField='name'
      dataItemKey='value'
      value={selectedOption}
      onChange={handleChange}
      className='dropdown-editor'
      style={{ width: '100%' }}
    />
  )
}

export default DynamicDropdown
