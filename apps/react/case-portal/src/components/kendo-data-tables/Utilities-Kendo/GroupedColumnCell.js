import React from 'react'
import { Tooltip } from '@mui/material'

export const formatValue = (val, formatStr) => {
  const parsed = parseFloat(val)
  if (val === null || val === undefined || isNaN(parsed)) {
    return ''
  }
  if (!formatStr) {
    return parsed
  }
  const matchDecimals = formatStr.match(/\.(\d+)/)
  if (matchDecimals) {
    const decimals = matchDecimals[1].length
    return parsed.toFixed(decimals)
  }
  const matchN = formatStr.match(/[nN](\d+)/)
  if (matchN) {
    const decimals = parseInt(matchN[1], 10)
    return parsed.toFixed(decimals)
  }
  return parsed
}

export const GroupedColumnCell = (props) => {
  const { dataItem, field, tdProps, processedRows = [], columns = [] } = props
  const value = dataItem[field]

  const colDef = columns.find((c) => c.field === field)
  const formatStr = colDef?.format || props.column?.format
  const displayValue = formatValue(value, formatStr)

  console.log('GroupedColumnCell Debug:', {
    field,
    value,
    formatStr,
    displayValue,
    colDef,
    columnPropFormat: props.column?.format,
  })

  const hoverTitle =
    dataItem.originalValueStr !== undefined && dataItem.originalValueStr !== null
      ? String(dataItem.originalValueStr)
      : value !== null && value !== undefined
      ? String(value)
      : ''

  const cellContent = hoverTitle ? (
    <Tooltip title={hoverTitle} arrow placement="top" disableInteractive>
      <span style={{ cursor: 'pointer', display: 'inline-block', width: '100%' }}>{displayValue}</span>
    </Tooltip>
  ) : (
    displayValue
  )

  const gName = dataItem.groupName
  if (!gName) {
    return (
      <td
        {...tdProps}
        style={{
          ...tdProps?.style,
          textAlign: 'right',
        }}
      >
        {cellContent}
      </td>
    )
  }

  const groupRows = processedRows.filter((r) => r.groupName === gName)
  const indexInGroup = groupRows.findIndex((r) => r.id === dataItem.id)

  if (indexInGroup > 0) {
    return (
      <td
        {...tdProps}
        style={{
          ...tdProps?.style,
          display: 'none',
        }}
      />
    )
  }

  const rowSpan = groupRows.length

  return (
    <td
      {...tdProps}
      rowSpan={rowSpan}
      style={{
        ...tdProps?.style,
        verticalAlign: 'middle',
        textAlign: 'right',
        backgroundColor: '#FFFFFF',
      }}
    >
      {cellContent}
    </td>
  )
}
