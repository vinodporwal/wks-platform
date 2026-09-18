import React, { useState } from 'react'
import {
  Box,
  Button,
  Typography,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  IconButton,
} from '@mui/material'
import { styled } from '@mui/material/styles'
import CloseIcon from '@mui/icons-material/Close'
import { TextArea } from '@progress/kendo-react-inputs'
import { SaveIcon } from 'assets/images/icons'

const CompactDialog = styled(Dialog)(() => ({
  '& .MuiPaper-root': {
    borderRadius: '12px',
    width: '450px',
    maxWidth: '90vw',
    boxShadow: '0 12px 40px rgba(0,0,0,0.15)',
  },
}))

const PerformanceHighlights = ({
  performanceSummary,
  setPerformanceSummary,
  performanceHighlightsEdited,
  setPerformanceHighlightsEdited,
  savePerformanceHighlightsSummary,
  readOnly,
}) => {
  const [openConfirmDialog, setOpenConfirmDialog] = useState(false)

  const handleSaveClick = () => {
    if (readOnly || !performanceHighlightsEdited) return
    setOpenConfirmDialog(true)
  }

  const handleConfirmSave = () => {
    setOpenConfirmDialog(false)
    savePerformanceHighlightsSummary()
  }

  const handleCloseDialog = () => {
    setOpenConfirmDialog(false)
  }

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        gap: 1,
        mb: 2,
        mt: 1,
      }}
    >
      {/* Header bar with Title on left and Save button on right */}
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          width: '100%',
        }}
      >
        <Typography className='grid-title' sx={{ whiteSpace: 'nowrap', m: 0 }}>
          Performance Highlights
        </Typography>

        <Button
          variant='contained'
          className='btn-save'
          startIcon={
            <Box component='img' src={SaveIcon} className='w16-icon' />
          }
          onClick={handleSaveClick}
          disabled={readOnly || !performanceHighlightsEdited}
        >
          Save
        </Button>
      </Box>

      {/* Text Area */}
      <TextArea
        value={performanceSummary || ''}
        rows={4}
        style={{
          width: '100%',
          borderRadius: '4px',
          fontFamily: 'inherit',
          fontSize: '13px',
        }}
        onChange={(e) => {
          setPerformanceSummary(e.target.value)
          setPerformanceHighlightsEdited(true)
        }}
        placeholder='Enter performance highlights summary here...'
        disabled={readOnly}
      />

      {/* Confirm Save Prompt Dialog */}
      <CompactDialog
        open={openConfirmDialog}
        onClose={handleCloseDialog}
        disableScrollLock
        slotProps={{ backdrop: { disableScrollLock: true } }}
      >
        <DialogTitle
          sx={{
            p: 1.5,
            px: 2,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            bgcolor: '#f8fafc',
            borderBottom: '1px solid #e2e8f0',
          }}
        >
          <Typography
            sx={{
              fontWeight: 800,
              fontSize: '0.8rem',
              color: '#334155',
              letterSpacing: '0.5px',
            }}
          >
            CONFIRM SAVE
          </Typography>

          <IconButton
            size='small'
            onClick={handleCloseDialog}
            sx={{ color: '#64748b' }}
          >
            <CloseIcon fontSize='small' />
          </IconButton>
        </DialogTitle>

        <DialogContent sx={{ p: 2, pt: '16px !important' }}>
          <Typography
            sx={{
              fontSize: '0.85rem',
              color: '#475569',
              lineHeight: 1.5,
              fontWeight: 500,
            }}
          >
            Are you sure you want to save these changes?
          </Typography>
        </DialogContent>

        <DialogActions sx={{ p: 1.5, pt: 0, gap: 1 }}>
          <Button onClick={handleCloseDialog} className='btn-no'>
            Cancel
          </Button>

          <Button
            onClick={handleConfirmSave}
            variant='contained'
            size='small'
            autoFocus
            className='btn-yes'
          >
            Save
          </Button>
        </DialogActions>
      </CompactDialog>
    </Box>
  )
}

export default PerformanceHighlights
