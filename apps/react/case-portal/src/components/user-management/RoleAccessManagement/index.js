import React, { useState, useEffect, useCallback } from 'react'
import { Box, Snackbar, Alert } from '@mui/material'

import { DataService } from 'services/DataService'
import { roleAccessApiService } from 'services/roleAccessApiService'

import {
  extractRoleItems,
  extractUserRoles,
  filterRoles,
  formatRolesForSelect,
} from './utilities/roleUtils'

import CreateRolePanel from './utilities/CreateRolePanel'
import SystemRolesCatalogPanel from './utilities/SystemRolesCatalogPanel'
import AssignRolesPanel from './utilities/AssignRolesPanel'
import UserRoleInspectorPanel from './utilities/UserRoleInspectorPanel'
import DeleteRoleDialog from './utilities/DeleteRoleDialog'
import UnassignRoleDialog from './utilities/UnassignRoleDialog'

const RoleAccessManagement = ({ keycloak }) => {
  // 1. Roles Catalog State
  const [rolesList, setRolesList] = useState([])
  const [rolesLoading, setRolesLoading] = useState(false)
  const [roleSearchQuery, setRoleSearchQuery] = useState('')

  // 2. Create Role State
  const [roleName, setRoleName] = useState('')
  const [roleDescription, setRoleDescription] = useState('')
  const [creatingRole, setCreatingRole] = useState(false)

  // 3. Role Assignment State
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

  // 6. Unassign Role Confirmation Dialog State
  const [unassignDialogOpen, setUnassignDialogOpen] = useState(false)
  const [roleToUnassign, setRoleToUnassign] = useState('')

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
        const items = extractRoleItems(res)
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
    if (!userObj || !userObj.id) {
      setLookupUser(null)
      setRetrievedUserRoles([])
      return
    }

    setLookupUser(userObj)
    setRetrievingRoles(true)
    try {
      const rolesRes = await roleAccessApiService.getUserRoles(
        keycloak,
        userObj.id,
      )
      const fetchedRoles = extractUserRoles(rolesRes)

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

  // Open Unassign Confirmation Dialog
  const handleOpenUnassignDialog = (roleName) => {
    setRoleToUnassign(roleName)
    setUnassignDialogOpen(true)
  }

  // Unassign Role Handler (called after confirmation)
  const handleConfirmUnassignRole = async () => {
    if (!lookupUser?.id || !roleToUnassign) return
    setUnassignDialogOpen(false)
    setRetrievingRoles(true)
    try {
      await roleAccessApiService.unassignRoleFromUser(
        keycloak,
        lookupUser.id,
        roleToUnassign,
      )
      showNotification(
        `Role "${roleToUnassign}" unassigned from ${lookupUser?.username || lookupUser.id} successfully!`,
        'success',
      )
      setRoleToUnassign('')
      handleRetrieveUserRoles(lookupUser)
    } catch (err) {
      console.error('Error unassigning role:', err)
      showNotification(
        err.message || `Failed to unassign role "${roleToUnassign}"`,
        'error',
      )
      setRoleToUnassign('')
      setRetrievingRoles(false)
    }
  }

  // Filtered Roles list based on search query
  const filteredRolesList = filterRoles(rolesList, roleSearchQuery)

  // Format roles for Autocomplete selection
  const rolesFormattedForSelect = formatRolesForSelect(rolesList)

  return (
    <Box
      sx={{
        width: '100%',
        pl: 0,
        ml: 0,
        pr: 3,
        pb: 2,
        fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
        '& *': {
          fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif !important",
        },
      }}
    >
      {/* 1. CREATE NEW ROLE INLINE CELL PANEL */}
      <CreateRolePanel
        roleName={roleName}
        setRoleName={setRoleName}
        roleDescription={roleDescription}
        setRoleDescription={setRoleDescription}
        creatingRole={creatingRole}
        handleCreateRole={handleCreateRole}
      />

      {/* 2. SYSTEM ROLES CATALOG CELL DATA GRID */}
      <SystemRolesCatalogPanel
        filteredRolesList={filteredRolesList}
        roleSearchQuery={roleSearchQuery}
        setRoleSearchQuery={setRoleSearchQuery}
        fetchRoles={fetchRoles}
        rolesLoading={rolesLoading}
        setSelectedRoles={setSelectedRoles}
        showNotification={showNotification}
        setRoleToDelete={setRoleToDelete}
        setDeleteDialogOpen={setDeleteDialogOpen}
      />

      {/* 3. ASSIGN ROLES TO USERS CELL PANEL */}
      <AssignRolesPanel
        userSearchOptions={userSearchOptions}
        selectedUsers={selectedUsers}
        setSelectedUsers={setSelectedUsers}
        handleUserSearchForAssign={handleUserSearchForAssign}
        userSearchLoading={userSearchLoading}
        rolesFormattedForSelect={rolesFormattedForSelect}
        selectedRoles={selectedRoles}
        setSelectedRoles={setSelectedRoles}
        assigningRoles={assigningRoles}
        handleAssignRoles={handleAssignRoles}
      />

      {/* 4. USER ROLE INSPECTOR CELL PANEL */}
      <UserRoleInspectorPanel
        lookupUser={lookupUser}
        lookupUserOptions={lookupUserOptions}
        handleUserSearchForLookup={handleUserSearchForLookup}
        lookupUserLoading={lookupUserLoading}
        handleRetrieveUserRoles={handleRetrieveUserRoles}
        retrievedUserRoles={retrievedUserRoles}
        retrievingRoles={retrievingRoles}
        handleOpenUnassignDialog={handleOpenUnassignDialog}
      />

      {/* 5. MUI DELETE ROLE DIALOG */}
      <DeleteRoleDialog
        deleteDialogOpen={deleteDialogOpen}
        setDeleteDialogOpen={setDeleteDialogOpen}
        roleToDelete={roleToDelete}
        deletingRole={deletingRole}
        handleDeleteRole={handleDeleteRole}
      />

      {/* 6. UNASSIGN ROLE CONFIRMATION DIALOG */}
      <UnassignRoleDialog
        unassignDialogOpen={unassignDialogOpen}
        setUnassignDialogOpen={setUnassignDialogOpen}
        roleToUnassign={roleToUnassign}
        setRoleToUnassign={setRoleToUnassign}
        lookupUser={lookupUser}
        handleConfirmUnassignRole={handleConfirmUnassignRole}
      />

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
          variant='filled'
          sx={{ width: '100%', fontWeight: 700, boxShadow: 3 }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  )
}

export default RoleAccessManagement
