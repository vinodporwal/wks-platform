import React from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  IconButton,
} from '@mui/material'
import { styled } from '@mui/material/styles'
import CloseIcon from '@mui/icons-material/Close'

// --- STYLED COMPONENTS (mirrors kendo-data-tables/index.js) ---

const CompactDialog = styled(Dialog)(() => ({
  '& .MuiPaper-root': {
    borderRadius: '12px',
    width: '600px',
    boxShadow: '0 12px 40px rgba(0,0,0,0.15)',
  },
}))

// --- COMPONENT ---

const SaveConfirmationDialog = ({
  openSaveDialogeBox,
  closeSaveDialogeBox,
  saveConfirmation,
  showNoteWhileSaving = false,
  noteOnSaveDialogeBox = null,
}) => {
  return (
    <CompactDialog
      open={!!openSaveDialogeBox}
      onClose={closeSaveDialogeBox}
      disableScrollLock
      slotProps={{ backdrop: { disableScrollLock: true } }}
    >
      {/* Header */}
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
          onClick={closeSaveDialogeBox}
          sx={{ color: '#64748b' }}
        >
          <CloseIcon fontSize='small' />
        </IconButton>
      </DialogTitle>

      {/* Content */}
      <DialogContent sx={{ p: 1.5, pt: '12px !important' }}>
        <Typography
          sx={{
            fontSize: '0.75rem',
            color: '#475569',
            lineHeight: 1.5,
            fontWeight: 500,
          }}
        >
          {showNoteWhileSaving && noteOnSaveDialogeBox ? (
            <>
              Are you sure you want to save these changes?
              <Box
                sx={{
                  mt: 1,
                  p: 1,
                  borderRadius: '6px',
                  bgcolor: '#f1f5f9',
                  fontSize: '0.7rem',
                  color: '#334155',
                  fontWeight: 600,
                }}
              >
                {noteOnSaveDialogeBox}
              </Box>
            </>
          ) : (
            'Are you sure you want to save these changes?'
          )}
        </Typography>
      </DialogContent>

      {/* Actions */}
      <DialogActions sx={{ p: 1.5, pt: 0, gap: 1 }}>
        <Button onClick={closeSaveDialogeBox} className='btn-no'>
          Cancel
        </Button>

        <Button
          onClick={saveConfirmation}
          variant='contained'
          size='small'
          autoFocus
          className='btn-yes'
        >
          Save
        </Button>
      </DialogActions>
    </CompactDialog>
  )
}

export default SaveConfirmationDialog
