import React, { useState, useEffect, useCallback } from 'react'
import {
  Box,
  Typography,
  Button,
  Chip,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Autocomplete,
  Snackbar,
  Alert,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Grid,
  CircularProgress,
  IconButton,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import RefreshIcon from '@mui/icons-material/Refresh'
import SearchIcon from '@mui/icons-material/Search'
import ShieldIcon from '@mui/icons-material/Shield'
import PersonAddIcon from '@mui/icons-material/PersonAdd'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import { DataService } from 'services/DataService'
import { roleAccessApiService } from 'services/roleAccessApiService'

const RoleAccessManagement = ({ keycloak }) => {
  // 1. Roles Catalog State
  const [rolesList, setRolesList] = useState([])
  const [rolesLoading, setRolesLoading] = useState(false)
  const [roleSearchQuery, setRoleSearchQuery] = useState('')
  const [page, setPage] = useState(0)
  const [rowsPerPage, setRowsPerPage] = useState(10)

  // 2. Role Creation Dialog State
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const [roleName, setRoleName] = useState('')
  const [roleDescription, setRoleDescription] = useState('')
  const [creatingRole, setCreatingRole] = useState(false)

  // 3. Role Assignment State (Multi-User, Multi-Role)
  const [selectedUsers, setSelectedUsers] = useState([])
  const [selectedRoles, setSelectedRoles] = useState([])
  const [userSearchOptions, setUserSearchOptions] = useState([])
  const [userSearchLoading, setUserSearchLoading] = useState(false)
  const [assigningRoles, setAssigningRoles] = useState(false)

  // 4. Role Retrieval State (by User)
  const [lookupUser, setLookupUser] = useState(null)
  const [lookupUserOptions, setLookupUserOptions] = useState([])
  const [lookupUserLoading, setLookupUserLoading] = useState(false)
  const [retrievedUserRoles, setRetrievedUserRoles] = useState([])
  const [retrievingRoles, setRetrievingRoles] = useState(false)

  // 5. Delete Role Dialog State
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [roleToDelete, setRoleToDelete] = useState('')
  const [deletingRole, setDeletingRole] = useState(false)

  // MUI Notification Toast State
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success',
  })

  const showNotification = (message, severity = 'success') => {
    setSnackbar({
      open: true,
      message,
      severity,
    })
  }

  const handleCloseSnackbar = (event, reason) => {
    if (reason === 'clickaway') return
    setSnackbar((prev) => ({ ...prev, open: false }))
  }

  // Fetch Roles List (GET /task/users/roles)
  const fetchRoles = useCallback(
    async (query = '') => {
      setRolesLoading(true)
      try {
        const res = await roleAccessApiService.getRoles(keycloak, {
          q: query,
          page: 1,
          size: 100,
        })
        let items = []
        if (Array.isArray(res?.data)) {
          items = res.data
        } else if (Array.isArray(res?.data?.roles)) {
          items = res.data.roles
        } else if (Array.isArray(res?.data?.roleDetails)) {
          items = res.data.roleDetails
        } else if (Array.isArray(res?.roles)) {
          items = res.roles
        } else if (Array.isArray(res)) {
          items = res
        }
        setRolesList(items)
      } catch (err) {
        console.error('Error fetching roles:', err)
        showNotification(
          err.message || 'Failed to fetch roles from server',
          'error',
        )
      } finally {
        setRolesLoading(false)
      }
    },
    [keycloak],
  )

  useEffect(() => {
    fetchRoles()
  }, [fetchRoles])

  // Role Creation Handler (POST /task/users/roles)
  const handleCreateRole = async () => {
    if (!roleName.trim()) {
      showNotification('Please enter a role name', 'error')
      return
    }

    setCreatingRole(true)
    try {
      await roleAccessApiService.createRole(keycloak, {
        name: roleName.trim(),
        description: roleDescription.trim(),
      })
      showNotification(
        `Role "${roleName.trim()}" created successfully!`,
        'success',
      )
      setRoleName('')
      setRoleDescription('')
      setCreateDialogOpen(false)
      fetchRoles()
    } catch (err) {
      console.error('Error creating role:', err)
      showNotification(err.message || 'Failed to create role', 'error')
    } finally {
      setCreatingRole(false)
    }
  }

  // Role Deletion Handler (DELETE /task/users/roles/{roleName})
  const handleDeleteRole = async () => {
    if (!roleToDelete) return
    setDeletingRole(true)
    try {
      await roleAccessApiService.deleteRole(keycloak, roleToDelete)
      showNotification(
        `Role "${roleToDelete}" deleted successfully!`,
        'success',
      )
      setDeleteDialogOpen(false)
      setRoleToDelete('')
      fetchRoles()
    } catch (err) {
      console.error('Error deleting role:', err)
      showNotification(err.message || 'Failed to delete role', 'error')
    } finally {
      setDeletingRole(false)
    }
  }

  // Dynamic User Search for Assignment (100k+ Records)
  const handleUserSearchForAssign = async (searchText) => {
    if (searchText && searchText.length >= 2) {
      setUserSearchLoading(true)
      try {
        const res = await DataService.getUserBySearch(keycloak, searchText)
        const fetched = res?.data || (Array.isArray(res) ? res : [])
        const mapped = fetched.map((u) => ({
          id: u.id || u.userId || u.username,
          username: u.username,
          email: u.email || '',
        }))

        const combined = [...selectedUsers]
        mapped.forEach((opt) => {
          if (
            !combined.some((item) =>
              item.id && opt.id
                ? item.id === opt.id
                : item.username === opt.username,
            )
          ) {
            combined.push(opt)
          }
        })
        setUserSearchOptions(combined)
      } catch (err) {
        console.error('User search failed:', err)
      } finally {
        setUserSearchLoading(false)
      }
    }
  }

  // Role Assignment Handler (POST /task/users/roles/assign)
  const handleAssignRoles = async () => {
    if (selectedUsers.length === 0) {
      showNotification('Please select at least one user', 'warning')
      return
    }
    if (selectedRoles.length === 0) {
      showNotification('Please select at least one role', 'warning')
      return
    }

    setAssigningRoles(true)
    try {
      const roleNames = selectedRoles.map((r) =>
        typeof r === 'string' ? r : r.name || r.code || r,
      )

      const assignments = selectedUsers.map((u) => ({
        userId: u.id || u.username,
        roles: roleNames,
      }))

      const response = await roleAccessApiService.assignRoles(
        keycloak,
        assignments,
      )
      const successMsg =
        response?.message || 'Roles assigned successfully to all users.'
      showNotification(successMsg, 'success')

      if (lookupUser && selectedUsers.some((u) => u.id === lookupUser.id)) {
        handleRetrieveUserRoles(lookupUser)
      }

      setSelectedUsers([])
      setSelectedRoles([])
    } catch (err) {
      console.error('Error assigning roles:', err)
      showNotification(err.message || 'Failed to assign roles', 'error')
    } finally {
      setAssigningRoles(false)
    }
  }

  // Dynamic User Search for Lookup
  const handleUserSearchForLookup = async (searchText) => {
    if (searchText && searchText.length >= 2) {
      setLookupUserLoading(true)
      try {
        const res = await DataService.getUserBySearch(keycloak, searchText)
        const fetched = res?.data || (Array.isArray(res) ? res : [])
        const mapped = fetched.map((u) => ({
          id: u.id || u.userId || u.username,
          username: u.username,
          email: u.email || '',
        }))
        setLookupUserOptions(mapped)
      } catch (err) {
        console.error('User lookup search failed:', err)
      } finally {
        setLookupUserLoading(false)
      }
    }
  }

  // Role Retrieval Handler (GET /task/users/{userId}/roles)
  const handleRetrieveUserRoles = async (userObj) => {
    if (!userObj || !userObj.id) return

    setLookupUser(userObj)
    setRetrievingRoles(true)
    try {
      const rolesRes = await roleAccessApiService.getUserRoles(
        keycloak,
        userObj.id,
      )

      let fetchedRoles = []
      if (Array.isArray(rolesRes?.data?.roles)) {
        fetchedRoles = rolesRes.data.roles
      } else if (Array.isArray(rolesRes?.data?.roleDetails)) {
        fetchedRoles = rolesRes.data.roleDetails.map((r) => r.name || r)
      } else if (Array.isArray(rolesRes?.roles)) {
        fetchedRoles = rolesRes.roles
      } else if (Array.isArray(rolesRes?.data)) {
        fetchedRoles = rolesRes.data
      } else if (Array.isArray(rolesRes)) {
        fetchedRoles = rolesRes
      }

      setRetrievedUserRoles(fetchedRoles)
      showNotification(
        `Fetched ${fetchedRoles.length} role(s) for ${userObj.username}`,
        'info',
      )
    } catch (err) {
      console.error('Error retrieving user roles:', err)
      showNotification(
        err.message || 'Failed to retrieve roles for user',
        'error',
      )
      setRetrievedUserRoles([])
    } finally {
      setRetrievingRoles(false)
    }
  }

  // Filtered Roles list based on search query
  const filteredRolesList = rolesList.filter((r) => {
    const nameStr = typeof r === 'string' ? r : r.name || r.code || ''
    const descStr = typeof r === 'string' ? '' : r.description || ''
    return (
      nameStr.toLowerCase().includes(roleSearchQuery.toLowerCase()) ||
      descStr.toLowerCase().includes(roleSearchQuery.toLowerCase())
    )
  })

  // Pagination handlers for Table
  const handleChangePage = (event, newPage) => {
    setPage(newPage)
  }

  const handleChangeRowsPerPage = (event) => {
    setRowsPerPage(parseInt(event.target.value, 10))
    setPage(0)
  }

  const paginatedRoles = filteredRolesList.slice(
    page * rowsPerPage,
    page * rowsPerPage + rowsPerPage,
  )

  const rolesFormattedForSelect = rolesList.map((r) =>
    typeof r === 'string'
      ? { id: r, name: r }
      : { id: r.name || r.id, name: r.name || r.code },
  )

  return (
    <Box sx={{ width: '100%', pb: 4, fontFamily: 'inherit' }}>
      {/* 1. HERO WORKBENCH HEADER */}
      <Paper
        elevation={3}
        sx={{
          background: 'linear-gradient(135deg, #0f172a 0%, #1e1b4b 100%)',
          borderRadius: '16px',
          padding: '24px 28px',
          color: '#ffffff',
          marginBottom: '24px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '16px',
        }}
      >
        <Box>
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
              marginBottom: '6px',
            }}
          >
            <ShieldIcon sx={{ fontSize: 28, color: '#38bdf8' }} />
            <Typography
              variant="h5"
              sx={{ fontWeight: 800, letterSpacing: '-0.3px' }}
            >
              Role Access Management
            </Typography>
            <Chip
              label={`${rolesList.length} Active System Roles`}
              variant="outlined"
              sx={{
                fontWeight: 700,
                borderColor: '#38bdf8',
                color: '#38bdf8',
                height: 28,
              }}
            />
          </Box>
          <Typography variant="body2" sx={{ color: '#cbd5e1' }}>
            High-performance enterprise role management powered by Material-UI
            Data Table, Autocomplete & Dialogs.
          </Typography>
        </Box>

        <Box sx={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <Button
            variant="contained"
            color="primary"
            size="large"
            startIcon={<AddIcon />}
            sx={{
              fontWeight: 800,
              padding: '10px 20px',
              borderRadius: '10px',
              textTransform: 'none',
              boxShadow: '0 4px 14px rgba(99, 102, 241, 0.4)',
            }}
            onClick={() => setCreateDialogOpen(true)}
          >
            Create New Role
          </Button>
        </Box>
      </Paper>

      {/* 2. SECTION 1: SYSTEM ROLES CATALOG DATA TABLE */}
      <Paper
        elevation={1}
        sx={{
          borderRadius: '16px',
          padding: '24px',
          border: '1px solid #e2e8f0',
          marginBottom: '24px',
        }}
      >
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '16px',
            flexWrap: 'wrap',
            gap: '12px',
          }}
        >
          <Box>
            <Typography
              variant="h6"
              sx={{ fontWeight: 800, color: '#0f172a', fontSize: '1.05rem' }}
            >
              System Roles Catalog ({filteredRolesList.length})
            </Typography>
            <Typography variant="caption" sx={{ color: '#64748b' }}>
              Interactive table with searching, pagination, and role actions.
            </Typography>
          </Box>

          <Box sx={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            <TextField
              size="small"
              placeholder="Filter catalog roles..."
              value={roleSearchQuery}
              onChange={(e) => setRoleSearchQuery(e.target.value)}
              InputProps={{
                startAdornment: (
                  <SearchIcon sx={{ color: '#94a3b8', mr: 1, fontSize: 20 }} />
                ),
              }}
              sx={{ width: 240, '& .MuiOutlinedInput-root': { borderRadius: '8px' } }}
            />
            <Button
              variant="outlined"
              color="inherit"
              size="small"
              startIcon={<RefreshIcon />}
              onClick={() => fetchRoles(roleSearchQuery)}
              sx={{ textTransform: 'none', borderRadius: '8px', fontWeight: 600 }}
            >
              Refresh
            </Button>
          </Box>
        </Box>

        {/* MUI TABLE */}
        <TableContainer sx={{ border: '1px solid #e2e8f0', borderRadius: '10px', maxHeight: 400 }}>
          <Table stickyHeader size="small">
            <TableHead>
              <TableRow sx={{ '& th': { fontWeight: 800, backgroundColor: '#f8fafc', color: '#334155' } }}>
                <TableCell width="240">Role Name</TableCell>
                <TableCell>Description</TableCell>
                <TableCell width="220" align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rolesLoading ? (
                <TableRow>
                  <TableCell colSpan={3} align="center" sx={{ py: 4 }}>
                    <CircularProgress size={28} />
                    <Typography variant="body2" sx={{ mt: 1, color: '#64748b' }}>
                      Loading system roles...
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : paginatedRoles.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={3} align="center" sx={{ py: 4, color: '#94a3b8' }}>
                    No roles found matching criteria.
                  </TableCell>
                </TableRow>
              ) : (
                paginatedRoles.map((r, index) => {
                  const rName = typeof r === 'string' ? r : r.name || r.code || '-'
                  const rDesc = typeof r === 'string' ? 'System Realm Role' : r.description || 'System Realm Role'
                  return (
                    <TableRow key={index} hover>
                      <TableCell>
                        <Chip
                          label={rName}
                          color="primary"
                          variant="soft"
                          size="small"
                          sx={{
                            fontWeight: 700,
                            backgroundColor: '#e0f2fe',
                            color: '#0369a1',
                            borderRadius: '6px',
                          }}
                        />
                      </TableCell>
                      <TableCell sx={{ color: '#475569', fontSize: '0.875rem' }}>{rDesc}</TableCell>
                      <TableCell align="center">
                        <Button
                          size="small"
                          variant="text"
                          color="primary"
                          startIcon={<PersonAddIcon fontSize="small" />}
                          onClick={() => {
                            setSelectedRoles([r])
                            showNotification(
                              `Role "${rName}" selected for assignment below.`,
                              'info',
                            )
                          }}
                          sx={{ textTransform: 'none', fontWeight: 700, mr: 1 }}
                        >
                          Assign
                        </Button>
                        <Button
                          size="small"
                          variant="text"
                          color="error"
                          startIcon={<DeleteOutlineIcon fontSize="small" />}
                          onClick={() => {
                            setRoleToDelete(rName)
                            setDeleteDialogOpen(true)
                          }}
                          sx={{ textTransform: 'none', fontWeight: 700 }}
                        >
                          Delete
                        </Button>
                      </TableCell>
                    </TableRow>
                  )
                })
              )}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          rowsPerPageOptions={[5, 10, 25, 50]}
          component="div"
          count={filteredRolesList.length}
          rowsPerPage={rowsPerPage}
          page={page}
          onPageChange={handleChangePage}
          onRowsPerPageChange={handleChangeRowsPerPage}
        />
      </Paper>

      {/* 3. SECTION 2: BATCH ROLE ASSIGNMENT STUDIO (MUI AUTOCOMPLETE MULTISELECT) */}
      <Paper
        elevation={1}
        sx={{
          borderRadius: '16px',
          padding: '24px',
          border: '1px solid #e2e8f0',
          borderLeft: '6px solid #0284c7',
          marginBottom: '24px',
        }}
      >
        <Box sx={{ marginBottom: '16px' }}>
          <Typography
            variant="h6"
            sx={{ fontWeight: 800, color: '#0f172a', fontSize: '1.05rem', mb: 0.5 }}
          >
            Assign Roles to Users (Many-to-Many Batch Studio)
          </Typography>
          <Typography variant="caption" sx={{ color: '#64748b' }}>
            Dynamic search & fetch supporting 100k+ user records paired with
            multi-role assignment.
          </Typography>
        </Box>

        <Grid container spacing={2} sx={{ mb: 2 }}>
          {/* Target Users MUI Autocomplete */}
          <Grid item xs={12} md={6}>
            <Autocomplete
              multiple
              options={userSearchOptions}
              value={selectedUsers}
              onChange={(event, newValue) => setSelectedUsers(newValue)}
              onInputChange={(event, newInputValue) =>
                handleUserSearchForAssign(newInputValue)
              }
              getOptionLabel={(option) =>
                typeof option === 'string'
                  ? option
                  : option.email
                    ? `${option.username} (${option.email})`
                    : option.username
              }
              isOptionEqualToValue={(option, value) =>
                option.id === value.id || option.username === value.username
              }
              loading={userSearchLoading}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="1. Target Users (100k+ Search)"
                  placeholder="Type username or email..."
                  size="small"
                  InputProps={{
                    ...params.InputProps,
                    endAdornment: (
                      <React.Fragment>
                        {userSearchLoading ? (
                          <CircularProgress color="inherit" size={18} />
                        ) : null}
                        {params.InputProps.endAdornment}
                      </React.Fragment>
                    ),
                  }}
                />
              )}
              renderTags={(value, getTagProps) =>
                value.map((option, index) => (
                  <Chip
                    size="small"
                    label={typeof option === 'string' ? option : option.username}
                    {...getTagProps({ index })}
                    key={index}
                  />
                ))
              }
            />
          </Grid>

          {/* Target Roles MUI Autocomplete */}
          <Grid item xs={12} md={6}>
            <Autocomplete
              multiple
              options={rolesFormattedForSelect}
              value={selectedRoles}
              onChange={(event, newValue) => setSelectedRoles(newValue)}
              getOptionLabel={(option) =>
                typeof option === 'string' ? option : option.name
              }
              isOptionEqualToValue={(option, value) =>
                (option.id && value.id && option.id === value.id) ||
                option.name === value.name
              }
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="2. Target Roles"
                  placeholder="Choose roles to assign..."
                  size="small"
                />
              )}
              renderTags={(value, getTagProps) =>
                value.map((option, index) => (
                  <Chip
                    size="small"
                    color="primary"
                    label={typeof option === 'string' ? option : option.name}
                    {...getTagProps({ index })}
                    key={index}
                  />
                ))
              }
            />
          </Grid>
        </Grid>

        <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Button
            variant="contained"
            color="primary"
            size="large"
            disabled={
              selectedUsers.length === 0 ||
              selectedRoles.length === 0 ||
              assigningRoles
            }
            onClick={handleAssignRoles}
            sx={{
              fontWeight: 800,
              padding: '10px 24px',
              borderRadius: '8px',
              textTransform: 'none',
            }}
          >
            {assigningRoles
              ? 'Assigning Roles...'
              : `Assign Roles (${selectedUsers.length} Users × ${selectedRoles.length} Roles)`}
          </Button>
        </Box>
      </Paper>

      {/* 4. SECTION 3: USER ROLE INSPECTOR (MUI AUTOCOMPLETE & CHIPS) */}
      <Paper
        elevation={1}
        sx={{
          borderRadius: '16px',
          padding: '24px',
          border: '1px solid #e2e8f0',
          borderLeft: '6px solid #10b981',
        }}
      >
        <Box sx={{ marginBottom: '16px' }}>
          <Typography
            variant="h6"
            sx={{ fontWeight: 800, color: '#0f172a', fontSize: '1.05rem', mb: 0.5 }}
          >
            User Role Inspector (Active Permissions)
          </Typography>
          <Typography variant="caption" sx={{ color: '#64748b' }}>
            Lookup any user to fetch their active role assignments via GET
            /task/users/&#123;userId&#125;/roles.
          </Typography>
        </Box>

        <Box
          sx={{
            display: 'flex',
            gap: '12px',
            alignItems: 'center',
            marginBottom: '16px',
            maxWidth: '550px',
          }}
        >
          <Autocomplete
            options={lookupUserOptions}
            getOptionLabel={(option) =>
              typeof option === 'string' ? option : option.username
            }
            onChange={(event, newValue) => {
              if (newValue) {
                handleRetrieveUserRoles(newValue)
              }
            }}
            onInputChange={(event, newInputValue) =>
              handleUserSearchForLookup(newInputValue)
            }
            loading={lookupUserLoading}
            sx={{ flexGrow: 1 }}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Search User to Inspect"
                placeholder="Type username..."
                size="small"
              />
            )}
          />
          {lookupUser && (
            <Button
              variant="outlined"
              color="success"
              onClick={() => handleRetrieveUserRoles(lookupUser)}
              disabled={retrievingRoles}
              sx={{ textTransform: 'none', fontWeight: 700 }}
            >
              Refresh
            </Button>
          )}
        </Box>

        {lookupUser ? (
          <Paper
            variant="outlined"
            sx={{
              padding: '16px',
              borderRadius: '10px',
              backgroundColor: '#f8fafc',
              border: '1px solid #e2e8f0',
            }}
          >
            <Typography
              variant="body2"
              sx={{ fontWeight: 700, color: '#0f172a', marginBottom: '10px' }}
            >
              Assigned Roles for{' '}
              <span style={{ color: '#059669', fontWeight: 800 }}>
                {lookupUser.username}
              </span>{' '}
              (ID: {lookupUser.id}):
            </Typography>

            {retrievingRoles ? (
              <Typography variant="caption" sx={{ color: '#64748b' }}>
                Fetching user roles...
              </Typography>
            ) : !Array.isArray(retrievedUserRoles) ||
              retrievedUserRoles.length === 0 ? (
              <Typography
                variant="caption"
                sx={{ color: '#94a3b8', fontStyle: 'italic' }}
              >
                No active roles assigned to this user.
              </Typography>
            ) : (
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                {retrievedUserRoles.map((r, idx) => {
                  const rName =
                    typeof r === 'string' ? r : r?.name || r?.code || String(r)
                  return (
                    <Chip
                      key={idx}
                      label={rName}
                      color="success"
                      size="small"
                      sx={{ fontWeight: 700 }}
                    />
                  )
                })}
              </Box>
            )}
          </Paper>
        ) : (
          <Typography
            variant="caption"
            sx={{ color: '#94a3b8', fontStyle: 'italic' }}
          >
            Select a user above to inspect their assigned roles.
          </Typography>
        )}
      </Paper>

      {/* 5. MUI CREATE ROLE DIALOG */}
      <Dialog
        open={createDialogOpen}
        onClose={() => setCreateDialogOpen(false)}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle sx={{ fontWeight: 800 }}>Create New System Role</DialogTitle>
        <DialogContent dividers>
          <TextField
            label="Role Name *"
            placeholder="e.g., gms_business_head"
            fullWidth
            margin="normal"
            size="small"
            value={roleName}
            onChange={(e) => setRoleName(e.target.value)}
          />
          <TextField
            label="Description"
            placeholder="Optional role scope description"
            fullWidth
            multiline
            rows={3}
            margin="normal"
            size="small"
            value={roleDescription}
            onChange={(e) => setRoleDescription(e.target.value)}
          />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setCreateDialogOpen(false)} color="inherit">
            Cancel
          </Button>
          <Button
            variant="contained"
            color="primary"
            disabled={!roleName.trim() || creatingRole}
            onClick={handleCreateRole}
            sx={{ fontWeight: 700 }}
          >
            {creatingRole ? 'Creating...' : 'Create Role'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* 6. MUI DELETE ROLE DIALOG */}
      <Dialog
        open={deleteDialogOpen}
        onClose={() => setDeleteDialogOpen(false)}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle sx={{ fontWeight: 800, color: '#ef4444' }}>
          Confirm Role Deletion
        </DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" sx={{ color: '#334155' }}>
            Are you sure you want to delete role{' '}
            <strong style={{ color: '#ef4444' }}>
              &quot;{roleToDelete}&quot;
            </strong>
            ? This action will permanently remove the role from the system.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setDeleteDialogOpen(false)} color="inherit">
            Cancel
          </Button>
          <Button
            variant="contained"
            color="error"
            disabled={deletingRole}
            onClick={handleDeleteRole}
            sx={{ fontWeight: 700 }}
          >
            {deletingRole ? 'Deleting...' : 'Delete Role'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* 7. MUI SNACKBAR NOTIFICATIONS */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={4500}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
        sx={{ mt: 7 }}
      >
        <Alert
          onClose={handleCloseSnackbar}
          severity={snackbar.severity}
          variant="filled"
          sx={{ width: '100%', fontWeight: 700, boxShadow: 3 }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  )
}

export default RoleAccessManagement
