import React, { useEffect, useState, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
import { FileService } from 'services'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { getRoleName } from 'services/role-service'
import {
  Box,
  Typography,
  Snackbar,
  Alert,
  LinearProgress,
  Card,
  CardContent,
} from '@mui/material'
import '../../kendo-data-grid.css'
import { CloudUpload as CloudUploadIcon } from '@mui/icons-material'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import KendoDataTables from './index'
import { ReportDataService } from 'services/ReportDataService'

const OtherDocumentUpload = ({ permissions }) => {
  const [loading, setLoading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [isUploading, setIsUploading] = useState(false)
  const [dragOver, setDragOver] = useState(false)
  const [rows, setRows] = useState([])

  const [columns, setColumns] = useState([])

  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [open1, setOpen1] = useState(false)

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const apiRef = useGridApiRef()

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    plantObject,
    year,
    oldYear,
    yearChanged,
    verticalObject,
    siteObject,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const VERTICAL_ID = verticalObject?.id
  const IS_OLD_YEAR = oldYear?.oldYear
  const keycloak = useSession()

  const valueFormat = ValueFormatterProduction()

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const isOldYear = false

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()

  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_${AOP_YEAR}_Season_Month`

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  useEffect(() => {
    setModifiedCells({})
    fetchData()
  }, [oldYear, yearChanged, keycloak, PLANT_ID, AOP_YEAR])

  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: true,
      isOldYear: isOldYear,
      allAction: false,
    }
  }

  const adjustedPermissionsManual = getAdjustedPermissions(
    {
      showAction: true,
      allAction: true,
      downloadExcelBtnFromUI: false,
      ExcelName: `${EXCEL_EXPORT_TITLE}`,
      showTitleNameBusiness: true,
      titleName: 'Document List',
    },
    IS_OLD_YEAR,
  )

  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setModifiedCells({})
    try {
      setLoading(true)
      // const response = await ReportDataService.getMonthlyProductionReportData(
      //   keycloak,
      //   PLANT_ID,
      //   AOP_YEAR,
      // )

      // const formattedData = response?.data?.data?.map((row, index) => ({
      //   ...row,
      //   id: row.id || index,
      //   isEditable: false,
      // }))

      // setRows(formattedData || [])
      const hardcodedColumns = [
        {
          field: 'name',
          title: 'Document Name',
          editable: false,
          minWidth: 200,
        },
        {
          field: 'uploadedAt',
          title: 'Date Time',
          editable: false,
          minWidth: 180,
        },
        {
          field: 'uploadedBy',
          title: 'Uploaded By',
          editable: false,
          minWidth: 180,
        },
        {
          field: 'size',
          title: 'Size',
          editable: false,
          minWidth: 180,
        },
        {
          field: 'type',
          title: 'Type',
          editable: false,
          minWidth: 120,
        },
        {
          field: 'action',
          title: 'Action',
          editable: false,
          minWidth: 100,
          isActionColumn: true,
        },
      ]

      setColumns(hardcodedColumns)
    } catch (error) {
      console.error('Error fetching data:', error)
      // setSnackbarOpen(true)
      // setSnackbarData({
      //   message: 'Error fetching data',
      //   severity: 'error',
      // })
    } finally {
      setLoading(false)
    }
  }

  // LocalStorage Key based on current Context
  const getStorageKey = useCallback(() => {
    return `other_docs_${PLANT_ID || 'default'}_${AOP_YEAR || 'default'}_${VERTICAL_ID || 'default'}`
  }, [PLANT_ID, AOP_YEAR, VERTICAL_ID])

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
      showMessage(
        error?.message || 'Error occurred during file upload.',
        'error',
      )
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
      {/* Header Context Display */}
      {/* <Paper
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
      </Paper> */}

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
            type='file'
            id='file-upload-input'
            multiple
            style={{ display: 'none' }}
            onChange={handleFileSelect}
          />
          <label
            htmlFor='file-upload-input'
            style={{ cursor: 'pointer', width: '100%' }}
          >
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
              <CloudUploadIcon
                sx={{ fontSize: 56, color: dragOver ? '#3f51b5' : '#757575' }}
              />
              <Box textAlign='center'>
                <Typography variant='h6' color='textPrimary' fontWeight='500'>
                  Drag and drop files here, or click to browse
                </Typography>
                <Typography
                  variant='body2'
                  color='textSecondary'
                  sx={{ mt: 0.5 }}
                >
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
            <Typography variant='body2' color='textSecondary'>
              Uploading documents...
            </Typography>
            <Typography variant='body2' fontWeight='bold' color='primary'>
              {uploadProgress}%
            </Typography>
          </Box>
          <LinearProgress
            variant='determinate'
            value={uploadProgress}
            sx={{ height: 8, borderRadius: 4 }}
          />
        </Box>
      )}

      <Box>
        <Box>
          <KendoDataTables
            modifiedCells={modifiedCells}
            setModifiedCells={setModifiedCells}
            setRows={setRows}
            columns={columns}
            rows={rows}
            paginationOptions={[100, 200, 300]}
            snackbarData={snackbarData}
            snackbarOpen={snackbarOpen}
            apiRef={apiRef}
            open1={open1}
            setOpen1={setOpen1}
            setSnackbarOpen={setSnackbarOpen}
            setSnackbarData={setSnackbarData}
            handleRemarkCellClick={handleRemarkCellClick}
            fetchData={fetchData}
            remarkDialogOpen={remarkDialogOpen}
            setRemarkDialogOpen={setRemarkDialogOpen}
            currentRemark={currentRemark}
            setCurrentRemark={setCurrentRemark}
            currentRowId={currentRowId}
            permissions={adjustedPermissionsManual}
            plantID={PLANT_ID}
          />
        </Box>
      </Box>

      {/* Snackbar Alert */}
      <Snackbar
        open={snackbarOpen}
        autoHideDuration={4000}
        onClose={handleSnackbarClose}
      >
        <Alert
          onClose={handleSnackbarClose}
          severity={snackbarData.severity}
          sx={{ width: '100%' }}
        >
          {snackbarData.message}
        </Alert>
      </Snackbar>
    </Box>
  )
}

export default OtherDocumentUpload
