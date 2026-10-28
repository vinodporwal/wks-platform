import React, { useState, useEffect, useRef, useCallback } from 'react'
import { InputBase } from '@mui/material'

export const NoSpinnerIntegerEditorForDays = ({
  dataItem,
  field,
  onChange,
  configType,
  AOP_YEAR,
}) => {
  const initialValue = dataItem?.[field] ?? ''
  const [localValue, setLocalValue] = useState(
    initialValue === null ? '' : String(initialValue),
  )
  const isFirstRender = useRef(true)

  const uom = String(dataItem?.UOM || dataItem?.uom || '')
    .toLowerCase()
    .trim()
  const isDays = uom === 'day' || uom === 'days'

  const normalizedConfigType = String(
    configType ||
      dataItem?.ConfigTypeName ||
      dataItem?.ConfigTypeDisplayName ||
      dataItem?.TypeName ||
      dataItem?.TypeDisplayName ||
      '',
  )
    .toLowerCase()
    .trim()

  const isConfiguration = normalizedConfigType === 'configuration'

  const isLeapYear = (year) => {
    const y = parseInt(year, 10)
    return (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0
  }

  const getMaxDays = (month, year) => {
    const leapYear = isLeapYear(year)
    const m = String(month || '')
      .toLowerCase()
      .slice(0, 3)
    const daysMap = {
      apr: 30,
      jun: 30,
      sep: 30,
      nov: 30,
      jan: 31,
      mar: 31,
      may: 31,
      jul: 31,
      aug: 31,
      oct: 31,
      dec: 31,
      feb: leapYear ? 29 : 28,
    }
    return daysMap[m] ?? 31
  }

  const handleChange = (e) => {
    let val = e.target.value ?? ''

    if (val === '') {
      setLocalValue('')
      return
    }

    if (isDays) {
      // Block decimals for ALL days rows regardless of configType
      if (/[^0-9]/.test(val)) return
      val = val.replace(/\D+/g, '')
      if (val === '') return

      const numVal = parseInt(val, 10)
      if (Number.isNaN(numVal) || numVal < 0) return

      // Max days check — only for configuration
      if (isConfiguration) {
        const maxDays = getMaxDays(field, AOP_YEAR)
        if (numVal > maxDays) return
      }

      setLocalValue(String(numVal))
    } else {
      // Non-days: allow numeric with optional decimal
      if (/^\d*(\.\d*)?$/.test(val)) {
        setLocalValue(val)
      }
    }
  }

  const handleKeyDown = (e) => {
    if (!isDays) return
    if (
      e.key === '.' ||
      e.key === ',' ||
      e.key === '-' ||
      e.key.toLowerCase() === 'e'
    ) {
      e.preventDefault()
    }
  }

  const handlePaste = (e) => {
    if (!isDays) return // ← already blocks paste for non-days
    const text = (e.clipboardData || window.clipboardData).getData('text') || ''
    const clean = text.replace(/\D+/g, '') // ← strips decimals from paste too
    if (!clean) {
      e.preventDefault()
      return
    }
    e.preventDefault()
    const num = parseInt(clean, 10)
    if (Number.isNaN(num) || num < 0) return

    if (isConfiguration) {
      const maxDays = getMaxDays(field, AOP_YEAR)
      if (num > maxDays) return
    }

    setLocalValue(String(num))
  }

  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false
      return
    }
    const handler = setTimeout(() => {
      if (localValue !== String(initialValue)) {
        onChange({
          dataItem,
          field,
          value: localValue === '' ? '' : localValue,
        })
      }
    }, 300)
    return () => clearTimeout(handler)
  }, [localValue, dataItem, field, onChange, initialValue])

  const maxDays =
    isDays && isConfiguration ? getMaxDays(field, AOP_YEAR) : undefined

  return (
    <td style={{ textAlign: 'end' }}>
      <InputBase
        value={localValue}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        onPaste={handlePaste}
        className='input-editor'
        inputProps={{
          inputMode: isDays ? 'numeric' : 'decimal',
          pattern: isDays ? '\\d*' : '\\d*(\\.\\d*)?',
          max: isDays && isConfiguration ? maxDays : undefined,
          title: isDays && isConfiguration ? `Max ${maxDays} days` : undefined,
        }}
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

// ─── Hook to create stable editor — use this inside your grid component ───────
export const useIntegerDaysEditor = (configType, AOP_YEAR) => {
  return useCallback(
    (cellProps) => (
      <NoSpinnerIntegerEditorForDays
        {...cellProps}
        configType={configType}
        AOP_YEAR={AOP_YEAR}
      />
    ),
    [configType, AOP_YEAR],
  )
}
