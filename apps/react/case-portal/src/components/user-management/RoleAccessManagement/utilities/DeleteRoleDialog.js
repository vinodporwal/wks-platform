import React from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Typography,
  Button,
} from '@mui/material'

const DeleteRoleDialog = ({
  deleteDialogOpen,
  setDeleteDialogOpen,
  roleToDelete,
  deletingRole,
  handleDeleteRole,
}) => {
  return (
    <Dialog
      open={deleteDialogOpen}
      onClose={() => setDeleteDialogOpen(false)}
      maxWidth='xs'
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
      <DialogTitle sx={{ fontWeight: 800, color: '#ef4444', fontSize: '1rem' }}>
        Confirm Role Deletion
      </DialogTitle>
      <DialogContent dividers>
        <Typography
          variant='body2'
          sx={{ color: '#334155', fontSize: '0.85rem' }}
        >
          Are you sure you want to delete role{' '}
          <strong style={{ color: '#ef4444' }}>
            &quot;{roleToDelete}&quot;
          </strong>
          ? This action will permanently remove the role from the system.
        </Typography>
      </DialogContent>
      <DialogActions sx={{ p: 1.5 }}>
        <Button
          onClick={() => setDeleteDialogOpen(false)}
          color='inherit'
          size='small'
          sx={{ textTransform: 'none' }}
        >
          Cancel
        </Button>
        <Button
          variant='contained'
          color='error'
          size='small'
          disabled={deletingRole}
          onClick={handleDeleteRole}
          sx={{ fontWeight: 700, textTransform: 'none' }}
        >
          {deletingRole ? 'Deleting...' : 'Delete Role'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default DeleteRoleDialog
