import React, { useState, useEffect } from 'react'
import { DatePicker as KendoDatePicker } from '@progress/kendo-react-dateinputs'
import { NoSpinnerNumericIntegerEditor } from './Utilities-Kendo/numbericIntegerColumns'
import { NoSpinnerNumericEditor } from './Utilities-Kendo/numbericColumns'

export const parseDateRobust = (value) => {
  if (!value) return null
  const str = String(value).trim()

  // 1. Try split by dash (-) first (handles DD-MM-YYYY and YYYY-MM-DD)
  if (str.includes('-')) {
    const parts = str.split('-')
    if (parts.length === 3) {
      const p0 = parseInt(parts[0], 10)
      const p1 = parseInt(parts[1], 10)
      const p2 = parseInt(parts[2], 10)
      if (!isNaN(p0) && !isNaN(p1) && !isNaN(p2)) {
        if (p0 > 31 && p2 <= 31) {
          // YYYY-MM-DD
          return new Date(p0, p1 - 1, p2)
        } else if (p0 <= 31 && p2 > 31) {
          // DD-MM-YYYY
          return new Date(p2, p1 - 1, p0)
        }
      }
    }
  }

  // 2. Try standard Date parsing (handles ISO, "30 July 2026", etc.)
  let d = new Date(str)
  if (!isNaN(d.getTime())) {
    return d
  }

  // 3. Try raw numeric string (handles millisecond epoch)
  const num = Number(str)
  if (!isNaN(num) && str.length >= 10) {
    d = new Date(num)
    if (!isNaN(d.getTime())) {
      return d
    }
  }

  // 4. Try parsing 8-digit formats: DDMMYYYY or YYYYMMDD
  if (str.length === 8 && !isNaN(Number(str))) {
    const part1 = parseInt(str.substring(0, 4), 10)
    const part2 = parseInt(str.substring(4, 8), 10)

    if (part1 >= 1900 && part1 <= 2100) {
      // YYYYMMDD
      const year = part1
      const month = parseInt(str.substring(4, 6), 10) - 1
      const day = parseInt(str.substring(6, 8), 10)
      return new Date(year, month, day)
    } else if (part2 >= 1900 && part2 <= 2100) {
      // DDMMYYYY
      const day = parseInt(str.substring(0, 2), 10)
      const month = parseInt(str.substring(2, 4), 10) - 1
      const year = part2
      return new Date(year, month, day)
    }
  }

  return null
}

export const ConstantValueEditCell = (props) => {
  const { dataItem, field, onChange } = props
  const uom = (dataItem?.UOM || '').toLowerCase()
  const isDateOrDay =
    uom === 'date' ||
    uom === 'day' ||
    field === 'startDate' ||
    field === 'StartDate' ||
    props.column?.type === 'crackerC2DatePicker'

  const currentRaw = dataItem?.[field]
  const initialDate = isDateOrDay ? parseDateRobust(currentRaw) : null

  const [localDate, setLocalDate] = useState(initialDate)

  useEffect(() => {
    setLocalDate(initialDate)
  }, [currentRaw])

  if (isDateOrDay) {
    const handleChange = (event) => {
      setLocalDate(event.value)

      if (event.value) {
        const date = event.value
        const year = date.getFullYear()
        if (year >= 1000 && year <= 9999) {
          const month = String(date.getMonth() + 1).padStart(2, '0')
          const day = String(date.getDate()).padStart(2, '0')
          const formattedValue = `${day}-${month}-${year}`
          onChange({
            dataItem,
            field,
            value: formattedValue,
            syntheticEvent: event.syntheticEvent,
          })
        }
      } else {
        onChange({
          dataItem,
          field,
          value: null,
          syntheticEvent: event.syntheticEvent,
        })
      }
    }

    return (
      <KendoDatePicker
        value={localDate}
        format='dd-MM-yyyy'
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
  }

  const isInteger = props.column?.isInteger
  if (isInteger) {
    return <NoSpinnerNumericIntegerEditor {...props} />
  }

  return <NoSpinnerNumericEditor {...props} />
}

export const ConstantValueDataCell = (props) => {
  const {
    dataItem,
    field,
    tdProps,
    showThreeColors,
    customModifiedCells,
    allRedCell,
    allRedCell2,
    disableRedHighlight,
    RedHighlightCell,
    RedHighlightCell2,
  } = props
  const value = dataItem[field]
  const uom = (dataItem?.UOM || '').toLowerCase()
  const isDateOrDay =
    uom === 'date' ||
    uom === 'day' ||
    field === 'startDate' ||
    field === 'StartDate' ||
    props.column?.type === 'crackerC2DatePicker'

  if (isDateOrDay) {
    let displayValue = value
    if (value) {
      const d = parseDateRobust(value)
      if (d) {
        const day = String(d.getDate()).padStart(2, '0')
        const month = String(d.getMonth() + 1).padStart(2, '0')
        const year = d.getFullYear()
        displayValue = `${day}-${month}-${year}`
      }
    }
    return (
      <td
        {...tdProps}
        style={{
          ...tdProps?.style,
          textAlign: 'center',
        }}
      >
        {displayValue !== null && displayValue !== undefined ? displayValue : ''}
      </td>
    )
  }

  return showThreeColors ? (
    <RedHighlightCell2
      {...props}
      customModifiedCells={customModifiedCells}
      allRedCell={allRedCell}
      allRedCell2={allRedCell2}
      disableRedHighlight={disableRedHighlight}
    />
  ) : (
    <RedHighlightCell
      {...props}
      customModifiedCells={customModifiedCells}
      allRedCell={allRedCell}
      disableRedHighlight={disableRedHighlight}
    />
  )
}
