import React, { useEffect, useState } from 'react'
import { DatePicker } from '@progress/kendo-react-dateinputs'
import { Box, Button, Typography } from '@mui/material'

const StartAndEndPicker = ({
  onLoad,
  disabled = false,
  dateFormat = 'YYYY-MM-DD',
}) => {
  const getDefaultStartDate = () => {
    const date = new Date()
    date.setFullYear(date.getFullYear() - 2)
    return date
  }

  const getDefaultEndDate = () => {
    return new Date()
  }

  const [startDate, setStartDate] = useState(getDefaultStartDate())
  const [endDate, setEndDate] = useState(getDefaultEndDate())

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
    if (onLoad) {
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
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 1,
        marginTop: '5px',
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1 }}>
        {/* Start Date */}
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
          <Typography className='button-title' sx={{ whiteSpace: 'nowrap' }}>
            Start Date
          </Typography>
          <DatePicker
            id='start-date'
            format='dd-MM-yyyy'
            value={startDate}
            onChange={(e) => setStartDate(e.value)}
            style={{ height: '80px' }}
            size={'medium'}
            disabled={disabled}
          />
        </Box>

        {/* End Date */}
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
          <Typography className='button-title' sx={{ whiteSpace: 'nowrap' }}>
            End Date
          </Typography>
          <DatePicker
            id='end-date'
            format='dd-MM-yyyy'
            value={endDate}
            onChange={(e) => setEndDate(e.value)}
            style={{ height: '80px' }}
            size={'medium'}
            disabled={disabled}
          />
        </Box>

        {/* Load Button */}
        <Button
          variant='contained'
          onClick={handleLoad}
          className='btn-save'
          disabled={disabled}
          sx={{ alignSelf: 'flex-end' }}
        >
          Load
        </Button>
      </Box>
    </Box>
  )
}

export default StartAndEndPicker
