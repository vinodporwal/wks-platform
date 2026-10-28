import React, { useState, useEffect } from 'react'
import { DatePicker as KendoDatePicker } from '@progress/kendo-react-dateinputs'
import { NoSpinnerNumericEditor } from './numbericColumns'
import moment from 'moment'

export const isRecordedDateRow = (dataItem) => {
  if (!dataItem) return false
  if (dataItem.isDateRow) return true
  const activity = String(dataItem.activity || '')
    .toLowerCase()
    .trim()
  return activity.includes('recorded date')
}

export const parseToDate = (val) => {
  if (!val) return null
  if (val instanceof Date) {
    return isNaN(val.getTime()) ? null : val
  }
  if (typeof val === 'string') {
    const trimmed = val.trim()
    if (!trimmed) return null
    const m = moment(
      trimmed,
      [
        'DD MMM YYYY',
        'DD MMM YY',
        'DD/MM/YY',
        'DD/MM/YYYY',
        'DD-MMM-YY',
        'DD-MMM-YYYY',
        'DD-MM-YYYY',
        'YYYY-MM-DD',
      ],
      true,
    )
    if (m.isValid()) return m.toDate()
    const d = new Date(trimmed)
    if (
      !isNaN(d.getTime()) &&
      d.getFullYear() >= 1970 &&
      d.getFullYear() <= 2100
    ) {
      return d
    }
  }
  return null
}

export const MaxHourlyRateEditCell = (props) => {
  const { dataItem, field, onChange } = props
  const isDate = isRecordedDateRow(dataItem)

  if (isDate) {
    const rawValue = dataItem?.[field]
    const initialDate = parseToDate(rawValue)
    const [localDate, setLocalDate] = useState(initialDate)

    useEffect(() => {
      setLocalDate(parseToDate(rawValue))
    }, [rawValue])

    const handleChange = (event) => {
      const newDate = event.value
      setLocalDate(newDate)
      onChange({
        dataItem,
        field,
        value: newDate,
        syntheticEvent: event.syntheticEvent,
      })
    }

    const picker = (
      <KendoDatePicker
        value={localDate}
        format='dd MMM yyyy'
        onChange={handleChange}
        width='100%'
        size='small'
        style={{
          width: '100%',
          fontSize: '15px',
          height: '40px',
        }}
        className='input-editor'
      />
    )

    if (props.tdProps) {
      return (
        <td
          {...props.tdProps}
          style={{
            ...props.tdProps?.style,
            padding: 0,
            textAlign: 'center',
          }}
        >
          {picker}
        </td>
      )
    }

    return picker
  }

  return <NoSpinnerNumericEditor {...props} />
}

export const MaxHourlyRateDataCell = (props) => {
  const { dataItem, field, tdProps, customModifiedCells } = props
  const isDate = isRecordedDateRow(dataItem)
  const value = dataItem?.[field]
  const rowId = dataItem?.id
  const isEdited = !!(
    customModifiedCells?.[rowId] && field in customModifiedCells[rowId]
  )

  if (isDate) {
    let display = ''
    if (value) {
      if (value instanceof Date && !isNaN(value.getTime())) {
        display = moment(value).format('DD MMM YYYY')
      } else {
        const d = parseToDate(value)
        if (d) {
          display = moment(d).format('DD MMM YYYY')
        } else {
          display = String(value)
        }
      }
    }

    return (
      <td
        {...tdProps}
        title={display}
        className={`${tdProps?.className || ''} ${isEdited ? 'edited-cell' : ''}`.trim()}
        style={{
          ...tdProps?.style,
          textAlign: 'center',
        }}
      >
        {display}
      </td>
    )
  }

  let formatted = value
  if (value !== '' && value !== null && value !== undefined) {
    const num = Number(value)
    if (!isNaN(num)) {
      formatted = num.toFixed(2)
    }
  }

  return (
    <td
      {...tdProps}
      title={
        formatted !== null && formatted !== undefined ? String(formatted) : ''
      }
      className={`${tdProps?.className || ''} ${isEdited ? 'edited-cell' : ''}`.trim()}
      style={{
        ...tdProps?.style,
        textAlign: 'right',
      }}
    >
      {formatted !== null && formatted !== undefined ? formatted : ''}
    </td>
  )
}
