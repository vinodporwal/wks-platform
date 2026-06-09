import React from 'react'
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Box,
  IconButton,
  Zoom,
} from '../../../../../../node_modules/@mui/material/index'
import { styled } from '../../../../../../node_modules/@mui/material/styles/index'
import CloseIcon from '@mui/icons-material/Close'
import PublishedWithChangesIcon from '@mui/icons-material/PublishedWithChanges'

const StyledConfirmDialog = styled(Dialog)(() => ({
  '& .MuiPaper-root': {
    borderRadius: '24px',
    padding: '12px',
    backgroundColor: 'rgba(255, 255, 255, 0.98)',
    backdropFilter: 'blur(16px)',
    boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.2)',
    border: '1px solid #ffffff',
  },
}))

const RevConfirmDialog = ({
  openConfirmDialogRev,
  handleCloseDialogRev,
  handleConfirmLoadRev,
}) => {
  return (
    <StyledConfirmDialog
      open={openConfirmDialogRev}
      onClose={handleCloseDialogRev}
      TransitionComponent={Zoom}
      transitionDuration={300}
      disableScrollLock
    >
      {/* Header close button */}
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: -1 }}>
        <IconButton onClick={handleCloseDialogRev} size='small'>
          <CloseIcon fontSize='small' />
        </IconButton>
      </Box>

      {/* Icon + Title */}
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          pt: 0,
        }}
      >
        <Box
          sx={{
            p: 0.5,
            borderRadius: '50%',
            bgcolor: 'rgba(1, 0, 203, 0.1)',
            color: '#0100cb',
            mb: 0.5,
          }}
        >
          <PublishedWithChangesIcon sx={{ fontSize: 32 }} />
        </Box>

        <DialogTitle
          sx={{
            textAlign: 'center',
            fontWeight: 800,
            fontSize: '1.15rem',
            color: '#1e293b',
            pb: 0,
          }}
        >
          Confirm Revision Change
        </DialogTitle>
      </Box>

      {/* Body */}
      <DialogContent sx={{ textAlign: 'center', pt: 1 }}>
        <DialogContentText
          sx={{
            color: '#64748b',
            fontSize: '0.85rem',
            lineHeight: 1.45,
          }}
        >
          Are you sure you want to change the revision?
        </DialogContentText>
      </DialogContent>

      {/* Actions */}
      <DialogActions
        sx={{
          justifyContent: 'center',
          gap: 1.5,
          pb: 0,
          px: 0,
        }}
      >
        <Button
          onClick={handleCloseDialogRev}
          sx={{
            color: '#64748b',
            textTransform: 'none',
            fontWeight: 700,
            fontSize: '0.85rem',
            px: 0,
          }}
        >
          No
        </Button>

        <Button
          onClick={handleConfirmLoadRev}
          variant='contained'
          autoFocus
          sx={{
            bgcolor: '#0100cb',
            textTransform: 'none',
            fontWeight: 700,
            fontSize: '0.85rem',
            px: 3,
            borderRadius: '10px',
            boxShadow: '0 8px 12px -3px rgba(1, 0, 203, 0.3)',
            transition: 'all 0.3s ease',
            '&:hover': {
              bgcolor: '#01008b',
              transform: 'scale(1.03)',
              boxShadow: '0 12px 16px -3px rgba(1, 0, 203, 0.4)',
            },
          }}
        >
          Yes, Change
        </Button>
      </DialogActions>
    </StyledConfirmDialog>
  )
}

export default RevConfirmDialog
