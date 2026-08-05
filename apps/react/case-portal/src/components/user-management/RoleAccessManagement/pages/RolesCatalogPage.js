import React from 'react'
import { Box } from '@mui/material'
import CreateRolePanel from '../utilities/CreateRolePanel'
import SystemRolesCatalogPanel from '../utilities/SystemRolesCatalogPanel'

const RolesCatalogPage = ({
  roleName,
  setRoleName,
  roleDescription,
  setRoleDescription,
  creatingRole,
  handleCreateRole,
  filteredRolesList,
  roleSearchQuery,
  setRoleSearchQuery,
  fetchRoles,
  rolesLoading,
  setSelectedRoles,
  showNotification,
  setRoleToDelete,
  setDeleteDialogOpen,
}) => {
  return (
    <Box>
      <CreateRolePanel
        roleName={roleName}
        setRoleName={setRoleName}
        roleDescription={roleDescription}
        setRoleDescription={setRoleDescription}
        creatingRole={creatingRole}
        handleCreateRole={handleCreateRole}
      />
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
    </Box>
  )
}

export default RolesCatalogPage
