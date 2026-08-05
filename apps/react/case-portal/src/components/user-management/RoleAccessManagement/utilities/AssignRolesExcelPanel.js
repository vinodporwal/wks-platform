import React, { useState, useRef } from 'react'
import {
  Box,
  Typography,
  Button,
  Paper,
  CircularProgress,
  Collapse,
  IconButton,
  Tooltip,
  Alert,
  AlertTitle,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Grid,
} from '@mui/material'
import UploadFileIcon from '@mui/icons-material/UploadFile'
import CloudUploadIcon from '@mui/icons-material/CloudUpload'
import DescriptionIcon from '@mui/icons-material/Description'
import FileDownloadIcon from '@mui/icons-material/FileDownload'
import ClearIcon from '@mui/icons-material/Clear'
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown'
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile'

const AssignRolesExcelPanel = ({
  onUploadExcel,
  loading = false,
  result = null,
  onResetResult,
}) => {
  const [expanded, setExpanded] = useState(true)
  const [selectedFile, setSelectedFile] = useState(null)
  const [dragOver, setDragOver] = useState(false)
  const fileInputRef = useRef(null)

  // Handle Drag & Drop Events
  const handleDragOver = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setDragOver(true)
  }

  const handleDragLeave = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setDragOver(false)
  }

  const handleDrop = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setDragOver(false)

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0]
      if (
        file.name.endsWith('.xlsx') ||
        file.name.endsWith('.xls') ||
        file.type ===
          'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
        file.type === 'application/vnd.ms-excel'
      ) {
        setSelectedFile(file)
      }
    }
  }

  // File Selection via Dialog
  const handleFileChange = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      setSelectedFile(e.target.files[0])
    }
  }

  const handleClearFile = () => {
    setSelectedFile(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const handleTriggerUpload = () => {
    if (selectedFile && onUploadExcel) {
      onUploadExcel(selectedFile)
    }
  }

  // Format sample template for user download
  const handleDownloadTemplate = () => {
    const csvContent =
      'data:text/csv;charset=utf-8,username,roles\njohn.doe,"role_user, role_admin"\njane.smith,role_analyst\n'
    const encodedUri = encodeURI(csvContent)
    const link = document.createElement('a')
    link.setAttribute('href', encodedUri)
    link.setAttribute('download', 'user_roles_assignment_template.csv')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  // Format file size utility
  const formatFileSize = (bytes) => {
    if (!bytes) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  // Determine status color & badge
  const resStatus = result?.status ?? 200
  const resMessage = result?.message || result?.msg || ''

  return (
    <Paper
      elevation={0}
      sx={{
        borderRadius: '10px',
        padding: '16px 18px',
        border: '1px solid #e2e8f0',
        backgroundColor: '#ffffff',
        marginBottom: '14px',
        width: '100%',
      }}
    >
      {/* Header Bar with Icon & Expand/Collapse Controls */}
      <Box
        onClick={() => setExpanded(!expanded)}
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          cursor: 'pointer',
          userSelect: 'none',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <Box
            sx={{
              backgroundColor: '#eff6ff',
              color: '#0284c7',
              borderRadius: '8px',
              p: 0.8,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: '1px solid #bae6fd',
            }}
          >
            <UploadFileIcon fontSize='small' />
          </Box>
          <Box>
            <Typography
              variant='subtitle1'
              sx={{
                fontWeight: 700,
                color: '#0f172a',
                fontSize: '0.95rem',
                lineHeight: 1.2,
              }}
            >
              Bulk Role Assignment via Excel
            </Typography>
            <Typography
              variant='caption'
              sx={{ color: '#64748b', fontSize: '0.75rem' }}
            >
              POST /task/users/roles/assign-excel &bull; Upload Excel (.xlsx) to
              replace direct realm roles for multiple users
            </Typography>
          </Box>
        </Box>

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Tooltip title={expanded ? 'Collapse Panel' : 'Expand Panel'}>
            <IconButton size='small' sx={{ color: '#64748b' }}>
              {expanded ? (
                <KeyboardArrowUpIcon fontSize='small' />
              ) : (
                <KeyboardArrowDownIcon fontSize='small' />
              )}
            </IconButton>
          </Tooltip>
        </Box>
      </Box>

      {/* Collapsible Content */}
      <Collapse in={expanded} timeout='auto' unmountOnExit>
        <Box sx={{ mt: 2.5 }}>
          {/* Format Instructions Banner */}
          <Alert
            severity='info'
            icon={<InfoOutlinedIcon fontSize='inherit' />}
            action={
              <Button
                color='inherit'
                size='small'
                startIcon={<FileDownloadIcon />}
                onClick={handleDownloadTemplate}
                sx={{
                  textTransform: 'none',
                  fontWeight: 600,
                  fontSize: '0.75rem',
                  whiteSpace: 'nowrap',
                }}
              >
                Download Sample CSV
              </Button>
            }
            sx={{
              borderRadius: '8px',
              backgroundColor: '#f0f9ff',
              border: '1px solid #e0f2fe',
              color: '#0369a1',
              mb: 2.5,
              '& .MuiAlert-icon': {
                color: '#0284c7',
              },
            }}
          >
            <AlertTitle sx={{ fontWeight: 700, fontSize: '0.85rem', mb: 0.5 }}>
              Excel File Specification (.xlsx)
            </AlertTitle>
            <Typography variant='caption' component='div' sx={{ lineHeight: 1.4 }}>
              The Excel sheet must contain two columns: <strong>username</strong> and{' '}
              <strong>roles</strong>. Roles should be comma-separated (e.g.{' '}
              <code>role_user, role_admin</code>). Direct realm roles for matching users
              will be updated.
            </Typography>
          </Alert>

          {/* File Picker & Drag-and-Drop Dropzone */}
          <Box
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => !selectedFile && fileInputRef.current?.click()}
            sx={{
              border: '2px dashed',
              borderColor: dragOver ? '#0284c7' : selectedFile ? '#10b981' : '#cbd5e1',
              backgroundColor: dragOver
                ? '#f0f9ff'
                : selectedFile
                ? '#f0fdf4'
                : '#f8fafc',
              borderRadius: '10px',
              p: 3,
              textAlign: 'center',
              cursor: selectedFile ? 'default' : 'pointer',
              transition: 'all 0.2s ease-in-out',
              '&:hover': {
                borderColor: selectedFile ? '#10b981' : '#0284c7',
                backgroundColor: selectedFile ? '#f0fdf4' : '#f0f9ff',
              },
            }}
          >
            <input
              type='file'
              accept='.xlsx, .xls'
              ref={fileInputRef}
              onChange={handleFileChange}
              style={{ display: 'none' }}
            />

            {!selectedFile ? (
              <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 1 }}>
                <CloudUploadIcon sx={{ fontSize: 42, color: dragOver ? '#0284c7' : '#94a3b8' }} />
                <Typography variant='body2' sx={{ fontWeight: 600, color: '#334155' }}>
                  Drag & drop your Excel file here, or{' '}
                  <Typography
                    component='span'
                    variant='body2'
                    sx={{ color: '#0284c7', fontWeight: 700, textDecoration: 'underline' }}
                  >
                    browse computer
                  </Typography>
                </Typography>
                <Typography variant='caption' sx={{ color: '#94a3b8' }}>
                  Supported Formats: .xlsx, .xls (Max 10MB)
                </Typography>
              </Box>
            ) : (
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  backgroundColor: '#ffffff',
                  p: 1.8,
                  borderRadius: '8px',
                  border: '1px solid #bbf7d0',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                  <InsertDriveFileIcon sx={{ fontSize: 36, color: '#16a34a' }} />
                  <Box sx={{ textAlign: 'left' }}>
                    <Typography
                      variant='body2'
                      sx={{ fontWeight: 700, color: '#1e293b', wordBreak: 'break-all' }}
                    >
                      {selectedFile.name}
                    </Typography>
                    <Typography variant='caption' sx={{ color: '#64748b' }}>
                      Size: {formatFileSize(selectedFile.size)} &bull; Type: Excel Workbook
                    </Typography>
                  </Box>
                </Box>

                <Tooltip title='Remove file'>
                  <IconButton
                    size='small'
                    onClick={(e) => {
                      e.stopPropagation()
                      handleClearFile()
                    }}
                    sx={{ color: '#ef4444', '&:hover': { backgroundColor: '#fef2f2' } }}
                  >
                    <ClearIcon fontSize='small' />
                  </IconButton>
                </Tooltip>
              </Box>
            )}
          </Box>

          {/* Action Buttons */}
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'flex-end',
              gap: 1.5,
              mt: 2.5,
            }}
          >
            {result && (
              <Button
                variant='outlined'
                size='small'
                onClick={() => {
                  if (onResetResult) onResetResult()
                }}
                sx={{
                  textTransform: 'none',
                  fontWeight: 600,
                  color: '#64748b',
                  borderColor: '#cbd5e1',
                  borderRadius: '6px',
                  '&:hover': {
                    borderColor: '#94a3b8',
                    backgroundColor: '#f8fafc',
                  },
                }}
              >
                Clear Response
              </Button>
            )}

            <Button
              variant='contained'
              disabled={!selectedFile || loading}
              onClick={handleTriggerUpload}
              startIcon={
                loading ? (
                  <CircularProgress size={18} color='inherit' />
                ) : (
                  <CloudUploadIcon />
                )
              }
              sx={{
                textTransform: 'none',
                fontWeight: 700,
                backgroundColor: '#0284c7',
                color: '#ffffff',
                borderRadius: '6px',
                px: 3,
                py: 0.9,
                boxShadow: 'none',
                '&:hover': {
                  backgroundColor: '#0369a1',
                  boxShadow: '0 2px 4px rgba(2, 132, 199, 0.25)',
                },
                '&.Mui-disabled': {
                  backgroundColor: '#e2e8f0',
                  color: '#94a3b8',
                },
              }}
            >
              {loading ? 'Processing Excel...' : 'Upload & Assign Roles'}
            </Button>
          </Box>

          {/* Results Summary Box */}
          {result && (
            <Paper
              elevation={0}
              sx={{
                mt: 3,
                p: 2.5,
                borderRadius: '8px',
                backgroundColor:
                  resStatus === 207
                    ? '#fffbe6'
                    : resStatus === 400
                    ? '#fff2f0'
                    : '#f6ffed',
                border: `1px solid ${
                  resStatus === 207
                    ? '#ffe58f'
                    : resStatus === 400
                    ? '#ffccc7'
                    : '#b7eb8f'
                }`,
              }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1.5 }}>
                {resStatus === 200 && (
                  <CheckCircleOutlineIcon sx={{ color: '#52c41a', fontSize: 24 }} />
                )}
                {resStatus === 207 && (
                  <WarningAmberIcon sx={{ color: '#faad14', fontSize: 24 }} />
                )}
                {resStatus === 400 && (
                  <ErrorOutlineIcon sx={{ color: '#ff4d4f', fontSize: 24 }} />
                )}

                <Typography
                  variant='subtitle2'
                  sx={{
                    fontWeight: 700,
                    color:
                      resStatus === 207
                        ? '#d48806'
                        : resStatus === 400
                        ? '#cf1322'
                        : '#389e0d',
                    fontSize: '0.9rem',
                  }}
                >
                  {resStatus === 200
                    ? '200 OK — Role Assignment Completed'
                    : resStatus === 207
                    ? '207 Multi-Status — Partial Role Assignment'
                    : `Status ${resStatus} — Process Request Result`}
                </Typography>

                <Chip
                  label={`HTTP ${resStatus}`}
                  size='small'
                  sx={{
                    fontWeight: 700,
                    fontSize: '0.7rem',
                    backgroundColor:
                      resStatus === 207
                        ? '#ffe58f'
                        : resStatus === 400
                        ? '#ffccc7'
                        : '#b7eb8f',
                    color:
                      resStatus === 207
                        ? '#873800'
                        : resStatus === 400
                        ? '#a8071a'
                        : '#135200',
                  }}
                />
              </Box>

              {resMessage && (
                <Typography
                  variant='body2'
                  sx={{ color: '#334155', mb: 2, fontWeight: 500 }}
                >
                  {resMessage}
                </Typography>
              )}

              {/* Grid Metrics Summary if provided in API result */}
              <Grid container spacing={2} sx={{ mb: 2 }}>
                {result.totalProcessed !== undefined && (
                  <Grid item xs={4}>
                    <Paper
                      elevation={0}
                      sx={{ p: 1.5, textAlign: 'center', backgroundColor: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '6px' }}
                    >
                      <Typography variant='caption' sx={{ color: '#64748b', fontWeight: 600 }}>
                        Total Processed
                      </Typography>
                      <Typography variant='h6' sx={{ fontWeight: 800, color: '#1e293b' }}>
                        {result.totalProcessed}
                      </Typography>
                    </Paper>
                  </Grid>
                )}
                {result.successCount !== undefined && (
                  <Grid item xs={4}>
                    <Paper
                      elevation={0}
                      sx={{ p: 1.5, textAlign: 'center', backgroundColor: '#ffffff', border: '1px solid #b7eb8f', borderRadius: '6px' }}
                    >
                      <Typography variant='caption' sx={{ color: '#389e0d', fontWeight: 600 }}>
                        Successful
                      </Typography>
                      <Typography variant='h6' sx={{ fontWeight: 800, color: '#389e0d' }}>
                        {result.successCount}
                      </Typography>
                    </Paper>
                  </Grid>
                )}
                {result.failureCount !== undefined && (
                  <Grid item xs={4}>
                    <Paper
                      elevation={0}
                      sx={{ p: 1.5, textAlign: 'center', backgroundColor: '#ffffff', border: '1px solid #ffccc7', borderRadius: '6px' }}
                    >
                      <Typography variant='caption' sx={{ color: '#cf1322', fontWeight: 600 }}>
                        Skipped / Failed
                      </Typography>
                      <Typography variant='h6' sx={{ fontWeight: 800, color: '#cf1322' }}>
                        {result.failureCount}
                      </Typography>
                    </Paper>
                  </Grid>
                )}
              </Grid>

              {/* Detailed Breakdown List if available */}
              {Array.isArray(result.details) && result.details.length > 0 && (
                <TableContainer
                  component={Paper}
                  elevation={0}
                  sx={{
                    border: '1px solid #e2e8f0',
                    borderRadius: '6px',
                    maxHeight: '220px',
                    overflowY: 'auto',
                  }}
                >
                  <Table size='small' stickyHeader>
                    <TableHead>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 700, backgroundColor: '#f8fafc' }}>
                          Username
                        </TableCell>
                        <TableCell sx={{ fontWeight: 700, backgroundColor: '#f8fafc' }}>
                          Status
                        </TableCell>
                        <TableCell sx={{ fontWeight: 700, backgroundColor: '#f8fafc' }}>
                          Roles / Details
                        </TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {result.details.map((item, idx) => (
                        <TableRow key={idx} hover>
                          <TableCell sx={{ fontWeight: 600, fontSize: '0.8rem' }}>
                            {item.username || item.userId || `Row #${idx + 1}`}
                          </TableCell>
                          <TableCell>
                            <Chip
                              label={item.status || (item.success ? 'Success' : 'Error')}
                              size='small'
                              color={item.success !== false ? 'success' : 'error'}
                              variant='outlined'
                              sx={{ fontSize: '0.7rem', height: '22px' }}
                            />
                          </TableCell>
                          <TableCell sx={{ fontSize: '0.8rem', color: '#475569' }}>
                            {Array.isArray(item.roles)
                              ? item.roles.join(', ')
                              : item.message || item.roles || JSON.stringify(item)}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </Paper>
          )}
        </Box>
      </Collapse>
    </Paper>
  )
}

export default AssignRolesExcelPanel
