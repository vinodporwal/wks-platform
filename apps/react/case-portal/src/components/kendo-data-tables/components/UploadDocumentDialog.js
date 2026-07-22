import React, { useState, useRef, useCallback } from 'react'
import {
  Box,
  Typography,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  IconButton,
  LinearProgress,
  Chip,
} from '@mui/material'
import {
  CloudUpload as CloudUploadIcon,
  Close as CloseIcon,
  InsertDriveFile as FileIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
} from '@mui/icons-material'
import { styled } from '@mui/material/styles'

// ─── Styled Components ───────────────────────────────────────────────────────

const CompactDialog = styled(Dialog)(() => ({
  '& .MuiPaper-root': {
    borderRadius: '12px',
    width: '560px',
    maxWidth: 'calc(100% - 32px)',
    boxShadow: '0 20px 60px rgba(0,0,0,0.18)',
    overflowX: 'hidden',
  },
}))

const DropZone = styled(Box)(({ dragover }) => ({
  border: dragover === 'true' ? '2px dashed #1a237e' : '2px dashed #c5cae9',
  borderRadius: '10px',
  backgroundColor: dragover === 'true' ? 'rgba(26,35,126,0.06)' : '#f8f9ff',
  cursor: 'pointer',
  transition: 'all 0.2s ease-in-out',
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  padding: '32px 24px',
  gap: '12px',
  '&:hover': {
    borderColor: '#3949ab',
    backgroundColor: 'rgba(57,73,171,0.04)',
  },
}))

// ─── Constants ────────────────────────────────────────────────────────────────

const ACCEPTED_TYPES = [
  // 'application/pdf',
  // 'application/msword',
  // 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  // 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  // 'application/vnd.ms-powerpoint',
  // 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  // 'image/jpeg',
  // 'image/png',
  // 'image/jpg',
  //'text/plain',
  // 'text/csv',
]

//const ACCEPTED_EXTENSIONS = '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.jpg,.jpeg,.png,.txt,.csv'
const ACCEPTED_EXTENSIONS = '.xls,.xlsx,.xlsm'
const MAX_FILE_SIZE_MB = 10
const MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024

const formatBytes = (bytes) => {
  if (!bytes) return '0 Bytes'
  if (typeof bytes === 'string') return bytes
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`
}

const getFileExtension = (name = '') => name.split('.').pop().toLowerCase()

// ─── Component ────────────────────────────────────────────────────────────────

const UploadDocumentDialog = ({
  open,
  onClose,
  uploadDialogMode, // 'add' | 'update'
  selectedRowForUpdate,
  isUploading,
  uploadProgress,
  onUpload, // (files: FileList|File[]) => void
  readOnly,
}) => {
  const fileInputRef = useRef(null)
  const [selectedFile, setSelectedFile] = useState(null)
  const [dragOver, setDragOver] = useState(false)
  const [validationError, setValidationError] = useState('')

  // ── Helpers ──────────────────────────────────────────────────────────────

  const validateFile = useCallback((file) => {
    if (!file) return 'Please select a file.'
    const isTypeValid =
      ACCEPTED_TYPES.includes(file.type) ||
      ACCEPTED_EXTENSIONS.split(',').some((ext) =>
        file.name.toLowerCase().endsWith(ext),
      )
    if (!isTypeValid) {
      return `Unsupported file type (.${getFileExtension(file.name)}). Allowed: XLS, XLSX, XLSM.`
    }
    if (file.size > MAX_FILE_SIZE_BYTES) {
      return `File size (${formatBytes(file.size)}) exceeds the ${MAX_FILE_SIZE_MB} MB limit.`
    }
    return ''
  }, [])

  const pickFile = useCallback(
    (file) => {
      const error = validateFile(file)
      setValidationError(error)
      setSelectedFile(error ? null : file)
    },
    [validateFile],
  )

  // ── Event Handlers ────────────────────────────────────────────────────────

  const handleDragOver = (e) => {
    e.preventDefault()
    setDragOver(true)
  }

  const handleDragLeave = () => setDragOver(false)

  const handleDrop = (e) => {
    e.preventDefault()
    setDragOver(false)
    const file = e.dataTransfer.files?.[0]
    if (file) pickFile(file)
  }

  const handleInputChange = (e) => {
    const file = e.target.files?.[0]
    if (file) pickFile(file)
    // Reset input so the same file can be re-selected after removal
    e.target.value = ''
  }

  const handleRemoveFile = (e) => {
    e.stopPropagation()
    setSelectedFile(null)
    setValidationError('')
  }

  const handleBrowseClick = () => {
    if (!readOnly) fileInputRef.current?.click()
  }

  const handleUpload = () => {
    if (!selectedFile || validationError || readOnly) return
    // Wrap the single file in a pseudo-FileList array
    onUpload([selectedFile])
  }

  const handleClose = () => {
    setSelectedFile(null)
    setValidationError('')
    setDragOver(false)
    onClose()
  }

  const isAddMode = uploadDialogMode === 'add'
  const canUpload =
    !!selectedFile && !validationError && !isUploading && !readOnly

  return (
    <CompactDialog
      open={open}
      onClose={!isUploading ? handleClose : undefined}
      disableScrollLock
      slotProps={{ backdrop: { disableScrollLock: true } }}
    >
      {/* ── Header ─────────────────────────────────────────────────────── */}
      <DialogTitle
        sx={{
          p: '10px 16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: 'linear-gradient(135deg, #1a237e 0%, #283593 100%)',
          color: '#fff',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <CloudUploadIcon sx={{ fontSize: '1.1rem' }} />
          <Typography
            sx={{
              fontWeight: 700,
              fontSize: '0.82rem',
              letterSpacing: '0.5px',
            }}
          >
            {isAddMode ? 'ADD DOCUMENT' : 'UPDATE DOCUMENT'}
          </Typography>
        </Box>

        <IconButton
          size='small'
          onClick={handleClose}
          disabled={isUploading}
          sx={{ color: 'rgba(255,255,255,0.85)', '&:hover': { color: '#fff' } }}
        >
          <CloseIcon fontSize='small' />
        </IconButton>
      </DialogTitle>

      {/* ── Content ────────────────────────────────────────────────────── */}
      <DialogContent
        sx={{ p: 2.5, display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}
      >
        {/* Update context label */}
        {!isAddMode && selectedRowForUpdate && (
          <Box
            sx={{
              p: 1.5,
              borderRadius: 1.5,
              bgcolor: '#fffde7',
              border: '1px solid #f9a825',
              display: 'flex',
              alignItems: 'center',
              gap: 1,
            }}
          >
            <FileIcon sx={{ fontSize: '1rem', color: '#f57f17' }} />
            <Typography sx={{ fontSize: '0.78rem', color: '#5d4037' }}>
              Replacing: <strong>{selectedRowForUpdate?.documentName || selectedRowForUpdate?.name}</strong>
            </Typography>
          </Box>
        )}

        {/* Drop zone */}
        <DropZone
          dragover={dragOver.toString()}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={handleBrowseClick}
        >
          <input
            ref={fileInputRef}
            type='file'
            accept={ACCEPTED_EXTENSIONS}
            style={{ display: 'none' }}
            onChange={handleInputChange}
          />
          <CloudUploadIcon
            sx={{
              fontSize: 52,
              color: dragOver ? '#1a237e' : '#9fa8da',
              transition: 'color 0.2s',
            }}
          />
          <Box textAlign='center'>
            <Typography
              variant='body1'
              fontWeight={600}
              color='#1a237e'
              fontSize='0.9rem'
            >
              Drag & Drop your file here
            </Typography>
            <Typography
              variant='body2'
              color='text.secondary'
              fontSize='0.76rem'
              sx={{ mt: 0.5 }}
            >
              or
            </Typography>
          </Box>
          <Button
            variant='outlined'
            size='small'
            disabled={readOnly || isUploading}
            onClick={(e) => {
              e.stopPropagation()
              handleBrowseClick()
            }}
            sx={{
              fontSize: '0.76rem',
              borderColor: '#3949ab',
              color: '#3949ab',
              '&:hover': {
                borderColor: '#1a237e',
                bgcolor: 'rgba(26,35,126,0.05)',
              },
            }}
          >
            Browse File
          </Button>
          <Typography
            variant='caption'
            color='text.secondary'
            textAlign='center'
            sx={{ lineHeight: 1.4 }}
          >
            Supported: XLS, XLSX, XLSM,
            <br />
            Max size: {MAX_FILE_SIZE_MB} MB
          </Typography>
        </DropZone>

        {/* Validation error */}
        {validationError && (
          <Box
            sx={{
              display: 'flex',
              alignItems: 'flex-start',
              gap: 1,
              color: '#c62828',
            }}
          >
            <ErrorIcon sx={{ fontSize: '1rem', mt: '2px', flexShrink: 0 }} />
            <Typography fontSize='0.76rem' color='inherit'>
              {validationError}
            </Typography>
          </Box>
        )}

        {/* Selected file preview */}
        {selectedFile && !validationError && (
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 1.5,
              p: 1.5,
              borderRadius: 1.5,
              border: '1px solid #c5cae9',
              bgcolor: '#f8f9ff',
            }}
          >
            <CheckCircleIcon
              sx={{ color: '#388e3c', fontSize: '1.2rem', flexShrink: 0 }}
            />
            <Box sx={{ flexGrow: 1, minWidth: 0 }}>
              <Typography
                fontSize='0.8rem'
                fontWeight={600}
                color='#1a237e'
                noWrap
                title={selectedFile.name}
              >
                {selectedFile.name}
              </Typography>
              <Typography fontSize='0.72rem' color='text.secondary'>
                {formatBytes(selectedFile.size)}
              </Typography>
            </Box>
            <Chip
              label={`.${getFileExtension(selectedFile.name)}`}
              size='small'
              sx={{
                fontSize: '0.68rem',
                height: 20,
                bgcolor: '#e8eaf6',
                color: '#283593',
              }}
            />
            <IconButton
              size='small'
              onClick={handleRemoveFile}
              disabled={isUploading}
              sx={{ color: '#757575', ml: 'auto', flexShrink: 0 }}
            >
              <CloseIcon fontSize='small' />
            </IconButton>
          </Box>
        )}

        {/* Upload progress */}
        {isUploading && (
          <Box>
            <Box
              sx={{
                display: 'flex',
                justifyContent: 'space-between',
                mb: 0.75,
              }}
            >
              <Typography
                variant='body2'
                fontSize='0.76rem'
                color='text.secondary'
              >
                Uploading...
              </Typography>
              <Typography
                variant='body2'
                fontSize='0.76rem'
                fontWeight={700}
                color='primary'
              >
                {uploadProgress}%
              </Typography>
            </Box>
            <LinearProgress
              variant='determinate'
              value={uploadProgress}
              sx={{ height: 7, borderRadius: 4 }}
            />
          </Box>
        )}
      </DialogContent>

      {/* ── Actions ────────────────────────────────────────────────────── */}
      <DialogActions
        sx={{ p: '12px 16px', gap: 1, borderTop: '1px solid #e0e0e0' }}
      >
        <Button
          onClick={handleClose}
          disabled={isUploading}
          size='small'
          sx={{
            fontSize: '0.78rem',
            color: '#546e7a',
            '&:hover': { bgcolor: '#eceff1' },
          }}
        >
          Cancel
        </Button>
        <Button
          onClick={handleUpload}
          disabled={!canUpload}
          variant='contained'
          size='small'
          startIcon={<CloudUploadIcon sx={{ fontSize: '0.9rem !important' }} />}
          sx={{
            fontSize: '0.78rem',
            background: 'linear-gradient(135deg, #1a237e 0%, #283593 100%)',
            '&:hover': {
              background: 'linear-gradient(135deg, #0d1b6e 0%, #1e2e85 100%)',
            },
            '&:disabled': { opacity: 0.5 },
          }}
        >
          {isUploading ? 'Uploading...' : 'Upload'}
        </Button>
      </DialogActions>
    </CompactDialog>
  )
}

export default UploadDocumentDialog
