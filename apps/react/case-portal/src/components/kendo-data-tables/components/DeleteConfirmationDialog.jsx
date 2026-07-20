import React from 'react'
import {
  Box,
  Typography,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  IconButton,
  CircularProgress,
} from '@mui/material'
import {
  Close as CloseIcon,
  DeleteForever as DeleteForeverIcon,
  WarningAmber as WarningIcon,
} from '@mui/icons-material'
import { styled } from '@mui/material/styles'

const CompactDialog = styled(Dialog)(() => ({
  '& .MuiPaper-root': {
    borderRadius: '12px',
    width: '460px',
    maxWidth: 'calc(100% - 32px)',
    boxShadow: '0 20px 60px rgba(0,0,0,0.18)',
    overflowX: 'hidden',
  },
}))

const DeleteConfirmationDialog = ({
  open,
  onClose,
  selectedRowForDelete,
  onConfirm,
  readOnly,
  isDeleting = false,
}) => {
  return (
    <CompactDialog
      open={open}
      onClose={!isDeleting ? onClose : undefined}
      disableScrollLock
      slotProps={{ backdrop: { disableScrollLock: true } }}
    >
      {/* ── Header ───────────────────────────────────────────────────── */}
      <DialogTitle
        sx={{
          p: '10px 16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: 'linear-gradient(135deg, #b71c1c 0%, #c62828 100%)',
          color: '#fff',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <DeleteForeverIcon sx={{ fontSize: '1.1rem' }} />
          <Typography sx={{ fontWeight: 700, fontSize: '0.82rem', letterSpacing: '0.5px' }}>
            CONFIRM DELETE
          </Typography>
        </Box>
        <IconButton
          size='small'
          onClick={onClose}
          disabled={isDeleting}
          sx={{ color: 'rgba(255,255,255,0.85)', '&:hover': { color: '#fff' } }}
        >
          <CloseIcon fontSize='small' />
        </IconButton>
      </DialogTitle>

      {/* ── Content ──────────────────────────────────────────────────── */}
      <DialogContent sx={{ p: 2.5, pt: '20px !important' }}>
        {/* Warning icon row */}
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2, mb: 2 }}>
          <Box sx={{
            width: 56,
            height: 56,
            borderRadius: '50%',
            bgcolor: '#ffebee',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}>
            <WarningIcon sx={{ fontSize: 32, color: '#c62828' }} />
          </Box>
          <Typography sx={{ fontSize: '0.9rem', fontWeight: 700, color: '#1a1a1a', textAlign: 'center' }}>
            Are you sure you want to delete this document?
          </Typography>
        </Box>

        {/* File name highlight */}
        {selectedRowForDelete?.name && (
          <Box sx={{
            p: 1.25,
            borderRadius: 1.5,
            bgcolor: '#fff8f8',
            border: '1px solid #ffcdd2',
            textAlign: 'center',
            mb: 1.5,
          }}>
            <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#b71c1c', wordBreak: 'break-all' }}>
              {selectedRowForDelete.name}
            </Typography>
          </Box>
        )}

        <Typography sx={{ fontSize: '0.74rem', color: '#757575', textAlign: 'center' }}>
          This action <strong>cannot be undone</strong>. The document will be permanently removed.
        </Typography>
      </DialogContent>

      {/* ── Actions ──────────────────────────────────────────────────── */}
      <DialogActions sx={{ p: '12px 16px', gap: 1, borderTop: '1px solid #ffcdd2' }}>
        <Button
          onClick={onClose}
          disabled={isDeleting}
          size='small'
          sx={{
            fontSize: '0.78rem',
            color: '#546e7a',
            '&:hover': { bgcolor: '#eceff1' },
          }}
        >
          No, Cancel
        </Button>

        <Button
          onClick={onConfirm}
          variant='contained'
          size='small'
          disabled={readOnly || isDeleting}
          startIcon={
            isDeleting
              ? <CircularProgress size={13} sx={{ color: 'inherit' }} />
              : <DeleteForeverIcon sx={{ fontSize: '0.9rem !important' }} />
          }
          sx={{
            fontSize: '0.78rem',
            background: 'linear-gradient(135deg, #b71c1c 0%, #c62828 100%)',
            '&:hover': { background: 'linear-gradient(135deg, #7f0000 0%, #b71c1c 100%)' },
            '&:disabled': { opacity: 0.5 },
          }}
        >
          {isDeleting ? 'Deleting...' : 'Yes, Delete'}
        </Button>
      </DialogActions>
    </CompactDialog>
  )
}

export default DeleteConfirmationDialog
