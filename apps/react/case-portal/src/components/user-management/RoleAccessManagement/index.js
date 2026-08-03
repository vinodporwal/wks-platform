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
  Grid,
  CircularProgress,
  Tabs,
  Tab,
  Checkbox,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import RefreshIcon from '@mui/icons-material/Refresh'
import SearchIcon from '@mui/icons-material/Search'
import PersonAddIcon from '@mui/icons-material/PersonAdd'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'

import { DataGrid } from '@mui/x-data-grid'
import { DataService } from 'services/DataService'
import { roleAccessApiService } from 'services/roleAccessApiService'

const RoleAccessManagement = ({ keycloak }) => {
  // 1. Roles Catalog State
  const [rolesList, setRolesList] = useState([])
  const [rolesLoading, setRolesLoading] = useState(false)
  const [roleSearchQuery, setRoleSearchQuery] = useState('')

  // 2. Create Role State (Inline Form & Layout Tabs)
  const [createRoleLayoutTab, setCreateRoleLayoutTab] = useState(0)
  const [roleName, setRoleName] = useState('')
  const [roleDescription, setRoleDescription] = useState('')
  const [creatingRole, setCreatingRole] = useState(false)

  // 3. Role Assignment State (Multi-User, Multi-Role Studio)
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

  const showNotification = useCallback((message, severity = 'success') => {
    setSnackbar({
      open: true,
      message,
      severity,
    })
  }, [])

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
    [keycloak, showNotification],
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

  // Dynamic User Search for Assignment
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

  // Unassign Role Handler
  const handleUnassignRoleFromUser = async (userId, roleName) => {
    if (!userId || !roleName) return
    setRetrievingRoles(true)
    try {
      await roleAccessApiService.unassignRoleFromUser(keycloak, userId, roleName)
      showNotification(
        `Role "${roleName}" unassigned from ${lookupUser?.username || userId} successfully!`,
        'success',
      )
      handleRetrieveUserRoles(lookupUser)
    } catch (err) {
      console.error('Error unassigning role:', err)
      showNotification(
        err.message || `Failed to unassign role "${roleName}"`,
        'error',
      )
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

  // Format roles for Autocomplete selection
  const rolesFormattedForSelect = rolesList.map((r) =>
    typeof r === 'string'
      ? { id: r, name: r }
      : { id: r.name || r.id, name: r.name || r.code },
  )

  // Map data for MUI DataGrid
  const gridRows = filteredRolesList.map((r, index) => {
    const nameStr = typeof r === 'string' ? r : r.name || r.code || '-'
    const descStr =
      typeof r === 'string'
        ? 'System Realm Role'
        : r.description || 'System Realm Role'
    return {
      id: typeof r === 'string' ? r : r.id || r.name || index,
      name: nameStr,
      description: descStr,
      rawRole: r,
    }
  })

  const columns = [
    {
      field: 'name',
      headerName: 'Role Name',
      flex: 1,
      minWidth: 180,
      renderCell: (params) => (
        <Chip
          label={params.value}
          size="small"
          sx={{
            fontWeight: 700,
            backgroundColor: '#e0f2fe',
            color: '#0369a1',
            borderRadius: '4px',
            fontSize: '0.75rem',
            height: '22px',
          }}
        />
      ),
    },
    {
      field: 'description',
      headerName: 'Description',
      flex: 2,
      minWidth: 260,
      renderCell: (params) => (
        <Typography
          variant="body2"
          sx={{ color: '#475569', fontSize: '0.8rem' }}
        >
          {params.value}
        </Typography>
      ),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      flex: 1,
      minWidth: 180,
      sortable: false,
      filterable: false,
      renderCell: (params) => {
        const rName = params.row.name
        return (
          <Box sx={{ display: 'flex', gap: 0.8, alignItems: 'center' }}>
            <Button
              size="small"
              variant="outlined"
              color="primary"
              startIcon={<PersonAddIcon style={{ fontSize: 14 }} />}
              onClick={() => {
                setSelectedRoles([params.row.rawRole])
                showNotification(
                  `Role "${rName}" selected for assignment below.`,
                  'info',
                )
              }}
              sx={{
                textTransform: 'none',
                fontWeight: 700,
                borderRadius: '4px',
                fontSize: '0.7rem',
                py: 0.2,
                px: 1,
                minWidth: 'auto',
              }}
            >
              Assign
            </Button>
            <Button
              size="small"
              variant="outlined"
              color="error"
              startIcon={<DeleteOutlineIcon style={{ fontSize: 14 }} />}
              onClick={() => {
                setRoleToDelete(rName)
                setDeleteDialogOpen(true)
              }}
              sx={{
                textTransform: 'none',
                fontWeight: 700,
                borderRadius: '4px',
                fontSize: '0.7rem',
                py: 0.2,
                px: 1,
                minWidth: 'auto',
              }}
            >
              Delete
            </Button>
          </Box>
        )
      },
    },
  ]

  return (
    <Box sx={{ width: '100%', pl: 0, ml: 0, pr: 3, pb: 2, fontFamily: 'inherit' }}>
      {/* 1. CREATE NEW ROLE INLINE CELL PANEL (MODERN CONSTRAINED TEXTFIELDS) */}
      <Paper
        elevation={0}
        sx={{
          borderRadius: '8px',
          padding: '12px 16px',
          border: '1px solid #e2e8f0',
          backgroundColor: '#ffffff',
          marginBottom: '12px',
          maxWidth: '920px',
        }}
      >
        <Typography
          variant="subtitle2"
          sx={{ fontWeight: 700, color: '#0f172a', fontSize: '0.875rem', mb: 1.5 }}
        >
          Create New Role
        </Typography>

        <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center', flexWrap: 'wrap' }}>
          <TextField
            label="Role Name *"
            placeholder="e.g. gms_business_head"
            size="small"
            value={roleName}
            onChange={(e) => setRoleName(e.target.value)}
            sx={{
              width: 250,
              '& .MuiOutlinedInput-root': {
                borderRadius: '8px',
                backgroundColor: '#f8fafc',
                transition: 'all 0.2s ease-in-out',
                '& fieldset': {
                  borderColor: '#cbd5e1',
                },
                '&:hover fieldset': {
                  borderColor: '#94a3b8',
                },
                '&.Mui-focused': {
                  backgroundColor: '#ffffff',
                  '& fieldset': {
                    borderColor: '#0284c7',
                    borderWidth: '1.5px',
                  },
                  boxShadow: '0 0 0 3px rgba(2, 132, 199, 0.12)',
                },
              },
              '& .MuiInputBase-input': {
                fontSize: '0.8rem',
                py: 0.9,
                fontWeight: 600,
                color: '#0f172a !important',
                '&::placeholder': {
                  color: '#64748b',
                  opacity: 1,
                },
              },
              '& .MuiInputLabel-root': {
                fontSize: '0.8rem',
                '&.Mui-focused': {
                  color: '#0284c7',
                  fontWeight: 700,
                },
              },
            }}
          />
          <TextField
            label="Description"
            placeholder="Optional role description"
            size="small"
            value={roleDescription}
            onChange={(e) => setRoleDescription(e.target.value)}
            sx={{
              width: 330,
              '& .MuiOutlinedInput-root': {
                borderRadius: '8px',
                backgroundColor: '#f8fafc',
                transition: 'all 0.2s ease-in-out',
                '& fieldset': {
                  borderColor: '#cbd5e1',
                },
                '&:hover fieldset': {
                  borderColor: '#94a3b8',
                },
                '&.Mui-focused': {
                  backgroundColor: '#ffffff',
                  '& fieldset': {
                    borderColor: '#0284c7',
                    borderWidth: '1.5px',
                  },
                  boxShadow: '0 0 0 3px rgba(2, 132, 199, 0.12)',
                },
              },
              '& .MuiInputBase-input': {
                fontSize: '0.8rem',
                py: 0.9,
                fontWeight: 600,
                color: '#0f172a !important',
                '&::placeholder': {
                  color: '#64748b',
                  opacity: 1,
                },
              },
              '& .MuiInputLabel-root': {
                fontSize: '0.8rem',
                '&.Mui-focused': {
                  color: '#0284c7',
                  fontWeight: 700,
                },
              },
            }}
          />
          <Button
            variant="contained"
            color="primary"
            size="small"
            disabled={!roleName.trim() || creatingRole}
            onClick={handleCreateRole}
            startIcon={
              creatingRole ? (
                <CircularProgress size={14} color="inherit" />
              ) : (
                <AddIcon style={{ fontSize: 16 }} />
              )
            }
            sx={{
              fontWeight: 700,
              borderRadius: '8px',
              textTransform: 'none',
              px: 2.5,
              height: '38px',
              fontSize: '0.78rem',
              boxShadow: 'none',
              backgroundColor: '#0284c7',
              '&:hover': {
                backgroundColor: '#0369a1',
              },
            }}
          >
            {creatingRole ? 'Creating...' : 'Create Role'}
          </Button>
          {(roleName || roleDescription) && (
            <Button
              variant="outlined"
              color="inherit"
              size="small"
              onClick={() => {
                setRoleName('')
                setRoleDescription('')
              }}
              sx={{
                borderRadius: '8px',
                textTransform: 'none',
                height: '38px',
                fontSize: '0.78rem',
                color: '#64748b',
                borderColor: '#cbd5e1',
              }}
            >
              Clear
            </Button>
          )}
        </Box>
      </Paper>

      {/* 2. SYSTEM ROLES CATALOG CELL DATA GRID (COLORED HEADERS, ALTERNATE ROWS, VERTICAL SEPARATORS) */}
      <Paper
        elevation={0}
        sx={{
          borderRadius: '8px',
          padding: '14px 16px',
          border: '1px solid #e2e8f0',
          backgroundColor: '#ffffff',
          marginBottom: '12px',
          maxWidth: '920px',
        }}
      >
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '12px',
            flexWrap: 'wrap',
            gap: '8px',
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography
              variant="subtitle2"
              sx={{ fontWeight: 700, color: '#0f172a', fontSize: '0.875rem' }}
            >
              System Roles Catalog
            </Typography>
            <Chip
              label={filteredRolesList.length}
              size="small"
              sx={{
                backgroundColor: '#0284c7',
                color: '#ffffff',
                fontWeight: 800,
                fontSize: '0.72rem',
                height: '20px',
                px: 0.5,
              }}
            />
          </Box>

          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
            <TextField
              size="small"
              placeholder="Filter roles..."
              value={roleSearchQuery}
              onChange={(e) => setRoleSearchQuery(e.target.value)}
              InputProps={{
                startAdornment: (
                  <SearchIcon sx={{ color: '#94a3b8', mr: 0.8, fontSize: 16 }} />
                ),
              }}
              sx={{
                width: 200,
                '& .MuiOutlinedInput-root': {
                  borderRadius: '6px',
                  backgroundColor: '#f8fafc',
                  '& fieldset': { borderColor: '#cbd5e1' },
                  '&:hover fieldset': { borderColor: '#94a3b8' },
                  '&.Mui-focused': {
                    backgroundColor: '#ffffff',
                    '& fieldset': { borderColor: '#0284c7', borderWidth: '1.5px' },
                    boxShadow: '0 0 0 3px rgba(2, 132, 199, 0.12)',
                  },
                },
                '& .MuiInputBase-input': {
                  fontSize: '0.78rem',
                  py: 0.7,
                  color: '#0f172a !important',
                  fontWeight: 600,
                  '&::placeholder': {
                    color: '#64748b',
                    opacity: 1,
                  },
                },
              }}
            />
            <Button
              variant="outlined"
              color="inherit"
              size="small"
              startIcon={<RefreshIcon style={{ fontSize: 14 }} />}
              onClick={() => fetchRoles(roleSearchQuery)}
              sx={{
                textTransform: 'none',
                borderRadius: '6px',
                fontWeight: 600,
                fontSize: '0.75rem',
                color: '#475569',
                borderColor: '#cbd5e1',
                height: '32px',
                '&:hover': { backgroundColor: '#f1f5f9' },
              }}
            >
              Refresh
            </Button>
          </Box>
        </Box>

        {/* ENHANCED MUI DATAGRID */}
        <Box sx={{ height: 350, width: '100%' }}>
          <DataGrid
            rows={gridRows}
            columns={columns}
            loading={rolesLoading}
            density="compact"
            rowHeight={38}
            columnHeaderHeight={36}
            disableRowSelectionOnClick
            hideFooterPagination
            getRowClassName={(params) =>
              params.indexRelativeToCurrentPage % 2 === 0 ? 'even-row' : 'odd-row'
            }
            sx={{
              border: '1px solid #cbd5e1',
              borderRadius: '6px',
              overflow: 'hidden',

              // Light Column Headers (#f1f5f9 light slate blue header)
              '& .MuiDataGrid-columnHeaders': {
                backgroundColor: '#f1f5f9',
                color: '#0f172a',
                fontWeight: 800,
                fontSize: '0.8rem',
                borderBottom: '2px solid #0284c7',
              },
              '& .MuiDataGrid-columnHeader': {
                borderRight: '1px solid #cbd5e1', // Header vertical separator
              },
              '& .MuiDataGrid-columnHeaderTitle': {
                fontWeight: 800,
                color: '#0f172a',
                letterSpacing: '0.3px',
              },
              '& .MuiDataGrid-iconSeparator': {
                color: '#94a3b8',
              },
              '& .MuiDataGrid-sortIcon': {
                color: '#64748b',
              },

              // Alternate Row Background Colors & Vertical Separators
              '& .even-row': {
                backgroundColor: '#ffffff',
              },
              '& .odd-row': {
                backgroundColor: '#f8fafc',
              },
              '& .MuiDataGrid-row': {
                marginTop: '2px',
                marginBottom: '2px',
                borderRadius: '4px',
                '&:hover': {
                  backgroundColor: '#e0f2fe !important', // Soft blue row hover
                },
              },

              // Vertical Column Cell Separators
              '& .MuiDataGrid-cell': {
                display: 'flex',
                alignItems: 'center',
                fontSize: '0.8rem',
                borderRight: '1px solid #e2e8f0', // Vertical separator between cells
                borderBottom: '1px solid #e2e8f0',
              },
            }}
          />
        </Box>
      </Paper>

      {/* 3. ASSIGN ROLES TO USERS CELL PANEL (CONSTRAINED 920px WIDTH, MODERN AUTOCOMPLETE, INSTANT API SEARCH) */}
      <Paper
        elevation={0}
        sx={{
          borderRadius: '8px',
          padding: '14px 16px',
          border: '1px solid #e2e8f0',
          backgroundColor: '#ffffff',
          marginBottom: '12px',
          maxWidth: '920px',
        }}
      >
        <Typography
          variant="subtitle2"
          sx={{ fontWeight: 700, color: '#0f172a', fontSize: '0.875rem', mb: 1.5 }}
        >
          Assign Roles to Users
        </Typography>

        <Grid container spacing={2} sx={{ mb: 1.5 }}>
          {/* Target Users Autocomplete */}
          <Grid item xs={12} md={6}>
            <Autocomplete
              multiple
              disableCloseOnSelect
              size="small"
              options={userSearchOptions}
              value={selectedUsers}
              onChange={(event, newValue) => setSelectedUsers(newValue)}
              onInputChange={(event, newInputValue) =>
                handleUserSearchForAssign(newInputValue)
              }
              filterOptions={(options) => options} // Render API search results instantly without local filtering lag
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
              renderOption={(props, option, { selected }) => (
                <li {...props} style={{ padding: '6px 10px', cursor: 'pointer' }}>
                  <Checkbox
                    size="small"
                    checked={selected}
                    sx={{
                      mr: 1,
                      color: '#94a3b8',
                      p: 0.5,
                      '&.Mui-checked': {
                        color: '#0284c7',
                      },
                    }}
                  />
                  <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                    <Typography
                      variant="body2"
                      sx={{
                        fontSize: '0.8rem',
                        fontWeight: selected ? 700 : 600,
                        color: selected ? '#0284c7' : '#0f172a',
                      }}
                    >
                      {typeof option === 'string' ? option : option.username}
                    </Typography>
                    {option.email && (
                      <Typography
                        variant="caption"
                        sx={{ fontSize: '0.72rem', color: '#64748b' }}
                      >
                        {option.email}
                      </Typography>
                    )}
                  </Box>
                </li>
              )}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Select Users"
                  placeholder="Type username to search..."
                  size="small"
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '8px',
                      backgroundColor: '#f8fafc',
                      '& fieldset': { borderColor: '#cbd5e1' },
                      '&:hover fieldset': { borderColor: '#94a3b8' },
                      '&.Mui-focused': {
                        backgroundColor: '#ffffff',
                        '& fieldset': { borderColor: '#0284c7', borderWidth: '1.5px' },
                        boxShadow: '0 0 0 3px rgba(2, 132, 199, 0.12)',
                      },
                    },
                    '& .MuiInputBase-input': {
                      fontSize: '0.8rem',
                      color: '#0f172a !important',
                      fontWeight: 600,
                      '&::placeholder': {
                        color: '#64748b',
                        opacity: 1,
                      },
                    },
                    '& .MuiInputLabel-root': {
                      fontSize: '0.8rem',
                      '&.Mui-focused': { color: '#0284c7', fontWeight: 700 },
                    },
                  }}
                  InputProps={{
                    ...params.InputProps,
                    endAdornment: (
                      <React.Fragment>
                        {userSearchLoading ? (
                          <CircularProgress color="inherit" size={14} />
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
                    variant="outlined"
                    color="primary"
                    label={
                      typeof option === 'string' ? option : option.username
                    }
                    {...getTagProps({ index })}
                    key={index}
                    sx={{
                      borderRadius: '4px',
                      fontWeight: 700,
                      fontSize: '0.72rem',
                      height: '22px',
                      backgroundColor: '#e0f2fe',
                      borderColor: '#0284c7',
                      color: '#0369a1',
                    }}
                  />
                ))
              }
            />
          </Grid>

          {/* Target Roles Autocomplete with Stylish Checkboxes */}
          <Grid item xs={12} md={6}>
            <Autocomplete
              multiple
              disableCloseOnSelect
              size="small"
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
              renderOption={(props, option, { selected }) => (
                <li {...props} style={{ padding: '4px 10px', cursor: 'pointer' }}>
                  <Checkbox
                    size="small"
                    checked={selected}
                    sx={{
                      mr: 1,
                      color: '#94a3b8',
                      p: 0.5,
                      '&.Mui-checked': {
                        color: '#0284c7',
                      },
                    }}
                  />
                  <Typography
                    variant="body2"
                    sx={{
                      fontSize: '0.8rem',
                      fontWeight: selected ? 700 : 500,
                      color: selected ? '#0284c7' : '#1e293b',
                    }}
                  >
                    {typeof option === 'string' ? option : option.name}
                  </Typography>
                </li>
              )}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Select Roles"
                  placeholder="Choose roles..."
                  size="small"
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '8px',
                      backgroundColor: '#f8fafc',
                      '& fieldset': { borderColor: '#cbd5e1' },
                      '&:hover fieldset': { borderColor: '#94a3b8' },
                      '&.Mui-focused': {
                        backgroundColor: '#ffffff',
                        '& fieldset': { borderColor: '#0284c7', borderWidth: '1.5px' },
                        boxShadow: '0 0 0 3px rgba(2, 132, 199, 0.12)',
                      },
                    },
                    '& .MuiInputBase-input': {
                      fontSize: '0.8rem',
                      color: '#0f172a !important',
                      fontWeight: 600,
                      '&::placeholder': {
                        color: '#64748b',
                        opacity: 1,
                      },
                    },
                    '& .MuiInputLabel-root': {
                      fontSize: '0.8rem',
                      '&.Mui-focused': { color: '#0284c7', fontWeight: 700 },
                    },
                  }}
                />
              )}
              renderTags={(value, getTagProps) =>
                value.map((option, index) => (
                  <Chip
                    size="small"
                    color="info"
                    variant="soft"
                    label={typeof option === 'string' ? option : option.name}
                    {...getTagProps({ index })}
                    key={index}
                    sx={{
                      borderRadius: '4px',
                      fontWeight: 700,
                      backgroundColor: '#e0f2fe',
                      color: '#0369a1',
                      fontSize: '0.72rem',
                      height: '22px',
                    }}
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
            size="small"
            disabled={
              selectedUsers.length === 0 ||
              selectedRoles.length === 0 ||
              assigningRoles
            }
            onClick={handleAssignRoles}
            startIcon={
              assigningRoles ? (
                <CircularProgress size={14} color="inherit" />
              ) : (
                <PersonAddIcon style={{ fontSize: 16 }} />
              )
            }
            sx={{
              fontWeight: 700,
              padding: '6px 18px',
              borderRadius: '8px',
              textTransform: 'none',
              fontSize: '0.78rem',
              boxShadow: 'none',
              backgroundColor: '#0284c7',
              '&:hover': { backgroundColor: '#0369a1' },
            }}
          >
            {assigningRoles ? 'Assigning...' : 'Assign Selected Roles'}
          </Button>
        </Box>
      </Paper>

      {/* 4. USER ROLE INSPECTOR CELL PANEL (CONSTRAINED 920px WIDTH) */}
      <Paper
        elevation={0}
        sx={{
          borderRadius: '8px',
          padding: '14px 16px',
          border: '1px solid #e2e8f0',
          backgroundColor: '#ffffff',
          maxWidth: '920px',
        }}
      >
        <Typography
          variant="subtitle2"
          sx={{ fontWeight: 700, color: '#0f172a', fontSize: '0.875rem', mb: 1.5 }}
        >
          User Role Inspector
        </Typography>

        <Box
          sx={{
            display: 'flex',
            gap: '10px',
            alignItems: 'center',
            marginBottom: '12px',
            maxWidth: '500px',
          }}
        >
          <Autocomplete
            options={lookupUserOptions}
            size="small"
            filterOptions={(options) => options} // Render API search results instantly without local filtering lag
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
                label="Select User to Inspect"
                placeholder="Search username..."
                size="small"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: '8px',
                    backgroundColor: '#f8fafc',
                    '& fieldset': { borderColor: '#cbd5e1' },
                    '&:hover fieldset': { borderColor: '#94a3b8' },
                    '&.Mui-focused': {
                      backgroundColor: '#ffffff',
                      '& fieldset': { borderColor: '#0284c7', borderWidth: '1.5px' },
                      boxShadow: '0 0 0 3px rgba(2, 132, 199, 0.12)',
                    },
                  },
                  '& .MuiInputBase-input': {
                    fontSize: '0.8rem',
                    color: '#0f172a !important',
                    fontWeight: 600,
                    '&::placeholder': {
                      color: '#64748b',
                      opacity: 1,
                    },
                  },
                  '& .MuiInputLabel-root': {
                    fontSize: '0.8rem',
                    '&.Mui-focused': { color: '#0284c7', fontWeight: 700 },
                  },
                }}
              />
            )}
          />
          {lookupUser && (
            <Button
              variant="outlined"
              color="primary"
              size="small"
              onClick={() => handleRetrieveUserRoles(lookupUser)}
              disabled={retrievingRoles}
              startIcon={<RefreshIcon style={{ fontSize: 14 }} />}
              sx={{
                textTransform: 'none',
                fontWeight: 600,
                borderRadius: '6px',
                height: '36px',
                fontSize: '0.78rem',
              }}
            >
              Refresh
            </Button>
          )}
        </Box>

        {lookupUser ? (
          <Paper
            variant="outlined"
            sx={{
              padding: '12px 14px',
              borderRadius: '6px',
              backgroundColor: '#f8fafc',
              border: '1px solid #e2e8f0',
            }}
          >
            <Typography
              variant="body2"
              sx={{ fontWeight: 700, color: '#0f172a', marginBottom: '8px', fontSize: '0.8rem' }}
            >
              Assigned Roles for{' '}
              <span style={{ color: '#0284c7', fontWeight: 800 }}>
                {lookupUser.username}
              </span>
              :
            </Typography>

            {retrievingRoles ? (
              <Box
                sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 0.5 }}
              >
                <CircularProgress size={14} />
                <Typography variant="caption" sx={{ color: '#64748b' }}>
                  Fetching roles...
                </Typography>
              </Box>
            ) : !Array.isArray(retrievedUserRoles) ||
              retrievedUserRoles.length === 0 ? (
              <Typography
                variant="caption"
                sx={{ color: '#94a3b8', fontStyle: 'italic', fontSize: '0.75rem' }}
              >
                No active roles assigned to this user.
              </Typography>
            ) : (
              <Box
                sx={{
                  display: 'flex',
                  flexWrap: 'wrap',
                  gap: '6px',
                  alignItems: 'center',
                }}
              >
                {retrievedUserRoles.map((r, idx) => {
                  const rName =
                    typeof r === 'string'
                      ? r
                      : r?.name || r?.code || String(r)
                  return (
                    <Chip
                      key={idx}
                      label={rName}
                      size="small"
                      onDelete={() => handleUnassignRoleFromUser(lookupUser.id, rName)}
                      sx={{
                        fontWeight: 700,
                        backgroundColor: '#e0f2fe',
                        color: '#0369a1',
                        borderRadius: '4px',
                        fontSize: '0.75rem',
                        height: '24px',
                        border: '1px solid #bae6fd',
                        '& .MuiChip-deleteIcon': {
                          color: '#0284c7',
                          fontSize: 14,
                          '&:hover': {
                            color: '#ef4444',
                          },
                        },
                      }}
                    />
                  )
                })}
              </Box>
            )}
          </Paper>
        ) : (
          <Typography
            variant="caption"
            sx={{ color: '#94a3b8', fontStyle: 'italic', fontSize: '0.75rem' }}
          >
            Select a user above to inspect their active assigned roles.
          </Typography>
        )}
      </Paper>

      {/* 5. MUI DELETE ROLE DIALOG */}
      <Dialog
        open={deleteDialogOpen}
        onClose={() => setDeleteDialogOpen(false)}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle sx={{ fontWeight: 800, color: '#ef4444', fontSize: '1rem' }}>
          Confirm Role Deletion
        </DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" sx={{ color: '#334155', fontSize: '0.85rem' }}>
            Are you sure you want to delete role{' '}
            <strong style={{ color: '#ef4444' }}>
              &quot;{roleToDelete}&quot;
            </strong>
            ? This action will permanently remove the role from the system.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 1.5 }}>
          <Button onClick={() => setDeleteDialogOpen(false)} color="inherit" size="small" sx={{ textTransform: 'none' }}>
            Cancel
          </Button>
          <Button
            variant="contained"
            color="error"
            size="small"
            disabled={deletingRole}
            onClick={handleDeleteRole}
            sx={{ fontWeight: 700, textTransform: 'none' }}
          >
            {deletingRole ? 'Deleting...' : 'Delete Role'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* 6. MUI SNACKBAR NOTIFICATIONS */}
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


