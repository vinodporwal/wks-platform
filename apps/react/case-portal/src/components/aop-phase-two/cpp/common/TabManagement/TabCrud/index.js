import { useState, useCallback, useEffect } from 'react'
import {
  Box,
  Backdrop,
  CircularProgress,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Typography,
  IconButton,
} from '@mui/material'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { TabAccessApiService } from 'components/aop-phase-two/services/common/tabAccessApiService'

const EMPTY_FORM = {
  name: '',
  displayName: '',
  displaySequence: 0,
}

const TabCrud = () => {
  const keycloak = useSession()

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  // Dialog state
  const [dialogOpen, setDialogOpen] = useState(false)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [deleteRow, setDeleteRow] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)

  // Custom action cell for Edit/Delete
  const customActionCell = (props) => {
    const dataItem = props.dataItem
    return (
      <td
        {...props.tdProps}
        style={{
          ...props.tdProps?.style,
          textAlign: 'center',
          verticalAlign: 'middle',
        }}
      >
        <IconButton size='small' onClick={() => handleOpenEdit(dataItem)}>
          <EditIcon fontSize='small' />
        </IconButton>
        <IconButton
          size='small'
          color='error'
          onClick={() => handleOpenDelete(dataItem)}
        >
          <DeleteIcon fontSize='small' />
        </IconButton>
      </td>
    )
  }

  const columns = [
    {
      field: 'name',
      title: 'Name',
      type: 'text',
      editable: false,
      minWidth: 250,
    },
    {
      field: 'displayName',
      title: 'Display Name',
      type: 'text',
      editable: false,
      minWidth: 200,
    },
    {
      field: 'displaySequence',
      title: 'Sequence',
      type: 'number',
      editable: false,
      minWidth: 100,
    },
    {
      field: 'actions',
      title: 'Actions',
      type: 'customAction',
      cell: customActionCell,
      minWidth: 100,
    },
  ]

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const response =
        await TabAccessApiService.getConfigurationAvailableTabs(keycloak)
      const list = response?.data?.configurationTypeList || []
      setRows(list)
    } catch (err) {
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Failed to load tabs', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const permissions = {
    showAction: false,
    addButton: true,
    deleteButton: false,
    editButton: false,
    saveBtn: false,
    allAction: true,
    showExport: false,
    showImport: false,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Tab Configuration',
    addBtnName: 'Add Tab',
  }

  // Dialog handlers
  const handleOpenCreate = () => {
    setForm(EMPTY_FORM)
    setEditingId(null)
    setDialogOpen(true)
  }

  const handleOpenEdit = (row) => {
    setForm({
      name: row.name || '',
      displayName: row.displayName || '',
      displaySequence: row.displaySequence ?? 0,
    })
    setEditingId(row.id)
    setDialogOpen(true)
  }

  const handleCloseDialog = () => {
    setDialogOpen(false)
    setForm(EMPTY_FORM)
    setEditingId(null)
  }

  const handleSave = async () => {
    if (!form.name || !form.displayName) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Name and Display Name are required',
        severity: 'warning',
      })
      return
    }
    const payload = {
      name: form.name,
      displayName: form.displayName,
      displaySequence: form.displaySequence,
    }
    setLoading(true)
    try {
      if (editingId) {
        await TabAccessApiService.updateConfigurationType(
          keycloak,
          editingId,
          payload,
        )
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Tab updated successfully',
          severity: 'success',
        })
      } else {
        await TabAccessApiService.createConfigurationType(keycloak, payload)
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Tab created successfully',
          severity: 'success',
        })
      }
      handleCloseDialog()
      await fetchData()
    } catch (err) {
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Failed to save tab', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }

  const handleOpenDelete = (row) => {
    setDeleteRow(row)
    setDeleteDialogOpen(true)
  }

  const handleConfirmDelete = async () => {
    if (!deleteRow) return
    setLoading(true)
    try {
      await TabAccessApiService.deleteConfigurationType(keycloak, deleteRow.id)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Record deleted successfully',
        severity: 'success',
      })
      setDeleteDialogOpen(false)
      setDeleteRow(null)
      await fetchData()
    } catch (err) {
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Failed to delete record', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box>
      <Backdrop
        sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
        open={!!loading}
      >
        <CircularProgress color='inherit' />
      </Backdrop>

      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        title='Tab Configuration'
        permissions={permissions}
        customAddRow={handleOpenCreate}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        paginationConfig={{
          threshold: 100,
          buttonCount: 5,
          pageSizes: [10, 20, 50, 100],
          defaultPageSize: 100,
        }}
      />

      {/* Create / Edit Dialog */}
      <Dialog
        open={dialogOpen}
        onClose={handleCloseDialog}
        maxWidth='sm'
        fullWidth
      >
        <DialogTitle>{editingId ? 'Edit Tab' : 'Add Tab'}</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <TextField
              label='Name'
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              fullWidth
              size='small'
            />
            <TextField
              label='Display Name'
              value={form.displayName}
              onChange={(e) =>
                setForm({ ...form, displayName: e.target.value })
              }
              fullWidth
              size='small'
            />
            <TextField
              label='Display Sequence'
              type='number'
              value={form.displaySequence}
              onChange={(e) =>
                setForm({
                  ...form,
                  displaySequence: parseInt(e.target.value) || 0,
                })
              }
              fullWidth
              size='small'
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button variant='contained' onClick={handleSave}>
            {editingId ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={() => setDeleteDialogOpen(false)}
      >
        <DialogTitle>Confirm Delete</DialogTitle>
        <DialogContent>
          <Typography>
            Are you sure you want to delete this tab?
            {deleteRow && (
              <Box
                component='span'
                sx={{ display: 'block', mt: 1, fontWeight: 'bold' }}
              >
                {deleteRow.displayName || deleteRow.name}
              </Box>
            )}
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>Cancel</Button>
          <Button
            color='error'
            variant='contained'
            onClick={handleConfirmDelete}
          >
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default TabCrud
