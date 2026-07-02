import React, { useEffect, useState, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { FileService } from 'services'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { getRoleName } from 'services/role-service'
import {
  Box,
  Button,
  Typography,
  Grid,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Snackbar,
  Alert,
  LinearProgress,
  Card,
  CardContent,
} from '@mui/material'
import {
  CloudUpload as CloudUploadIcon,
  Delete as DeleteIcon,
  Download as DownloadIcon,
  InsertDriveFile as FileIcon,
} from '@mui/icons-material'

const OtherDocumentUpload = ({ permissions }) => {
  const [loading, setLoading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [isUploading, setIsUploading] = useState(false)
  const [dragOver, setDragOver] = useState(false)
  const [rows, setRows] = useState([])
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    yearChanged,
    oldYear,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
    isReleased,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const PLANT_NAME = plantObject?.name
  const SITE_ID = siteObject?.id
  const SITE_NAME = siteObject?.name
  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name
  const AOP_YEAR = year?.selectedYear
  const vertName = verticalChange?.selectedVertical
  const SCREEN_NAME = screenTitle?.title

  const keycloak = useSession()
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  // LocalStorage Key based on current Context
  const getStorageKey = useCallback(() => {
    return `other_docs_${PLANT_ID || 'default'}_${AOP_YEAR || 'default'}_${VERTICAL_ID || 'default'}`
  }, [PLANT_ID, AOP_YEAR, VERTICAL_ID])

  // Fetch / Load documents
  const fetchData = useCallback(() => {
    if (!PLANT_ID || !AOP_YEAR) {
      setRows([])
      return
    }
    try {
      const key = getStorageKey()
      const stored = localStorage.getItem(key)
      if (stored) {
        setRows(JSON.parse(stored))
      } else {
        setRows([])
      }
    } catch (error) {
      console.error('Error fetching documents list:', error)
      setRows([])
    }
  }, [PLANT_ID, AOP_YEAR, getStorageKey])

  useEffect(() => {
    fetchData()
  }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak, fetchData])

  const handleSnackbarClose = () => {
    setSnackbarOpen(false)
  }

  const showMessage = (message, severity = 'info') => {
    setSnackbarData({ message, severity })
    setSnackbarOpen(true)
  }

  // Handle Drag Events
  const handleDragOver = (e) => {
    e.preventDefault()
    if (READ_ONLY) return
    setDragOver(true)
  }

  const handleDragLeave = () => {
    setDragOver(false)
  }

  // File Upload Logic
  const uploadFiles = async (filesList) => {
    if (READ_ONLY) {
      showMessage('You do not have permission to upload files.', 'error')
      return
    }

    if (!filesList || filesList.length === 0) return

    setLoading(true)
    setIsUploading(true)
    setUploadProgress(0)

    try {
      const newUploads = []

      for (let i = 0; i < filesList.length; i++) {
        const file = filesList[i]
        const dirPath = `other-docs/${PLANT_ID}/${AOP_YEAR}`

        const args = {
          dir: dirPath,
          file: file,
          keycloak,
          progress: (e, percent) => {
            setUploadProgress(Math.round(percent))
          },
        }

        // Upload to storage API via FileService
        const uploadResult = await FileService.upload(args)

        // Save successfully uploaded document metadata
        newUploads.push({
          id: `${Date.now()}_${i}_${Math.random().toString(36).substr(2, 9)}`,
          name: uploadResult.name || file.name,
          size: uploadResult.size || file.size,
          type: uploadResult.type || file.type,
          dir: uploadResult.dir || dirPath,
          url: uploadResult.url || file.name,
          uploadedAt: new Date().toLocaleString(),
          uploadedBy: keycloak?.tokenParsed?.preferred_username || 'Unknown',
        })
      }

      // Merge and save to localStorage
      const key = getStorageKey()
      const existing = JSON.parse(localStorage.getItem(key) || '[]')
      const updated = [...existing, ...newUploads]
      localStorage.setItem(key, JSON.stringify(updated))

      setRows(updated)
      showMessage('Files uploaded successfully!', 'success')
    } catch (error) {
      console.error('File upload error:', error)
      showMessage(error?.message || 'Error occurred during file upload.', 'error')
    } finally {
      setLoading(false)
      setIsUploading(false)
      setUploadProgress(0)
    }
  }

  const handleDrop = (e) => {
    e.preventDefault()
    setDragOver(false)
    if (READ_ONLY) return
    const files = e.dataTransfer.files
    uploadFiles(files)
  }

  const handleFileSelect = (e) => {
    const files = e.target.files
    uploadFiles(files)
  }

  // File Download Logic
  const handleDownload = async (file) => {
    setLoading(true)
    try {
      await FileService.download(file, keycloak)
      showMessage('Download started.', 'success')
    } catch (error) {
      console.error('Download error:', error)
      showMessage('Failed to download the file.', 'error')
    } finally {
      setLoading(false)
    }
  }

  // File Delete Logic
  const handleDelete = (id) => {
    if (READ_ONLY) {
      showMessage('You do not have permission to delete files.', 'error')
      return
    }

    try {
      const key = getStorageKey()
      const existing = JSON.parse(localStorage.getItem(key) || '[]')
      const updated = existing.filter((item) => item.id !== id)
      localStorage.setItem(key, JSON.stringify(updated))
      setRows(updated)
      showMessage('File deleted successfully.', 'success')
    } catch (error) {
      console.error('Delete error:', error)
      showMessage('Failed to delete file.', 'error')
    }
  }

  // Format file size
  const formatBytes = (bytes) => {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  return (
    <Box sx={{ p: 3, display: 'flex', flexDirection: 'column', gap: 3 }}>
      <LoaderBackdrop open={!!loading && !isUploading} />

      {/* Header Context Display */}
      <Paper
        elevation={0}
        sx={{
          p: 2.5,
          background: 'linear-gradient(135deg, #1e3c72 0%, #2a5298 100%)',
          color: '#ffffff',
          borderRadius: 2,
          boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
        }}
      >
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={8}>
            <Typography variant="h4" fontWeight="600" gutterBottom>
              Other Document Upload
            </Typography>
            <Typography variant="body2" sx={{ opacity: 0.9 }}>
              Upload and manage supporting documents for the current configuration cycle.
            </Typography>
          </Grid>
          <Grid item xs={12} sm={4} sx={{ textAlign: { sm: 'right' } }}>
            <Box
              sx={{
                display: 'inline-block',
                p: 1.5,
                background: 'rgba(255, 255, 255, 0.1)',
                backdropFilter: 'blur(10px)',
                borderRadius: 1.5,
                border: '1px solid rgba(255, 255, 255, 0.2)',
                textAlign: 'left',
              }}
            >
              <Typography variant="caption" display="block" sx={{ opacity: 0.8, textTransform: 'uppercase', fontWeight: 'bold' }}>
                Active Context
              </Typography>
              <Typography variant="body2" fontWeight="600">
                Plant: {PLANT_NAME || 'N/A'}
              </Typography>
              <Typography variant="body2" fontWeight="600">
                Year: {AOP_YEAR || 'N/A'}
              </Typography>
            </Box>
          </Grid>
        </Grid>
      </Paper>

      {/* Drag & Drop Upload Component */}
      {!READ_ONLY && (
        <Card
          elevation={0}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          sx={{
            border: dragOver ? '2px dashed #3f51b5' : '2px dashed #ccc',
            backgroundColor: dragOver ? 'rgba(63, 81, 181, 0.04)' : '#fafafa',
            cursor: 'pointer',
            borderRadius: 2,
            transition: 'all 0.2s ease-in-out',
            '&:hover': {
              borderColor: '#3f51b5',
              backgroundColor: 'rgba(63, 81, 181, 0.02)',
            },
          }}
        >
          <input
            type="file"
            id="file-upload-input"
            multiple
            style={{ display: 'none' }}
            onChange={handleFileSelect}
          />
          <label htmlFor="file-upload-input" style={{ cursor: 'pointer', width: '100%' }}>
            <CardContent
              sx={{
                py: 6,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 2,
              }}
            >
              <CloudUploadIcon sx={{ fontSize: 56, color: dragOver ? '#3f51b5' : '#757575' }} />
              <Box textAlign="center">
                <Typography variant="h6" color="textPrimary" fontWeight="500">
                  Drag and drop files here, or click to browse
                </Typography>
                <Typography variant="body2" color="textSecondary" sx={{ mt: 0.5 }}>
                  Support for Excel documents.
                </Typography>
              </Box>
            </CardContent>
          </label>
        </Card>
      )}

      {/* Uploading Progress Bar */}
      {isUploading && (
        <Box sx={{ width: '100%', mt: 1 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
            <Typography variant="body2" color="textSecondary">
              Uploading documents...
            </Typography>
            <Typography variant="body2" fontWeight="bold" color="primary">
              {uploadProgress}%
            </Typography>
          </Box>
          <LinearProgress variant="determinate" value={uploadProgress} sx={{ height: 8, borderRadius: 4 }} />
        </Box>
      )}

      {/* Uploaded Documents Table */}
      <TableContainer component={Paper} elevation={0} sx={{ border: '1px solid #e0e0e0', borderRadius: 2 }}>
        <Table>
          <TableHead sx={{ backgroundColor: '#f5f5f5' }}>
            <TableRow>
              <TableCell width="60px"></TableCell>
              <TableCell sx={{ fontWeight: '600' }}>Document Name</TableCell>
              <TableCell sx={{ fontWeight: '600' }}>Size</TableCell>
              <TableCell sx={{ fontWeight: '600' }}>Type</TableCell>
              <TableCell sx={{ fontWeight: '600' }}>Uploaded Date</TableCell>
              <TableCell sx={{ fontWeight: '600' }}>Uploaded By</TableCell>
              <TableCell align="right" sx={{ fontWeight: '600', pr: 3 }}>
                Actions
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                  <Typography variant="body1" color="textSecondary">
                    No documents uploaded yet for {PLANT_NAME || 'this plant'} in {AOP_YEAR || 'this year'}.
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              rows.map((row) => (
                <TableRow
                  key={row.id}
                  sx={{
                    '&:hover': {
                      backgroundColor: '#fafafa',
                    },
                  }}
                >
                  <TableCell align="center">
                    <FileIcon color="action" />
                  </TableCell>
                  <TableCell sx={{ fontWeight: '500' }}>{row.name}</TableCell>
                  <TableCell>{formatBytes(row.size)}</TableCell>
                  <TableCell>
                    <Typography
                      variant="caption"
                      sx={{
                        backgroundColor: '#e3f2fd',
                        color: '#0d47a1',
                        px: 1,
                        py: 0.5,
                        borderRadius: 1,
                        fontWeight: '600',
                      }}
                    >
                      {row.type ? row.type.split('/')[1] || row.type : 'N/A'}
                    </Typography>
                  </TableCell>
                  <TableCell>{row.uploadedAt}</TableCell>
                  <TableCell>{row.uploadedBy}</TableCell>
                  <TableCell align="right" sx={{ pr: 2 }}>
                    <IconButton color="primary" onClick={() => handleDownload(row)} title="Download Document">
                      <DownloadIcon />
                    </IconButton>
                    {!READ_ONLY && (
                      <IconButton color="error" onClick={() => handleDelete(row.id)} title="Delete Document">
                        <DeleteIcon />
                      </IconButton>
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Snackbar Alert */}
      <Snackbar open={snackbarOpen} autoHideDuration={4000} onClose={handleSnackbarClose}>
        <Alert onClose={handleSnackbarClose} severity={snackbarData.severity} sx={{ width: '100%' }}>
          {snackbarData.message}
        </Alert>
      </Snackbar>
    </Box>
  )
}

export default OtherDocumentUpload
