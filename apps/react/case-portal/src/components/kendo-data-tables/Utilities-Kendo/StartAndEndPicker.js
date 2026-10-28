import React, { useEffect, useState } from 'react'
import { DatePicker } from '@progress/kendo-react-dateinputs'
import { Box, Button, Typography } from '@mui/material'

const StartAndEndPicker = ({
  onLoad,
  disabled = false,
  dateFormat = 'YYYY-MM-DD',
  startDate: startDateProp = null,
  endDate: endDateProp = null,
}) => {
  const parseDate = (val) => {
    if (!val) return null
    if (val instanceof Date) return val
    const d = new Date(val)
    return isNaN(d.getTime()) ? null : d
  }

  const [startDate, setStartDate] = useState(() => parseDate(startDateProp))
  const [endDate, setEndDate] = useState(() => parseDate(endDateProp))

  // Sync internal state when parent prop changes
  useEffect(() => {
    setStartDate(parseDate(startDateProp))
  }, [startDateProp])

  useEffect(() => {
    setEndDate(parseDate(endDateProp))
  }, [endDateProp])

  const formatDate = (date, format) => {
    if (!date) return ''
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    switch (format) {
      case 'YYYY-MM-DD':
        return `${year}-${month}-${day}`
      case 'YYYY/MM/DD':
        return `${year}/${month}/${day}`
      case 'DD-MM-YYYY':
        return `${day}-${month}-${year}`
      case 'DD/MM/YYYY':
        return `${day}/${month}/${year}`
      default:
        return `${year}-${month}-${day}`
    }
  }

  const handleLoad = () => {
    if (onLoad && startDate && endDate) {
      onLoad({
        startDate: formatDate(startDate, dateFormat),
        endDate: formatDate(endDate, dateFormat),
        startDateObj: startDate,
        endDateObj: endDate,
      })
    }
  }

  return (
    <Box
      sx={{ display: 'flex', alignItems: 'center', gap: 1, marginTop: '5px' }}
    >
      <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1 }}>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
          <Typography className='button-title' sx={{ whiteSpace: 'nowrap' }}>
            Start Date
          </Typography>
          <DatePicker
            id='start-date'
            format='dd-MM-yyyy'
            value={startDate} // null = shows placeholder, not old date
            onChange={(e) => setStartDate(e.value)}
            size='medium'
            disabled={disabled}
          />
        </Box>

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
          <Typography className='button-title' sx={{ whiteSpace: 'nowrap' }}>
            End Date
          </Typography>
          <DatePicker
            id='end-date'
            format='dd-MM-yyyy'
            value={endDate} // null = shows placeholder, not old date
            onChange={(e) => setEndDate(e.value)}
            size='medium'
            disabled={disabled}
          />
        </Box>

        <Button
          variant='contained'
          onClick={handleLoad}
          className='btn-save'
          disabled={disabled || !startDate || !endDate} // Disable if no dates
          sx={{ alignSelf: 'flex-end' }}
        >
          Load
        </Button>
      </Box>
    </Box>
  )
}

export default StartAndEndPicker
