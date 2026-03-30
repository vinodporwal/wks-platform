import React from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Button,
} from '@mui/material'

const DeleteDialog = ({
  message = null,
  openDeleteDialogeBox,
  setOpenDeleteDialogeBox,
  deleteTheRecord,
  confirmButtonText,
}) => {
  return (
    <div>
      <Dialog
        open={openDeleteDialogeBox}
        onClose={() => setOpenDeleteDialogeBox(false)}
        aria-labelledby='alert-dialog-title'
        aria-describedby='alert-dialog-description'
      >
        <DialogTitle id='alert-dialog-title'>
          {`${confirmButtonText} ?` || 'Delete ?'}
        </DialogTitle>
        <DialogContent>
          <DialogContentText id='alert-dialog-description'>
            {message || 'Are you sure you want to delete this row?'}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDeleteDialogeBox(false)}>Cancel</Button>
          <Button onClick={deleteTheRecord} autoFocus>
            {confirmButtonText || 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  )
}
export default DeleteDialog
