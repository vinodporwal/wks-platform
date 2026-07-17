import React, { useEffect, useState, useCallback, useMemo } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
import { FileService } from 'services'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { getRoleName } from 'services/role-service'
import {
  Box,
  Snackbar,
  Alert,
  IconButton,
  Tooltip,
  TextField,
  Typography,
  Button,
} from '@mui/material'
import { styled } from '@mui/material/styles'
import SendIcon from '@mui/icons-material/Send'
import '../../kendo-data-grid.css'
import AddIcon from '@mui/icons-material/Add'
import UploadIcon from '@mui/icons-material/Upload'
import CloudDownloadIcon from '@mui/icons-material/CloudDownload'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import KendoDataTables from './index'
import UploadDocumentDialog from './components/UploadDocumentDialog'
import DeleteConfirmationDialog from './components/DeleteConfirmationDialog'
import { TextArea } from '../../../node_modules/@progress/kendo-react-inputs/index'
import { SaveIcon } from 'assets/images/icons'

// ─── Styled Components (matching project reference in index.js) ─────────────────

const CompactTextField = styled(TextField)({
  '& .MuiOutlinedInput-root': {
    fontSize: '0.85rem',
    backgroundColor: '#fff',
    '& fieldset': { borderColor: '#e2e8f0' },
    '&:hover fieldset': { borderColor: '#cbd5e1' },
    '&.Mui-focused fieldset': { borderColor: '#0100cb' },
  },
})

// ─── Helpers ──────────────────────────────────────────────────────────────────

const formatBytes = (bytes) => {
  if (typeof bytes === 'string') return bytes
  if (!bytes || bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`
}

const DUMMY_DOCS = [
  {
    id: 'dummy_1',
    name: 'Monthly_Production_Overview.pdf',
    uploadedAt: new Date().toLocaleString(),
    uploadedBy: 'Demo User',
    size: formatBytes(524288),
    type: 'pdf',
    isDummy: true,
  },
  {
    id: 'dummy_2',
    name: 'Site_Configuration_Details.docx',
    uploadedAt: new Date().toLocaleString(),
    uploadedBy: 'System Admin',
    size: formatBytes(204800),
    type: 'docx',
    isDummy: true,
  },
  {
    id: 'dummy_3',
    name: 'Annual_Report_2025.xlsx',
    uploadedAt: new Date().toLocaleString(),
    uploadedBy: 'System Admin',
    size: formatBytes(358400),
    type: 'xlsx',
    isDummy: true,
  },
]

// ─── Main Component ───────────────────────────────────────────────────────────

const OtherDocumentUpload = ({ permissions }) => {
  // ── Core state ───────────────────────────────────────────────────────────
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})

  // ── Dialog state ─────────────────────────────────────────────────────────
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [open1, setOpen1] = useState(false)

  // ── Upload dialog ────────────────────────────────────────────────────────
  const [openUploadDialog, setOpenUploadDialog] = useState(false)
  const [uploadDialogMode, setUploadDialogMode] = useState('add')
  const [selectedRowForUpdate, setSelectedRowForUpdate] = useState(null)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [isUploading, setIsUploading] = useState(false)

  // ── Delete dialog ────────────────────────────────────────────────────────
  const [openDeleteConfirm, setOpenDeleteConfirm] = useState(false)
  const [selectedRowForDelete, setSelectedRowForDelete] = useState(null)
  const [isDeleting, setIsDeleting] = useState(false)

  // ── Notes textarea state ───────────────────────────────────────────────────
  const [notes, setNotes] = useState('')
  const [isSubmittingNotes, setIsSubmittingNotes] = useState(false)

  // ── Toast ────────────────────────────────────────────────────────────────
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({ message: '', severity: 'info' })

  const [summary, setSummary] = useState('')

  // ── Store / session ──────────────────────────────────────────────────────
  const apiRef = useGridApiRef()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, oldYear, yearChanged, verticalObject, siteObject } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const VERTICAL_ID = verticalObject?.id
  const IS_OLD_YEAR = oldYear?.oldYear
  const keycloak = useSession()

  const valueFormat = ValueFormatterProduction()
  const { isReleased } = dataGridStore
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, isReleased)

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_${AOP_YEAR}_Season_Month`

  // ─── Toast ────────────────────────────────────────────────────────────────

  const showMessage = useCallback((message, severity = 'info') => {
    setSnackbarData({ message, severity })
    setSnackbarOpen(true)
  }, [])

  // ─── Storage key ──────────────────────────────────────────────────────────

  const getStorageKey = useCallback(
    () => `other_docs_${PLANT_ID || 'default'}_${AOP_YEAR || 'default'}_${VERTICAL_ID || 'default'}`,
    [PLANT_ID, AOP_YEAR, VERTICAL_ID],
  )

  // ─── Fetch data ───────────────────────────────────────────────────────────

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setModifiedCells({})
    try {
      setLoading(true)
      const key = getStorageKey()
      const existing = JSON.parse(localStorage.getItem(key) || '[]')
      if (existing.length === 0) {
        const seeded = DUMMY_DOCS.map((d) => ({
          ...d,
          uploadedBy: keycloak?.tokenParsed?.preferred_username || d.uploadedBy,
        }))
        localStorage.setItem(key, JSON.stringify(seeded))
        setRows(seeded)
      } else {
        const cleaned = existing.map((row) => ({
          ...row,
          size: typeof row.size === 'number' ? formatBytes(row.size) : row.size,
        }))
        setRows(cleaned)
      }
    } catch (err) {
      console.error('Error loading documents:', err)
      showMessage('Failed to load document list.', 'error')
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, AOP_YEAR, getStorageKey, keycloak, showMessage])

  // ─── Upload (Add) ─────────────────────────────────────────────────────────

  const uploadFiles = useCallback(
    async (filesList) => {
      if (READ_ONLY) { showMessage('You do not have permission to upload files.', 'error'); return }
      if (!filesList?.length) return

      setIsUploading(true)
      setUploadProgress(0)
      setLoading(true)

      try {
        const newUploads = []
        for (let i = 0; i < filesList.length; i++) {
          const file = filesList[i]
          const dirPath = `other-docs/${PLANT_ID}/${AOP_YEAR}`
          const uploadResult = await FileService.upload({
            dir: dirPath, file, keycloak,
            progress: (_e, pct) => setUploadProgress(Math.round(pct)),
          })
          newUploads.push({
            id: `${Date.now()}_${i}_${Math.random().toString(36).substr(2, 9)}`,
            name: uploadResult?.name || file.name,
            size: formatBytes(uploadResult?.size || file.size),
            type: (uploadResult?.type || file.name.split('.').pop()).toLowerCase(),
            dir: uploadResult?.dir || dirPath,
            url: uploadResult?.url || file.name,
            uploadedAt: new Date().toLocaleString(),
            uploadedBy: keycloak?.tokenParsed?.preferred_username || 'Unknown',
            isDummy: false,
          })
        }
        const key = getStorageKey()
        const existing = JSON.parse(localStorage.getItem(key) || '[]')
        const updated = [...existing, ...newUploads]
        localStorage.setItem(key, JSON.stringify(updated))
        setRows(updated)
        showMessage('Document uploaded successfully!', 'success')
        setOpenUploadDialog(false)
      } catch (err) {
        console.error('Upload error:', err)
        showMessage(err?.message || 'Upload failed. Please try again.', 'error')
      } finally {
        setIsUploading(false)
        setUploadProgress(0)
        setLoading(false)
      }
    },
    [READ_ONLY, PLANT_ID, AOP_YEAR, keycloak, getStorageKey, showMessage],
  )

  // ─── Update (Replace) ─────────────────────────────────────────────────────

  const updateFile = useCallback(
    async (filesList, rowToUpdate) => {
      if (READ_ONLY) { showMessage('You do not have permission to update files.', 'error'); return }
      if (!filesList?.length || !rowToUpdate) return

      setIsUploading(true)
      setUploadProgress(0)
      setLoading(true)

      try {
        const file = filesList[0]
        const dirPath = `other-docs/${PLANT_ID}/${AOP_YEAR}`
        const uploadResult = await FileService.upload({
          dir: dirPath, file, keycloak,
          progress: (_e, pct) => setUploadProgress(Math.round(pct)),
        })
        const key = getStorageKey()
        const existing = JSON.parse(localStorage.getItem(key) || '[]')
        const updated = existing.map((item) =>
          item.id === rowToUpdate.id
            ? {
              ...item,
              name: uploadResult?.name || file.name,
              size: formatBytes(uploadResult?.size || file.size),
              type: (uploadResult?.type || file.name.split('.').pop()).toLowerCase(),
              dir: uploadResult?.dir || dirPath,
              url: uploadResult?.url || file.name,
              uploadedAt: new Date().toLocaleString(),
              uploadedBy: keycloak?.tokenParsed?.preferred_username || 'Unknown',
              isDummy: false,
            }
            : item,
        )
        localStorage.setItem(key, JSON.stringify(updated))
        setRows(updated)
        showMessage('Document updated successfully!', 'success')
        setOpenUploadDialog(false)
        setSelectedRowForUpdate(null)
      } catch (err) {
        console.error('Update error:', err)
        showMessage(err?.message || 'Update failed. Please try again.', 'error')
      } finally {
        setIsUploading(false)
        setUploadProgress(0)
        setLoading(false)
      }
    },
    [READ_ONLY, PLANT_ID, AOP_YEAR, keycloak, getStorageKey, showMessage],
  )

  // ─── Download ─────────────────────────────────────────────────────────────

  const handleDownload = useCallback(
    async (file) => {
      if (file?.isDummy || String(file?.id || '').startsWith('dummy')) {
        try {
          const blob = new Blob([`Mock content for ${file.name}`], { type: 'application/octet-stream' })
          const url = URL.createObjectURL(blob)
          const a = document.createElement('a')
          a.href = url
          a.download = file.name
          document.body.appendChild(a)
          a.click()
          a.remove()
          URL.revokeObjectURL(url)
          showMessage('Download started.', 'success')
        } catch {
          showMessage('Download failed.', 'error')
        }
        return
      }
      setLoading(true)
      try {
        await FileService.download(file, keycloak)
        showMessage('Download started.', 'success')
      } catch (err) {
        console.error('Download error:', err)
        showMessage('The file is unavailable or download failed.', 'error')
      } finally {
        setLoading(false)
      }
    },
    [keycloak, showMessage],
  )

  // ─── Delete ───────────────────────────────────────────────────────────────

  const handleDelete = useCallback(
    async (id) => {
      if (READ_ONLY) { showMessage('You do not have permission to delete files.', 'error'); return }
      setIsDeleting(true)
      try {
        const key = getStorageKey()
        const existing = JSON.parse(localStorage.getItem(key) || '[]')
        const updated = existing.filter((item) => item.id !== id)
        localStorage.setItem(key, JSON.stringify(updated))
        setRows(updated)
        showMessage('Document deleted successfully.', 'success')
      } catch (err) {
        console.error('Delete error:', err)
        showMessage('Delete failed. Please try again.', 'error')
      } finally {
        setIsDeleting(false)
        setOpenDeleteConfirm(false)
        setSelectedRowForDelete(null)
      }
    },
    [READ_ONLY, getStorageKey, showMessage],
  )

  // ─── ActionCell ───────────────────────────────────────────────────────────
  // Kendo's cells.data receives the full props object including `dataItem`.
  // We use direct onClick handlers here — no DOM event listener tricks needed.

  const ActionCell = useMemo(() => {
    const Cell = ({ dataItem }) => {
      const handleAddClick = (e) => {
        e.stopPropagation()
        if (!READ_ONLY) {
          setUploadDialogMode('add')
          setSelectedRowForUpdate(null)
          setOpenUploadDialog(true)
        }
      }

      const handleUpdateClick = (e) => {
        e.stopPropagation()
        if (!READ_ONLY) {
          setSelectedRowForUpdate(dataItem)
          setUploadDialogMode('update')
          setOpenUploadDialog(true)
        }
      }

      const handleDownloadClick = (e) => {
        e.stopPropagation()
        handleDownload(dataItem)
      }

      const handleDeleteClick = (e) => {
        e.stopPropagation()
        if (!READ_ONLY) {
          setSelectedRowForDelete(dataItem)
          setOpenDeleteConfirm(true)
        }
      }

      return (
        <td style={{ textAlign: 'center', verticalAlign: 'middle', whiteSpace: 'nowrap' }}>
          <Tooltip title="Add Document" arrow>
            <span>
              <IconButton
                size="small"
                disabled={READ_ONLY}
                onClick={handleAddClick}
              //sx={{ color: READ_ONLY ? 'action.disabled' : '#1565c0' }}
              >
                <AddIcon fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>

          <Tooltip title="Update Document" arrow>
            <span>
              <IconButton
                size="small"
                disabled={READ_ONLY}
                onClick={handleUpdateClick}
              //sx={{ color: READ_ONLY ? 'action.disabled' : '#2e7d32' }}
              >
                <UploadIcon fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>

          <Tooltip title="Download Document" arrow>
            <span>
              <IconButton
                size="small"
                onClick={handleDownloadClick}
              // sx={{ color: '#0277bd' }}
              >
                <CloudDownloadIcon fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>

          <Tooltip title="Delete Document" arrow>
            <span>
              <IconButton
                size="small"
                disabled={READ_ONLY}
                onClick={handleDeleteClick}
              // sx={{ color: READ_ONLY ? 'action.disabled' : '#c62828' }}
              >
                <DeleteOutlineIcon fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>
        </td>
      )
    }
    Cell.displayName = 'ActionCell'
    return Cell
  }, [READ_ONLY, handleDownload])

  // ─── Columns ──────────────────────────────────────────────────────────────

  const columns = useMemo(
    () => [
      { field: 'name', title: 'Document Name', editable: false, minWidth: 220 },
      { field: 'uploadedAt', title: 'Date & Time', editable: false, minWidth: 180 },
      { field: 'uploadedBy', title: 'Uploaded By', editable: false, minWidth: 160 },
      { field: 'size', title: 'Size', editable: false, minWidth: 120 },
      { field: 'type', title: 'Type', editable: false, minWidth: 100 },
      { field: 'action', title: 'Action', editable: false, minWidth: 200, cell: ActionCell },
    ],
    [ActionCell],
  )

  // ─── Effects ──────────────────────────────────────────────────────────────

  useEffect(() => {
    setModifiedCells({})
    fetchData()
  }, [oldYear, yearChanged, PLANT_ID, AOP_YEAR])

  // ─── Misc ─────────────────────────────────────────────────────────────────

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const getAdjustedPermissions = (perms, isOldYear) => {
    if (isOldYear !== 1) return perms
    return {
      ...perms,
      showAction: false, addButton: false, deleteButton: false,
      downloadExcelBtn: false, uploadExcelBtn: false, editButton: false,
      showUnit: false, saveWithRemark: false, saveBtn: true,
      isOldYear, allAction: false,
    }
  }

  const adjustedPermissionsManual = getAdjustedPermissions(
    {
      showAction: true,
      allAction: true,
      downloadExcelBtnFromUI: false,
      ExcelName: EXCEL_EXPORT_TITLE,
      showTitleNameBusiness: true,
      titleName: 'Document List',
    },
    IS_OLD_YEAR,
  )

  // ─── Upload dialog handler ────────────────────────────────────────────────

  const handleUploadDialogUpload = useCallback(
    (files) => {
      if (uploadDialogMode === 'add') {
        uploadFiles(files)
      } else {
        updateFile(files, selectedRowForUpdate)
      }
    },
    [uploadDialogMode, uploadFiles, updateFile, selectedRowForUpdate],
  )

  // ─── Delete confirm handler ───────────────────────────────────────────────

  const handleDeleteConfirm = useCallback(() => {
    if (selectedRowForDelete) handleDelete(selectedRowForDelete.id)
  }, [selectedRowForDelete, handleDelete])

  // ─── Notes submit ─────────────────────────────────────────────────────────

  const handleNotesSubmit = useCallback(async () => {
    if (!summary.trim() || READ_ONLY) return
    setIsSubmittingNotes(true)
    try {
      // TODO: Replace with actual API call, e.g.:
      // await SomeService.saveNotes({ plantId: PLANT_ID, year: AOP_YEAR, notes: summary, keycloak })
      await new Promise((resolve) => setTimeout(resolve, 600)) // simulated async
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Remarks saved successfully!',
        severity: 'success',
      })
      setSummary('')
    } catch (err) {
      console.error('Remarks save error:', err)
      showMessage(err?.message || 'Failed to save remarks. Please try again.', 'error')
    } finally {
      setIsSubmittingNotes(false)
    }
  }, [summary, READ_ONLY, showMessage])

  // ─────────────────────────────────────────────────────────────────────────

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      {loading && <LoaderBackdrop />}

      {/* Document Grid */}
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

      {/* ── Notes / Remarks Textarea ─────────────────────────────────── */}


      <Box sx={{ width: '100%' }}>
        {/* Label + Save button on same horizontal row */}
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            mb: 0.5,
          }}
        >
          <Typography
            variant='caption'
            className='aop-design-basis-label'
          >
            Remarks
          </Typography>

          <Button
            variant='contained'
            className='btn-save'
            startIcon={
              <Box component='img' src={SaveIcon} className='w16-icon' />
            }
            disabled={!summary.trim() || Boolean(READ_ONLY) || isSubmittingNotes}
            onClick={handleNotesSubmit}
          >
            Save
          </Button>
        </Box>

        <TextArea
          className='vertical-resize-textarea'
          disabled={Boolean(READ_ONLY)}
          value={summary}
          rows={2}
          onChange={(e) => {
            setSummary(e.target.value)
          }}
        />
      </Box>

      {/* ── Upload Dialog (Add / Update) ──────────────────────────────── */}
      <UploadDocumentDialog
        open={openUploadDialog}
        onClose={() => {
          if (!isUploading) {
            setOpenUploadDialog(false)
            setSelectedRowForUpdate(null)
          }
        }}
        uploadDialogMode={uploadDialogMode}
        selectedRowForUpdate={selectedRowForUpdate}
        isUploading={isUploading}
        uploadProgress={uploadProgress}
        onUpload={handleUploadDialogUpload}
        readOnly={READ_ONLY}
      />

      {/* ── Delete Confirmation Dialog ────────────────────────────────── */}
      <DeleteConfirmationDialog
        open={openDeleteConfirm}
        onClose={() => {
          if (!isDeleting) {
            setOpenDeleteConfirm(false)
            setSelectedRowForDelete(null)
          }
        }}
        selectedRowForDelete={selectedRowForDelete}
        onConfirm={handleDeleteConfirm}
        readOnly={READ_ONLY}
        isDeleting={isDeleting}
      />

      {/* ── Toast Notifications ───────────────────────────────────────── */}
      {/* <Snackbar
        open={snackbarOpen}
        autoHideDuration={4000}
        onClose={() => setSnackbarOpen(false)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert
          onClose={() => setSnackbarOpen(false)}
          severity={snackbarData.severity}
          variant="filled"
          sx={{ width: '100%', fontSize: '0.82rem' }}
        >
          {snackbarData.message}
        </Alert>
      </Snackbar> */}
    </Box>
  )
}

export default OtherDocumentUpload
