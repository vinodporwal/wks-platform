import React from 'react'
import { Box, Button, Typography } from '@mui/material'
import { TextArea } from '@progress/kendo-react-inputs'

const PerformanceHighlights = ({
  performanceSummary,
  setPerformanceSummary,
  performanceHighlightsEdited,
  setPerformanceHighlightsEdited,
  savePerformanceHighlightsSummary,
  readOnly,
}) => {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'flex-start',
        gap: 0,
        mt: 1,
      }}
    >
      <Typography className='grid-title' sx={{ whiteSpace: 'nowrap' }}>
        Performance Highlights
      </Typography>

      <Button
        variant='contained'
        onClick={savePerformanceHighlightsSummary}
        className='btn-save'
        disabled={readOnly || !performanceHighlightsEdited}
        sx={{ alignSelf: 'flex-end' }}
      >
        Save
      </Button>
      <TextArea
        value={performanceSummary}
        rows={6}
        style={{
          width: '100%',
        }}
        onChange={(e) => {
          setPerformanceSummary(e.target.value)
          setPerformanceHighlightsEdited(true)
        }}
        placeholder='Enter summary here...'
        disabled={readOnly}
      />
    </Box>
  )
}

export default PerformanceHighlights
