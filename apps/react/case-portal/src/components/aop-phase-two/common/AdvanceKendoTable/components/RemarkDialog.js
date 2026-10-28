import React from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
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

const CompactTextField = styled(TextField)({
  '& .MuiOutlinedInput-root': {
    fontSize: '0.85rem',
    backgroundColor: '#fff',
    '& fieldset': { borderColor: '#e2e8f0' },
    '&:hover fieldset': { borderColor: '#cbd5e1' },
    '&.Mui-focused fieldset': { borderColor: '#0100cb' },
  },
})

// --- COMPONENT ---

const RemarkDialog = ({
  remarkDialogOpen,
  setRemarkDialogOpen,
  currentRemark,
  setCurrentRemark,
  handleRemarkSave,
  readOnly = false,
}) => {
  return (
    <CompactDialog
      open={!!remarkDialogOpen}
      onClose={() => setRemarkDialogOpen(false)}
      disableScrollLock
      slotProps={{ backdrop: { disableScrollLock: true } }}
    >
      {/* Compact Header */}
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
          ADD REMARK
        </Typography>
        <IconButton
          size='small'
          onClick={() => setRemarkDialogOpen(false)}
          sx={{ color: '#64748b' }}
        >
          <CloseIcon fontSize='small' />
        </IconButton>
      </DialogTitle>

      <DialogContent sx={{ p: 1.5, pt: '12px !important' }}>
        <CompactTextField
          autoFocus
          placeholder='Type your remarks here...'
          fullWidth
          multiline
          rows={6}
          value={currentRemark || ''}
          disabled={readOnly}
          onChange={(e) => setCurrentRemark(e.target.value)}
          // Power-user shortcut: Ctrl + Enter to Save
          onKeyDown={(e) => {
            if (
              e.ctrlKey &&
              e.key === 'Enter' &&
              currentRemark?.trim() &&
              !readOnly
            ) {
              handleRemarkSave()
            }
          }}
        />
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 0.5 }}>
          <Typography
            variant='caption'
            sx={{
              fontSize: '0.65rem',
              color: 'text.disabled',
              fontWeight: 600,
            }}
          >
            {currentRemark?.length || 0} characters | Ctrl+Enter to save
          </Typography>
        </Box>
      </DialogContent>

      <DialogActions sx={{ p: 1.5, pt: 0, gap: 1 }}>
        <Button onClick={() => setRemarkDialogOpen(false)} className='btn-no'>
          Cancel
        </Button>
        <Button
          onClick={handleRemarkSave}
          variant='contained'
          size='small'
          disabled={readOnly || !currentRemark?.trim()}
          className='btn-yes'
        >
          Add
        </Button>
      </DialogActions>
    </CompactDialog>
  )
}

export default RemarkDialog
