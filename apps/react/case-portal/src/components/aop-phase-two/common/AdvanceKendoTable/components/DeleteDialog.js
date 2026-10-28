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
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'

// --- STYLED COMPONENTS (mirrors kendo-data-tables/index.js) ---

const CompactDialog = styled(Dialog)(() => ({
  '& .MuiPaper-root': {
    borderRadius: '12px',
    width: '600px',
    boxShadow: '0 12px 40px rgba(0,0,0,0.15)',
  },
}))

// --- COMPONENT ---

const DeleteDialog = ({
  message = null,
  openDeleteDialogeBox,
  setOpenDeleteDialogeBox,
  deleteTheRecord,
  confirmButtonText,
  readOnly = false,
  showNoteWhileDeleting = false,
  deleteNote = null,
}) => {
  return (
    <CompactDialog
      open={!!openDeleteDialogeBox}
      onClose={() => setOpenDeleteDialogeBox(false)}
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
          bgcolor: '#fef2f2',
          borderBottom: '1px solid #fee2e2',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <DeleteOutlineIcon sx={{ fontSize: '1rem', color: '#dc2626' }} />
          <Typography
            sx={{
              fontWeight: 800,
              fontSize: '0.8rem',
              color: '#7f1d1d',
              letterSpacing: '0.4px',
            }}
          >
            CONFIRM DELETE
          </Typography>
        </Box>

        <IconButton
          size='small'
          onClick={() => setOpenDeleteDialogeBox(false)}
          sx={{ color: '#7f1d1d' }}
        >
          <CloseIcon fontSize='small' />
        </IconButton>
      </DialogTitle>

      {/* Content */}
      <DialogContent sx={{ p: 1.5, pt: '12px !important' }}>
        <Typography
          sx={{
            fontSize: '0.75rem',
            color: '#7f1d1d',
            lineHeight: 1.5,
            fontWeight: 600,
          }}
        >
          {showNoteWhileDeleting && deleteNote ? (
            <>
              {message || 'Are you sure you want to delete this row?'}
              <Box
                sx={{
                  mt: 1,
                  p: 1,
                  borderRadius: '6px',
                  bgcolor: '#fee2e2',
                  fontSize: '0.7rem',
                  color: '#7f1d1d',
                  fontWeight: 600,
                }}
              >
                {deleteNote}
              </Box>
            </>
          ) : (
            message || 'Are you sure you want to delete this row?'
          )}
        </Typography>

        <Typography
          sx={{
            mt: 1,
            fontSize: '0.7rem',
            color: '#991b1b',
            fontWeight: 600,
          }}
        >
          This action cannot be undone.
        </Typography>
      </DialogContent>

      {/* Actions */}
      <DialogActions sx={{ p: 1.5, pt: 0, gap: 1 }}>
        <Button
          onClick={() => setOpenDeleteDialogeBox(false)}
          className='btn-no'
        >
          Cancel
        </Button>

        <Button
          onClick={deleteTheRecord}
          variant='contained'
          size='small'
          disabled={readOnly}
          className='btn-yes'
        >
          {confirmButtonText || 'Delete'}
        </Button>
      </DialogActions>
    </CompactDialog>
  )
}

export default DeleteDialog
