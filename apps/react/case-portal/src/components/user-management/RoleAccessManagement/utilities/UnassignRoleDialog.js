import React from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Typography,
  Button,
} from '@mui/material'

const UnassignRoleDialog = ({
  unassignDialogOpen,
  setUnassignDialogOpen,
  roleToUnassign,
  setRoleToUnassign,
  lookupUser,
  handleConfirmUnassignRole,
}) => {
  const handleClose = () => {
    setUnassignDialogOpen(false)
    setRoleToUnassign('')
  }

  return (
    <Dialog
      open={unassignDialogOpen}
      onClose={handleClose}
      maxWidth="xs"
      fullWidth
      PaperProps={{
        sx: {
          fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
          '& *': {
            fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif !important",
          },
        },
      }}
    >
      <DialogTitle sx={{ fontWeight: 800, color: '#f59e0b', fontSize: '1rem' }}>
        Confirm Role Removal
      </DialogTitle>
      <DialogContent dividers>
        <Typography variant="body2" sx={{ color: '#334155', fontSize: '0.85rem' }}>
          Are you sure you want to remove role{' '}
          <strong style={{ color: '#0284c7' }}>
            &quot;{roleToUnassign}&quot;
          </strong>{' '}
          from user{' '}
          <strong style={{ color: '#0f172a' }}>
            {lookupUser?.username || 'this user'}
          </strong>
          ? The user will lose all permissions associated with this role.
        </Typography>
      </DialogContent>
      <DialogActions sx={{ p: 1.5 }}>
        <Button
          onClick={handleClose}
          color="inherit"
          size="small"
          sx={{ textTransform: 'none' }}
        >
          Cancel
        </Button>
        <Button
          variant="contained"
          color="warning"
          size="small"
          onClick={handleConfirmUnassignRole}
          sx={{ fontWeight: 700, textTransform: 'none' }}
        >
          Remove Role
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default UnassignRoleDialog
