import React, { useState, useEffect, useCallback } from 'react'
import { Box, Tabs, Tab, Paper, Snackbar, Alert } from '@mui/material'
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline'
import SecurityIcon from '@mui/icons-material/Security'
import PersonAddIcon from '@mui/icons-material/PersonAdd'
import PersonSearchIcon from '@mui/icons-material/PersonSearch'
import PeopleIcon from '@mui/icons-material/People'
import UploadFileIcon from '@mui/icons-material/UploadFile'

import { DataService } from 'services/DataService'
import { roleAccessApiService } from 'services/roleAccessApiService'

import {
  extractRoleItems,
  extractUserRoles,
  filterRoles,
  formatRolesForSelect,
} from './utilities/roleUtils'

import CreateRolePage from './pages/CreateRolePage'
import SystemRolesCatalogPage from './pages/SystemRolesCatalogPage'
import AssignRolesPage from './pages/AssignRolesPage'
import UserRoleInspectorPage from './pages/UserRoleInspectorPage'
import UsersByRolesPage from './pages/UsersByRolesPage'
import AssignRolesExcelPage from './pages/AssignRolesExcelPage'
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

  // 5. Users by Roles Directory State
  const [usersByRolesSelected, setUsersByRolesSelected] = useState([])
  const [usersByRolesData, setUsersByRolesData] = useState([])
  const [usersByRolesLoading, setUsersByRolesLoading] = useState(false)
  const [usersByRolesPage, setUsersByRolesPage] = useState(1)
  const [usersByRolesSize, setUsersByRolesSize] = useState(20)
  const [usersByRolesTotal, setUsersByRolesTotal] = useState(0)

  // 6. Excel Bulk Role Assignment State
  const [excelAssigning, setExcelAssigning] = useState(false)
  const [excelResult, setExcelResult] = useState(null)

  // 7. Delete Role Dialog State
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [roleToDelete, setRoleToDelete] = useState('')
  const [deletingRole, setDeletingRole] = useState(false)

  // 6. Unassign Role Confirmation Dialog State
  const [unassignDialogOpen, setUnassignDialogOpen] = useState(false)
  const [roleToUnassign, setRoleToUnassign] = useState('')

  // Active Tab State (0: Roles Catalog, 1: Assign Roles, 2: User Inspector, 3: Users Directory, 4: Excel Bulk Assign)
  const [activeTab, setActiveTab] = useState(0)

  const handleTabChange = (event, newValue) => {
    setActiveTab(newValue)
  }

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
        const searchQuery = typeof query === 'string' ? query : ''
        const res = await roleAccessApiService.getRoles(keycloak, {
          q: searchQuery,
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

  // Fetch Users by Roles Handler (POST /task/users/by-roles)
  const handleFetchUsersByRoles = useCallback(
    async (rolesToFetch = usersByRolesSelected, page = usersByRolesPage, size = usersByRolesSize) => {
      const roleNames = (Array.isArray(rolesToFetch) ? rolesToFetch : [rolesToFetch])
        .map((r) => (typeof r === 'string' ? r : r?.name || r?.code || r?.value || String(r)))
        .filter(Boolean)

      if (roleNames.length === 0) {
        showNotification('Please select at least one role to search users', 'warning')
        return
      }

      setUsersByRolesLoading(true)
      try {
        const res = await roleAccessApiService.getUsersByRoles(keycloak, {
          roles: roleNames,
          page,
          size,
        })
        const fetchedList = res?.data || []
        setUsersByRolesData(fetchedList)
        setUsersByRolesTotal(res?.total || fetchedList.length)
        setUsersByRolesPage(res?.page || page)
        setUsersByRolesSize(res?.size || size)
        showNotification(
          `Found ${res?.total || fetchedList.length} user(s) matching selected role(s)`,
          'info',
        )
      } catch (err) {
        console.error('Error fetching users by roles:', err)
        showNotification(err.message || 'Failed to fetch users by roles', 'error')
        setUsersByRolesData([])
        setUsersByRolesTotal(0)
      } finally {
        setUsersByRolesLoading(false)
      }
    },
    [keycloak, usersByRolesSelected, usersByRolesPage, usersByRolesSize, showNotification],
  )

  const handleClearUsersByRoles = useCallback(() => {
    setUsersByRolesData([])
    setUsersByRolesTotal(0)
    setUsersByRolesPage(1)
  }, [])

  // Excel Role Assignment Handler (POST /task/users/roles/assign-excel)
  const handleAssignRolesFromExcel = async (file) => {
    if (!file) {
      showNotification('Please select an Excel file (.xlsx) to upload', 'warning')
      return
    }

    setExcelAssigning(true)
    setExcelResult(null)
    try {
      const res = await roleAccessApiService.assignRolesFromExcel(keycloak, file)
      setExcelResult(res)
      const status = res?.status || 200
      const msg = res?.message || 'Excel roles assigned successfully.'
      if (status === 207) {
        showNotification(`Partial Success (207): ${msg}`, 'warning')
      } else {
        showNotification(msg, 'success')
      }
      fetchRoles()
    } catch (err) {
      console.error('Error assigning roles from Excel:', err)
      showNotification(err.message || 'Failed to assign roles from Excel', 'error')
    } finally {
      setExcelAssigning(false)
    }
  }

  const handleResetExcelResult = () => {
    setExcelResult(null)
  }

  // Filtered Roles list based on search query
  const filteredRolesList = filterRoles(rolesList, roleSearchQuery)

  // Format roles for Autocomplete selection
  const rolesFormattedForSelect = formatRolesForSelect(rolesList)

  return (
    <Box
      sx={{
        width: '100%',
        height: '100%',
        minHeight: 'calc(100vh - 120px)',
        display: 'flex',
        flexDirection: 'column',
        pl: 0,
        ml: 0,
        pr: 0,
        pb: 0,
        fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
        '& *': {
          fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif !important",
        },
      }}
    >
      {/* NAVIGATION TABS HEADER BAR */}
      <Paper
        elevation={0}
        sx={{
          mb: 2,
          borderRadius: '10px',
          border: '1px solid #e2e8f0',
          backgroundColor: '#ffffff',
          overflow: 'hidden',
          width: '100%',
          boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
        }}
      >
        <Tabs
          value={activeTab}
          onChange={handleTabChange}
          variant="scrollable"
          scrollButtons="auto"
          sx={{
            minHeight: '48px',
            px: 1,
            backgroundColor: '#f8fafc',
            borderBottom: '1px solid #e2e8f0',
            '& .MuiTab-root': {
              textTransform: 'none',
              fontWeight: 600,
              fontSize: '0.875rem',
              minHeight: '48px',
              color: '#64748b',
              py: 1,
              px: 2,
              '&.Mui-selected': {
                color: '#0284c7',
                fontWeight: 700,
              },
            },
            '& .MuiTabs-indicator': {
              backgroundColor: '#0284c7',
              height: '3px',
              borderRadius: '3px 3px 0 0',
            },
          }}
        >
          <Tab icon={<AddCircleOutlineIcon sx={{ fontSize: 18 }} />} iconPosition="start" label="Create Role" />
          <Tab icon={<SecurityIcon sx={{ fontSize: 18 }} />} iconPosition="start" label="System Roles Catalog" />
          <Tab icon={<PersonAddIcon sx={{ fontSize: 18 }} />} iconPosition="start" label="Assign Roles" />
          <Tab icon={<PersonSearchIcon sx={{ fontSize: 18 }} />} iconPosition="start" label="User Inspector" />
          <Tab icon={<PeopleIcon sx={{ fontSize: 18 }} />} iconPosition="start" label="Users Directory" />
          <Tab icon={<UploadFileIcon sx={{ fontSize: 18 }} />} iconPosition="start" label="Excel Bulk Assign" />
        </Tabs>
      </Paper>

      {/* RENDER ACTIVE TAB PAGE SECTION */}
      {activeTab === 0 && (
        <CreateRolePage
          roleName={roleName}
          setRoleName={setRoleName}
          roleDescription={roleDescription}
          setRoleDescription={setRoleDescription}
          creatingRole={creatingRole}
          handleCreateRole={handleCreateRole}
        />
      )}

      {activeTab === 1 && (
        <SystemRolesCatalogPage
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
      )}

      {activeTab === 2 && (
        <AssignRolesPage
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
      )}

      {activeTab === 3 && (
        <UserRoleInspectorPage
          lookupUser={lookupUser}
          lookupUserOptions={lookupUserOptions}
          handleUserSearchForLookup={handleUserSearchForLookup}
          lookupUserLoading={lookupUserLoading}
          handleRetrieveUserRoles={handleRetrieveUserRoles}
          retrievedUserRoles={retrievedUserRoles}
          retrievingRoles={retrievingRoles}
          handleOpenUnassignDialog={handleOpenUnassignDialog}
          rolesFormattedForSelect={rolesFormattedForSelect}
          showNotification={showNotification}
          keycloak={keycloak}
        />
      )}

      {activeTab === 4 && (
        <UsersByRolesPage
          rolesFormattedForSelect={rolesFormattedForSelect}
          selectedRoles={usersByRolesSelected}
          setSelectedRoles={setUsersByRolesSelected}
          onFetchUsers={(roles, page, size) => handleFetchUsersByRoles(roles, page, size)}
          onClearUsers={handleClearUsersByRoles}
          usersData={usersByRolesData}
          loading={usersByRolesLoading}
          totalUsers={usersByRolesTotal}
          page={usersByRolesPage}
          pageSize={usersByRolesSize}
          onPageChange={(newPage) => handleFetchUsersByRoles(usersByRolesSelected, newPage, usersByRolesSize)}
          onPageSizeChange={(newSize) => handleFetchUsersByRoles(usersByRolesSelected, 1, newSize)}
        />
      )}

      {activeTab === 5 && (
        <AssignRolesExcelPage
          onUploadExcel={handleAssignRolesFromExcel}
          loading={excelAssigning}
          result={excelResult}
          onResetResult={handleResetExcelResult}
        />
      )}

      {/* MUI DELETE ROLE DIALOG */}
      <DeleteRoleDialog
        deleteDialogOpen={deleteDialogOpen}
        setDeleteDialogOpen={setDeleteDialogOpen}
        roleToDelete={roleToDelete}
        deletingRole={deletingRole}
        handleDeleteRole={handleDeleteRole}
      />

      {/* UNASSIGN ROLE CONFIRMATION DIALOG */}
      <UnassignRoleDialog
        unassignDialogOpen={unassignDialogOpen}
        setUnassignDialogOpen={setUnassignDialogOpen}
        roleToUnassign={roleToUnassign}
        setRoleToUnassign={setRoleToUnassign}
        lookupUser={lookupUser}
        handleConfirmUnassignRole={handleConfirmUnassignRole}
      />

      {/* MUI SNACKBAR NOTIFICATIONS */}
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
