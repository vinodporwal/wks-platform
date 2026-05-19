import { Switch } from '@mui/material'
import { useEffect, useRef } from 'react'
import { NoSpinnerNumericEditor } from './numbericColumns'

const SwitchEditor = (props) => {
  const switchRef = useRef(null)
  const {
    dataItem,
    field,
    onChange,
    directEditMode,
    condition,
    fallback: Fallback = NoSpinnerNumericEditor,
    customModifiedCells,
    rowId,
    setRows,
    ...rest
  } = props

  // Condition not met → delegate to fallback editor (e.g. normal numeric input)
  if (condition && !condition(dataItem)) {
    return (
      <Fallback
        dataItem={dataItem}
        field={field}
        onChange={onChange}
        {...rest}
      />
    )
  }

  const handleSwitchChange = (e) => {
    const value = e.target.checked ? 1 : 0

    // In directEditMode, mark row as inEdit before calling onChange
    if (directEditMode && setRows) {
      setRows((prev) =>
        prev.map((r) => (r.id === dataItem.id ? { ...r, inEdit: true } : r)),
      )
    }

    if (typeof onChange === 'function') {
      onChange({
        dataItem,
        field,
        value,
      })
    }
  }

  const currentValue = dataItem[field]
  const isChecked =
    currentValue === 1 || currentValue === true || !!currentValue

  // Check if this cell has been edited
  const isEdited = !!(
    customModifiedCells?.[rowId] && field in customModifiedCells[rowId]
  )

  // Edit mode (when onChange is provided)
  if (typeof onChange === 'function') {
    return (
      <td
        style={{
          textAlign: 'center',
          padding: 0,
          overflow: 'visible',
        }}
      >
        <Switch
          checked={isChecked}
          // color={isEdited ? 'warning' : 'primary'}
          size='small'
          onChange={handleSwitchChange}
          inputProps={{ 'aria-label': 'Switch toggle' }}
        />
      </td>
    )
  }

  // Display mode with direct edit (clickable switch)
  if (directEditMode) {
    return (
      <td
        style={{
          textAlign: 'center',
          padding: '0.5rem',
          cursor: 'pointer',
        }}
      >
        <Switch
          checked={isChecked}
          size='small'
          onChange={handleSwitchChange}
          inputProps={{ 'aria-label': 'Switch toggle' }}
        />
      </td>
    )
  }

  // Display mode (read-only)
  return (
    <td
      style={{
        textAlign: 'center',
        padding: '0.5rem',
      }}
    >
      <Switch
        checked={isChecked}
        size='small'
        disabled
        inputProps={{ 'aria-label': 'Switch toggle' }}
      />
    </td>
  )
}

export default SwitchEditor
