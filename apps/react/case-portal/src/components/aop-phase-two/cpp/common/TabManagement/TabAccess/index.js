import { useState, useCallback, useEffect, useMemo } from 'react'
import {
  Box,
  Backdrop,
  CircularProgress,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Autocomplete,
  TextField,
  Chip,
  Typography,
  IconButton,
} from '@mui/material'
import { useSelector } from 'react-redux'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { TabAccessApiService } from 'components/aop-phase-two/services/common/tabAccessApiService'
import { useDebounce } from 'hooks/useDebounce'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const EMPTY_FORM = {
  type: '',
  verticalId: '',
  siteId: '',
  plantId: '',
  configurationTabs: [],
}

const TabAccess = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id

  const [allRows, setAllRows] = useState([])
  const [rows, setRows] = useState([])
  const [allTabs, setAllTabs] = useState([])
  const [hierarchy, setHierarchy] = useState([])
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

  // Build dropdown options from hierarchy
  const verticalOptions = useMemo(
    () => hierarchy.map((v) => ({ id: v.id, label: v.displayName || v.name })),
    [hierarchy],
  )

  const tabOptions = useMemo(
    () => allTabs.map((t) => ({ id: t.id, label: t.displayName || t.name })),
    [allTabs],
  )

  // Dialog cascading options (based on form selection)
  const formSelectedVertical = useMemo(
    () =>
      hierarchy.find(
        (v) => v.id?.toLowerCase() === form.verticalId?.toLowerCase(),
      ),
    [hierarchy, form.verticalId],
  )
  const formSiteOptions = useMemo(
    () =>
      (formSelectedVertical?.sites || []).map((s) => ({
        id: s.id,
        label: s.displayName || s.name,
      })),
    [formSelectedVertical],
  )
  const formSelectedSite = useMemo(
    () =>
      formSelectedVertical?.sites?.find(
        (s) => s.id?.toLowerCase() === form.siteId?.toLowerCase(),
      ),
    [formSelectedVertical, form.siteId],
  )
  const formPlantOptions = useMemo(
    () =>
      (formSelectedSite?.plants || []).map((p) => ({
        id: p.id,
        label: p.displayName || p.name,
      })),
    [formSelectedSite],
  )

  // Resolve helpers for display
  const resolveTabsDisplay = (configTabsStr) => {
    try {
      const ids = JSON.parse(configTabsStr) || []
      return ids
        .map((id) => {
          const tab = allTabs.find(
            (t) => t.id?.toLowerCase() === id?.toLowerCase(),
          )
          return tab ? tab.displayName || tab.name : id
        })
        .join(', ')
    } catch {
      return ''
    }
  }

  // Custom action cell for Edit/Delete buttons
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

  // Table columns — read-only display with resolved labels
  const columns = [
    {
      field: 'verticalDisplay',
      title: 'Vertical',
      type: 'text',
      editable: false,
      minWidth: 150,
    },
    {
      field: 'siteDisplay',
      title: 'Site',
      type: 'text',
      editable: false,
      minWidth: 150,
    },
    {
      field: 'plantDisplay',
      title: 'Plant',
      type: 'text',
      editable: false,
      minWidth: 150,
    },
    {
      field: 'tabsDisplay',
      title: 'Tabs',
      type: 'text',
      editable: false,
      minWidth: 300,
    },
    {
      field: 'typeDisplay',
      title: 'Type',
      type: 'text',
      editable: false,
      minWidth: 150,
    },
    {
      field: 'actions',
      title: 'Actions',
      type: 'customAction',
      cell: customActionCell,
      minWidth: 150,
    },
  ]

  const fetchData = useCallback(async () => {
    if (!allTabs.length || !hierarchy.length) return
    setLoading(true)
    try {
      const response = await TabAccessApiService.getAllAccessMatrix(keycloak)
      const rawRows = response?.data || []
      // Enrich rows with display labels
      const enriched = rawRows.map((row, index) => {
        const vert = hierarchy.find(
          (v) => v.id?.toLowerCase() === row.verticalId?.toLowerCase(),
        )
        const site = vert?.sites?.find(
          (s) => s.id?.toLowerCase() === row.siteId?.toLowerCase(),
        )
        const plant = site?.plants?.find(
          (p) => p.id?.toLowerCase() === row.plantId?.toLowerCase(),
        )
        return {
          ...row,
          id: row.id || `row_${index}`,
          typeDisplay: row.type || '',
          verticalDisplay:
            vert?.displayName || vert?.name || row.verticalId || '',
          siteDisplay: site?.displayName || site?.name || row.siteId || '',
          plantDisplay: plant?.displayName || plant?.name || row.plantId || '',
          tabsDisplay: resolveTabsDisplay(row.configurationTabs),
        }
      })
      setAllRows(enriched)
    } catch (err) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to load access matrix',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [keycloak, allTabs, hierarchy])

  const fetchAllTabs = useCallback(async () => {
    try {
      const response =
        await TabAccessApiService.getConfigurationAvailableTabs(keycloak)
      setAllTabs(response?.data?.configurationTypeList || [])
    } catch (err) {
      console.error('Error fetching tabs:', err)
    }
  }, [keycloak])

  const fetchHierarchy = useCallback(async () => {
    try {
      const response = await TabAccessApiService.getPlantSiteVertical(keycloak)
      setHierarchy(response || [])
    } catch (err) {
      console.error('Error fetching hierarchy:', err)
    }
  }, [keycloak])

  useEffect(() => {
    fetchAllTabs()
    fetchHierarchy()
  }, [fetchAllTabs, fetchHierarchy])

  useEffect(() => {
    if (allTabs.length > 0 && hierarchy.length > 0) {
      fetchData()
    }
  }, [allTabs, hierarchy, fetchData])

  // Filter rows based on global vertical selection from header
  useEffect(() => {
    let filtered = allRows
    if (VERTICAL_ID) {
      filtered = filtered.filter(
        (r) => r.verticalId?.toLowerCase() === VERTICAL_ID.toLowerCase(),
      )
    }
    setRows(filtered)
  }, [allRows, VERTICAL_ID])

  // Force re-mount when reference data changes
  const tableKey = `tabaccess_${hierarchy.length}_${allTabs.length}_${rows.length}`

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
    titleName: 'Tab Access Matrix',
    addBtnName: 'Add Access',
  }

  // Dialog handlers
  const handleOpenCreate = () => {
    // Pre-fill form from global selection
    setForm({
      type: '',
      verticalId: VERTICAL_ID || '',
      siteId: SITE_ID || '',
      plantId: PLANT_ID || '',
      configurationTabs: [],
    })
    setEditingId(null)
    setDialogOpen(true)
  }

  const handleOpenEdit = (row) => {
    let parsedTabs = []
    try {
      parsedTabs = JSON.parse(row.configurationTabs) || []
    } catch {
      parsedTabs = []
    }
    setForm({
      type: row.type || '',
      verticalId: row.verticalId || '',
      siteId: row.siteId || '',
      plantId: row.plantId || '',
      configurationTabs: parsedTabs,
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
    if (!form.verticalId || !form.siteId || !form.plantId) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Vertical, Site and Plant are required',
        severity: 'warning',
      })
      return
    }
    const payload = {
      type: form.type,
      verticalId: form.verticalId,
      siteId: form.siteId,
      plantId: form.plantId,
      configurationTabs: JSON.stringify(form.configurationTabs),
    }
    setLoading(true)
    try {
      if (editingId) {
        await TabAccessApiService.updateAccessMatrix(
          keycloak,
          editingId,
          payload,
        )
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Access matrix updated successfully',
          severity: 'success',
        })
      } else {
        await TabAccessApiService.createAccessMatrix(keycloak, payload)
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Access matrix created successfully',
          severity: 'success',
        })
      }
      handleCloseDialog()
      await fetchData()
    } catch (err) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to save access matrix',
        severity: 'error',
      })
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
      await TabAccessApiService.deleteAccessMatrix(keycloak, deleteRow.id)
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
      <LoaderBackdrop open={!!loading} />

      <AdvanceKendoTable
        key={tableKey}
        columns={columns}
        rows={rows}
        setRows={setRows}
        title='Tab Access Matrix'
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
        maxWidth='md'
        fullWidth
      >
        <DialogTitle>
          {editingId ? 'Edit Access Matrix' : 'Add Access Matrix'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            {/* Vertical */}
            <Autocomplete
              options={verticalOptions}
              getOptionLabel={(option) => option.label || ''}
              isOptionEqualToValue={(option, value) =>
                option.id?.toLowerCase() === value?.id?.toLowerCase()
              }
              value={
                verticalOptions.find(
                  (v) => v.id?.toLowerCase() === form.verticalId?.toLowerCase(),
                ) || null
              }
              onChange={(e, newValue) =>
                setForm({
                  ...form,
                  verticalId: newValue?.id || '',
                  siteId: '',
                  plantId: '',
                })
              }
              renderInput={(params) => (
                <TextField
                  {...params}
                  label='Vertical'
                  placeholder='Select vertical'
                  size='small'
                />
              )}
            />

            {/* Site (cascaded from vertical) */}
            <Autocomplete
              options={formSiteOptions}
              getOptionLabel={(option) => option.label || ''}
              isOptionEqualToValue={(option, value) =>
                option.id?.toLowerCase() === value?.id?.toLowerCase()
              }
              value={
                formSiteOptions.find(
                  (s) => s.id?.toLowerCase() === form.siteId?.toLowerCase(),
                ) || null
              }
              onChange={(e, newValue) =>
                setForm({ ...form, siteId: newValue?.id || '', plantId: '' })
              }
              disabled={!form.verticalId}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label='Site'
                  placeholder='Select site'
                  size='small'
                />
              )}
            />

            {/* Plant (cascaded from site) */}
            <Autocomplete
              options={formPlantOptions}
              getOptionLabel={(option) => option.label || ''}
              isOptionEqualToValue={(option, value) =>
                option.id?.toLowerCase() === value?.id?.toLowerCase()
              }
              value={
                formPlantOptions.find(
                  (p) => p.id?.toLowerCase() === form.plantId?.toLowerCase(),
                ) || null
              }
              onChange={(e, newValue) =>
                setForm({ ...form, plantId: newValue?.id || '' })
              }
              disabled={!form.siteId}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label='Plant'
                  placeholder='Select plant'
                  size='small'
                />
              )}
            />

            {/* Configuration Tabs multi-select */}
            <Autocomplete
              multiple
              options={tabOptions}
              getOptionLabel={(option) => option.label || ''}
              isOptionEqualToValue={(option, value) =>
                option.id?.toLowerCase() === value?.id?.toLowerCase()
              }
              value={form.configurationTabs.map(
                (tabId) =>
                  tabOptions.find(
                    (t) => t.id?.toLowerCase() === tabId?.toLowerCase(),
                  ) || { id: tabId, label: tabId },
              )}
              onChange={(e, newValue) =>
                setForm({
                  ...form,
                  configurationTabs: newValue.map((v) => v.id),
                })
              }
              renderTags={(value, getTagProps) =>
                value.map((option, index) => (
                  <Chip
                    key={option.id}
                    label={option.label}
                    size='small'
                    {...getTagProps({ index })}
                  />
                ))
              }
              renderInput={(params) => (
                <TextField
                  {...params}
                  label='Configuration Tabs'
                  placeholder='Select tabs'
                  size='small'
                />
              )}
            />

            {/* Type */}
            <TextField
              label='Type (optional)'
              placeholder='e.g. TCS, OutputReport, InputReport'
              value={form.type}
              onChange={(e) => setForm({ ...form, type: e.target.value })}
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
            Are you sure you want to delete this access matrix entry?
            {deleteRow && (
              <Box
                component='span'
                sx={{ display: 'block', mt: 1, fontWeight: 'bold' }}
              >
                {deleteRow.typeDisplay} - {deleteRow.verticalDisplay} /{' '}
                {deleteRow.siteDisplay} / {deleteRow.plantDisplay}
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

export default TabAccess
