import React from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  Box,
  Typography,
  IconButton,
} from '@mui/material'
import FactCheckIcon from '@mui/icons-material/FactCheck'
import CloseIcon from '@mui/icons-material/Close'
import AopMyApprovals from './index'

/**
 * Utility Dialog Component for My Pending Approvals Modal
 *
 * @param {Object} props
 * @param {boolean} props.open - State indicating if dialog is open
 * @param {Function} props.onClose - Callback function to close the dialog
 */
const AopMyApprovalsDialog = ({ open, onClose }) => {
  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth='lg'
      fullWidth
      PaperProps={{
        sx: {
          borderRadius: '12px',
          p: 1,
          boxShadow: '0 12px 32px rgba(0,0,0,0.18)',
        },
      }}
    >
      <DialogTitle
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          pb: 1.5,
          px: 2,
          pt: 1.5,
          fontWeight: 700,
          fontSize: '1.15rem',
          fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
          color: '#1f2937',
          borderBottom: '1px solid #e5e7eb',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <FactCheckIcon sx={{ color: '#005eb8', fontSize: 24 }} />
          <Typography variant='h6' sx={{ fontWeight: 700, fontSize: '1.1rem' }}>
            My Pending Approvals
          </Typography>
        </Box>
        <IconButton
          onClick={onClose}
          size='small'
          sx={{ color: '#6b7280', '&:hover': { backgroundColor: '#f3f4f6' } }}
        >
          <CloseIcon fontSize='small' />
        </IconButton>
      </DialogTitle>
      <DialogContent sx={{ p: 2, pt: 2.5 }}>
        <AopMyApprovals onClose={onClose} />
      </DialogContent>
    </Dialog>
  )
}

export default AopMyApprovalsDialog
